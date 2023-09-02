package org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.sirs;

import java.time.Year;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.SingleFileMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.WithKeywords;

@Entity
@Indexed
public class NasaSirsMedia extends SingleFileMedia implements WithKeywords {

    @Column(nullable = false)
    private String category;

    @Column(name = "photo_year", nullable = false)
    private Year year;

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
    public Year getYear() {
        return year;
    }

    public void setYear(Year photoYear) {
        this.year = photoYear;
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
                + (year != null ? "photoYear=" + year + ", " : "")
                + (description != null ? "description=" + description + ", " : "")
                + (keywords != null ? "keywords=" + keywords + ", " : "")
                + "metadata=" + getMetadata() + "]";
    }

    @Override
    public boolean isAudio() {
        return false;
    }

    @Override
    public boolean isImage() {
        return true;
    }

    @Override
    public boolean isVideo() {
        return false;
    }
}
