package org.wikimedia.commons.donvip.spacemedia.data.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonFormat;

public class Statistics implements Comparable<Statistics> {
    private final String org;
    private final String id;
    private final long totalMedia;
    private final long uploadedMedia;
    private final long ignoredMedia;
    private final long missingImages;
    private final long missingVideos;
    private final long missingDocuments;
    private final long hashedMedia;
    private final LocalDateTime lastUpdate;
    private List<Statistics> details;

    public Statistics(String org, String id, long totalMedia, long uploadedMedia, long ignoredMedia,
            long missingImages, long missingVideos, long missingDocuments, long hashedMedia, LocalDateTime lastUpdate) {
        this(org, id, totalMedia, uploadedMedia, ignoredMedia, missingImages, missingVideos, missingDocuments,
                hashedMedia, lastUpdate, null);
    }

    public Statistics(String org, String id, long totalMedia, long uploadedMedia, long ignoredMedia,
            long missingImages, long missingVideos, long missingDocuments, long hashedMedia, LocalDateTime lastUpdate,
            List<Statistics> details) {
        this.org = Objects.requireNonNull(org);
        this.id = Objects.requireNonNull(id);
        this.totalMedia = totalMedia;
        this.uploadedMedia = uploadedMedia;
        this.ignoredMedia = ignoredMedia;
        this.missingImages = missingImages;
        this.missingVideos = missingVideos;
        this.missingDocuments = missingDocuments;
        this.hashedMedia = hashedMedia;
        this.lastUpdate = lastUpdate;
        this.details = details;
    }

    /**
     * @return the org
     */
    public String getOrg() {
        return org;
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
     * @return the missingImages
     */
    public long getMissingImages() {
        return missingImages;
    }

    /**
     * @return the missingVideos
     */
    public long getMissingVideos() {
        return missingVideos;
    }

    /**
     * @return the missingDocuments
     */
    public long getMissingDocuments() {
        return missingDocuments;
    }

    /**
     * @return the hashedMedia
     */
    public long getHashedMedia() {
        return hashedMedia;
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

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm")
    public LocalDateTime getLastUpdate() {
        return lastUpdate;
    }

    @Override
    public int compareTo(Statistics o) {
        return org.compareTo(o.org);
    }
}
