package io.quarkiverse.langchain4j.deployment.devui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.quarkiverse.langchain4j.deployment.DeclarativeAiServiceBuildItem;
import io.quarkiverse.langchain4j.deployment.EmbeddingStoreBuildItem;
import io.quarkiverse.langchain4j.deployment.LangChain4jDotNames;
import io.quarkiverse.langchain4j.deployment.ToolProviderMetaBuildItem;
import io.quarkiverse.langchain4j.deployment.ToolsMetadataBuildItem;
import io.quarkiverse.langchain4j.deployment.items.ChatModelProviderCandidateBuildItem;
import io.quarkiverse.langchain4j.deployment.items.EmbeddingModelProviderCandidateBuildItem;
import io.quarkiverse.langchain4j.deployment.items.InMemoryEmbeddingStoreBuildItem;
import io.quarkiverse.langchain4j.deployment.items.InProcessEmbeddingBuildItem;
import io.quarkiverse.langchain4j.deployment.items.SelectedChatModelProviderBuildItem;
import io.quarkiverse.langchain4j.runtime.devui.ChatJsonRPCService;
import io.quarkiverse.langchain4j.runtime.devui.EmbeddingStoreJsonRPCService;
import io.quarkiverse.langchain4j.runtime.tool.ToolMethodCreateInfo;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.devui.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.Page;

public class LangChain4jDevUIProcessor {

    @BuildStep(onlyIf = IsDevelopment.class)
    CardPageBuildItem cardPage(List<DeclarativeAiServiceBuildItem> aiServices,
            ToolProviderMetaBuildItem toolProviderMetaBuildItem,
            ToolsMetadataBuildItem toolsMetadataBuildItem,
            List<EmbeddingModelProviderCandidateBuildItem> embeddingModelCandidateBuildItems,
            List<InProcessEmbeddingBuildItem> inProcessEmbeddingModelBuildItems,
            List<EmbeddingStoreBuildItem> embeddingStoreBuildItem,
            List<SelectedChatModelProviderBuildItem> chatModelProviders,
            Optional<InMemoryEmbeddingStoreBuildItem> inMemoryEmbeddingStoreBuildItem,
            List<AdditionalDevUiCardBuildItem> additionalDevUiCardBuildItems) {
        CardPageBuildItem card = new CardPageBuildItem();
        addAiServicesPage(card, aiServices);
        if (toolsMetadataBuildItem != null) {
            addToolsPage(card, toolsMetadataBuildItem);
        }
        // For now, add the embedding store page only if
        // - there is at least one embedding model (use the default one if there's more)
        // - there is a single embedding store (in case there's more, we need a way to select
        //   it via a qualifier or something to avoid ambiguity)
        if ((!embeddingModelCandidateBuildItems.isEmpty() || !inProcessEmbeddingModelBuildItems.isEmpty()) &&
                (embeddingStoreBuildItem.size() == 1 || inMemoryEmbeddingStoreBuildItem.isPresent())) {
            addEmbeddingStorePage(card);
        }
        if (!chatModelProviders.isEmpty()) {
            addChatPage(card, aiServices);
        }

        for (AdditionalDevUiCardBuildItem additionalDevUiCardBuildItem : additionalDevUiCardBuildItems) {
            card.addPage(Page.webComponentPageBuilder()
                    .title(additionalDevUiCardBuildItem.getTitle())
                    .icon(additionalDevUiCardBuildItem.getIcon())
                    .componentLink(additionalDevUiCardBuildItem.getComponentLink()));

            additionalDevUiCardBuildItem.getBuildTimeData().forEach((k, v) -> card.addBuildTimeData(k, v));
        }

        List<ToolProviderInfo> toolProviderInfos = toolProviderMetaBuildItem.getMetadata();
        card.addBuildTimeData("toolProviders", toolProviderInfos);

        return card;
    }

