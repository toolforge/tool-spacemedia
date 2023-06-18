package org.wikimedia.commons.donvip.spacemedia.data.domain.base;

import static javax.persistence.GenerationType.SEQUENCE;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.urlToUriUnchecked;

import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.wikimedia.commons.donvip.spacemedia.utils.HashHelper;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
@Table(indexes = { @Index(columnList = "assetUrl"), @Index(columnList = "sha1, phash") })
public class FileMetadata implements FileMetadataProjection {

    private static final Set<String> AUDIO_EXTENSIONS = Set.of("wav", "mp3", "flac", "midi");
    private static final Set<String> IMAGE_EXTENSIONS = Set.of("bmp", "jpg", "tiff", "png", "webp", "xcf", "gif",
            "svg");
    private static final Set<String> VIDEO_EXTENSIONS = Set.of("mp4", "webm", "ogv", "mpeg");

    @Id
    @JsonIgnore
    @GeneratedValue(strategy = SEQUENCE, generator = "file_metadata_sequence")
    private Long id;

    /**
     * SHA-1 hash.
     */
    @Column(nullable = true, length = 42)
    private String sha1;

    /**
     * Perceptual hash.
     */
    @Column(nullable = true, columnDefinition = "VARCHAR(52)", length = 52)
    private String phash;

    /**
     * Determines if the image is readable:
     * <ul>
     * <li>TRUE: image is readable</li>
     * <li>FALSE: image is not readable (error occured during reading)</li>
     * <li>NULL: status not determined yet, or not an image</li>
     * </ul>
     */
    @Column(nullable = true)
    @JsonProperty("readable_image")
    private Boolean readableImage;

    @Column(nullable = false, length = 380)
    @JsonProperty("asset_url")
    private URL assetUrl;

    @Column(nullable = true, length = 4)
    private String extension;

    /**
     * File size in bytes.
     */
    @Column(name = "`size`", nullable = true)
    private Long size;

    @Embedded
    private ImageDimensions dimensions;

    @OneToOne
    @JoinColumn(name = "exif_id", referencedColumnName = "id")
    private ExifMetadata exif;

    @ElementCollection(fetch = FetchType.EAGER)
    @JsonProperty("commons_file_names")
    protected Set<String> commonsFileNames = new HashSet<>();

    public FileMetadata() {

    }

    public FileMetadata(URL assetUrl) {
        this.assetUrl = assetUrl;
    }

    public FileMetadata(String assetUrl) throws MalformedURLException {
        this(new URL(assetUrl));
    }

    public String getSha1() {
        return sha1;
    }

    public void setSha1(String sha1) {
        this.sha1 = sha1;
    }

    @Transient
    @JsonIgnore
    public boolean hasSha1() {
        return sha1 != null;
    }

    @Override
    public String getPhash() {
        return phash;
    }

    public void setPhash(String phash) {
        this.phash = phash;
    }

    @Transient
    @JsonIgnore
    public boolean hasPhash() {
        return phash != null;
    }

    @Transient
    public BigInteger getPerceptualHash() {
        return HashHelper.decode(getPhash());
    }

    @Transient
    public void setPerceptualHash(BigInteger phash) {
        setPhash(HashHelper.encode(phash));
    }

    @Transient
    public boolean shouldUpload() {
        return isNotBlank(sha1) && isEmpty(getCommonsFileNames());
    }

    public Boolean isReadableImage() {
        return readableImage;
    }

    public void setReadableImage(Boolean readableImage) {
        this.readableImage = readableImage;
    }

    @Transient
    @JsonIgnore
    public URI getAssetUri() {
        return urlToUriUnchecked(assetUrl);
    }

    public URL getAssetUrl() {
        return assetUrl;
    }

