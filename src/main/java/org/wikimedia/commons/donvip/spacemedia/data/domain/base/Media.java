package org.wikimedia.commons.donvip.spacemedia.data.domain.base;

import static java.util.stream.Collectors.toSet;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.urlToUriUnchecked;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.temporal.ChronoField;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import javax.persistence.Column;
import javax.persistence.EntityListeners;
import javax.persistence.FetchType;
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.wikimedia.commons.donvip.spacemedia.service.wikimedia.CommonsService;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

/**
 * Base class of all media.
 *
 * @param <ID> the identifier type
 * @param <D>  the media date type
 */
@MappedSuperclass
@EntityListeners(MediaListener.class)
@JsonTypeInfo(use = Id.CLASS, include = As.PROPERTY, property = "class")
public abstract class Media<ID, D extends Temporal> implements MediaProjection<ID> {

    private static final Pattern ONLY_DIGITS = Pattern.compile("\\d+");
    private static final Pattern URI_LIKE = Pattern.compile("Https?\\-\\-.*", Pattern.CASE_INSENSITIVE);

    @ManyToMany(fetch = FetchType.EAGER)
    private List<FileMetadata> metadata = new ArrayList<>();

    @Column(nullable = true, length = 380)
    @JsonProperty("thumbnail_url")
    protected URL thumbnailUrl;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    @FullTextField
    protected String title;

    @Lob
    @Column(nullable = true, columnDefinition = "MEDIUMTEXT")
    @FullTextField
    protected String description;

    @Column(nullable = true)
    protected Boolean ignored;

    @Lob
    @Column(nullable = true, columnDefinition = "TEXT")
    @JsonProperty("ignored_reason")
    protected String ignoredReason;

    @Column(nullable = true)
    @JsonProperty("last_update")
    protected LocalDateTime lastUpdate;

    @Override
    public List<FileMetadata> getMetadata() {
        return metadata;
    }

    public void setMetadata(List<FileMetadata> metadata) {
        this.metadata = metadata;
    }

    public boolean addMetadata(FileMetadata metadata) {
        return getMetadata().add(metadata);
    }

    public boolean containsMetadata(String assetUrl) throws MalformedURLException, URISyntaxException {
        return containsMetadata(new URL(assetUrl));
    }

    public boolean containsMetadata(URL assetUrl) throws URISyntaxException {
        URI uri = assetUrl.toURI();
        return getMetadata().stream()
                .anyMatch(m -> m.getAssetUrl() != null && uri.equals(urlToUriUnchecked(m.getAssetUrl())));
    }

    public URL getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(URL thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public String getUploadTitle(FileMetadata fileMetadata) {
        // Upload title must not exceed mediawiki limit (240 characters, filename-toolong API error)
        String id = getUploadId();
        String s = CommonsService.normalizeFilename(title);
        if (id.equals(s)) {
            return isTitleBlacklisted(s)
                    ? getUploadTitle(CommonsService.normalizeFilename(getFirstSentence(description)), id)
                    : s.substring(0, Math.min(234, s.length()));
        } else {
            return getUploadTitle(s, id);
        }
    }

    protected String getUploadId() {
        return CommonsService.normalizeFilename(getId().toString());
    }

    protected static String getFirstSentence(String desc) {
        if (desc == null) {
            return "";
        }
        int idxDotInDesc = desc.indexOf('.');
        return idxDotInDesc > 0 ? desc.substring(0, idxDotInDesc) : desc;
    }

    private static String getUploadTitle(String s, String id) {
        return new StringBuilder(s.substring(0, Math.min(234 - id.length() - 3, s.length()))).append(" (").append(id)
                .append(')').toString();
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
        return getMetadata().stream()
                .flatMap(m -> Optional.ofNullable(m.getCommonsFileNames()).orElse(Set.of()).stream()).collect(toSet());
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

    /**
     * Returns the identifier usually used in Commons.
     *
     * @return the identifier usually used in Commons
     */
    public String getIdUsedInCommons() {
        return getId().toString();
    }

    public abstract D getDate();

    public abstract void setDate(D date);

    public Year getYear() {
        return Year.of(getDate().get(ChronoField.YEAR));
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
        return getMetadata().stream().filter(FileMetadata::shouldUpload).map(FileMetadata::getSha1).toList();
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
    public boolean isAudio() {
        return getMetadata().stream().anyMatch(FileMetadata::isAudio);
    }

    /**
     * Determines if this media is an image.
     *
     * @return {@code true} if this media is an image
     */
    public boolean isImage() {
        return getMetadata().stream().anyMatch(FileMetadata::isImage);
    }

    /**
     * Determines if this media is a video.
     *
     * @return {@code true} if this media is a video
     */
    public boolean isVideo() {
        return getMetadata().stream().anyMatch(FileMetadata::isVideo);
    }

    /**
     * Returns the preview URL to display in UI. Thumbnail if available, otherwise asset.
     *
     * @return the preview URL to display in UI. Thumbnail if available, otherwise asset.
     */
    public final URL getPreviewUrl() {
        return Optional.ofNullable(getThumbnailUrl()).orElse(
                getMetadata().stream().map(FileMetadata::getAssetUrl).filter(Objects::nonNull).findFirst().orElse(null));
    }

    /**
     * Returns either the first file name found in Commons database, or the upload
     * title followed by the given extension.
     *
     * @param metadata file metadata
     * @return either the first file name found in Commons database, or the upload
     *         title followed by the given extension
     */
    public final String getFirstCommonsFileNameOrUploadTitle(FileMetadata metadata) {
        Set<String> commonsFileNames = metadata.getCommonsFileNames();
        return isEmpty(commonsFileNames) ? getUploadTitle(metadata) + '.' + metadata.getFileExtension()
                : commonsFileNames.iterator().next();
    }

    /**
     * Copies data that could have been updated after the initial API call.
     *
     * @param mediaFromApi updated media from agency API
     */
    public final void copyDataFrom(Media<ID, D> mediaFromApi) {
        setDescription(mediaFromApi.getDescription());
        setTitle(mediaFromApi.getTitle());
        if (isNotEmpty(mediaFromApi.getMetadata())) {
            for (FileMetadata m : getMetadata()) {
                if (mediaFromApi.getMetadata().stream().map(FileMetadata::getAssetUrl).noneMatch(m.getAssetUrl()::equals)) {
                    getMetadata().remove(m);
                }
            }
            for (FileMetadata apiMetadata : mediaFromApi.getMetadata()) {
                if (getMetadata().stream().map(FileMetadata::getAssetUrl).noneMatch(apiMetadata.getAssetUrl()::equals)) {
                    getMetadata().add(apiMetadata);
                }
            }
        }
    }

    /**
     * Determines if title is blacklisted according to
     * https://commons.wikimedia.org/wiki/MediaWiki:Titleblacklist or
     * https://meta.wikimedia.org/wiki/Title_blacklist
     *
     * @return {@code true} if title is blacklisted by mediawiki
     */
    public boolean isTitleBlacklisted(String title) {
        return ONLY_DIGITS.matcher(title.replace(" ", "").replace("/", "").replace("_", "").replace("-", "")).matches()
                || URI_LIKE.matcher(title).matches();
    }

    /**
     * Determines if the given string is included in title or description.
     *
     * @param string string to search
     * @return {@code true} if the given string is included in title or description
     */
    public boolean containsInTitleOrDescription(String string) {
        return (title != null && title.contains(string)) || (description != null && description.contains(string));
    }
}
