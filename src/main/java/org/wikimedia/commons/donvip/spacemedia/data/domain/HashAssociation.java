package org.wikimedia.commons.donvip.spacemedia.data.domain;

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;

@Entity
@Table(indexes = { @Index(columnList = "phash") })
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

    public HashAssociation() {
        // Default constructor
    }

    /**
     * Constructs a new {@code HashAssociation}
     *
     * @param sha1  base 36 SHA-1
     * @param phash perceptual hash
     */
    public HashAssociation(String sha1, String phash) {
        this.sha1 = sha1;
        this.phash = phash;
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
                + (phash != null ? "phash=" + phash : "") + "]";
    }
}
