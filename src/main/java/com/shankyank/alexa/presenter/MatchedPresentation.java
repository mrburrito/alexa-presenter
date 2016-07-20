package com.shankyank.alexa.presenter;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A matched presentation with speech input and confidence.
 */
public class MatchedPresentation implements Comparable<MatchedPresentation> {
    private final String spokenName;
    private final double confidence;
    private final Presentation presentation;

    @JsonCreator
    public MatchedPresentation(@JsonProperty("spokenName") String spokenName,
                               @JsonProperty("confidence") double confidence,
                               @JsonProperty("presentation") Presentation presentation) {
        this.spokenName = spokenName;
        this.confidence = confidence;
        this.presentation = presentation;
    }

    public String getSpokenName() {
        return spokenName;
    }

    public double getConfidence() {
        return confidence;
    }

    public Presentation getPresentation() {
        return presentation;
    }

    @Override
    public String toString() {
        return "MatchedPresentation{" +
                "spokenName='" + spokenName + '\'' +
                ", confidence=" + confidence +
                ", presentation=" + presentation +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MatchedPresentation that = (MatchedPresentation) o;

        if (Double.compare(that.confidence, confidence) != 0) return false;
        if (!spokenName.equals(that.spokenName)) return false;
        return presentation.equals(that.presentation);

    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = spokenName.hashCode();
        temp = Double.doubleToLongBits(confidence);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + presentation.hashCode();
        return result;
    }

    @Override
    public int compareTo(MatchedPresentation other) {
        return Double.compare(confidence, other.confidence);
    }
}
