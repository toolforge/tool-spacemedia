package org.wikimedia.commons.donvip.spacemedia.data.jpa.entity;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.FetchType;
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import javax.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class ContentPublication extends Publication {

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String title;

    @Lob
    @Column(nullable = true, columnDefinition = "MEDIUMTEXT")
    private String description;

    /**
     * ISO 639-1 code of language used for both title and description.
     */
    @Column(nullable = true, length = 2)
    private String lang;

    @ManyToMany(cascade = CascadeType.REMOVE, fetch = FetchType.EAGER)
    private Set<FilePublication> filePublications = new HashSet<>();

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public Set<FilePublication> getFilePublications() {
        return filePublications;
    }

    public void setFilePublications(Set<FilePublication> filePublications) {
        this.filePublications = filePublications;
    }

    public boolean addFilePublication(FilePublication filePub) {
        return filePublications.add(filePub);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hash(description, lang, title);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj) || getClass() != obj.getClass())
            return false;
        ContentPublication other = (ContentPublication) obj;
        return Objects.equals(description, other.description) && Objects.equals(lang, other.lang)
                && Objects.equals(title, other.title);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + (getTitle() != null ? "title=" + getTitle() + ", " : "")
                + (getId() != null ? "id=" + getId() + ", " : "")
                + (getUrl() != null ? "url=" + getUrl() : "")
                + "]";
    }
}
