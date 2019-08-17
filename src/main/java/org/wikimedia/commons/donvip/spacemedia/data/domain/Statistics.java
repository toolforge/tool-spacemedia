package org.wikimedia.commons.donvip.spacemedia.data.domain;

import java.util.List;
import java.util.Objects;

public class Statistics implements Comparable<Statistics> {
    private final String agency;
    private final long totalMedia;
    private final long missingMedia;
    private final Long problemsCount;
    private List<Statistics> details;

    public Statistics(String agency, long totalMedia, long missingMedia, Long problemsCount) {
        this(agency, totalMedia, missingMedia, problemsCount, null);
    }

    public Statistics(String agency, long totalMedia, long missingMedia, Long problemsCount, List<Statistics> details) {
        this.agency = Objects.requireNonNull(agency);
        this.totalMedia = totalMedia;
        this.missingMedia = missingMedia;
        this.problemsCount = problemsCount;
        this.details = details;
    }

    /**
     * @return the agency
     */
    public String getAgency() {
        return agency;
    }

    /**
     * @return the totalMedia
     */
    public long getTotalMedia() {
        return totalMedia;
    }

    /**
     * @return the missingMedia
     */
    public long getMissingMedia() {
        return missingMedia;
    }

    /**
     * @return the problemsCount
     */
    public Long getProblemsCount() {
        return problemsCount;
    }

    /**
     * @return the details
     */
    public List<Statistics> getDetails() {
        return details;
    }

    public void setDetails(List<Statistics> details) {
        this.details = details;
    }

    @Override
    public int compareTo(Statistics o) {
        return agency.compareTo(o.agency);
    }
}
