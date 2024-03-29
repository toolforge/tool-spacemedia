package org.wikimedia.commons.donvip.spacemedia.data.domain.base;

import static jakarta.persistence.GenerationType.SEQUENCE;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.findExtension;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.getNormalizedExtension;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.newURL;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.urlToUriUnchecked;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wikimedia.commons.donvip.spacemedia.utils.HashHelper;
import org.wikimedia.commons.donvip.spacemedia.utils.Utils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity
@Table(indexes = { @Index(columnList = "assetUrl"), @Index(columnList = "sha1, phash") })
public class FileMetadata implements FileMetadataProjection, MediaDescription {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileMetadata.class);

    private static final Set<String> AUDIO_EXTENSIONS = Set.of("wav", "mp3", "flac", "midi");
    private static final Set<String> IMAGE_EXTENSIONS = Set.of("bmp", "jpg", "tiff", "png", "webp", "xcf", "gif",
            "svg", "exr");
    private static final Set<String> VIDEO_EXTENSIONS = Set.of("mp4", "webm", "ogv", "mpeg", "wmv");
    private static final Set<String> DOC_EXTENSIONS = Set.of("pdf", "stl", "epub", "ppt", "pptm", "pptx");

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

    // 540 for
    // https://www.esa.int/var/esa/storage/images/esa_multimedia/images/2014/05/briefing_prasowy_z_udzialem_wiceminister_gospodarki_grazyny_henclewskiej_dr_karlheinza_kreuzberga_z_esa_dyrektora_cnk_roberta_firmhofera_oraz_dyrektora_zpsk_pawla_wojtkiewicza_przedstawiciela_polskiego_sektora_kosmicznego/14500198-1-eng-GB/Briefing_prasowy_z_udzialem_wiceminister_gospodarki_Grazyny_Henclewskiej_Dr_Karlheinza_Kreuzberga_z_ESA_dyrektora_CNK_Roberta_Firmhofera_oraz_dyrektora_ZPSK_Pawla_Wojtkiewicza_przedstawiciela_polskiego_sektora_kosmicznego.jpg
    @Column(nullable = false, length = 540)
    @JsonProperty("asset_url")
    private URL assetUrl;

    @Column(nullable = true)
    private String originalFileName;

    @Column(nullable = true, length = 4)
    private String extension;

    /**
     * File size in bytes.
     */
    @Column(name = "`size`", nullable = true)
    private Long size;

    @Embedded
    private ImageDimensions dimensions;

    @Lob
    @Column(nullable = true, columnDefinition = "MEDIUMTEXT")
    protected String description;

    @OneToOne(cascade = { CascadeType.MERGE, CascadeType.REMOVE, CascadeType.REFRESH })
    @JoinColumn(name = "exif_id", referencedColumnName = "id")
    private ExifMetadata exif;

    @ElementCollection(fetch = FetchType.EAGER)
    @JsonProperty("commons_file_names")
    protected Set<String> commonsFileNames = new HashSet<>();

    @Column(nullable = true)
    protected Boolean ignored;

    @Lob
    @Column(nullable = true, columnDefinition = "TEXT")
    @JsonProperty("ignored_reason")
    protected String ignoredReason;

    public FileMetadata() {

    }

    public FileMetadata(URL assetUrl) {
        this.assetUrl = assetUrl;
        updateFilenameAndExtension(assetUrl.getPath());
    }

    public FileMetadata(String assetUrl) {
        this(newURL(assetUrl));
    }

    public boolean updateFilenameAndExtension(String assetPath) {
        int idx = assetPath.lastIndexOf('/');
        if (idx > -1) {
            assetPath = assetPath.substring(idx + 1);
        }
        String ext = findExtension(assetPath);
        if (ext != null) {
            try {
                setOriginalFileName(URLDecoder.decode(assetPath, "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                LOGGER.error(e.getMessage(), e);
            }
            setExtension(ext);
            return true;
        }
        return false;
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

    @Transient
    @JsonIgnore
    public boolean hasSize() {
        return size != null;
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
        return Boolean.TRUE != ignored && isNotBlank(sha1) && isEmpty(getCommonsFileNames());
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

    public String getOriginalFileName() {
        return originalFileName;
    }

    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = getNormalizedExtension(extension);
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

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(String description) {
        this.description = description;
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

    @Transient
    @JsonIgnore
    public String getFileExtension() {
        if (getExtension() != null) {
            return getNormalizedExtension(getExtension());
        } else if (getAssetUrl() == null) {
            return null;
        }
        return findExtension(getAssetUrl().toExternalForm());
    }

    @Transient
    @JsonIgnore
    public String getFileName() {
        return Utils.getFilename(getAssetUrl());
    }

    @Transient
    @JsonIgnore
    public String getMime() {
        return getMime(getFileExtension());
    }

    public static String getMime(String ext) {
        return ext == null ? null : switch (ext) {
        case "djvu" -> "image/vnd.djvu";
        case "jpe", "jpg", "jpeg" -> "image/jpeg";
        case "tif", "tiff" -> "image/tiff";
        case "bmp", "gif", "png", "webp" -> "image/" + ext;
        case "svg" -> "image/svg+xml";
        case "xcf" -> "image/x-xcf";
        case "midi", "flac", "wav" -> "audio/" + ext;
        case "ogv" -> "video/ogg";
        case "webm" -> "video/" + ext;
        case "pdf" -> "application/pdf";
        case "stl" -> "application/sla";
        case "epub" -> "application/epub+zip";
        default -> null;
        };
    }

    /**
     * Determines if this media is an audio.
     *
     * @return {@code true} if this media is an audio
     */
    @Transient
    @JsonIgnore
    public boolean isAudio() {
        return isOf(AUDIO_EXTENSIONS, "/audio/");
    }

    /**
     * Determines if this media is an image.
     *
     * @return {@code true} if this media is an image
     */
    @Transient
    @JsonIgnore
    public boolean isImage() {
        return isOf(IMAGE_EXTENSIONS, "/image/");
    }

    /**
     * Determines if this media is a video.
     *
     * @return {@code true} if this media is a video
     */
    @Transient
    @JsonIgnore
    public boolean isVideo() {
        return isOf(VIDEO_EXTENSIONS, "/video/");
    }

    /**
     * Determines if this media is a document.
     *
     * @return {@code true} if this media is a document
     */
    @Transient
    @JsonIgnore
    public boolean isDocument() {
        return isOf(DOC_EXTENSIONS, "/document/");
    }

    private boolean isOf(Set<String> extensions, String type) {
        String ext = getFileExtension();
        return ext != null ? extensions.contains(ext) : assetUrl.toString().contains(type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(phash, sha1, readableImage, assetUrl, size, commonsFileNames, exif, dimensions, extension,
                description, originalFileName);
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
                && Objects.equals(dimensions, other.dimensions) && Objects.equals(extension, other.extension)
                && Objects.equals(description, other.description)
                && Objects.equals(originalFileName, other.originalFileName);
    }

    @Override
    public String toString() {
        return "FileMetadata [id=" + id + ", " + (sha1 != null ? "sha1=" + sha1 + ", " : "")
                + (phash != null ? "phash=" + phash + ", " : "")
                + (dimensions != null ? "dimensions=" + dimensions + ", " : "")
                + (extension != null ? "extension=" + extension + ", " : "")
                + (readableImage != null ? "readableImage=" + readableImage + ", " : "")
                + (assetUrl != null ? "assetUrl=" + assetUrl + ", " : "") + (size != null ? "size=" + size + ", " : "")
                + (exif != null ? "exif=" + exif + ", " : "")
                + (originalFileName != null ? "originalFileName=" + originalFileName + ", " : "")
                + (commonsFileNames != null ? "commonsFileNames=" + commonsFileNames : "") + ']';
    }
}
