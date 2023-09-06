package org.wikimedia.commons.donvip.spacemedia.data.domain.base;

import static java.util.Locale.ENGLISH;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.deleteWhitespace;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.stripAccents;
import static org.wikimedia.commons.donvip.spacemedia.service.wikimedia.CommonsService.normalizeFilename;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.newURL;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.urlToUriUnchecked;

import java.net.URI;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.EntityListeners;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import javax.persistence.MappedSuperclass;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.IdentifierBridgeRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;

/**
 * Base class of all media.
 */
@MappedSuperclass
@EntityListeners(MediaListener.class)
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = As.PROPERTY, property = "class")
public class Media implements MediaProjection, MediaDescription {

    private static final Logger LOGGER = LoggerFactory.getLogger(Media.class);

    private static final Pattern ONLY_DIGITS = Pattern.compile("\\d+");
    private static final Pattern IMG = Pattern.compile("IMG[-_]\\d+(-[A-Z]+)?");
    private static final Pattern URI_LIKE = Pattern.compile("Https?\\-\\-.*", Pattern.CASE_INSENSITIVE);

    @Id
    @Embedded
    @DocumentId(identifierBridge = @IdentifierBridgeRef(type = CompositeMediaIdBridge.class))
    private CompositeMediaId id;

    @ManyToMany(fetch = FetchType.EAGER)
    private Set<FileMetadata> metadata = new LinkedHashSet<>();

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

    @Lob
    @Column(name = "credits", nullable = true, columnDefinition = "TEXT")
    private String credits;

    @Column(nullable = true)
    protected LocalDate creationDate;

    @Column(nullable = true)
    protected ZonedDateTime creationDateTime;

    @Column(nullable = false)
    protected LocalDate publicationDate;

    @Column(nullable = true)
    protected ZonedDateTime publicationDateTime;

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
    public Set<FileMetadata> getMetadata() {
        return metadata;
    }

    public void setMetadata(Set<FileMetadata> metadata) {
        this.metadata = metadata;
    }

    @JsonIgnore
    public Stream<FileMetadata> getMetadataStream() {
        return getMetadata().stream();
    }

    @JsonIgnore
    public boolean hasMetadata() {
        return !getMetadata().isEmpty();
    }

    @JsonIgnore
    public int getMetadataCount() {
        return getMetadata().size();
    }

    public boolean addMetadata(FileMetadata metadata) {
        return getMetadata().add(metadata);
    }

    public boolean addAllMetadata(Collection<FileMetadata> metadata) {
        return getMetadata().addAll(metadata);
    }

    public boolean containsMetadata(String assetUrl) {
        return containsMetadata(newURL(assetUrl));
    }

    public boolean containsMetadata(URL assetUrl) {
        URI uri = urlToUriUnchecked(assetUrl);
        return getMetadataStream().anyMatch(m -> m.getAssetUrl() != null && uri.equals(m.getAssetUri()));
    }

