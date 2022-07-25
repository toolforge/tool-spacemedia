package org.wikimedia.commons.donvip.spacemedia.data.commons;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;

/**
 * <a href="https://www.mediawiki.org/wiki/Manual:Image_table">Mediawiki Image
 * table</a>
 *
 * <pre>
 * +--------------------+-------------------------------------------------------------------------------------------------------------+------+-----+---------+-------+
 * | Field              | Type                                                                                                        | Null | Key | Default | Extra |
 * +--------------------+-------------------------------------------------------------------------------------------------------------+------+-----+---------+-------+
 * | img_name           | varbinary(255)                                                                                              | NO   | PRI |         |       |
 * | img_size           | int(10) unsigned                                                                                            | NO   | MUL | 0       |       |
 * | img_width          | int(11)                                                                                                     | NO   |     | 0       |       |
 * | img_height         | int(11)                                                                                                     | NO   |     | 0       |       |
 * | img_metadata       | mediumblob                                                                                                  | NO   |     | NULL    |       |
 * | img_bits           | int(11)                                                                                                     | NO   |     | 0       |       |
 * | img_media_type     | enum('UNKNOWN','BITMAP','DRAWING','AUDIO','VIDEO','MULTIMEDIA','OFFICE','TEXT','EXECUTABLE','ARCHIVE','3D') | YES  | MUL | NULL    |       |
 * | img_major_mime     | enum('unknown','application','audio','image','text','video','message','model','multipart','chemical')       | NO   |     | unknown |       |
 * | img_minor_mime     | varbinary(100)                                                                                              | NO   |     | unknown |       |
 * | img_description_id | bigint(20) unsigned                                                                                         | NO   |     | NULL    |       |
 * | img_user           | int(10) unsigned                                                                                            | NO   | MUL | 0       |       |
 * | img_user_text      | varbinary(255)                                                                                              | NO   | MUL |         |       |
 * | img_actor          | bigint(20) unsigned                                                                                         | NO   | MUL | 0       |       |
 * | img_timestamp      | varbinary(14)                                                                                               | NO   | MUL |         |       |
 * | img_sha1           | varbinary(32)                                                                                               | NO   | MUL |         |       |
 * +--------------------+-------------------------------------------------------------------------------------------------------------+------+-----+---------+-------+
 * </pre>
 */
@Entity
@Table(name = "image")
public class CommonsImage {
    @Id
    @Column(name = "img_name", nullable = false, length = 255, columnDefinition = "VARBINARY")
    private String name;
    @Column(name = "img_size", nullable = false, length = 10)
    private long size;
    @Column(name = "img_width", nullable = false, length = 5)
    private int width;
    @Column(name = "img_height", nullable = false, length = 5)
    private int height;
    @Lob
    @Column(name = "img_metadata", nullable = false)
    private String metadata;
    @Column(name = "img_bits", nullable = false, length = 3)
    private int bits;
    @Column(name = "img_media_type", nullable = true)
    @Enumerated(EnumType.STRING)
    private CommonsMediaType mediaType;
    @Column(name = "img_major_mime", nullable = false)
    @Enumerated(EnumType.STRING)
    private CommonsMajorMime majorMime;
    @Column(name = "img_minor_mime", nullable = false, length = 100, columnDefinition = "VARBINARY")
    private String minorMime;
    @Column(name = "img_description_id", nullable = false, length = 20, columnDefinition = "BIGINT")
    private long descriptionId;
    @Column(name = "img_actor", nullable = false, length = 20, columnDefinition = "BIGINT")
    private long actor;
    @Column(name = "img_timestamp", nullable = false, length = 14)
    private String timestamp;
    @Column(name = "img_sha1", nullable = false, length = 32, columnDefinition = "VARBINARY")
    private String sha1;

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
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

    public String getMinorMime() {
        return minorMime;
    }

    public void setMinorMime(String minorMime) {
        this.minorMime = minorMime;
    }
    public double getDescriptionId() {
        return descriptionId;
    }
    public void setDescriptionId(long descriptionId) {
        this.descriptionId = descriptionId;
    }
    public long getActor() {
        return actor;
    }
    public void setActor(long actor) {
        this.actor = actor;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
    public String getSha1() {
        return sha1;
    }
    public void setSha1(String sha1) {
        this.sha1 = sha1;
    }

    @Override
    public String toString() {
        return "CommonsImage [" + (name != null ? "name=" + name + ", " : "") + "size=" + size + ", width=" + width
                + ", height=" + height + ", " + (metadata != null ? "metadata=" + metadata + ", " : "") + "bits=" + bits
                + ", " + (mediaType != null ? "mediaType=" + mediaType + ", " : "")
                + (majorMime != null ? "majorMime=" + majorMime + ", " : "")
                + (minorMime != null ? "minorMime=" + minorMime + ", " : "") + "descriptionId="
                + descriptionId + ", actor=" + actor + ", "
                + (timestamp != null ? "timestamp=" + timestamp + ", " : "")
                + (sha1 != null ? "sha1=" + sha1 : "") + "]";
    }
}
