package org.wikimedia.commons.donvip.spacemedia.data.commons;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;

@Entity(name = "oldimage")
public class CommonsOldImage {
    @Id
    @Column(name = "oi_name", nullable = false, length = 255)
    private String name;
    @Column(name = "oi_archive_name", nullable = false, length = 255)
    private String archiveName;
    @Column(name = "oi_size", nullable = false, length = 8)
    private int size;
    @Column(name = "oi_width", nullable = false, length = 5)
    private int width;
    @Column(name = "oi_height", nullable = false, length = 5)
    private int height;
    @Column(name = "oi_metadata", nullable = false)
    private String metadata;
    @Column(name = "oi_bits", nullable = false, length = 3)
    private int bits;
    @Column(name = "oi_media_type", nullable = true)
    @Enumerated(EnumType.STRING)
    private CommonsMediaType mediaType;
    @Column(name = "oi_major_mime", nullable = false)
    @Enumerated(EnumType.STRING)
    private CommonsMajorMime majorMime; 
    @Column(name = "oi_minor_mime", nullable = false, length = 100)
    private byte[] minorMime;
    @Column(name = "oi_description_id", nullable = false, precision = 20, scale = 0)
    private double descriptionId;
    @Column(name = "oi_actor", nullable = false, length = 20)
    private long actor;
    @Column(name = "oi_timestamp", nullable = false, length = 14)
    private byte[] timestamp;
    @Column(name = "oi_sha1", nullable = false, length = 32, columnDefinition = "VARBINARY")
    private String sha1;
    @Column(name = "oi_deleted", nullable = false, length = 3)
    private int deleted;

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public int getSize() {
        return size;
    }
    public void setSize(int size) {
        this.size = size;
    }
    public int getWidth() {
        return width;
    }
    public void setWidth(int width) {
        this.width = width;
    }
    public int getHeight() {
        return height;
    }
    public void setHeight(int height) {
        this.height = height;
    }
    public String getMetadata() {
        return metadata;
    }
    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }
    public int getBits() {
        return bits;
    }
    public void setBits(int bits) {
        this.bits = bits;
    }
    public CommonsMediaType getMediaType() {
        return mediaType;
    }
    public void setMediaType(CommonsMediaType mediaType) {
        this.mediaType = mediaType;
    }
    public CommonsMajorMime getMajorMime() {
        return majorMime;
    }
    public void setMajorMime(CommonsMajorMime majorMime) {
        this.majorMime = majorMime;
    }
    public byte[] getMinorMime() {
        return minorMime;
    }
    public void setMinorMime(byte[] minorMime) {
        this.minorMime = minorMime;
    }
    public double getDescriptionId() {
        return descriptionId;
    }
    public void setDescriptionId(double descriptionId) {
        this.descriptionId = descriptionId;
    }
    public long getActor() {
        return actor;
    }
    public void setActor(long actor) {
        this.actor = actor;
    }
    public byte[] getTimestamp() {
        return timestamp;
    }
    public void setTimestamp(byte[] timestamp) {
        this.timestamp = timestamp;
    }
    public String getSha1() {
        return sha1;
    }
    public void setSha1(String sha1) {
        this.sha1 = sha1;
    }
    public String getArchiveName() {
        return archiveName;
    }
    public void setArchiveName(String archiveName) {
        this.archiveName = archiveName;
    }
    public int getDeleted() {
        return deleted;
    }
    public void setDeleted(int deleted) {
        this.deleted = deleted;
    }
}
