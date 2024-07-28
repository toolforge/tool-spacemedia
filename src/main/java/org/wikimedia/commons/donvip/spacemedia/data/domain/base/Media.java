package org.wikimedia.commons.donvip.spacemedia.data.domain.base;

import static java.util.Locale.ENGLISH;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.deleteWhitespace;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.stripAccents;
import static org.wikimedia.commons.donvip.spacemedia.service.wikimedia.CommonsService.normalizeFilename;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.getFirstSentence;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.newURL;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.urlToUriUnchecked;

import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embedded;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

/**
 * Base class of all media.
 */
@MappedSuperclass
@Table(indexes = {
        @Index(name = "repo_id", columnList = "repo_id"),
        @Index(name = "media_id", columnList = "media_id"),
        @Index(name = "publication_date", columnList = "publication_date"),
        @Index(name = "publication_month", columnList = "publication_month"),
        @Index(name = "publication_year", columnList = "publication_year") })
@EntityListeners(MediaListener.class)
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = As.PROPERTY, property = "class")
public class Media implements MediaProjection, MediaDescription {

    private static final Logger LOGGER = LoggerFactory.getLogger(Media.class);

    private static final Pattern ONLY_DIGITS = Pattern.compile("[\\da-f]+[on]?");
    private static final Pattern IMG = Pattern.compile("(IMA?GE?|DSC|GOPR|DCIM)[-_\\\\]?[_\\d]+.*");
    private static final Pattern URI_LIKE = Pattern.compile("Https?\\-\\-.*", Pattern.CASE_INSENSITIVE);
    private static final Pattern IMG2 = Pattern.compile(
            "(Untitled|No[-_]?name|Unbenannt|Picture|Pict?|Image[mn]?|Img|Immagine|Clip|Photo|Foto|Bild|Scan[\\W\\d_]|Panorama|Sin_título)_?\\P{L}*",
            Pattern.CASE_INSENSITIVE | Pattern.CANON_EQ | Pattern.UNICODE_CASE);

    @Id
    @Embedded
    private CompositeMediaId id;

    @ManyToMany(fetch = FetchType.EAGER)
    private Set<FileMetadata> metadata = new LinkedHashSet<>();

    @Column(nullable = true, length = 380)
    @JsonProperty("thumbnail_url")
    protected URL thumbnailUrl;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    protected String title;

    @Lob
    @Column(nullable = true, columnDefinition = "MEDIUMTEXT")
    protected String description;

    @Lob
    @Column(name = "credits", nullable = true, columnDefinition = "TEXT")
    private String credits;

    @Column(nullable = true)
    protected LocalDate creationDate;

    @Column(nullable = true)
    protected ZonedDateTime creationDateTime;

    @Column(nullable = true)
    protected LocalDate publicationDate;

    @Column(nullable = true, columnDefinition = "CHAR(7)")
    @Convert(converter = YearMonthAttributeConverter.class)
    protected YearMonth publicationMonth;

    @Column(nullable = false, columnDefinition = "YEAR(4)")
    protected Year publicationYear;

    @Column(nullable = true)
    protected ZonedDateTime publicationDateTime;

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

    public boolean containsMetadataWithFilename(String filename) {
        return filename != null && getMetadataStream().anyMatch(m -> filename.equals(m.getOriginalFileName()));
    }

    public URL getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(URL thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public String getUploadTitle(FileMetadata fileMetadata) {
        // Upload title must not exceed mediawiki limit (240 characters, filename-toolong API error)
        String uid = getUploadId(fileMetadata);
        String s = getUploadTitle();
        if (strippedLower(uid).equals(strippedLower(s))) {
            return isWrongtitle(s)
                    ? getUploadTitle(
                            normalizeFilename(
                                    getAlbumName().orElseGet(() -> getFirstSentence(getDescription(fileMetadata)))),
                            uid)
                    : stringShortened(s, "");
        } else {
            return getUploadTitle(isWrongtitle(s)
                    ? normalizeFilename(getAlbumName().orElseGet(() -> getFirstSentence(getDescription(fileMetadata))))
                    : s, uid);
        }
    }

    private static boolean isWrongtitle(String s) {
        return isTitleBlacklisted(s) || ("n-a".equals(s.replace('/', '-').toLowerCase(ENGLISH)));
    }

    @Transient
    public Collection<String> getAlbumNames() {
        return List.of();
    }

    @Transient
    public final Optional<String> getAlbumName() {
        return getAlbumNames().stream().findFirst();
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
                .replace("_", "").replace("'", "").replace(",", "").replace(".", "").replace("“", "").replace("”", "")
                .replace("(", "").replace(")", "").replace(":", "").replace("’", "").replace("–", ""));
    }

    protected String getUploadTitle() {
        return normalizeFilename(title);
    }

