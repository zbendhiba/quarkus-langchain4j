package io.quarkiverse.langchain4j.sample.chatbot;

import java.util.HashMap;
import java.util.Map;

import io.quarkiverse.langchain4j.sample.chatbot.model.external.Genre;
import io.quarkiverse.langchain4j.sample.chatbot.model.external.MovieGenres;


public class GenreTransformer {
    public static Map<Integer, String> transformToMap(MovieGenres movieGenres) {
        Map<Integer, String> genreMap = new HashMap<>();
        for (Genre genre : movieGenres.getGenres()) {
            genreMap.put(genre.getId(), genre.getName());
        }
        return genreMap;
    }
}
