package org.wikimedia.commons.donvip.spacemedia.data.domain;

import java.util.Objects;

import javax.persistence.Embeddable;

@Embeddable
public class Duplicate {

    private String originalId;

    private double similarityScore;

    public Duplicate() {

    }

    public Duplicate(String originalId, double similarityScore) {
        this.originalId = originalId;
        this.similarityScore = similarityScore;
    }

    public String getOriginalId() {
        return originalId;
    }

    public void setOriginalId(String originalId) {
        this.originalId = originalId;
    }

    public double getSimilarityScore() {
        return similarityScore;
    }

    public void setSimilarityScore(double similarityScore) {
        this.similarityScore = similarityScore;
    }

    @Override
    public int hashCode() {
        return Objects.hash(originalId, similarityScore);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        Duplicate other = (Duplicate) obj;
        return Objects.equals(originalId, other.originalId)
                && Double.doubleToLongBits(similarityScore) == Double.doubleToLongBits(other.similarityScore);
    }
}
