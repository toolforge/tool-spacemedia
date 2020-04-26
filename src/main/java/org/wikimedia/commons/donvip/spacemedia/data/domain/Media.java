package org.wikimedia.commons.donvip.spacemedia.data.domain;

import java.net.URL;
import java.time.Year;
import java.time.temporal.ChronoField;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embedded;
import javax.persistence.FetchType;
import javax.persistence.Lob;
import javax.persistence.MappedSuperclass;
import javax.persistence.PostLoad;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Store;

/**
 * Base class of all media.
 *
 * @param <ID> the identifier type
 * @param <D>  the media date type
 */
@MappedSuperclass
public abstract class Media<ID, D extends Temporal> {

    @Embedded
    protected Metadata metadata = new Metadata();

    @Column(nullable = true, length = 380)
    protected URL thumbnailUrl;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    @Field(index = Index.YES, analyze = Analyze.YES, store = Store.NO)
    protected String title;

    @Lob
	@Column(nullable = true, columnDefinition = "MEDIUMTEXT")
    @Field(index = Index.YES, analyze = Analyze.YES, store = Store.NO)
    protected String description;

    @ElementCollection(fetch = FetchType.EAGER)
    protected Set<String> commonsFileNames;

    @Column(nullable = true)
    protected Boolean ignored;

    @Column(nullable = true)
    protected String ignoredReason;

    @ElementCollection(fetch = FetchType.EAGER)
    protected Set<String> originalIds;

    @PostLoad
    protected void initData() {
        if (metadata == null) {
            metadata = new Metadata();
        }
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public void setMetadata(Metadata metadata) {
        this.metadata = metadata;
    }

    public URL getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(URL thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public String getUploadTitle() {
        return title + " (" + getId() + ")";
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Set<String> getCommonsFileNames() {
        return commonsFileNames;
    }

    public void setCommonsFileNames(Set<String> commonsFileNames) {
        this.commonsFileNames = commonsFileNames;
    }

    public Boolean isIgnored() {
        return ignored;
    }

    public void setIgnored(Boolean ignored) {
        this.ignored = ignored;
    }

    public String getIgnoredReason() {
        return ignoredReason;
    }

    public void setIgnoredReason(String ignoredReason) {
        this.ignoredReason = ignoredReason;
    }

    public abstract ID getId();

    public abstract void setId(ID id);

    public abstract D getDate();

    public abstract void setDate(D date);

    public Year getYear() {
        return Year.of(getDate().get(ChronoField.YEAR));
    }

    public void setYear(Year photoYear) {
        throw new UnsupportedOperationException();
    }

    public Set<String> getOriginalIds() {
        return originalIds;
    }

    public void setOriginalIds(Set<String> originalIds) {
        this.originalIds = originalIds;
    }

    public boolean addOriginalId(String originalId) {
        if (originalIds == null) {
            originalIds = new HashSet<>();
        }
        return originalIds.add(originalId);
    }

    public boolean removeOriginalId(String originalId) {
        return originalIds != null && originalIds.remove(originalId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(commonsFileNames, metadata);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        Media<?, ?> other = (Media<?, ?>) obj;
        return Objects.equals(commonsFileNames, other.commonsFileNames)
                && Objects.equals(metadata, other.metadata);
    }

    public List<String> getAssetsToUpload() {
        List<String> result = new ArrayList<>();
        String sha1 = metadata.getSha1();
        if (StringUtils.isNotBlank(sha1) && CollectionUtils.isEmpty(getCommonsFileNames())) {
            result.add(sha1);
        }
        return result;
    }

    /**
     * Determines if this media is an audio.
     * 
     * @return {@code true} if this media is an audio
     */
    public abstract boolean isAudio();

    /**
     * Determines if this media is an image.
     * 
     * @return {@code true} if this media is an image
     */
    public abstract boolean isImage();

    /**
     * Determines if this media is a video.
     * 
     * @return {@code true} if this media is a video
     */
    public abstract boolean isVideo();
}
