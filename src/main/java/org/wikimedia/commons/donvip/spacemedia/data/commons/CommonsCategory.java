package org.wikimedia.commons.donvip.spacemedia.data.commons;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * <a href="https://www.mediawiki.org/wiki/Manual:Category_table">Mediawiki
 * Category table</a>
 * <pre>
 * +-------------+------------------+------+-----+---------+----------------+
 * | Field       | Type             | Null | Key | Default | Extra          |
 * +-------------+------------------+------+-----+---------+----------------+
 * | cat_id      | int(10) unsigned | NO   | PRI | NULL    | auto_increment |
 * | cat_title   | varbinary(255)   | NO   | UNI | NULL    |                |
 * | cat_pages   | int(11)          | NO   | MUL | 0       |                |
 * | cat_subcats | int(11)          | NO   |     | 0       |                |
 * | cat_files   | int(11)          | NO   |     | 0       |                |
 * +-------------+------------------+------+-----+---------+----------------+
 * </pre>
 */
@Entity
@Table(name = "category")
public class CommonsCategory {

    @Id
    @GeneratedValue
    @Column(name = "cat_id", nullable = false, length = 10)
    private int id;

    @Column(name = "cat_title", nullable = false, length = 255, columnDefinition = "VARBINARY", unique = true)
    private String title;

    @Column(name = "cat_pages", nullable = false, length = 11)
    private int pages;

    @Column(name = "cat_subcats", nullable = false, length = 11)
    private int subcats;

    @Column(name = "cat_files", nullable = false, length = 11)
    private int files;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getPages() {
        return pages;
    }

    public void setPages(int pages) {
        this.pages = pages;
    }

    public int getSubcats() {
        return subcats;
    }

    public void setSubcats(int subcats) {
        this.subcats = subcats;
    }

    public int getFiles() {
        return files;
    }

    public void setFiles(int files) {
        this.files = files;
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
        CommonsCategory other = (CommonsCategory) obj;
        return id == other.id;
    }

    @Override
    public String toString() {
        return "CommonsCategory [title=" + title + ", " + "pages=" + pages + ", subcats=" + subcats + ", files=" + files
                + "]";
    }
}