    public URL getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(URL thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public String getUploadTitle(FileMetadata fileMetadata) {
        // Upload title must not exceed mediawiki limit (240 characters, filename-toolong API error)
        String id = getUploadId(fileMetadata);
        String s = getUploadTitle();
        if (strippedLower(id).equals(strippedLower(s))) {
            return isTitleBlacklisted(s)
                    ? getUploadTitle(normalizeFilename(getFirstSentence(getDescription(fileMetadata))), id)
                    : s.substring(0, Math.min(234, s.length()));
        } else {
            return getUploadTitle(s, id);
        }
    }

    public String getDescription(FileMetadata fileMetadata) {
        return fileMetadata != null && isNotBlank(fileMetadata.getDescription()) ? fileMetadata.getDescription()
                : getDescription();
    }

    private static String strippedLower(String s) {
        return deleteWhitespace((" " + stripAccents(s)).toLowerCase(ENGLISH).replace(" a ", " ").replace(" as ", " ")
                .replace(" at ", " ").replace(" by ", " ").replace(" for ", " ").replace(" from ", " ")
                .replace(" in ", " ").replace(" is ", " ").replace(" of ", " ").replace(" off ", " ")
                .replace(" on ", " ").replace(" the ", " ").replace(" to ", " ").replace(" with ", " ").replace("-", "")
                .replace("_", "").replace("'", "").replace(",", "").replace(".", "").replace("“", "").replace("(", "")
                .replace(")", "").replace(":", "").replace("’", "").replace("–", ""));
    }

    protected String getUploadTitle() {
        return normalizeFilename(title);
    }

    protected String getUploadId(FileMetadata fileMetadata) {
        return normalizeFilename(getIdUsedInOrg());
    }

    protected static String getFirstSentence(String desc) {
        if (desc == null) {
            return "";
        }
        int idxDotInDesc = desc.indexOf('.');
        return idxDotInDesc > 0 ? desc.substring(0, idxDotInDesc) : desc;
    }

    protected static String getUploadTitle(String s, String id) {
        String firstPart = s.substring(0, Math.min(234 - id.length() - 3, s.length()));
        return firstPart.contains(id) ? firstPart
                : new StringBuilder(firstPart).append(" (").append(id).append(')').toString();
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(String description) {
        this.description = description;
    }

    @JsonIgnore
    public List<MediaDescription> getDescriptionObjects() {
        List<MediaDescription> result = new ArrayList<>(List.of(this));
        result.addAll(getMetadata());
        return result;
    }

    @JsonIgnore
    public List<String> getDescriptions() {
        return getDescriptionObjects().stream().map(MediaDescription::getDescription).filter(StringUtils::isNotBlank)
                .distinct().toList();
    }

    public String getCredits() {
        return credits;
    }

    public void setCredits(String credits) {
        this.credits = credits;
    }

    public LocalDate getCreationDate() {
        return getLocalDate(creationDate, creationDateTime);
    }

    public void setCreationDate(LocalDate creationDate) {
        this.creationDate = creationDate;
    }

    public ZonedDateTime getCreationDateTime() {
        return creationDateTime;
    }

    public void setCreationDateTime(ZonedDateTime creationDateTime) {
        this.creationDateTime = creationDateTime;
        setCreationDate(creationDateTime != null ? creationDateTime.toLocalDate() : null);
    }

    public LocalDate getPublicationDate() {
        return getLocalDate(publicationDate, publicationDateTime);
    }

    public void setPublicationDate(LocalDate publicationDate) {
        this.publicationDate = publicationDate;
    }

    public ZonedDateTime getPublicationDateTime() {
        return publicationDateTime;
    }

    public void setPublicationDateTime(ZonedDateTime publicationDateTime) {
        this.publicationDateTime = publicationDateTime;
        setPublicationDate(publicationDateTime != null ? publicationDateTime.toLocalDate() : null);
    }

    private static LocalDate getLocalDate(LocalDate localDate, ZonedDateTime zonedDateTime) {
        return localDate != null ? localDate : zonedDateTime != null ? zonedDateTime.toLocalDate() : null;
    }

    @JsonIgnore
    public Set<String> getAllCommonsFileNames() {
        return getMetadataStream()
                .flatMap(m -> ofNullable(m.getCommonsFileNames()).orElse(Set.of()).stream()).collect(toSet());
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

    @Override
    public CompositeMediaId getId() {
        return id;
    }

    public void setId(CompositeMediaId id) {
        this.id = id;
    }

    /**
     * Returns the identifier usually used in org.
     *
     * @return the identifier usually used in org
     */
    public String getIdUsedInOrg() {
        return getId().getMediaId();
    }

    /**
     * Returns the identifiers usually used in Commons.
     *
     * @return the identifiers usually used in Commons
     */
    public List<String> getIdUsedInCommons() {
        return List.of(getIdUsedInOrg());
    }

    public List<String> getSearchTermsInCommons() {
        List<String> result = new ArrayList<>(getIdUsedInCommons());
        ofNullable(getDescription()).map(x -> x.contains(" --- ") ? x.substring(x.indexOf(" --- ") + 5) : x)
                .ifPresent(result::add);
        return result;
    }

    public Temporal getBestTemporal() {
        return Stream
                .<Temporal>of(getCreationDateTime(), getCreationDate(), getPublicationDateTime(), getPublicationDate())
                .filter(Objects::nonNull).findFirst()
                .orElseThrow(() -> new IllegalStateException("No best temporal found for " + this));
    }

    public Year getYear() {
        return Year.of(getBestTemporal().get(ChronoField.YEAR));
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
        Media other = (Media) obj;
        return Objects.equals(title, other.title) && Objects.equals(metadata, other.metadata);
    }

    @JsonIgnore
    public List<String> getAssetsToUpload() {
        return getMetadataStream().filter(FileMetadata::shouldUpload).map(FileMetadata::getSha1).toList();
    }

    @JsonIgnore
    public List<String> getAudioAssetsToUpload() {
        return getMetadataStream().filter(FileMetadata::shouldUpload).filter(FileMetadata::isAudio)
                .map(FileMetadata::getSha1).toList();
    }

    @JsonIgnore
    public List<String> getImageAssetsToUpload() {
        return getMetadataStream().filter(FileMetadata::shouldUpload).filter(FileMetadata::isImage)
                .map(FileMetadata::getSha1).toList();
    }

    @JsonIgnore
    public List<String> getVideoAssetsToUpload() {
        return getMetadataStream().filter(FileMetadata::shouldUpload).filter(FileMetadata::isVideo)
                .map(FileMetadata::getSha1).toList();
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
        return getMetadataStream().anyMatch(FileMetadata::isAudio);
    }

    /**
     * Determines if this media is an image.
     *
     * @return {@code true} if this media is an image
     */
    public boolean isImage() {
        return getMetadataStream().anyMatch(FileMetadata::isImage);
    }

    /**
     * Determines if this media is a video.
     *
     * @return {@code true} if this media is a video
     */
    public boolean isVideo() {
        return getMetadataStream().anyMatch(FileMetadata::isVideo);
    }

    /**
     * Returns the preview URL to display in UI. Thumbnail if available, otherwise asset.
     *
     * @return the preview URL to display in UI. Thumbnail if available, otherwise asset.
     */
    public final URL getPreviewUrl() {
        return ofNullable(getThumbnailUrl()).orElse(
                getMetadataStream().map(FileMetadata::getAssetUrl).filter(Objects::nonNull).findFirst().orElse(null));
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
     * @param mediaFromApi updated media from org API
     */
    public final void copyDataFrom(Media mediaFromApi) {
        setDescription(mediaFromApi.getDescription());
        setTitle(mediaFromApi.getTitle());
        setCredits(mediaFromApi.getCredits());
        setCreationDateTime(mediaFromApi.getCreationDateTime());
        setCreationDate(mediaFromApi.getCreationDate());
        setPublicationDateTime(mediaFromApi.getPublicationDateTime());
        setPublicationDate(mediaFromApi.getPublicationDate());
        if (mediaFromApi.hasMetadata()) {
            for (Iterator<FileMetadata> it = getMetadata().iterator(); it.hasNext();) {
                FileMetadata m = it.next();
                if (mediaFromApi.getMetadataStream().map(FileMetadata::getAssetUri)
                        .noneMatch(x -> areSameUris(x, m.getAssetUri()))) {
                    LOGGER.info("Remove database metadata not found anymore in API by asset URI: {}", m);
                    it.remove();
                }
            }
            for (FileMetadata apiMetadata : mediaFromApi.getMetadata()) {
                if (getMetadataStream().map(FileMetadata::getAssetUri)
                        .noneMatch(x -> areSameUris(x, apiMetadata.getAssetUri()))) {
                    LOGGER.info("Add API metadata not yet found in database by asset URI: {}", apiMetadata);
                    addMetadata(apiMetadata);
                }
            }
        }
        if (ignored != Boolean.TRUE && mediaFromApi.ignored == Boolean.TRUE) {
            setIgnored(mediaFromApi.isIgnored());
            setIgnoredReason(mediaFromApi.getIgnoredReason());
        }
        if (this instanceof WithKeywords kw && mediaFromApi instanceof WithKeywords kwApi) {
            kw.setKeywords(kwApi.getKeywords());
        }
    }

    protected boolean areSameUris(URI a, URI b) {
        return a.equals(b);
    }

    /**
     * Determines if title is blacklisted according to
     * https://commons.wikimedia.org/wiki/MediaWiki:Titleblacklist or
     * https://meta.wikimedia.org/wiki/Title_blacklist
     *
     * @return {@code true} if title is blacklisted by mediawiki
     */
    public static boolean isTitleBlacklisted(String title) {
        return ONLY_DIGITS.matcher(title.replace(" ", "").replace("/", "").replace("_", "").replace("-", "")).matches()
                || URI_LIKE.matcher(title).matches() || IMG.matcher(title).matches();
    }

    /**
     * Determines if the given string is included in title or description.
     *
     * @param string string to search
     * @return {@code true} if the given string is included in title or description
     */
    public boolean containsInTitleOrDescription(String string) {
        String lc = string.toLowerCase(ENGLISH);
        return (title != null && title.toLowerCase(ENGLISH).contains(lc))
                || (description != null && description.toLowerCase(ENGLISH).contains(lc)) || (hasMetadata()
                        && getMetadataStream().map(FileMetadata::getDescription).filter(StringUtils::isNotBlank)
                                .distinct().anyMatch(x -> x.toLowerCase(ENGLISH).contains(lc)));
    }
}
