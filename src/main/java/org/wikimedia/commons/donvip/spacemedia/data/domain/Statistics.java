package org.wikimedia.commons.donvip.spacemedia.data.domain;

import java.util.List;
import java.util.Objects;

public class Statistics implements Comparable<Statistics> {
    private final String agency;
    private final String id;
    private final long totalMedia;
    private final long uploadedMedia;
    private final long ignoredMedia;
    private final long missingMedia;
    private final long hashedMedia;
    private final Long problemsCount;
    private List<Statistics> details;

    public Statistics(String agency, String id, long totalMedia, long uploadedMedia, long ignoredMedia, long missingMedia, long hashedMedia,
            Long problemsCount) {
        this(agency, id, totalMedia, uploadedMedia, ignoredMedia, missingMedia, hashedMedia, problemsCount, null);
    }

    public Statistics(String agency, String id, long totalMedia, long uploadedMedia, long ignoredMedia, long missingMedia, long hashedMedia,
            Long problemsCount, List<Statistics> details) {
        this.agency = Objects.requireNonNull(agency);
        this.id = Objects.requireNonNull(id);
        this.totalMedia = totalMedia;
        this.uploadedMedia = uploadedMedia;
        this.ignoredMedia = ignoredMedia;
        this.missingMedia = missingMedia;
        this.hashedMedia = hashedMedia;
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
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * @return the totalMedia
     */
    public long getTotalMedia() {
        return totalMedia;
    }

    /**
     * @return the uploadedMedia
     */
    public long getUploadedMedia() {
        return uploadedMedia;
    }

    /**
     * @return the missingMedia
     */
    public long getMissingMedia() {
        return missingMedia;
    }

    /**
     * @return the hashedMedia
     */
    public long getHashedMedia() {
        return hashedMedia;
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

    public long getIgnoredMedia() {
        return ignoredMedia;
    }

    @Override
    public int compareTo(Statistics o) {
        return agency.compareTo(o.agency);
    }
}
