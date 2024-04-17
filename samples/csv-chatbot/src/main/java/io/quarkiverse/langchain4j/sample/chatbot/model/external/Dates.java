package io.quarkiverse.langchain4j.sample.chatbot.model.external;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Dates {
    @JsonProperty("maximum")
    private String maximum;

    @JsonProperty("minimum")
    private String minimum;

    // Getters and setters
    public String getMaximum() {
        return maximum;
    }

    public void setMaximum(String maximum) {
        this.maximum = maximum;
    }

    public String getMinimum() {
        return minimum;
    }

    public void setMinimum(String minimum) {
        this.minimum = minimum;
    }
}
