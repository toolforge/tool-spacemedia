package org.wikimedia.commons.donvip.spacemedia.data.jpa.entity;

import java.io.Serializable;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Embeddable;

import org.apache.commons.codec.digest.DigestUtils;

@Embeddable
public class MetadataKey implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "ctx", length = 50)
    private String context;

    @Column(name = "k", length = 50)
    private String key;

    @Column(name = "sha1", length = 42)
    private String sha1;

    public MetadataKey() {

    }

    public MetadataKey(String context, String key, String sha1) {
        this.context = context;
        this.key = key;
        this.sha1 = sha1;
    }

    public static MetadataKey from(String context, String key, String value) {
        return new MetadataKey(context, key, DigestUtils.sha1Hex(value));
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getSha1() {
        return sha1;
    }

    public void setSha1(String sha1) {
        this.sha1 = sha1;
    }

    @Override
    public int hashCode() {
        return Objects.hash(context, key, sha1);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        MetadataKey other = (MetadataKey) obj;
        return Objects.equals(context, other.context) && Objects.equals(key, other.key)
                && Objects.equals(sha1, other.sha1);
    }

    @Override
    public String toString() {
        return "MetadataKey [" + (context != null ? "context=" + context + ", " : "")
                + (key != null ? "key=" + key + ", " : "") + (sha1 != null ? "sha1=" + sha1 : "") + "]";
    }
}
