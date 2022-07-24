package org.wikimedia.commons.donvip.spacemedia.data.commons;

import java.io.Serializable;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * <a href="https://www.mediawiki.org/wiki/Manual:Page_restrictions_table">Mediawiki
 * page_restrictions table</a>
 *
 * +------------+------------------+------+-----+---------+----------------+
 * | Field      | Type             | Null | Key | Default | Extra          |
 * +------------+------------------+------+-----+---------+----------------+
 * | pr_id      | int(10) unsigned | NO   | PRI | NULL    | auto_increment |
 * | pr_page    | int(10) unsigned | NO   | MUL | NULL    |                |
 * | pr_type    | varbinary(60)    | NO   | MUL | NULL    |                |
 * | pr_level   | varbinary(60)    | NO   | MUL | NULL    |                |
 * | pr_cascade | tinyint(4)       | NO   | MUL | NULL    |                |
 * | pr_expiry  | varbinary(14)    | YES  |     | NULL    |                |
 * +------------+------------------+------+-----+---------+----------------+
 */
@Entity
@Table(name = "page_restrictions")
public class CommonsPageRestrictions implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "pr_id", nullable = false, length = 10)
    private int id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pr_page", nullable = false)
    private CommonsPage page;

    @Column(name = "pr_type", nullable = false, length = 60, columnDefinition = "VARBINARY")
    private String type;

    @Column(name = "pr_level", nullable = false, length = 60, columnDefinition = "VARBINARY")
    private String level;

    @Column(name = "pr_cascade", nullable = false, length = 4, columnDefinition = "TINYINT")
    private short cascade;

    @Column(name = "pr_expiry", nullable = true, length = 14, columnDefinition = "VARBINARY")
    private String expiry;

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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public short getCascade() {
        return cascade;
    }

    public void setCascade(short cascade) {
        this.cascade = cascade;
    }

    public String getExpiry() {
        return expiry;
    }

    public void setExpiry(String expiry) {
        this.expiry = expiry;
    }

    public static long getSerialversionuid() {
        return serialVersionUID;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        CommonsPageRestrictions other = (CommonsPageRestrictions) obj;
        return id == other.id;
    }

    @Override
    public String toString() {
        return "CommonsPageRestrictions [id=" + id + ", " + (page != null ? "page=" + page + ", " : "")
                + (type != null ? "type=" + type + ", " : "") + (level != null ? "level=" + level + ", " : "")
                + "cascade=" + cascade + ", " + (expiry != null ? "expiry=" + expiry : "") + "]";
    }
}
