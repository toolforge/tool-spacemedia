package org.wikimedia.commons.donvip.spacemedia.data.jpa;

import java.util.Objects;
import java.util.Set;

import javax.persistence.Column;
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
    private String lang;

    @ManyToMany
    private Set<FilePublication> filePublications;

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

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hash(description, filePublications, lang, title);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj) || getClass() != obj.getClass())
            return false;
        ContentPublication other = (ContentPublication) obj;
        return Objects.equals(description, other.description)
                && Objects.equals(filePublications, other.filePublications) && Objects.equals(lang, other.lang)
                && Objects.equals(title, other.title);
    }
}
