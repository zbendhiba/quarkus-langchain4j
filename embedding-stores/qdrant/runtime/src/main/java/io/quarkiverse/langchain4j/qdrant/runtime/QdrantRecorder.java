package io.quarkiverse.langchain4j.qdrant.runtime;

import java.util.function.Function;

import io.quarkiverse.qdrant.runtime.QdrantClient;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class QdrantRecorder {

    public Function<SyntheticCreationalContext<QdrantEmbeddingStore>, QdrantEmbeddingStore> qdrantStoreFunction(
            String collectionName, String payloadTextKey) {
        return context -> new QdrantEmbeddingStore(
                context.getInjectedReference(QdrantClient.class),
                collectionName,
                payloadTextKey);
    }
}
