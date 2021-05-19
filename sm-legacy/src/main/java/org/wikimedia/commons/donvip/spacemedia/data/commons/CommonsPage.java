package org.wikimedia.commons.donvip.spacemedia.data.commons;

import java.io.Serializable;
import java.util.Objects;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.SecondaryTable;
import javax.persistence.Table;

/**
 * <a href="https://www.mediawiki.org/wiki/Manual:Page_table">Mediawiki Page
 * table</a>
 *
 * <pre>
 * +--------------------+---------------------+------+-----+----------------+----------------+
 * | Field              | Type                | Null | Key | Default        | Extra          |
 * +--------------------+---------------------+------+-----+----------------+----------------+
 * | page_id            | int(10) unsigned    | NO   | PRI | NULL           | auto_increment |
 * | page_namespace     | int(11)             | NO   | MUL | NULL           |                |
 * | page_title         | varbinary(255)      | NO   |     | NULL           |                |
 * | page_restrictions  | tinyblob            | NO   |     | NULL           |                |
 * | page_is_redirect   | tinyint(3) unsigned | NO   | MUL | 0              |                |
 * | page_is_new        | tinyint(3) unsigned | NO   |     | 0              |                |
 * | page_random        | double unsigned     | NO   | MUL | NULL           |                |
 * | page_touched       | binary(14)          | NO   |     |                |                |
 * | page_links_updated | varbinary(14)       | YES  |     | NULL           |                |
 * | page_latest        | int(10) unsigned    | NO   |     | NULL           |                |
 * | page_len           | int(10) unsigned    | NO   | MUL | NULL           |                |
 * | page_content_model | varbinary(32)       | YES  |     | NULL           |                |
 * | page_lang          | varbinary(35)       | YES  |     | NULL           |                |
 * +--------------------+---------------------+------+-----+----------------+----------------+
 * </pre>
 */
@Entity
@Table(name = "page")
@SecondaryTable(name = "redirect", pkJoinColumns = @PrimaryKeyJoinColumn(name = "rd_from"))
public class CommonsPage implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "page_id", nullable = false, length = 10)
    private int id;

    @Column(name = "page_namespace", nullable = false, length = 11)
    private int namespace;

    @Column(name = "page_title", nullable = false, length = 255, columnDefinition = "VARBINARY")
    private String title;

    @Lob
    @Column(name = "page_restrictions", nullable = false, columnDefinition = "TINYBLOB")
    private String restrictions;

    @Column(name = "page_is_redirect", nullable = false)
    private Boolean isRedirect;

    @Column(name = "page_is_new", nullable = false)
    private Boolean isNew;

    @Column(name = "page_random", nullable = false)
    private double random;

    @Column(name = "page_touched", nullable = false, length = 14, columnDefinition = "BINARY")
    private String touched;

    @Column(name = "page_links_updated", nullable = true, length = 14, columnDefinition = "VARBINARY")
    private String linksUpdated;

    @Column(name = "page_latest", nullable = false, length = 10)
    private int latest;

    @Column(name = "page_len", nullable = false, length = 10)
    private int len;

    @Column(name = "page_content_model", nullable = true, length = 32, columnDefinition = "VARBINARY")
    private String contentModel;

    @Column(name = "page_lang", nullable = true, length = 35, columnDefinition = "VARBINARY")
    private String lang;

    @JoinColumn(name = "pp_page")
    @OneToMany(fetch = FetchType.EAGER)
    private Set<CommonsPageProp> props;

    @JoinColumn(name = "cl_from")
    @OneToMany(fetch = FetchType.EAGER)
    private Set<CommonsCategoryLink> categoryLinks;

    @JoinColumn(name = "rd_from", table = "redirect", insertable = false, updatable = false)
    @OneToOne(fetch = FetchType.EAGER, optional = true)
    private CommonsRedirect redirect;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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

    public String getRestrictions() {
        return restrictions;
    }

    public void setRestrictions(String restrictions) {
        this.restrictions = restrictions;
    }

    public Boolean getIsRedirect() {
        return isRedirect;
    }

    public void setIsRedirect(Boolean isRedirect) {
        this.isRedirect = isRedirect;
    }

    public Boolean getIsNew() {
        return isNew;
    }

    public void setIsNew(Boolean isNew) {
        this.isNew = isNew;
    }

    public double getRandom() {
        return random;
    }

    public void setRandom(double random) {
        this.random = random;
    }

    public String getTouched() {
        return touched;
    }

    public void setTouched(String touched) {
        this.touched = touched;
    }

    public String getLinksUpdated() {
        return linksUpdated;
    }

    public void setLinksUpdated(String linksUpdated) {
        this.linksUpdated = linksUpdated;
    }

    public int getLatest() {
        return latest;
    }

    public void setLatest(int latest) {
        this.latest = latest;
    }

    public int getLen() {
        return len;
    }

    public void setLen(int len) {
        this.len = len;
    }

    public String getContentModel() {
        return contentModel;
    }

    public void setContentModel(String contentModel) {
        this.contentModel = contentModel;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public Set<CommonsPageProp> getProps() {
        return props;
    }

    public void setProps(Set<CommonsPageProp> props) {
        this.props = props;
    }

    public Set<CommonsCategoryLink> getCategoryLinks() {
        return categoryLinks;
    }

    public void setCategoryLinks(Set<CommonsCategoryLink> categoryLinks) {
        this.categoryLinks = categoryLinks;
    }

    public CommonsRedirect getRedirect() {
        return redirect;
    }

    public void setRedirect(CommonsRedirect redirect) {
        this.redirect = redirect;
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
        CommonsPage other = (CommonsPage) obj;
        return id == other.id;
    }

    @Override
    public String toString() {
        return "CommonsPage [id=" + id + ", namespace=" + namespace + ", "
                + (title != null ? "title=" + title + ", " : "")
                + (restrictions != null ? "restrictions=" + restrictions + ", " : "")
                + (isRedirect != null ? "isRedirect=" + isRedirect + ", " : "")
                + (isNew != null ? "isNew=" + isNew + ", " : "") + "random=" + random + ", "
                + (touched != null ? "touched=" + touched + ", " : "")
                + (linksUpdated != null ? "linksUpdated=" + linksUpdated + ", " : "") + "latest=" + latest + ", len="
                + len + ", " + (contentModel != null ? "contentModel=" + contentModel + ", " : "")
                + (lang != null ? "lang=" + lang : "") + "]";
    }
}
