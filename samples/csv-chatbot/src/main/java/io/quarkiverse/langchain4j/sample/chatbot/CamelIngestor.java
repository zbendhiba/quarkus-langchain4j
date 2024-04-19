package io.quarkiverse.langchain4j.sample.chatbot;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.BindyType;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class CamelIngestor extends RouteBuilder {
    @ConfigProperty(name = "themoviedb.api.key")
    String apiToken;


    public void configure() throws Exception {

        // ingesting a CSV files listing the top 100 movies from IMDB
        from("file:src/main/resources/data?fileName=movies.csv&noop=true")
                .log("**** ingesting a CSV files listing the top 100 movies from IMDB")
                .unmarshal().bindy(BindyType.Csv, Movie.class)
                .split(body())
                .streaming()
                .to("jpa:" + Movie.class.getName());



        // importing playing now movies from an external API
        from("direct:import-movies-playing-now")
                .setHeader("Authorization", constant("Bearer " + apiToken))
                .to("https://api.themoviedb.org/3/movie/now_playing?language=en-US&page=1")
                .split().jsonpathWriteAsString("$.results[*]")
                .to("direct:process-each-movie");

        from("direct:process-each-movie")
                // transform body
                .process(e->{
                    JsonNode movieNode = e.getIn().getBody(JsonNode.class);
                    Movie movie = new Movie();
                    movie.setMovieName(movieNode.get("original_title").asText());
                    String releaseDate = movieNode.get("release_date").asText();
                    int yearOfRelease = Integer.parseInt(releaseDate.substring(0, 4));
                    movie.setYearOfRelease(yearOfRelease);
                    movie.setImdbRating(movieNode.get("vote_average").floatValue());
                    movie.setVotes(movieNode.get("vote_count").asInt());
                    movie.setGrossTotal(movieNode.get("vote_average").floatValue());
                    e.getIn().setBody(movie);
                })
                .to("jpa:" + Movie.class.getName());


    }
}
