package org.wikimedia.commons.donvip.spacemedia.data.commons;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * <a href="https://www.mediawiki.org/wiki/Manual:Slots_table">ID for Mediawiki
 * Slots table</a>
 *
 * <pre>
 * +------------------+----------------------+------+-----+---------+-------+
 * | Field            | Type                 | Null | Key | Default | Extra |
 * +------------------+----------------------+------+-----+---------+-------+
 * | slot_revision_id | bigint(20) unsigned  | NO   | PRI | NULL    |       |
 * | slot_role_id     | smallint(5) unsigned | NO   | PRI | NULL    |       |
 * +------------------+----------------------+------+-----+---------+-------+
 * </pre>
 */
@Embeddable
public class CommonsSlotId implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Reference to revision.rev_id or archive.ar_rev_id. slot_revision_id and
     * slot_role_id together comprise the primary key.
     */
    @Column(name = "slot_revision_id", nullable = false, length = 20, columnDefinition = "bigint")
    private int revisionId;

    /**
     * Reference to slot_roles.role_id
     */
    @Column(name = "slot_role_id", nullable = false, length = 5, columnDefinition = "smallint")
    private int roleId;

    public CommonsSlotId() {
        // Empty constructor required by JPA
    }

    public CommonsSlotId(int revisionId, int roleId) {
        this.revisionId = revisionId;
        this.roleId = roleId;
    }

    public int getRevisionId() {
        return revisionId;
    }

    public void setRevisionId(int revisionId) {
        this.revisionId = revisionId;
    }

    public int getRoleId() {
        return roleId;
    }

    public void setRoleId(int roleId) {
        this.roleId = roleId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(revisionId, roleId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        CommonsSlotId other = (CommonsSlotId) obj;
        return revisionId == other.revisionId && roleId == other.roleId;
    }

    @Override
    public String toString() {
        return "CommonsSlotId [revisionId=" + revisionId + ", roleId=" + roleId + "]";
    }
}
