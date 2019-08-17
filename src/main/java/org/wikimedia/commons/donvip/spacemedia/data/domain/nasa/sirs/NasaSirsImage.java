package org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.sirs;

import java.time.LocalDate;
import java.time.Year;
import java.util.Objects;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;

import org.wikimedia.commons.donvip.spacemedia.data.domain.Media;

import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
public class NasaSirsImage extends Media {

    @Id
    @Column(nullable = false, length = 60)
    @JsonProperty("nasa_id")
    private String nasaId;

    @Column(nullable = false)
    private String category;

    @Column(nullable = true)
    private LocalDate photoDate;

    @Column(nullable = false)
    private Year photoYear;

    @Column(length = 340)
    @ElementCollection(fetch = FetchType.EAGER)
    private Set<String> keywords;

    public String getNasaId() {
        return nasaId;
    }

    public void setNasaId(String nasaId) {
        this.nasaId = nasaId;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public LocalDate getPhotoDate() {
        return photoDate;
    }

    public void setPhotoDate(LocalDate photoDate) {
        this.photoDate = photoDate;
    }

    public Year getPhotoYear() {
        return photoYear;
    }

    public void setPhotoYear(Year photoYear) {
        this.photoYear = photoYear;
    }

    public Set<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(Set<String> keywords) {
        this.keywords = keywords;
    }

    @Override
    public String toString() {
        return "NasaSirsImage [" + (nasaId != null ? "nasaId=" + nasaId + ", " : "")
                + (title != null ? "title=" + title + ", " : "")
                + (category != null ? "category=" + category + ", " : "")
                + (photoDate != null ? "photoDate=" + photoDate + ", " : "")
                + (photoYear != null ? "photoYear=" + photoYear + ", " : "")
                + (description != null ? "description=" + description + ", " : "")
                + (keywords != null ? "keywords=" + keywords + ", " : "")
                + (getAssetUrl() != null ? "assetUrl=" + getAssetUrl() + ", " : "")
                + (sha1 != null ? "sha1=" + sha1 : "") + "]";
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hash(nasaId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj) || getClass() != obj.getClass())
            return false;
        NasaSirsImage other = (NasaSirsImage) obj;
        return Objects.equals(nasaId, other.nasaId);
    }
}
