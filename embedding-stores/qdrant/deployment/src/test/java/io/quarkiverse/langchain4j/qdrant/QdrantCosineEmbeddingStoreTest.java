package io.quarkiverse.langchain4j.qdrant;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.RegisterExtension;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2q.AllMiniLmL6V2QuantizedEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreWithoutMetadataIT;
import io.quarkiverse.langchain4j.qdrant.runtime.QdrantEmbeddingStore;
import io.quarkus.test.QuarkusUnitTest;

// Tests the embedding store with Cosine distance, the recommended metric for text embeddings.
public class QdrantCosineEmbeddingStoreTest extends EmbeddingStoreWithoutMetadataIT {

    public static final String COLLECTION_NAME = "qdrant_test_embeddings";

    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .setArchiveProducer(QdrantCosineEmbeddingStoreTest::archive);

    @Inject
    QdrantEmbeddingStore embeddingStore;

    private static EmbeddingModel embeddingModel;

    /**
     * FIXME: This is a workaround to avoid loading the embedding model in this test class' static initializer,
     * because otherwise we hit
     * java.lang.UnsatisfiedLinkError: Native Library (/path/to/the/library) already loaded in another classloader
     * because the test class is loaded by JUnit and by Quarkus in different class loaders.
     */
    @BeforeAll
    public static void initEmbeddingModel() {
        embeddingModel = new AllMiniLmL6V2QuantizedEmbeddingModel();
    }

    @Override
    protected void clearStore() {
        embeddingStore.clearStore();
    }

    @Override
    protected EmbeddingStore<TextSegment> embeddingStore() {
        return embeddingStore;
    }

    @Override
    protected EmbeddingModel embeddingModel() {
        return embeddingModel;
    }

    private static JavaArchive archive() {
        Asset properties = new StringAsset(String.join("\n",
                "quarkus.langchain4j.qdrant.collection-name=" + COLLECTION_NAME,
                "quarkus.qdrant.devservices.collections." + COLLECTION_NAME + ".vector-size=384",
                "quarkus.qdrant.devservices.collections." + COLLECTION_NAME + ".distance=Cosine"));

        return ShrinkWrap.create(JavaArchive.class).addAsResource(properties, "application.properties");
    }

}
