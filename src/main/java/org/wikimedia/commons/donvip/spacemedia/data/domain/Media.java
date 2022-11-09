package org.wikimedia.commons.donvip.spacemedia.data.domain;

import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.temporal.ChronoField;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embedded;
import javax.persistence.EntityListeners;
import javax.persistence.FetchType;
import javax.persistence.Lob;
import javax.persistence.MappedSuperclass;
import javax.persistence.PostLoad;
import javax.persistence.Transient;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Base class of all media.
 *
 * @param <ID> the identifier type
 * @param <D>  the media date type
 */
@MappedSuperclass
@EntityListeners(MediaListener.class)
public abstract class Media<ID, D extends Temporal> implements MediaProjection<ID> {

    @Embedded
    protected Metadata metadata = new Metadata();

    @Column(nullable = true, length = 380)
    protected URL thumbnailUrl;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    @FullTextField
    protected String title;

    @Lob
    @Column(nullable = true, columnDefinition = "MEDIUMTEXT")
    @FullTextField
    protected String description;

    @ElementCollection(fetch = FetchType.EAGER)
    protected Set<String> commonsFileNames = new HashSet<>();

    @Column(nullable = true)
    protected Boolean ignored;

    @Lob
    @Column(nullable = true, columnDefinition = "TEXT")
    protected String ignoredReason;

    /**
     * Duplicates are other media considered strictly or nearly identical, thus ignored and not to be uploaded.
     */
    @Column(nullable = false)
    @ElementCollection(fetch = FetchType.EAGER)
    protected Set<Duplicate> duplicates = new HashSet<>();

    /**
     * Variants are other media considered similar but not identical, thus not ignored and to be uploaded and linked to this media.
     */
    @Column(nullable = false)
    @ElementCollection(fetch = FetchType.EAGER)
    protected Set<Duplicate> variants = new HashSet<>();

    @Column(nullable = true)
    protected LocalDateTime lastUpdate;

    @PostLoad
    protected void initData() {
        if (metadata == null) {
            metadata = new Metadata();
        }
    }

    @Override
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

    @Transient
    @JsonIgnore
    public Set<String> getAllCommonsFileNames() {
        return getCommonsFileNames();
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

    public abstract void setId(ID id);

    public abstract D getDate();

    public abstract void setDate(D date);

    public Year getYear() {
        return Year.of(getDate().get(ChronoField.YEAR));
    }

    public void setYear(Year photoYear) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<Duplicate> getDuplicates() {
        return duplicates;
    }

    public void setDuplicates(Set<Duplicate> originalIds) {
        this.duplicates = originalIds;
    }

    public boolean addDuplicate(Duplicate duplicate) {
        if (duplicates == null) {
            duplicates = new HashSet<>();
        }
        return duplicates.add(duplicate);
    }

    public boolean removeDuplicate(Duplicate duplicate) {
        return duplicates != null && duplicates.remove(duplicate);
    }

    public void clearDuplicates() {
        if (duplicates != null) {
            duplicates.clear();
        }
    }

    @Override
    public Set<Duplicate> getVariants() {
        return variants;
    }

    public void setVariants(Set<Duplicate> originalIds) {
        this.variants = originalIds;
    }

    public boolean addVariant(Duplicate variant) {
        if (variants == null) {
            variants = new HashSet<>();
        }
        return variants.add(variant);
    }

    public boolean removeVariant(Duplicate variant) {
        return variants != null && variants.remove(variant);
    }

    public void clearVariants() {
        if (variants != null) {
            variants.clear();
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, metadata);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        Media<?, ?> other = (Media<?, ?>) obj;
        return Objects.equals(title, other.title)
                && Objects.equals(metadata, other.metadata);
    }

    public List<String> getAssetsToUpload() {
        List<String> result = new ArrayList<>();
        String sha1 = metadata.getSha1();
        if (isNotBlank(sha1) && isEmpty(getCommonsFileNames())) {
            result.add(sha1);
        }
        return result;
    }

    public LocalDateTime getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(LocalDateTime lastUpdate) {
        this.lastUpdate = lastUpdate;
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

    /**
     * Returns the preview URL to display in UI. Thumbnail if available, otherwise asset.
     *
     * @return the preview URL to display in UI. Thumbnail if available, otherwise asset.
     */
    public final URL getPreviewUrl() {
        return Optional.ofNullable(getThumbnailUrl()).orElse(getMetadata().getAssetUrl());
    }

    /**
     * Determines if other media with a low but positive perceptual hash difference are considered as variants instead of duplicates.
     *
     * @return {@code true} if other media with a low but positive perceptual hash difference are considered as variants instead of duplicates.
     */
    public boolean considerVariants() {
        return false;
    }

    /**
     * Returns either the first file name found in Commons database, or the upload title followed by the given extension.
     *
     * @param commonsFileNames file names found in Commons database
     * @param ext file extension to be appended to upload title no filename is found in Commons database
     * @return either the first file name found in Commons database, or the upload title followed by the given extension
     */
    public final String getFirstCommonsFileNameOrUploadTitle(Set<String> commonsFileNames, String ext) {
        return isEmpty(commonsFileNames) ? getUploadTitle() + '.' + ext : commonsFileNames.iterator().next();
    }
}