    private void addEmbeddingStorePage(CardPageBuildItem card) {
        card.addPage(Page.webComponentPageBuilder().title("Embedding store")
                .componentLink("qwc-embedding-store.js")
                .icon("font-awesome-solid:database"));
    }

    private void addAiServicesPage(CardPageBuildItem card, List<DeclarativeAiServiceBuildItem> aiServices) {
        List<AiServiceInfo> infos = new ArrayList<>();
        for (DeclarativeAiServiceBuildItem aiService : aiServices) {
            List<String> tools = aiService.getToolClassInfos().stream().map(ci -> ci.name().toString()).toList();
            infos.add(new AiServiceInfo(aiService.getServiceClassInfo().name().toString(), tools));
        }

        card.addBuildTimeData("aiservices", infos);
        card.addPage(Page.webComponentPageBuilder().title("AI Services")
                .componentLink("qwc-aiservices.js")
                .staticLabel(String.valueOf(aiServices.size()))
                .icon("font-awesome-solid:robot"));
    }

    private void addToolsPage(CardPageBuildItem card, ToolsMetadataBuildItem metadataBuildItem) {
        List<ToolMethodInfo> infos = new ArrayList<>();
        Map<String, List<ToolMethodCreateInfo>> metadata = metadataBuildItem.getMetadata();
        for (Map.Entry<String, List<ToolMethodCreateInfo>> toolClassEntry : metadata.entrySet()) {
            for (ToolMethodCreateInfo toolMethodCreateInfo : toolClassEntry.getValue()) {
                infos.add(new ToolMethodInfo(toolClassEntry.getKey(),
                        toolMethodCreateInfo.toolSpecification().name(),
                        toolMethodCreateInfo.toolSpecification().description()));
            }
        }
        card.addBuildTimeData("tools", infos);
        card.addPage(Page.webComponentPageBuilder().title("Tools")
                .componentLink("qwc-tools.js")
                .staticLabel(String.valueOf(infos.size()))
                .icon("font-awesome-solid:toolbox"));
    }

    private void addChatPage(CardPageBuildItem card, List<DeclarativeAiServiceBuildItem> aiServices) {
        List<String> systemMessages = aiServices.stream()
                .map(s -> s.getServiceClassInfo())
                .flatMap(c -> c.annotations().stream()) //This includes method annotations
                .filter(a -> a.name().equals(LangChain4jDotNames.SYSTEM_MESSAGE))
                // TODO: remove and support 'fromResource'
                .filter(a -> a.value() != null)
                .map(a -> String.join("", a.value().asStringArray()))
                .toList();

        card.addBuildTimeData("systemMessages", systemMessages);
        card.addPage(Page.webComponentPageBuilder().title("Chat")
                .componentLink("qwc-chat.js")
                .icon("font-awesome-solid:comments"));
    }

    @BuildStep
    void jsonRpcProviders(BuildProducer<JsonRPCProvidersBuildItem> producers,
            List<InProcessEmbeddingBuildItem> inProcessEmbeddingModelBuildItems,
            List<EmbeddingModelProviderCandidateBuildItem> embeddingModelCandidateBuildItems,
            List<EmbeddingStoreBuildItem> embeddingStoreBuildItem,
            List<ChatModelProviderCandidateBuildItem> chatModelCandidates,
            Optional<InMemoryEmbeddingStoreBuildItem> inMemoryEmbeddingStoreBuildItem) {
        if ((!embeddingModelCandidateBuildItems.isEmpty() || !inProcessEmbeddingModelBuildItems.isEmpty()) &&
                (embeddingStoreBuildItem.size() == 1 || inMemoryEmbeddingStoreBuildItem.isPresent())) {
            producers.produce(new JsonRPCProvidersBuildItem(EmbeddingStoreJsonRPCService.class));
        }
        if (!chatModelCandidates.isEmpty()) {
            producers.produce(new JsonRPCProvidersBuildItem(ChatJsonRPCService.class));
        }
    }

}
