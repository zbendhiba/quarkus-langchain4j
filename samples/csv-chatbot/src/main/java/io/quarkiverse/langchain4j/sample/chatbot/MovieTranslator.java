package io.quarkiverse.langchain4j.sample.chatbot;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;


@ApplicationScoped
public class MovieTranslator {
    public Movie transform(JsonNode movieNode) throws Exception {
        Movie movie = new Movie();
        movie.setMovieName(movieNode.get("original_title").asText());
        String releaseDate = movieNode.get("release_date").asText();
        int yearOfRelease = Integer.parseInt(releaseDate.substring(0, 4));
        movie.setYearOfRelease(yearOfRelease);
        movie.setImdbRating(movieNode.get("vote_average").floatValue());
        movie.setVotes(movieNode.get("vote_count").asInt());
        movie.setGrossTotal(movieNode.get("vote_average").floatValue());
        return movie;
    }
}
