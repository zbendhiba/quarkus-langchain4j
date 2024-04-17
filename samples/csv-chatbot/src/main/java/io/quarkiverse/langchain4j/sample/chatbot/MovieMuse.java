package io.quarkiverse.langchain4j.sample.chatbot;

import jakarta.inject.Singleton;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;

@RegisterAiService(retrievalAugmentor = MovieMuseRetrievalAugmentor.class)
@Singleton // this is singleton because WebSockets currently never closes the scope
public interface MovieMuse {

    @UserMessage("""
            You are MovieMuse, an AI answering questions about the top 100 movies from IMDB.
            Your response must be polite, use the same language as the question, and be relevant to the question.
            Don't use any knowledge that is not in the database.

            Introduce yourself with: "Hello, I'm MovieMuse, how can I help you?"
            {question}
        """)
    String chat(@MemoryId Object session,  String question);
}
