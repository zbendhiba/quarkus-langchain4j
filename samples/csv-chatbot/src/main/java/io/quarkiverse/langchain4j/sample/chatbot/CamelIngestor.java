package io.quarkiverse.langchain4j.sample.chatbot;

import org.apache.camel.builder.RouteBuilder;

public class CamelIngestor extends RouteBuilder {
    public void configure() throws Exception {

        from("file:src/main/resources?fileName=movies.csv")
                .split().tokenize("\n")
                .unmarshal().csv()
                .process(exchange -> {
                    Movie movie = new Movie();
                    movie.setIndex(Integer.parseInt((String) exchange.getIn().getHeader("CamelCsvRecordIndex")));
                    movie.setMovieName((String) exchange.getIn().getHeader("movie_name"));
                    movie.setYearOfRelease(Integer.parseInt((String) exchange.getIn().getHeader("year_of_release")));
                    movie.setCategory((String) exchange.getIn().getHeader("category"));
                    movie.setRunTime(Integer.parseInt((String) exchange.getIn().getHeader("run_time")));
                    movie.setGenre((String) exchange.getIn().getHeader("genre"));
                    movie.setImdbRating(Float.parseFloat((String) exchange.getIn().getHeader("imdb_rating")));
                    String votes = (String) exchange.getIn().getHeader("votes");
                    movie.setVotes(Integer.parseInt(votes.substring(1, votes.length() - 1).replace(",", "")));
                    movie.setGrossTotal(Float.parseFloat((String) exchange.getIn().getHeader("gross_total")));
                    exchange.getIn().setBody(movie);
                })
                .to("direct:insertMovie");

        from("direct:insertMovie")
                .to("jpa:" + Movie.class.getName());
    }
}
