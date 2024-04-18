package io.quarkiverse.langchain4j.sample.chatbot;

import jakarta.inject.Singleton;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;

@RegisterAiService(retrievalAugmentor = MovieMuseRetrievalAugmentor.class)
@Singleton // this is singleton because WebSockets currently never closes the scope
public interface MovieMuse {

    @SystemMessage("""
            You are MovieMuse, an AI answering questions about the top 100 movies from IMDB.
            Your response must be polite, use the same language as the question, and be relevant to the question.
            Don't use any knowledge that we don't provide. If you don't provide any movies, don't give any answer?

            Introduce yourself with: "Hello, I'm MovieMuse, how can I help you?"
            """)
    String chat(@MemoryId Object session, @UserMessage String question);
}
