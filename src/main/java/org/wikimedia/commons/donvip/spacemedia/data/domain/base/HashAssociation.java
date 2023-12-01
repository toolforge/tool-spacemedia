package org.wikimedia.commons.donvip.spacemedia.data.domain.base;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(indexes = { @Index(columnList = "phash"), @Index(columnList = "phash,mime"), @Index(columnList = "sha1,mime") })
public class HashAssociation {

    /**
     * SHA-1 hash, encoded in base 36 (31 characters).
     */
    @Id
    @Column(nullable = false, length = 32)
    private String sha1;

    /**
     * Perceptual hash.
     */
    @Column(nullable = true, columnDefinition = "VARCHAR(52)", length = 52)
    private String phash;

    /**
     * MIME type.
     */
    @Column(nullable = true, columnDefinition = "VARCHAR(16)", length = 16)
    private String mime;

    public HashAssociation() {
        // Default constructor
    }

    /**
     * Constructs a new {@code HashAssociation}
     *
     * @param sha1  base 36 SHA-1
     * @param phash perceptual hash
     * @param mime  MIME type
     */
    public HashAssociation(String sha1, String phash, String mime) {
        this.sha1 = sha1;
        this.phash = phash;
        this.mime = mime;
    }

    /**
     * Returns the base36 SHA-1.
     *
     * @return the base36 SHA-1
     */
    public String getSha1() {
        return sha1;
    }

    /**
     * Sets the base36 SHA-1.
     *
     * @param sha1 the base36 SHA-1
     */
    public void setSha1(String sha1) {
        this.sha1 = sha1;
    }

    public String getPhash() {
        return phash;
    }

    public void setPhash(String phash) {
        this.phash = phash;
    }

    public String getMime() {
        return mime;
    }

    public void setMime(String mime) {
        this.mime = mime;
    }

    @Override
    public int hashCode() {
        return Objects.hash(sha1);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        HashAssociation other = (HashAssociation) obj;
        return Objects.equals(sha1, other.sha1);
    }

    @Override
    public String toString() {
        return "HashAssociation [" + (sha1 != null ? "sha1=" + sha1 + ", " : "")
                + (phash != null ? "phash=" + phash : "") + (mime != null ? "mime=" + mime : "") + "]";
    }
}
