package io.quarkiverse.langchain4j.qdrant.runtime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.CosineSimilarity;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.RelevanceScore;
import io.quarkiverse.qdrant.runtime.QdrantClient;
import io.quarkiverse.qdrant.runtime.model.PointStruct;
import io.quarkiverse.qdrant.runtime.model.ScoredPoint;

public class QdrantEmbeddingStore implements EmbeddingStore<TextSegment> {

    private final QdrantClient qdrantClient;
    private final String collectionName;
    private final String payloadTextKey;

    public QdrantEmbeddingStore(QdrantClient qdrantClient, String collectionName, String payloadTextKey) {
        this.qdrantClient = qdrantClient;
        this.collectionName = collectionName;
        this.payloadTextKey = payloadTextKey;
    }

    @Override
    public String add(Embedding embedding) {
        String id = UUID.randomUUID().toString();
        add(id, embedding);
        return id;
    }

    @Override
    public void add(String id, Embedding embedding) {
        qdrantClient.upsert(collectionName)
                .point(new PointStruct(id, toFloatList(embedding.vector()), Map.of()))
                .execute();
    }

    @Override
    public String add(Embedding embedding, TextSegment textSegment) {
        String id = UUID.randomUUID().toString();
        Map<String, Object> payload = new HashMap<>();
        if (textSegment != null) {
            payload.put(payloadTextKey, textSegment.text());
        }
        qdrantClient.upsert(collectionName)
                .point(new PointStruct(id, toFloatList(embedding.vector()), payload))
                .execute();
        return id;
    }

    @Override
    public List<String> addAll(List<Embedding> embeddings) {
        List<String> ids = new ArrayList<>();
        List<PointStruct> points = new ArrayList<>();
        for (Embedding embedding : embeddings) {
            String id = UUID.randomUUID().toString();
            ids.add(id);
            points.add(new PointStruct(id, toFloatList(embedding.vector()), Map.of()));
        }
        qdrantClient.upsert(collectionName).points(points).execute();
        return ids;
    }

    @Override
    public List<String> addAll(List<Embedding> embeddings, List<TextSegment> textSegments) {
        List<String> ids = new ArrayList<>();
        List<PointStruct> points = new ArrayList<>();
        for (int i = 0; i < embeddings.size(); i++) {
            String id = UUID.randomUUID().toString();
            ids.add(id);
            Map<String, Object> payload = new HashMap<>();
            if (textSegments != null && i < textSegments.size() && textSegments.get(i) != null) {
                payload.put(payloadTextKey, textSegments.get(i).text());
            }
            points.add(new PointStruct(id, toFloatList(embeddings.get(i).vector()), payload));
        }
        qdrantClient.upsert(collectionName).points(points).execute();
        return ids;
    }

    @Override
    public void addAll(List<String> ids, List<Embedding> embeddings, List<TextSegment> textSegments) {
        List<PointStruct> points = new ArrayList<>();
        for (int i = 0; i < ids.size(); i++) {
            Map<String, Object> payload = new HashMap<>();
            if (textSegments != null && i < textSegments.size() && textSegments.get(i) != null) {
                payload.put(payloadTextKey, textSegments.get(i).text());
            }
            points.add(new PointStruct(ids.get(i), toFloatList(embeddings.get(i).vector()), payload));
        }
        qdrantClient.upsert(collectionName).points(points).execute();
    }

    @Override
    public EmbeddingSearchResult<TextSegment> search(EmbeddingSearchRequest request) {
        float[] queryVector = request.queryEmbedding().vector();
        List<ScoredPoint> scoredPoints = qdrantClient.search(collectionName)
                .vector(queryVector)
                .limit(request.maxResults())
                .withPayload(true)
                .withVector(true)
                .execute();

        List<EmbeddingMatch<TextSegment>> matches = new ArrayList<>();
        if (scoredPoints != null) {
            for (ScoredPoint point : scoredPoints) {
                if (point.getVector() == null) {
                    continue;
                }
                Embedding embedding = Embedding.from(point.getVector());
                // Recompute cosine similarity locally to stay metric-agnostic, matching the behaviour
                // of the previous gRPC-based implementation. Note: Euclidean distance collections
                // are not supported — scores for such collections will be cosine-based and will not
                // reflect the Euclidean distance used by Qdrant for nearest-neighbour search.
                double score = RelevanceScore.fromCosineSimilarity(
                        CosineSimilarity.between(embedding, request.queryEmbedding()));
                if (score < request.minScore()) {
                    continue;
                }
                TextSegment textSegment = null;
                if (point.getPayload() != null) {
                    Object text = point.getPayload().get(payloadTextKey);
                    if (text instanceof String s && !s.isEmpty()) {
                        textSegment = TextSegment.from(s);
                    }
                }
                matches.add(new EmbeddingMatch<>(score, point.getId(), embedding, textSegment));
            }
        }

        return new EmbeddingSearchResult<>(matches);
    }

    @Override
    public void removeAll(Collection<String> ids) {
        qdrantClient.delete(collectionName)
                .byIds(new ArrayList<>(ids))
                .execute();
    }

    @Override
    public void removeAll() {
        // Empty filter matches all points in Qdrant
        qdrantClient.delete(collectionName)
                .byFilter(Map.of())
                .execute();
    }

    public void clearStore() {
        removeAll();
    }

    private static List<Float> toFloatList(float[] vector) {
        List<Float> result = new ArrayList<>(vector.length);
        for (float v : vector) {
            result.add(v);
        }
        return result;
    }
}
