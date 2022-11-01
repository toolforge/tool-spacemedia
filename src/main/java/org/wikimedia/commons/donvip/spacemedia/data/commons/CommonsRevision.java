package org.wikimedia.commons.donvip.spacemedia.data.commons;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;

/**
 * <a href="https://www.mediawiki.org/wiki/Manual:Revision_table">Mediawiki
 * Revision table</a>
 *
 * <pre>
 * +----------------+---------------------+------+-----+---------+----------------+
 * | Field          | Type                | Null | Key | Default | Extra          |
 * +----------------+---------------------+------+-----+---------+----------------+
 * | rev_id         | int(10) unsigned    | NO   | PRI | NULL    | auto_increment |
 * | rev_page       | int(10) unsigned    | NO   | MUL | NULL    |                |
 * | rev_comment_id | bigint(20) unsigned | NO   |     | 0       |                |
 * | rev_actor      | bigint(20) unsigned | NO   | MUL | 0       |                |
 * | rev_timestamp  | binary(14)          | NO   | MUL | NULL    |                |
 * | rev_minor_edit | tinyint(3) unsigned | NO   |     | 0       |                |
 * | rev_deleted    | tinyint(3) unsigned | NO   |     | 0       |                |
 * | rev_len        | int(10) unsigned    | YES  |     | NULL    |                |
 * | rev_parent_id  | int(10) unsigned    | YES  |     | NULL    |                |
 * | rev_sha1       | varbinary(32)       | NO   |     |         |                |
 * +----------------+---------------------+------+-----+---------+----------------+
 * </pre>
 */
@Entity
@Table(name = "revision")
public class CommonsRevision {

    @Id
    @Column(name = "rev_id", nullable = false, length = 10)
    private int id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rev_page", nullable = false)
    private CommonsPage page;

    @Column(name = "rev_comment_id", nullable = false, length = 20, columnDefinition = "BIGINT")
    private long commentId;

    @Column(name = "rev_actor", nullable = false, length = 20, columnDefinition = "BIGINT")
    private long actor;

    @Column(name = "rev_timestamp", nullable = true, length = 14, columnDefinition = "BINARY")
    private LocalDateTime timestamp;

    @Column(name = "rev_minor_edit", nullable = false, length = 3, columnDefinition = "TINYINT")
    private Boolean minorEdit;

    @Column(name = "rev_deleted", nullable = false, length = 3, columnDefinition = "TINYINT")
    private Boolean deleted;

    @Column(name = "rev_len", nullable = true, length = 10)
    private Integer length;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rev_parent_id", nullable = true)
    private CommonsRevision parent;

    @Column(name = "rev_sha1", nullable = false, length = 32, columnDefinition = "VARBINARY")
    private String sha1;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public CommonsPage getPage() {
        return page;
    }

    public void setPage(CommonsPage page) {
        this.page = page;
    }

    public long getCommentId() {
        return commentId;
    }

    public void setCommentId(long commentId) {
        this.commentId = commentId;
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

    public Boolean getMinorEdit() {
        return minorEdit;
    }

    public void setMinorEdit(Boolean minorEdit) {
        this.minorEdit = minorEdit;
    }

    public Boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }

    public Integer getLength() {
        return length;
    }

    public void setLength(Integer length) {
        this.length = length;
    }

    public CommonsRevision getParent() {
        return parent;
    }

    public void setParent(CommonsRevision parent) {
        this.parent = parent;
    }

    public String getSha1() {
        return sha1;
    }

    public void setSha1(String sha1) {
        this.sha1 = sha1;
    }

    @Override
    public String toString() {
        return "CommonsRevision [id=" + id + ", " + (page != null ? "page=" + page + ", " : "") + "commentId="
                + commentId + ", actor=" + actor + ", " + (timestamp != null ? "timestamp=" + timestamp + ", " : "")
                + (minorEdit != null ? "minorEdit=" + minorEdit + ", " : "")
                + (deleted != null ? "deleted=" + deleted + ", " : "")
                + (length != null ? "length=" + length + ", " : "") + (parent != null ? "parent=" + parent + ", " : "")
                + (sha1 != null ? "sha1=" + sha1 : "") + "]";
    }
}
