package org.wikimedia.commons.donvip.spacemedia.data.domain.s3;

import java.io.Serializable;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class S3MediaId implements Serializable {

    private static final long serialVersionUID = 1L;

    /** The name of the bucket in which this object is stored */
    @Column(nullable = false, length = 64)
    private String bucketName;

    /** The objectKey under which this object is stored */
    @Column(nullable = false, length = 512)
    private String objectKey;

    public S3MediaId() {

    }

    public S3MediaId(String bucketName, String key) {
        this.bucketName = bucketName;
        this.objectKey = key;
    }

    public S3MediaId(String jsonId) {
        String[] tab = jsonId.split(":");
        this.bucketName = tab[0];
        this.objectKey = tab[1];
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public void setObjectKey(String objectKey) {
        this.objectKey = objectKey;
    }

    public String getUrl() {
        return "http://" + bucketName + ".s3.amazonaws.com/" + objectKey;
    }

    @Override
    public int hashCode() {
        return Objects.hash(bucketName, objectKey);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        S3MediaId other = (S3MediaId) obj;
        return Objects.equals(bucketName, other.bucketName) && Objects.equals(objectKey, other.objectKey);
    }

    @Override
    public String toString() {
        return bucketName + ':' + objectKey;
    }
}
