package org.wikimedia.commons.donvip.spacemedia.data.commons;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * <a href="https://www.mediawiki.org/wiki/Manual:Page_props_table">Mediawiki
 * page_props table</a>
 *
 * <pre>
 * +-------------+---------------+------+-----+---------+-------+
 * | Field       | Type          | Null | Key | Default | Extra |
 * +-------------+---------------+------+-----+---------+-------+
 * | pp_page     | int(11)       | NO   | PRI | NULL    |       |
 * | pp_propname | varbinary(60) | NO   | PRI | NULL    |       |
 * | pp_value    | blob          | NO   |     | NULL    |       |
 * | pp_sortkey  | float         | YES  |     | NULL    |       |
 * +-------------+---------------+------+-----+---------+-------+
 * </pre>
 */
@Entity
@Table(name = "page_props")
public class CommonsPageProp implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pp_page", nullable = false)
    private CommonsPage page;

    @Id
    @Column(name = "pp_propname", nullable = false, length = 60, columnDefinition = "VARBINARY")
    private String propname;

    @Lob
    @Column(name = "pp_value", nullable = false, columnDefinition = "BLOB")
    private byte[] value;

    @Column(name = "pp_sortkey", nullable = true)
    private Float sortkey;

    public CommonsPage getPage() {
        return page;
    }

    public void setPage(CommonsPage page) {
        this.page = page;
    }

    public String getPropname() {
        return propname;
    }

    public void setPropname(String propname) {
        this.propname = propname;
    }

    public byte[] getValue() {
        return value;
    }

    public void setValue(byte[] value) {
        this.value = value;
    }

    public Float getSortkey() {
        return sortkey;
    }

    public void setSortkey(Float sortkey) {
        this.sortkey = sortkey;
    }

    @Override
    public int hashCode() {
        return Objects.hash(page, propname);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        CommonsPageProp other = (CommonsPageProp) obj;
        return Objects.equals(page, other.page) && Objects.equals(propname, other.propname);
    }

    @Override
    public String toString() {
        return "CommonsPageProp [" + (propname != null ? "propname=" + propname + ", " : "")
                + (value != null ? "value=" + value + ", " : "") + "sortkey=" + sortkey + "]";
    }
}