    public String getUploadId(FileMetadata fileMetadata) {
        String result = getIdUsedInOrg();
        if (getMetadataStream().map(fm -> fm.getFileExtensionOnCommons()).filter(Objects::nonNull)
                .filter(ext -> ext.equals(fileMetadata.getFileExtensionOnCommons())).count() > 1) {
            String filename = fileMetadata.getFileName();
            if (!strippedLower(result).equals(strippedLower(filename))) {
                if (filename.contains(result)) {
                    result = filename;
                } else {
                    result += " - " + filename;
                }
            }
        }
        return normalizeFilename(result);
    }

    protected static String getUploadTitle(String s, String id) {
        String firstPart = stringShortened(s, id);
        return firstPart.contains(id) ? firstPart
                : new StringBuilder(firstPart).append(" (").append(id).append(')').toString();
    }

    private static String stringShortened(String s, String id) {
        int maxLen = 234 - id.length() - 3;
        String res = s.substring(0, Math.min(maxLen, s.length()));
        while (res.getBytes(StandardCharsets.UTF_8).length > maxLen) {
            res = res.substring(0, res.length() - 1);
        }
        return res;
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
        setPublicationMonth(publicationDate != null ? YearMonth.from(publicationDate) : null);
    }

    public YearMonth getPublicationMonth() {
        return publicationMonth;
    }

    public void setPublicationMonth(YearMonth publicationMonth) {
        this.publicationMonth = publicationMonth;
        setPublicationYear(publicationMonth != null ? Year.from(publicationMonth) : null);
    }

    public Year getPublicationYear() {
        return publicationYear;
    }

    public void setPublicationYear(Year publicationYear) {
        this.publicationYear = publicationYear;
    }

    public ZonedDateTime getPublicationDateTime() {
        return publicationDateTime;
    }

    public void setPublicationDateTime(ZonedDateTime publicationDateTime) {
        this.publicationDateTime = publicationDateTime;
        setPublicationDate(publicationDateTime != null ? publicationDateTime.toLocalDate() : null);
    }

    public void setPublication(Temporal t) {
        if (t instanceof LocalDate d) {
            setPublicationDate(d);
        } else if (t instanceof LocalDateTime lt) {
            setPublicationDateTime(lt.atZone(ZoneOffset.UTC));
        } else if (t instanceof ZonedDateTime dt) {
            setPublicationDateTime(dt);
        } else if (t instanceof YearMonth m) {
            setPublicationMonth(m);
        } else if (t instanceof Year y) {
            setPublicationYear(y);
        } else {
            throw new IllegalArgumentException("Unsupported temporal: " + t.getClass().getSimpleName() + " => " + t);
        }
    }

    private static LocalDate getLocalDate(LocalDate localDate, ZonedDateTime zonedDateTime) {
        return localDate != null ? localDate : zonedDateTime != null ? zonedDateTime.toLocalDate() : null;
    }

    @JsonIgnore
    public Set<String> getAllCommonsFileNames() {
        return getMetadataStream()
                .flatMap(m -> ofNullable(m.getCommonsFileNames()).orElse(Set.of()).stream()).collect(toSet());
    }

    @Transient
    @JsonIgnore
    public boolean isIgnored() {
        return getMetadataStream().allMatch(fm -> fm.isIgnored() == Boolean.TRUE);
    }

