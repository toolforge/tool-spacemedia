package org.wikimedia.commons.donvip.spacemedia.data.commons;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;

/**
 * <a href="https://www.mediawiki.org/wiki/Manual:Filearchive_table">Mediawiki
 * File Archive table</a>
 *
 * <pre>
 * +----------------------+-------------------------------------------------------------------------------------------------------------+------+-----+----------------+----------------+
 * | Field                | Type                                                                                                        | Null | Key | Default        | Extra          |
 * +----------------------+-------------------------------------------------------------------------------------------------------------+------+-----+----------------+----------------+
 * | fa_id                | int(11)                                                                                                     | NO   | PRI | NULL           | auto_increment |
 * | fa_name              | varbinary(255)                                                                                              | NO   | MUL |                |                |
 * | fa_archive_name      | varbinary(255)                                                                                              | YES  |     |                |                |
 * | fa_storage_group     | varbinary(16)                                                                                               | YES  | MUL | NULL           |                |
 * | fa_storage_key       | varbinary(64)                                                                                               | YES  |     |                |                |
 * | fa_deleted_user      | int(11)                                                                                                     | YES  |     | NULL           |                |
 * | fa_deleted_timestamp | binary(14)                                                                                                  | YES  | MUL |                |                |
 * | fa_deleted_reason_id | bigint(20) unsigned                                                                                         | NO   |     | NULL           |                |
 * | fa_size              | int(10) unsigned                                                                                            | YES  |     | 0              |                |
 * | fa_width             | int(11)                                                                                                     | YES  |     | 0              |                |
 * | fa_height            | int(11)                                                                                                     | YES  |     | 0              |                |
 * | fa_metadata          | mediumblob                                                                                                  | YES  |     | NULL           |                |
 * | fa_bits              | int(11)                                                                                                     | YES  |     | 0              |                |
 * | fa_media_type        | enum('UNKNOWN','BITMAP','DRAWING','AUDIO','VIDEO','MULTIMEDIA','OFFICE','TEXT','EXECUTABLE','ARCHIVE','3D') | YES  |     | NULL           |                |
 * | fa_major_mime        | enum('unknown','application','audio','image','text','video','message','model','multipart','chemical')       | YES  |     | unknown        |                |
 * | fa_minor_mime        | varbinary(100)                                                                                              | YES  |     | unknown        |                |
 * | fa_description_id    | bigint(20) unsigned                                                                                         | NO   |     | NULL           |                |
 * | fa_actor             | bigint(20) unsigned                                                                                         | NO   | MUL | 0              |                |
 * | fa_timestamp         | binary(14)                                                                                                  | YES  |     |                |                |
 * | fa_deleted           | tinyint(3) unsigned                                                                                         | NO   |     | 0              |                |
 * | fa_sha1              | varbinary(32)                                                                                               | NO   | MUL |                |                |
 * +----------------------+-------------------------------------------------------------------------------------------------------------+------+-----+----------------+----------------+
 * </pre>
 */
@Entity
@Table(name = "filearchive")
public class CommonsFileArchive {

    @Id
    @GeneratedValue
    @Column(name = "fa_id", nullable = false, length = 11)
    private int id;

    @Column(name = "fa_name", nullable = false, length = 255, columnDefinition = "VARBINARY")
    private String name;

    @Column(name = "fa_archive_name", nullable = true, length = 255, columnDefinition = "VARBINARY")
    private String archiveName;

    @Column(name = "fa_storage_group", nullable = true, length = 16, columnDefinition = "VARBINARY")
    private String storageGroup;

    @Column(name = "fa_storage_key", nullable = true, length = 64, columnDefinition = "VARBINARY")
    private String storageKey;

    @Column(name = "fa_deleted_user", nullable = true, length = 11)
    private Integer deletedUser;

    @Column(name = "fa_deleted_timestamp", nullable = true, length = 14, columnDefinition = "BINARY")
    private LocalDateTime deletedTimestamp;

    @Column(name = "fa_deleted_reason_id", nullable = false, length = 20, columnDefinition = "BIGINT")
    private long deletedReasonId;

