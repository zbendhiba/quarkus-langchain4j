# M2 — Named client support plan

## Problem

Other embedding stores in this project allow each named store to target a different server instance:

- **Redis** — `RedisNamedStoreBuildTimeConfig.clientName()` points to a named Redis client
  (`quarkus.redis.<name>.*`), enabling different Redis instances per named store
- **pgvector** — `PgVectorNamedStoreBuildTimeConfig.datasource()` points to a named Agroal
  datasource (`quarkus.datasource.<name>.*`), enabling different PostgreSQL instances per named store

Qdrant currently has no equivalent. All named stores share the single connection configured
via `quarkus.qdrant.*`. The old gRPC-based implementation supported per-store `host`, `port`,
and `api-key`, so this is a regression.

## Prerequisite: quarkus-qdrant issue

Create the following issue in **quarkus-qdrant**:

---

### Issue title

`Add named client support (multiple Qdrant connections)`

### Issue body

```
## Problem

The quarkus-langchain4j-qdrant extension supports multiple named embedding stores, each
targeting a different Qdrant collection. However, all named stores must share the same
Qdrant connection, because `quarkus-qdrant` only supports a single connection today.

This is inconsistent with how peer Quarkus extensions work:
- `quarkus-redis-client` supports named clients via `quarkus.redis.<name>.*`
- `quarkus-datasource` (Agroal) supports named datasources via `quarkus.datasource.<name>.*`

Applications that need to store embeddings in different Qdrant instances (e.g. per tenant,
per environment, or per domain) cannot do so with the current single-connection model.

## Proposed solution

Mirror the pattern used by `quarkus-redis-client`:

1. Allow multiple named Qdrant client configurations:

       quarkus.qdrant.host=primary.qdrant.example.com        # default client
       quarkus.qdrant.port=6333

       quarkus.qdrant."secondary".host=secondary.qdrant.example.com   # named client
       quarkus.qdrant."secondary".port=6333

2. Inject a named `QdrantClient` via a qualifier:

       @Inject
       @QdrantClientName("secondary")
       QdrantClient secondaryClient;

3. Expose a `Map<String, QdrantClient>` or a `QdrantClientName` qualifier so that
   consumers (like quarkus-langchain4j-qdrant) can inject a specific client by name.

## Context

Requested by quarkus-langchain4j-qdrant named store support:
https://github.com/quarkiverse/quarkus-langchain4j/issues/451
```

---

## What to change here once named client support lands in quarkus-qdrant

### 1. Bump `quarkus-qdrant.version` in root `pom.xml`

```xml
<quarkus-qdrant.version>0.2.0</quarkus-qdrant.version>  <!-- or whatever version ships named clients -->
```

### 2. Add `clientName` to `QdrantNamedStoreBuildTimeConfig`

Mirror the Redis and pgvector pattern exactly:

```java
@ConfigGroup
public interface QdrantNamedStoreBuildTimeConfig {

    /**
     * The name of the Qdrant client to use for this store.
     * Qdrant clients are configured via the {@code quarkus-qdrant} extension
     * ({@code quarkus.qdrant."<name>".*}).
     * If not set, the default Qdrant client is used.
     */
    @WithDefault("<default>")
    Optional<String> clientName();

    /** The name of the Qdrant collection to use. */
    @WithDefault("default")
    String collectionName();

    /** The field name of the text segment in the payload. */
    @WithDefault("text_segment")
    String payloadTextKey();
}
```

### 3. Update `QdrantProcessor` to inject the right client per named store

```java
for (Map.Entry<String, QdrantNamedStoreBuildTimeConfig> entry : namedStores.entrySet()) {
    String storeName = entry.getKey();
    String clientName = entry.getValue().clientName()
            .filter(n -> !NamedConfigUtil.isDefault(n))
            .orElse(null);

    SyntheticBeanBuildItem.ExtendedBeanConfigurator cfg = SyntheticBeanBuildItem
            .configure(QDRANT_EMBEDDING_STORE)
            ...
            .addInjectionPoint(
                ClassType.create(DotName.createSimple(QdrantClient.class)),
                clientName != null
                    ? AnnotationInstance.builder(QdrantClientName.class).add("value", clientName).build()
                    : null)
            .createWith(recorder.qdrantStoreFunction(
                    entry.getValue().collectionName(),
                    entry.getValue().payloadTextKey()));
    beanProducer.produce(cfg.done());
}
```

*(Adjust for the exact qualifier annotation name exposed by quarkus-qdrant.)*

### 4. Update the docs

Replace the current NOTE in `rag-qdrant-store.adoc`:

```
NOTE: Named stores previously supported per-store connection settings (host, port, api-key per store name).
All named stores now share the single Qdrant connection configured via `quarkus.qdrant.*`.
```

With the full named-client documentation, mirroring `rag-redis.adoc`:

```adoc
== Named Stores

You can configure multiple named Qdrant stores, each using a different collection
and optionally a different Qdrant client (i.e. a different Qdrant instance).

[source,properties]
----
# Named store pointing to the default Qdrant client, different collection
quarkus.langchain4j.qdrant.products.collection-name=product_embeddings

# Named store pointing to a different Qdrant instance
quarkus.langchain4j.qdrant.archive.client-name=archive
quarkus.langchain4j.qdrant.archive.collection-name=archive_embeddings
quarkus.qdrant."archive".host=archive.qdrant.example.com
quarkus.qdrant."archive".port=6333
----
```

### 5. Add a test

Add a test similar to `QdrantMultipleNamedStoresTest` but with two different clients
configured, verifying that each named store talks to its own Qdrant instance.

## Summary of gains after this change

| Concern | Before (this PR) | After |
|---|---|---|
| All named stores share one connection | Yes (regression) | No — each can target a different client |
| Pattern consistency with Redis/pgvector | No | Yes |
| Multi-instance Qdrant applications | Not possible | Supported |
