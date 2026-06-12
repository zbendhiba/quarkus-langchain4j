# B2 — Metric-aware score conversion plan

## Problem

`QdrantEmbeddingStore.search()` currently ignores `ScoredPoint.getScore()` (Qdrant's
metric-correct score). Instead it fetches full vectors back (`.withVector(true)`) and
recomputes cosine similarity locally. This means:

- Every search pays the bandwidth cost of transferring all result vectors
- Euclidean-distance collections return cosine scores, silently wrong
- `minScore` filtering happens client-side instead of server-side via `scoreThreshold`

`SearchBuilder` already has `.scoreThreshold(float)` and `ScoredPoint` already has
`getScore()`, but we cannot use them correctly without knowing the collection's distance
metric at search time.

## Prerequisite: quarkus-qdrant issue

Create the following issue in **quarkus-qdrant**:

---

### Issue title

`Add getCollectionInfo API to expose collection distance metric`

### Issue body

```
## Problem

Integrations that wrap Qdrant (e.g. quarkus-langchain4j-qdrant) need the distance
metric configured on a collection to:

1. Convert `ScoredPoint.getScore()` to a normalised [0, 1] relevance score correctly
   (conversion formula differs per metric)
2. Convert LangChain4j's `minScore` ([0, 1]) to Qdrant's score space for server-side
   filtering via `SearchBuilder.scoreThreshold()`
3. Stop fetching vectors on every search just to recompute cosine locally

Currently `QdrantClient` exposes search / upsert / delete but no way to query
collection metadata.

## Proposed API

Add a method to `QdrantClient`:

    CollectionInfo getCollectionInfo(String collectionName);

Where `CollectionInfo` contains at minimum:

    String getDistance();  // "Cosine" | "Dot" | "Euclid" | "Manhattan"

## Underlying REST endpoint

    GET /collections/{collection_name}

Response (relevant excerpt):

    {
      "result": {
        "config": {
          "params": {
            "vectors": {
              "size": 384,
              "distance": "Cosine"
            }
          }
        }
      }
    }

## Context

Requested by quarkus-langchain4j-qdrant:
https://github.com/quarkiverse/quarkus-langchain4j/issues/451
```

---

## What to change here once the new quarkus-qdrant is released

### 1. Bump `quarkus-qdrant.version` in root `pom.xml`

```xml
<quarkus-qdrant.version>0.2.0</quarkus-qdrant.version>  <!-- or whatever version ships getCollectionInfo -->
```

### 2. Add distance caching to `QdrantEmbeddingStore`

Cache the distance metric lazily on first search (avoids an extra REST call at
construction time for collections that may not exist yet).

```java
private volatile String distance;   // "Cosine" | "Dot" | "Euclid" | "Manhattan"

private String distance() {
    if (distance == null) {
        synchronized (this) {
            if (distance == null) {
                distance = qdrantClient.getCollectionInfo(collectionName).getDistance();
            }
        }
    }
    return distance;
}
```

### 3. Rewrite `search()` — remove vector fetch, use Qdrant score

Replace the current implementation with:

```java
@Override
public EmbeddingSearchResult<TextSegment> search(EmbeddingSearchRequest request) {
    String dist = distance();
    List<ScoredPoint> scoredPoints = qdrantClient.search(collectionName)
            .vector(request.queryEmbedding().vector())
            .limit(request.maxResults())
            .scoreThreshold(toQdrantScore(dist, request.minScore()))  // server-side filter
            .withPayload(true)
            // .withVector(true)  ← removed; no longer needed
            .execute();

    List<EmbeddingMatch<TextSegment>> matches = new ArrayList<>();
    if (scoredPoints != null) {
        for (ScoredPoint point : scoredPoints) {
            double score = toRelevanceScore(dist, point.getScore());
            TextSegment textSegment = null;
            if (point.getPayload() != null) {
                Object text = point.getPayload().get(payloadTextKey);
                if (text instanceof String s && !s.isEmpty()) {
                    textSegment = TextSegment.from(s);
                }
            }
            matches.add(new EmbeddingMatch<>(score, point.getId(), null, textSegment));
        }
    }
    return new EmbeddingSearchResult<>(matches);
}
```

Note: `EmbeddingMatch` embedding field becomes `null` — we no longer have the vector.
Check whether `EmbeddingStoreWithoutMetadataIT` or any consumer relies on it being
non-null before dropping it.

### 4. Add score conversion helpers

```java
private static double toRelevanceScore(String distance, float qdrantScore) {
    return switch (distance) {
        case "Cosine" -> RelevanceScore.fromCosineSimilarity(qdrantScore); // [-1,1] -> [0,1]
        case "Dot"    -> RelevanceScore.fromCosineSimilarity(qdrantScore); // normalised vectors
        default       -> 1.0 / (1.0 + qdrantScore);                        // Euclid / Manhattan
    };
}

private static float toQdrantScore(String distance, double relevanceScore) {
    return switch (distance) {
        case "Cosine", "Dot" -> (float) (2 * relevanceScore - 1);          // [0,1] -> [-1,1]
        default              -> (float) (1.0 / relevanceScore - 1);         // inverse
    };
}
```

### 5. Clean up imports

Remove `CosineSimilarity`, `Embedding.from(...)` reconstruction, and `withVector` usage.

### 6. Update tests

- Verify cosine and dot-product tests still pass
- Add a test for Euclidean distance (currently untestable because scores are wrong)
- Verify that `EmbeddingMatch.embedding()` returning `null` does not break tests

## Summary of gains after this change

| Concern | Before | After |
|---|---|---|
| Vectors fetched on search | Yes (always) | No |
| Score correctness (Cosine) | Correct (coincidence) | Correct |
| Score correctness (DotProduct) | Correct for normalised vectors | Correct |
| Score correctness (Euclidean) | Wrong (cosine used) | Correct |
| minScore filtering | Client-side | Server-side via scoreThreshold |
