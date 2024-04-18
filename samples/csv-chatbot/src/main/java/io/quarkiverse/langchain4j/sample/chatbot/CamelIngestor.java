package io.quarkiverse.langchain4j.sample.chatbot;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.quarkiverse.langchain4j.sample.chatbot.model.external.MovieGenres;
import io.quarkiverse.langchain4j.sample.chatbot.model.external.MoviesNow;
import io.quarkiverse.langchain4j.sample.chatbot.model.external.TheMovie;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.BindyType;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class CamelIngestor extends RouteBuilder {
    @ConfigProperty(name = "themoviedb.api.key")
    String apiToken;

    @Inject
    ProducerTemplate producerTemplate;

    Map<Integer, String> movieGenres;

    public void ingest(@Observes StartupEvent event) {
        movieGenres =  producerTemplate.requestBody("direct:getGenres", null, Map.class);
    }

    public void configure() throws Exception {

        // ingesting a CSV files listing the top 100 movies from IMDB
        from("file:src/main/resources/data?fileName=movies.csv&noop=true")
                .log("$$$$$$$$$$hello")
                .split(body().tokenize("\n")).streaming()
                .choice()
                    .when(header("CamelSplitIndex").isGreaterThan(0))
                        .unmarshal().bindy(BindyType.Csv, Movie.class)
                        .to("direct:insertMovie")
                     .end();

                //.unmarshal().bindy(BindyType.Csv, Movie.class)
                //.unmarshal().csv()
               // .split().body()
              /*  .process(e -> {
                    Movie movie = new Movie();
                    movie.setIndex(e.getIn().getHeader("CamelCsvRecordIndex", Integer.class));
                    movie.setMovieName(e.getIn().getHeader("movie_name", String.class));
                    movie.setYearOfRelease(e.getIn().getHeader("year_of_release", Integer.class));
                    movie.setCategory(e.getIn().getHeader("category", String.class));
                    movie.setRunTime(e.getIn().getHeader("run_time", Integer.class));
                    movie.setGenre(e.getIn().getHeader("genre", String.class));
                    movie.setImdbRating(e.getIn().getHeader("imdb_rating", Float.class));
                    String votes = e.getIn().getHeader("votes", String.class);
                    movie.setVotes(Integer.parseInt(votes.substring(1, votes.length() - 1).replace(",", "")));
                    movie.setGrossTotal(e.getIn().getHeader("gross_total", Float.class));
                    e.getIn().setBody(movie);
                    System.out.println("title :"+movie.getMovieName());
                })*/
               ;

        from("direct:insertMovie")
                .to("jpa:" + Movie.class.getName());

        // importing playing now movies from an external API
        from("direct:import-movies-playing-now")
                .setHeader("Authorization", constant("Bearer " + apiToken))
                .to("https://api.themoviedb.org/3/movie/now_playing?language=en-US&page=1")
                .unmarshal().json(MoviesNow.class)
                .log("before split ${body}")
                .split(simple("${body.results}"))
                .to("direct:process-each-movie");

        from("direct:process-each-movie")
                .log("body ${body}")
                // transform body
                .process(e->{
                    TheMovie theMovie = e.getIn().getBody(TheMovie.class);
                    String genres = theMovie.getGenreIds().stream()
                            .map(movieGenres::get)
                            .collect(Collectors.joining(", "));

                    Movie movie = new Movie();
                    movie.setIndex(theMovie.getId());
                    movie.setMovieName(theMovie.getTitle());
                    movie.setYearOfRelease(theMovie.getReleaseDate().getYear());

                    movie.setGenre(genres);
                    movie.setImdbRating(theMovie.getVoteAverage());

                    movie.setVotes(theMovie.getVoteCount());
                    movie.setGrossTotal(theMovie.getVoteAverage());
                   /* movie.setYearOfRelease(theMovie.getReleaseDate().));
                    movie.setCategory((String) exchange.getIn().getHeader("category"));
                    movie.setRunTime(Integer.parseInt((String) exchange.getIn().getHeader("run_time")));
                    movie.setGenre((String) exchange.getIn().getHeader("genre"));
                    movie.setImdbRating(Float.parseFloat((String) exchange.getIn().getHeader("imdb_rating")));
                    String votes = (String) exchange.getIn().getHeader("votes");
                    movie.setVotes(Integer.parseInt(votes.substring(1, votes.length() - 1).replace(",", "")));
                    movie.setGrossTotal(Float.parseFloat((String) exchange.getIn().getHeader("gross_total")));
                    e.getIn().setBody(myMovie);*/
                    e.getIn().setBody(movie);
                })
                .log("transformed :: ${body}")

                .to("jpa:" + Movie.class.getName());


        from("direct:getGenres")
                .setHeader("Authorization", constant("Bearer " + apiToken))
                .to("https://api.themoviedb.org/3/genre/movie/list?language=en")
                .unmarshal().json(MovieGenres.class)
                .log("result is ${body}")
                .bean(GenreTransformer.class);

    }
}
