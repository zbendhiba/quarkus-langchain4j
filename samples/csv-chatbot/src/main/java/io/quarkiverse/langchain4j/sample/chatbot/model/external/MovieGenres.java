package io.quarkiverse.langchain4j.sample.chatbot.model.external;

import java.util.List;

public class MovieGenres {

    private List<Genre> genres;

    public MovieGenres() {
    }

    public MovieGenres(List<Genre> genres) {
        this.genres = genres;
    }

    public List<Genre> getGenres() {
        return genres;
    }

    public void setGenres(List<Genre> genres) {
        this.genres = genres;
    }
}