    @Transient
    @JsonIgnore
    public List<String> getIgnoredReasons() {
        return getMetadataStream().map(FileMetadata::getIgnoredReason).filter(Objects::nonNull).distinct().sorted()
                .toList();
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

    public List<String> getSearchTermsInCommons(Collection<FileMetadata> metadata) {
        List<String> result = new ArrayList<>(getIdUsedInCommons());
        ofNullable(getDescription()).map(x -> x.contains(" --- ") ? x.substring(x.indexOf(" --- ") + 5) : x)
                .ifPresent(result::add);
        metadata.stream().map(FileMetadata::getDescription).filter(Objects::nonNull).distinct().sorted()
                .forEach(result::add);
        metadata.stream().map(FileMetadata::getAssetUri).map(Object::toString).distinct().sorted().forEach(result::add);
        return result;
    }

    public Temporal getBestTemporal() {
        return Stream
                .<Temporal>of(getCreationDateTime(), getCreationDate(), getPublicationDateTime(), getPublicationDate(),
                        getPublicationYear())
                .filter(Objects::nonNull).findFirst()
                .orElseThrow(() -> new IllegalStateException("No best temporal found for " + this));
    }

    public Year getYear() {
        return Year.of(getBestTemporal().get(ChronoField.YEAR));
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        Media other = (Media) obj;
        return Objects.equals(id, other.id);
    }

    @JsonIgnore
    public boolean hasAssetsToUpload() {
        return getMetadataStream().anyMatch(FileMetadata::shouldUpload);
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

    @JsonIgnore
    public List<String> getDocumentAssetsToUpload() {
        return getMetadataStream().filter(FileMetadata::shouldUpload).filter(FileMetadata::isDocument)
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
     * Determines if this media is a document.
     *
     * @return {@code true} if this media is a document
     */
    public boolean isDocument() {
        return getMetadataStream().anyMatch(FileMetadata::isDocument);
    }

    /**
     * Determines if this media is a Cloud-Optimized GeoTIFF.
     *
     * @return {@code true} if this media is a Cloud-Optimized GeoTIFF
     */
    public boolean isCOG() {
        return false;
    }

    /**
     * Returns the preview URL to display in UI. Thumbnail if available, otherwise
     * asset.
     *
     * @return the preview URL to display in UI. Thumbnail if available, otherwise
     *         asset.
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
        return isEmpty(commonsFileNames) ? getUploadTitle(metadata) + '.' + metadata.getFileExtensionOnCommons()
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
        setPublicationYear(mediaFromApi.getPublicationYear());
        setThumbnailUrl(mediaFromApi.getThumbnailUrl());
        synchronizeMetadataWith(mediaFromApi);
        if (this instanceof WithKeywords kw && mediaFromApi instanceof WithKeywords kwApi) {
            kw.setKeywords(kwApi.getKeywords());
        }
        if (this instanceof WithLatLon ll && mediaFromApi instanceof WithLatLon llApi) {
            ll.setLatitude(llApi.getLatitude());
            ll.setLongitude(llApi.getLongitude());
        }
    }

    public final void synchronizeMetadataWith(Media mediaFromApi) {
        removeMetadataNotFoundIn(mediaFromApi);
        addMetadataNewlyFoundIn(mediaFromApi, null);
    }

    public final void removeMetadataNotFoundIn(Media mediaFromApi) {
        if (mediaFromApi.hasMetadata()) {
            for (Iterator<FileMetadata> it = getMetadata().iterator(); it.hasNext();) {
                FileMetadata m = it.next();
                if (mediaFromApi.getMetadataStream().map(FileMetadata::getAssetUri)
                        .noneMatch(x -> areSameUris(x, m.getAssetUri()))) {
                    LOGGER.info("Remove database metadata not found anymore in API by asset URI: {}", m);
                    it.remove();
                }
            }
        }
    }

    public final void addMetadataNewlyFoundIn(Media mediaFromApi, Consumer<FileMetadata> modifier) {
        if (mediaFromApi.hasMetadata()) {
            for (FileMetadata apiMetadata : mediaFromApi.getMetadata()) {
                if (getMetadataStream().map(FileMetadata::getAssetUri)
                        .noneMatch(x -> areSameUris(x, apiMetadata.getAssetUri()))) {
                    LOGGER.info("Add API metadata not yet found in database by asset URI: {}", apiMetadata);
                    if (modifier != null) {
                        modifier.accept(apiMetadata);
                    }
                    addMetadata(apiMetadata);
                }
            }
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
                || URI_LIKE.matcher(title).matches() || IMG.matcher(title.toUpperCase(ENGLISH)).matches()
                || IMG2.matcher(title.toUpperCase(ENGLISH)).matches();
    }

    /**
     * Determines if at least one of the given strings is included in title,
     * description or keywords, ignoring spaces and dashes.
     *
     * @param strings strings to search
     * @return {@code true} if one of the given strings is included in title or
     *         description
     */
    public boolean containsInTitleOrDescriptionOrKeywords(String... strings) {
        return Arrays.stream(strings).anyMatch(s -> containsInTitleOrDescriptionOrKeywords(s, true, true, true));
    }

    /**
     * Determines if the given string is included in title, description or keywords,
     * ignoring spaces and dashes.
     *
     * @param string string to search
     * @return {@code true} if the given string is included in title or description
     */
    public boolean containsInTitleOrDescriptionOrKeywords(String string, boolean lookInTitle, boolean lookInDescription,
            boolean lookInKeywords) {
        String lc = normalize(string);
        boolean result = lookInTitle && normalize(title).contains(lc);
        if (!result) {
            result = lookInDescription && normalize(description).contains(lc)
                    || (hasMetadata() && getMetadataStream().map(x -> normalize(x.getDescription()))
                            .filter(StringUtils::isNotBlank).distinct().anyMatch(x -> x.contains(lc)));
        }
        if (!result) {
            result = lookInKeywords && this instanceof WithKeywords mkw
                    && mkw.getKeywordStream().map(Media::normalize).anyMatch(kw -> kw.contains(lc));
        }
        return result;
    }

    public Optional<LocalDate> deduceApproximatePublicationDate() {
        if (getPublicationDate() != null) {
            return Optional.of(getPublicationDate());
        } else if (getPublicationDateTime() != null) {
            return Optional.of(getPublicationDateTime().toLocalDate());
        } else if (getPublicationMonth() != null) {
            return Optional.of(LocalDate.of(getPublicationMonth().getYear(), getPublicationMonth().getMonth(), 1));
        } else if (getPublicationYear() != null) {
            return Optional.of(LocalDate.of(getPublicationYear().getValue(), 1, 1));
        }
        return Optional.empty();
    }

    private static String normalize(String s) {
        return ofNullable(s).orElse("").toLowerCase(ENGLISH).replace(" ", "").replace("-", "");
    }
}