    public void setAssetUrl(URL assetUrl) {
        this.assetUrl = assetUrl;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public ImageDimensions getImageDimensions() {
        return dimensions;
    }

    public void setImageDimensions(ImageDimensions dimensions) {
        this.dimensions = dimensions;
    }

    @Transient
    @JsonIgnore
    public boolean hasValidDimensions() {
        return dimensions != null && dimensions.isValid();
    }

    public ExifMetadata getExif() {
        return exif;
    }

    public void setExif(ExifMetadata exif) {
        this.exif = exif;
    }

    public Set<String> getCommonsFileNames() {
        return commonsFileNames;
    }

    public void setCommonsFileNames(Set<String> commonsFileNames) {
        this.commonsFileNames = commonsFileNames;
    }

    @Transient
    @JsonIgnore
    public String getFileExtension() {
        if (getExtension() != null) {
            return getExtension();
        } else if (getAssetUrl() == null) {
            return null;
        }
        String url = getAssetUrl().toExternalForm();
        String ext = url.substring(url.lastIndexOf('.') + 1).toLowerCase(Locale.ENGLISH);
        switch (ext) {
        case "apng":
            return "png";
        case "djv":
            return "djvu";
        case "jpe", "jpeg", "jps":
            return "jpg"; // Use the same extension as flickr2commons as it solely relies on filenames
        case "tif":
            return "tiff";
        case "mid", "kar":
            return "midi";
        case "mpe", "mpg":
            return "mpeg";
        default:
            return ext;
        }
    }

    @Transient
    @JsonIgnore
    public Set<String> getFileExtensions() {
        String ext = getFileExtension();
        switch (ext) {
        case "jpg":
            return Set.of("jpg", "jpeg");
        case "tiff":
            return Set.of("tif", "tiff");
        default:
            return Set.of(ext);
        }
    }

    @Transient
    @JsonIgnore
    public String getMime() {
        return getMime(getFileExtension());
    }

    public static String getMime(String ext) {
        switch (ext) {
        case "djvu":
            return "image/vnd.djvu";
        case "jpg", "jpeg":
            return "image/jpeg";
        case "tif", "tiff":
            return "image/tiff";
        case "bmp", "gif", "png", "webp":
            return "image/" + ext;
        case "svg":
            return "image/svg+xml";
        case "xcf":
            return "image/x-xcf";
        case "midi", "flac", "wav":
            return "audio/" + ext;
        case "ogv":
            return "video/ogg";
        case "webm":
            return "video/" + ext;
        case "pdf":
            return "application/pdf";
        case "stl":
            return "application/sla";
        default:
            return null;
        }
    }

    /**
     * Determines if this media is an audio.
     *
     * @return {@code true} if this media is an audio
     */
    @Transient
    @JsonIgnore
    public boolean isAudio() {
        String ext = getFileExtension();
        return ext != null && AUDIO_EXTENSIONS.contains(ext);
    }

    /**
     * Determines if this media is an image.
     *
     * @return {@code true} if this media is an image
     */
    @Transient
    @JsonIgnore
    public boolean isImage() {
        String ext = getFileExtension();
        return ext != null && IMAGE_EXTENSIONS.contains(ext);
    }

    /**
     * Determines if this media is a video.
     *
     * @return {@code true} if this media is a video
     */
    @Transient
    @JsonIgnore
    public boolean isVideo() {
        String ext = getFileExtension();
        return ext != null && VIDEO_EXTENSIONS.contains(ext);
    }

    @Override
    public int hashCode() {
        return Objects.hash(phash, sha1, readableImage, assetUrl, size, commonsFileNames, exif, dimensions);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        FileMetadata other = (FileMetadata) obj;
        return size == other.size && Objects.equals(phash, other.phash) && Objects.equals(sha1, other.sha1)
                && Objects.equals(readableImage, other.readableImage) && Objects.equals(assetUrl, other.assetUrl)
                && Objects.equals(commonsFileNames, other.commonsFileNames) && Objects.equals(exif, other.exif)
                && Objects.equals(dimensions, other.dimensions);
    }

    @Override
    public String toString() {
        return "FileMetadata [" + (sha1 != null ? "sha1=" + sha1 + ", " : "")
                + (phash != null ? "phash=" + phash + ", " : "")
                + (dimensions != null ? "dimensions=" + dimensions + ", " : "")
                + (readableImage != null ? "readableImage=" + readableImage + ", " : "")
                + (assetUrl != null ? "assetUrl=" + assetUrl + ", " : "") + (size != null ? "size=" + size + ", " : "")
                + (commonsFileNames != null ? "commonsFileNames=" + commonsFileNames : "") + ']';
    }
}
