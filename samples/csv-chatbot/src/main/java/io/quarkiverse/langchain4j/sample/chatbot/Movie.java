package io.quarkiverse.langchain4j.sample.chatbot;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.apache.camel.dataformat.bindy.annotation.CsvRecord;
import org.apache.camel.dataformat.bindy.annotation.DataField;

@Entity
@Table(name = "movie", schema = "public")
@CsvRecord(separator = ",", skipFirstLine = true )
public class Movie {

    @Id
    @GeneratedValue
    private int id;

    @Column(name = "index")
    @DataField(pos = 1)
    private int index;

    @Column(name = "movie_name")
    @DataField(pos = 2)
    private String movieName;

    @Column(name = "year_of_release")
    @DataField(pos = 3)
    private int yearOfRelease;

    @Column(name = "category")
    @DataField(pos = 4)
    private String category;

    @Column(name = "run_time")
    @DataField(pos = 5)
    private int runTime;

    @Column(name = "genre")
    @DataField(pos = 6)
    private String genre;

    @Column(name = "imdb_rating")
    @DataField(pos = 7)
    private float imdbRating;

    @Column(name = "votes")
    @DataField(pos = 8)
    private int votes;

    @Column(name = "gross_total")
    @DataField(pos = 9)
    private float grossTotal;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public String getMovieName() {
        return movieName;
    }

    public void setMovieName(String movieName) {
        this.movieName = movieName;
    }

    public int getYearOfRelease() {
        return yearOfRelease;
    }

    public void setYearOfRelease(int yearOfRelease) {
        this.yearOfRelease = yearOfRelease;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getRunTime() {
        return runTime;
    }

    public void setRunTime(int runTime) {
        this.runTime = runTime;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public float getImdbRating() {
        return imdbRating;
    }

    public void setImdbRating(float imdbRating) {
        this.imdbRating = imdbRating;
    }

    public int getVotes() {
        return votes;
    }

    public void setVotes(int votes) {
        this.votes = votes;
    }

    public float getGrossTotal() {
        return grossTotal;
    }

    public void setGrossTotal(float grossTotal) {
        this.grossTotal = grossTotal;
    }
}
