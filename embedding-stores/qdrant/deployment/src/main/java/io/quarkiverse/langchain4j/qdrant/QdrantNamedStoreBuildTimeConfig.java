package io.quarkiverse.langchain4j.qdrant;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;

@ConfigGroup
public interface QdrantNamedStoreBuildTimeConfig {

    /**
     * The name of the Qdrant collection to use.
     */
    @WithDefault("default")
    String collectionName();

    /**
     * The field name of the text segment in the payload.
     */
    @WithDefault("text_segment")
    String payloadTextKey();
}
