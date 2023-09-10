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

    /**
     * Uniquely identifying primary key. This value is preserved across edits and
     * renames.
     *
     * Page IDs do not change when pages are moved, but they may change when pages
     * are deleted and then restored.
     */
    @Id
    @GeneratedValue
    @Column(name = "page_id", nullable = false, length = 10)
    private int id;

    /**
     * A page name is broken into a namespace and a title. The namespace keys are
     * UI-language-independent constants, defined in includes/Defines.php.
     *
     * This field contains the number of the page's namespace. The values range from
     * 0 to 99 for the core namespaces, and from 100 to 10,000 for custom
     * namespaces.
     */
    @Column(name = "page_namespace", nullable = false, length = 11)
    private int namespace;

    /**
     * The sanitized page title, without the namespace, with a maximum of 255
     * characters (binary). It is stored as text, with spaces replaced by
     * underscores. The real title shown in articles is just this title with
     * underscores (_) converted to spaces ( ). For example, a page titled "Talk:Foo
     * Bar" would have "Foo_Bar" in this field.
     */
    @Column(name = "page_title", nullable = false, length = 255, columnDefinition = "VARBINARY")
    private String title;

    /**
     * A value of 1 here indicates the article is a redirect; it is 0 in all other
     * cases.
     */
    @Column(name = "page_is_redirect", nullable = false)
    private Boolean isRedirect;

    /**
     * This field stores whether the page is new, meaning it either has only one
     * revision or has not been edited since being restored, even if there is more
     * than one revision. If the field contains a value of 1, then it indicates that
     * the page is new; otherwise, it is 0. Rollback links are not displayed if the
     * page is new, since there is nothing to roll back to.
     */
    @Column(name = "page_is_new", nullable = false)
    private Boolean isNew;

    /**
     * Random decimal value, between 0 and 1, used for Special:Random (see
     * Manual:Random page for more details). Generated by wfRandom().
     */
    @Column(name = "page_random", nullable = false)
    private double random;

    /**
     * This timestamp is updated whenever the page changes in a way requiring it to
     * be re-rendered, invalidating caches. Aside from editing, this includes
     * permission changes, creation or deletion of linked pages, and alteration of
     * contained templates. Set to $dbw->timestamp() at the time of page creation.
     */
    @Column(name = "page_touched", nullable = false, length = 14, columnDefinition = "BINARY")
    private String touched;

    /**
     * This timestamp is updated whenever a page is re-parsed and it has all the
     * link tracking tables updated for it. This is useful for de-duplicating
     * expensive backlink update jobs. Set to the default value of NULL when the
     * page is created by WikiPage::insertOn().
     */
    @Column(name = "page_links_updated", nullable = true, length = 14, columnDefinition = "VARBINARY")
    private String linksUpdated;

    /**
     * This is a foreign key to rev_id for the current revision. It may be 0 during
     * page creation. It needs to link to a revision with a valid revision.rev_page,
     * or there will be the "The revision #0 of the page named 'Foo' does not exist"
     * error when one tries to view the page. Can be obtained via
     * WikiPage::getLatest().
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "page_latest", nullable = false)
    private CommonsRevision latest;

    /**
     * Uncompressed length in bytes of the page's current source text.
     *
     * This however, does not apply to images which still have records in this
     * table. Instead, the uncompressed length in bytes of the description for the
     * file is used as the latter is in the text.old_text field.
     */
    @Column(name = "page_len", nullable = false, length = 10)
    private int len;

    /**
     * Content model, see CONTENT_MODEL_XXX constants. Comparable to
     * revision.rev_content_model.
     */
    @Column(name = "page_content_model", nullable = true, length = 32, columnDefinition = "VARBINARY")
    private String contentModel;

    /**
     * Page content language. Set to the default value of NULL at the time of page
     * creation.
     */
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

    public CommonsRevision getLatest() {
        return latest;
    }

    public void setLatest(CommonsRevision latest) {
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
                + (isRedirect != null ? "isRedirect=" + isRedirect + ", " : "")
                + (isNew != null ? "isNew=" + isNew + ", " : "") + "random=" + random + ", "
                + (touched != null ? "touched=" + touched + ", " : "")
                + (linksUpdated != null ? "linksUpdated=" + linksUpdated + ", " : "") + "latest=" + latest + ", len="
                + len + ", " + (contentModel != null ? "contentModel=" + contentModel + ", " : "")
                + (lang != null ? "lang=" + lang : "") + "]";
    }
}
