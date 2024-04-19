package io.quarkiverse.langchain4j.sample.chatbot;

import jakarta.enterprise.context.ApplicationScoped;
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
                .bean(MovieTranslator.class)
                .to("jpa:" + Movie.class.getName());

    }
}