    @Column(name = "fa_size", nullable = true, length = 10)
    private Integer size;

    @Column(name = "fa_width", nullable = true, length = 11)
    private Integer width;

    @Column(name = "fa_height", nullable = true, length = 11)
    private Integer height;

    @Lob
    @Column(name = "fa_metadata", nullable = true)
    private String metadata;

    @Column(name = "fa_bits", nullable = true, length = 11)
    private int bits;

    @Column(name = "fa_media_type", nullable = true)
    @Convert(converter = CommonsMediaTypeConverter.class)
    private CommonsMediaType mediaType;

    @Enumerated(EnumType.STRING)
    @Column(name = "fa_major_mime", nullable = true)
    private CommonsMajorMime majorMime;

    @Column(name = "fa_minor_mime", nullable = true, length = 100, columnDefinition = "VARBINARY")
    private String minorMime;

    @Column(name = "fa_description_id", nullable = false, length = 20, columnDefinition = "BIGINT")
    private long descriptionId;

    @Column(name = "fa_actor", nullable = false, length = 20, columnDefinition = "BIGINT")
    private long actor;

    @Column(name = "fa_timestamp", nullable = true, length = 14, columnDefinition = "BINARY")
    private LocalDateTime timestamp;

    @Column(name = "fa_deleted", nullable = false, length = 3, columnDefinition = "TINYINT")
    private Boolean deleted;

    @Column(name = "fa_sha1", nullable = false, length = 32, columnDefinition = "VARBINARY")
    private String sha1;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getArchiveName() {
        return archiveName;
    }

    public void setArchiveName(String archiveName) {
        this.archiveName = archiveName;
    }

    public String getStorageGroup() {
        return storageGroup;
    }

    public void setStorageGroup(String storageGroup) {
        this.storageGroup = storageGroup;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public void setStorageKey(String storageKey) {
        this.storageKey = storageKey;
    }

    public Integer getDeletedUser() {
        return deletedUser;
    }

    public void setDeletedUser(Integer deletedUser) {
        this.deletedUser = deletedUser;
    }

    public LocalDateTime getDeletedTimestamp() {
        return deletedTimestamp;
    }

    public void setDeletedTimestamp(LocalDateTime deletedTimestamp) {
        this.deletedTimestamp = deletedTimestamp;
    }

    public long getDeletedReasonId() {
        return deletedReasonId;
    }

    public void setDeletedReasonId(long deletedReasonId) {
        this.deletedReasonId = deletedReasonId;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
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

    public long getDescriptionId() {
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

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }

    public String getSha1() {
        return sha1;
    }

    public void setSha1(String sha1) {
        this.sha1 = sha1;
    }

    @Override
    public String toString() {
        return "CommonsFileArchive [id=" + id + ", " + (name != null ? "name=" + name + ", " : "")
                + (archiveName != null ? "archiveName=" + archiveName + ", " : "")
                + (storageGroup != null ? "storageGroup=" + storageGroup + ", " : "")
                + (storageKey != null ? "storageKey=" + storageKey + ", " : "")
                + (deletedUser != null ? "deletedUser=" + deletedUser + ", " : "")
                + (deletedTimestamp != null ? "deletedTimestamp=" + deletedTimestamp + ", " : "") + "deletedReasonId="
                + deletedReasonId + ", " + (size != null ? "size=" + size + ", " : "")
                + (width != null ? "width=" + width + ", " : "") + (height != null ? "height=" + height + ", " : "")
                + (metadata != null ? "metadata=" + metadata + ", " : "") + "bits=" + bits + ", "
                + (mediaType != null ? "mediaType=" + mediaType + ", " : "")
                + (majorMime != null ? "majorMime=" + majorMime + ", " : "")
                + (minorMime != null ? "minorMime=" + minorMime + ", " : "") + "descriptionId=" + descriptionId + ", "
                + "actor=" + actor + ", "
                + (timestamp != null ? "timestamp=" + timestamp + ", " : "")
                + (deleted != null ? "deleted=" + deleted + ", " : "") + (sha1 != null ? "sha1=" + sha1 : "") + "]";
    }
}
