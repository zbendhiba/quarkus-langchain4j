package io.quarkiverse.langchain4j.sample.chatbot.model.external;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MoviesNow {
    @JsonProperty("dates")
    private Dates dates;

    @JsonProperty("page")
    private int page;

    @JsonProperty("results")
    private List<TheMovie> results;

    @JsonProperty("total_pages")
    private int totalPages;

    @JsonProperty("total_results")
    private int totalResults;

    // Getters and setters
    public Dates getDates() {
        return dates;
    }

    public void setDates(Dates dates) {
        this.dates = dates;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public List<TheMovie> getResults() {
        return results;
    }

    public void setResults(List<TheMovie> results) {
        this.results = results;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public int getTotalResults() {
        return totalResults;
    }

    public void setTotalResults(int totalResults) {
        this.totalResults = totalResults;
    }
}

