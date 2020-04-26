package org.wikimedia.commons.donvip.spacemedia.data.domain;

import java.math.BigInteger;
import java.net.URL;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class Metadata {

    /**
     * SHA-1 hash.
     */
    @Column(nullable = false, length = 42)
    private String sha1;

    /**
     * Perceptual hash.
     */
    @Column(nullable = true)
    private BigInteger phash;

    /**
     * Determines if the image is readable:
     * <ul>
     * <li>TRUE: image is readable</li>
     * <li>FALSE: image is not readable (error occured during reading)</li>
     * <li>NULL: status not determined yet, or not an image</li>
     * </ul>
     */
    @Column(nullable = true)
    private Boolean readableImage;

    @Column(nullable = false, length = 380)
    protected URL assetUrl;

    public String getSha1() {
        return sha1;
    }

    public void setSha1(String sha1) {
        this.sha1 = sha1;
    }

    public BigInteger getPhash() {
        return phash;
    }

    public void setPhash(BigInteger phash) {
        this.phash = phash;
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

    @Override
    public int hashCode() {
        return Objects.hash(phash, sha1, readableImage, assetUrl);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        Metadata other = (Metadata) obj;
        return Objects.equals(phash, other.phash) && Objects.equals(sha1, other.sha1)
                && Objects.equals(readableImage, other.readableImage) && Objects.equals(assetUrl, other.assetUrl);
    }

    @Override
    public String toString() {
        return "Metadata [sha1=" + sha1 + ", assetUrl=" + assetUrl + (phash != null ? ", phash=" + phash : "") + "]";
    }
}