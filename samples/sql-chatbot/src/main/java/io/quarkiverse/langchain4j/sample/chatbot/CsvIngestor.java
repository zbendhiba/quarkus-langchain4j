package io.quarkiverse.langchain4j.sample.chatbot;

import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.BindyType;

@ApplicationScoped
public class CsvIngestor extends RouteBuilder {

    public void configure() throws Exception {

        // ingesting a CSV files listing the top 100 movies from IMDB
        from("file:src/main/resources/data?fileName=movies.csv&noop=true")
                .log("**** ingesting a CSV files listing the top 100 movies from IMDB")
                .unmarshal().bindy(BindyType.Csv, Movie.class)
                .to("jpa:" + Movie.class.getName()+"?entityType=java.util.List ");
    }
}
