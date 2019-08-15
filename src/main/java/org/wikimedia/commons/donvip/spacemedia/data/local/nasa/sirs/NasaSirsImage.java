package org.wikimedia.commons.donvip.spacemedia.data.local.nasa.sirs;

import java.net.URL;
import java.time.LocalDate;
import java.time.Year;
import java.util.Objects;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Lob;

import org.wikimedia.commons.donvip.spacemedia.data.local.Media;

import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
public class NasaSirsImage extends Media {

    @Id
    @Column(nullable = false, length = 60)
    @JsonProperty("nasa_id")
    private String nasaId;

    @Lob
    private String title;

    @Column(nullable = false)
    private String category;

    @Column(nullable = true)
    private LocalDate photoDate;

    @Column(nullable = false)
    private Year photoYear;

    @Lob
    private String description;

    @Column(length = 340)
    @ElementCollection(fetch = FetchType.EAGER)
    private Set<String> keywords;

    @Column(length = 200)
    @JsonProperty("asset_url")
    private URL assetUrl;

    public String getNasaId() {
        return nasaId;
    }

    public void setNasaId(String nasaId) {
        this.nasaId = nasaId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Set<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(Set<String> keywords) {
        this.keywords = keywords;
    }

    public URL getAssetUrl() {
        return assetUrl;
    }

    public void setAssetUrl(URL assetUrl) {
        this.assetUrl = assetUrl;
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
                + (assetUrl != null ? "assetUrl=" + assetUrl + ", " : "") + (sha1 != null ? "sha1=" + sha1 : "") + "]";
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hash(assetUrl, nasaId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj) || getClass() != obj.getClass())
            return false;
        NasaSirsImage other = (NasaSirsImage) obj;
        return Objects.equals(assetUrl, other.assetUrl) && Objects.equals(nasaId, other.nasaId);
    }
}
