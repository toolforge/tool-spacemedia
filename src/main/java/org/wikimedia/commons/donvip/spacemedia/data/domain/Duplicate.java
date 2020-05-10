package org.wikimedia.commons.donvip.spacemedia.data.domain;

import java.util.Objects;

import javax.persistence.Embeddable;

@Embeddable
public class Duplicate implements Comparable<Duplicate> {

    /**
     * Media identifier in original repository.
     */
    private String originalId;

    /**
     * Similarity score. The lower the more similar (0 = identical)
     */
    private double similarityScore;

    /**
     * Constructs a new {@code Duplicate}.
     */
    public Duplicate() {
        // Public no-arg constructor required by JPA
    }

    /**
     * Constructs a new {@code Duplicate}.
     *
     * @param originalId Media identifier in original repository
     * @param similarityScore Similarity score. The lower the more similar (0 = identical)
     */
    public Duplicate(String originalId, double similarityScore) {
        this.originalId = originalId;
        this.similarityScore = similarityScore;
    }

    /**
     * Returns the media identifier in original repository.
     *
     * @return the media identifier in original repository
     */
    public String getOriginalId() {
        return originalId;
    }

    /**
     * Sets the media identifier in original repository.
     *
     * @param originalId the media identifier in original repository
     */
    public void setOriginalId(String originalId) {
        this.originalId = originalId;
    }

    /**
     * Returns the similarity score. The lower the more similar (0 = identical)
     *
     * @return the similarity score. The lower the more similar (0 = identical)
     */
    public double getSimilarityScore() {
        return similarityScore;
    }

    /**
     * Sets the similarity score. The lower the more similar (0 = identical)
     *
     * @param similarityScore similarity score. The lower the more similar (0 = identical)
     */
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

    @Override
    public int compareTo(Duplicate o) {
        return Double.compare(similarityScore, o.similarityScore);
    }
}
