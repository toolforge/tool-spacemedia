package org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.sirs;

import java.util.HashSet;
import java.util.Set;

import org.wikimedia.commons.donvip.spacemedia.data.domain.base.SingleFileMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.WithKeywords;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;

@Entity
public class NasaSirsMedia extends SingleFileMedia implements WithKeywords {

    @Column(nullable = false)
    private String category;

    @Column(length = 340)
    @ElementCollection(fetch = FetchType.EAGER)
    private Set<String> keywords = new HashSet<>();

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    @Override
    public Set<String> getKeywords() {
        return keywords;
    }

    @Override
    public void setKeywords(Set<String> keywords) {
        this.keywords = keywords;
    }

    @Override
    public String toString() {
        return "NasaSirsMedia [" + (getId() != null ? "id=" + getId() + ", " : "")
                + (title != null ? "title=" + title + ", " : "")
                + (category != null ? "category=" + category + ", " : "")
                + (description != null ? "description=" + description + ", " : "")
                + (keywords != null ? "keywords=" + keywords + ", " : "")
                + "metadata=" + getMetadata() + "]";
    }
}
