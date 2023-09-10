package org.wikimedia.commons.donvip.spacemedia.data.commons;

import java.io.Serializable;
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
public class CommonsRevision implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * This field holds the primary key for each revision. page_latest is a foreign
     * key to this field. The rev_id numbers have been preserved across
     * deletion/undeletion since the table's inception in MediaWiki 1.5, when rev_id
     * and text_id superseded the old cur_id.
     *
     * Note that while rev_id almost always increases monotonically for successive
     * revisions of a page, this is not strictly guaranteed as importing from
     * another wiki can cause revisions to be created out of order.
     */
    @Id
    @Column(name = "rev_id", nullable = false, length = 10)
    private int id;

    /**
     * This field holds a reference to the page to which this revision pertains. The
     * number in this field is equal to the page_id field of said page. This should
     * never be invalid; if it is, that revision won't show up in the page history.
     * If page.page_latest links to a revision with an invalid rev_page, this will
     * cause the "The revision #0 of the page named 'Foo' does not exist" error.
     * (similar issue might occur when slots and content are missing for the
     * revision)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rev_page", nullable = false)
    private CommonsPage page;

    /**
     * This is a foreign key to comment_id in the comment table.
     *
     * If this field contains zero in each record, the comment id must be retrieved
     * from the revision_comment_temp table. Supposedly, this table will be merged
     * with the table revision again in the future.
     */
    @Column(name = "rev_comment_id", nullable = false, length = 20, columnDefinition = "BIGINT")
    private long commentId;

    /**
     * This is a foreign key to actor_id in the actor table. If this field contains
     * zero in each record, the actor id must be retrieved from the
     * revision_actor_temp table. Supposedly, this table will be merged with the
     * table revision again in the future. See Actor migration.
     */
    @Column(name = "rev_actor", nullable = false, length = 20, columnDefinition = "BIGINT")
    private long actor;

    /**
     * Holds the timestamp of the edit. Looks like, for example, 20080326231614
     * (MW_TS time format). Corresponds to recentchanges.rc_timestamp (and
     * logging.log_timestamp for page creations).
     *
     * Unlike image.img_timestamp, this value is not fudged to be unique for a given
     * page, so edits happening in quick succession can have the same timestamp.
     */
    @Column(name = "rev_timestamp", nullable = true, length = 14, columnDefinition = "BINARY")
    private LocalDateTime timestamp;

    /**
     * Records whether the user marked the 'minor edit' checkbox. If the value for
     * this field is 1, then the edit was declared as 'minor'; it is 0 otherwise.
     * Many automated edits are marked as minor.
     */
    @Column(name = "rev_minor_edit", nullable = false, length = 3, columnDefinition = "TINYINT")
    private Boolean minorEdit;

    /**
     * This field is reserved for RevisionDelete system. It's a bitfield in which
     * the values are DELETED_TEXT = 1; DELETED_COMMENT = 2; DELETED_USER = 4; and
     * DELETED_RESTRICTED = 8. So, for example, if nothing has been deleted from
     * that revision, then the value is 0; if both the comment and user have been
     * deleted, then the value is 6. DELETED_RESTRICTED indicates suppression; when
     * this flag is set content hidden by the other DELETED_* flags is only visible
     * to suppressors (i.e. oversighters) instead of admins.
     */
    @Column(name = "rev_deleted", nullable = false, length = 3, columnDefinition = "TINYINT")
    private Boolean deleted;

    /**
     * This field contains the length of the article after the revision, in bytes.
     * Used in history pages. Corresponds to rc_new_len.
     */
    @Column(name = "rev_len", nullable = true, length = 10)
    private Integer length;

    /**
     * The rev_id of the previous revision to the page. Corresponds to
     * rc_last_oldid. For edits which are new page creations, rev_parent_id = 0.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rev_parent_id", nullable = true)
    private CommonsRevision parent;

    /**
     * This field is used to add the SHA-1 text content hash in base-36 (generated
     * by Wikimedia\base_convert().) Since 1.32, it's a nested hash of hashes of
     * content_sha1 across all slots of the revision. If the revision only has one
     * slot, the values of the rev_sha1 and content_sha1 fields are identical. The
     * nested hash algorithm is implemented in RevisionSlots::computeSha1(). It can
     * be outlined as:
     */
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
        return "CommonsRevision [id=" + id + ", " + "commentId="
                + commentId + ", actor=" + actor + ", " + (timestamp != null ? "timestamp=" + timestamp + ", " : "")
                + (minorEdit != null ? "minorEdit=" + minorEdit + ", " : "")
                + (deleted != null ? "deleted=" + deleted + ", " : "")
                + (length != null ? "length=" + length + ", " : "")
                + (sha1 != null ? "sha1=" + sha1 : "") + "]";
    }
}
