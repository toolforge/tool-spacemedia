package org.wikimedia.commons.donvip.spacemedia.data.commons;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * <a href="https://www.mediawiki.org/wiki/Manual:Redirect_table">Mediawiki Redirect table</a>
 * 
 * <pre>
 * +--------------+------------------+------+-----+---------+-------+
 * | Field        | Type             | Null | Key | Default | Extra |
 * +--------------+------------------+------+-----+---------+-------+
 * | rd_from      | int(10) unsigned | NO   | PRI | 0       |       |
 * | rd_namespace | int(11)          | NO   | MUL | 0       |       |
 * | rd_title     | varbinary(255)   | NO   |     |         |       |
 * | rd_interwiki | varbinary(32)    | YES  |     | NULL    |       |
 * | rd_fragment  | varbinary(255)   | YES  |     | NULL    |       |
 * +--------------+------------------+------+-----+---------+-------+
 * </pre>
 */
@Entity
@Table(name = "redirect")
public class CommonsRedirect implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "rd_from", nullable = false, length = 10)
    private int from;

    @Column(name = "rd_namespace", nullable = false, length = 11)
    private int namespace;

    @Column(name = "rd_title", nullable = false, length = 255, columnDefinition = "VARBINARY")
    private String title;

    @Column(name = "rd_interwiki", nullable = true, length = 32, columnDefinition = "VARBINARY")
    private String interwiki;

    @Column(name = "rd_fragment", nullable = true, length = 255, columnDefinition = "VARBINARY")
    private String fragment;

    public int getFrom() {
        return from;
    }

    public void setFrom(int from) {
        this.from = from;
    }

    public int getNamespace() {
        return namespace;
    }

    public void setNamespace(int namespace) {
        this.namespace = namespace;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getInterwiki() {
        return interwiki;
    }

    public void setInterwiki(String interwiki) {
        this.interwiki = interwiki;
    }

    public String getFragment() {
        return fragment;
    }

    public void setFragment(String fragment) {
        this.fragment = fragment;
    }

    @Override
    public String toString() {
        return "CommonsRedirect [from=" + from + ", namespace=" + namespace + ", "
                + (title != null ? "title=" + title + ", " : "")
                + (interwiki != null ? "interwiki=" + interwiki + ", " : "")
                + (fragment != null ? "fragment=" + fragment : "") + "]";
    }
}
