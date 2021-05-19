package org.wikimedia.commons.donvip.spacemedia.commons.data;

import java.io.Serializable;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

/**
 * <a href="https://www.mediawiki.org/wiki/Manual:Categorylinks_table">ID for
 * Mediawiki Category Links table</a>
 * 
 * <pre>
 * +---------+------------------+------+-----+---------+-------+
 * | Field   | Type             | Null | Key | Default | Extra |
 * +---------+------------------+------+-----+---------+-------+
 * | cl_from | int(10) unsigned | NO   | PRI | 0       |       |
 * | cl_to   | varbinary(255)   | NO   | PRI |         |       |
 * +---------+------------------+------+-----+---------+-------+
 * </pre>
 */
@Embeddable
public class CommonsCategoryLinkId implements Serializable {

    private static final long serialVersionUID = 1L;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cl_from", nullable = false)
    private CommonsPage from;

    @Column(name = "cl_to", nullable = false, length = 255, columnDefinition = "VARBINARY")
    private String to;

    public CommonsCategoryLinkId() {
        // Empty constructor required by JPA
    }

    public CommonsCategoryLinkId(CommonsPage from, String to) {
        this.from = from;
        this.to = to;
    }

    public CommonsPage getFrom() {
        return from;
    }

    public void setFrom(CommonsPage from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, to);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        CommonsCategoryLinkId other = (CommonsCategoryLinkId) obj;
        return Objects.equals(from, other.from) && Objects.equals(to, other.to);
    }

    @Override
    public String toString() {
        return "CommonsCategoryLinkId [from=" + from + ", to=" + to + "]";
    }
}
