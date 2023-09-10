package org.wikimedia.commons.donvip.spacemedia.data.commons;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * <a href="https://www.mediawiki.org/wiki/Manual:Slots_table">Mediawiki Slots
 * table</a>
 *
 * <pre>
 * +------------------+----------------------+------+-----+---------+-------+
 * | Field            | Type                 | Null | Key | Default | Extra |
 * +------------------+----------------------+------+-----+---------+-------+
 * | slot_revision_id | bigint(20) unsigned  | NO   | PRI | NULL    |       |
 * | slot_role_id     | smallint(5) unsigned | NO   | PRI | NULL    |       |
 * | slot_content_id  | bigint(20) unsigned  | NO   |     | NULL    |       |
 * | slot_origin      | bigint(20) unsigned  | NO   |     | NULL    |       |
 * +------------------+----------------------+------+-----+---------+-------+
 * </pre>
 */
@Entity
@Table(name = "slots")
public class CommonsSlot implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Embedded
    private CommonsSlotId id;

    /**
     * Reference to content.content_id
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slot_content_id", nullable = false)
    private CommonsContent content;

    /**
     * The revision.rev_id of the revision that originated the slot's content. To
     * find revisions that changed slots, look for slot_origin = slot_revision_id.
     */
    @Column(name = "slot_origin", nullable = false, length = 20, columnDefinition = "bigint")
    private int origin;

    public CommonsSlotId getId() {
        return id;
    }

    public void setId(CommonsSlotId id) {
        this.id = id;
    }

    public CommonsContent getContent() {
        return content;
    }

    public void setContentId(CommonsContent content) {
        this.content = content;
    }

    public int getOrigin() {
        return origin;
    }

    public void setOrigin(int origin) {
        this.origin = origin;
    }

    @Override
    public String toString() {
        return "CommonsSlot [id=" + id + ", origin=" + origin + ']';
    }
}
