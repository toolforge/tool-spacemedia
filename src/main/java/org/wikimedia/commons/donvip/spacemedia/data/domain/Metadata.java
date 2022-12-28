package org.wikimedia.commons.donvip.spacemedia.data.domain;

import java.math.BigInteger;
import java.net.URL;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Transient;

import org.wikimedia.commons.donvip.spacemedia.utils.HashHelper;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

@Embeddable
public class Metadata implements MetadataProjection {

    private static final Set<String> AUDIO_EXTENSIONS = Set.of("wav", "mp3", "flac", "midi");
    private static final Set<String> IMAGE_EXTENSIONS = Set.of("jpeg", "tiff", "png", "webp", "xcf", "gif", "svg");
    private static final Set<String> VIDEO_EXTENSIONS = Set.of("mp4", "webm", "ogv", "mpeg");

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

    /**
     * File size in bytes.
     */
    @Column(name = "`size`", nullable = true)
    private Long size;

    public String getSha1() {
        return sha1;
    }

    public void setSha1(String sha1) {
        this.sha1 = sha1;
    }

    @Override
    public String getPhash() {
        return phash;
    }

    public void setPhash(String phash) {
        this.phash = phash;
    }

    @Transient
    public BigInteger getPerceptualHash() {
        return HashHelper.decode(getPhash());
    }

    @Transient
    public void setPerceptualHash(BigInteger phash) {
        setPhash(HashHelper.encode(phash));
    }

    public Boolean isReadableImage() {
        return readableImage;
    }

    public void setReadableImage(Boolean readableImage) {
        this.readableImage = readableImage;
    }

    public URL getAssetUrl() {
        return assetUrl;
    }

    public void setAssetUrl(URL assetUrl) {
        this.assetUrl = assetUrl;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    @Transient
    @JsonIgnore
    public String getFileExtension() {
        if (getAssetUrl() == null) {
            return null;
        }
        String url = getAssetUrl().toExternalForm();
        String ext = url.substring(url.lastIndexOf('.') + 1).toLowerCase(Locale.ENGLISH);
        switch (ext) {
            case "jpg": return "jpeg";
            case "tif": return "tiff";
            default: return ext;
        }
    }

    @Transient
    @JsonIgnore
    public Set<String> getFileExtensions() {
        String ext = getFileExtension();
        switch (ext) {
        case "jpeg":
            return Set.of("jpg", "jpeg");
        case "tiff":
            return Set.of("tif", "tiff");
        default:
            return Set.of(ext);
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
        return Objects.hash(phash, sha1, readableImage, assetUrl, size);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        Metadata other = (Metadata) obj;
        return size == other.size && Objects.equals(phash, other.phash) && Objects.equals(sha1, other.sha1)
                && Objects.equals(readableImage, other.readableImage) && Objects.equals(assetUrl, other.assetUrl);
    }

    @Override
    public String toString() {
        return "Metadata [" + (sha1 != null ? "sha1=" + sha1 + ", " : "")
                + (phash != null ? "phash=" + phash + ", " : "")
                + (readableImage != null ? "readableImage=" + readableImage + ", " : "")
                + (assetUrl != null ? "assetUrl=" + assetUrl + ", " : "") + (size != null ? "size=" + size : "") + "]";
    }
}
