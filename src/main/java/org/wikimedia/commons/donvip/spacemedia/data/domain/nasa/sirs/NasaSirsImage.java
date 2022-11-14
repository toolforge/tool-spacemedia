package org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.sirs;

import java.time.LocalDate;
import java.time.Year;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.wikimedia.commons.donvip.spacemedia.data.domain.Media;

import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
@Indexed
@Table(indexes = { @Index(columnList = "sha1, phash") })
public class NasaSirsImage extends Media<String, LocalDate> {

    @Id
    @Column(name = "nasa_id", nullable = false, length = 60)
    @JsonProperty("nasa_id")
    private String id;

    @Column(nullable = false)
    private String category;

    @Column(name = "photo_date", nullable = true)
    private LocalDate date;

    @Column(name = "photo_year", nullable = false)
    private Year year;

    @Column(length = 340)
    @ElementCollection(fetch = FetchType.EAGER)
    private Set<String> keywords = new HashSet<>();

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    @Override
    public LocalDate getDate() {
        return date;
    }

    @Override
    public void setDate(LocalDate date) {
        this.date = date;
    }

    @Override
    public Year getYear() {
        return year;
    }

    public void setYear(Year photoYear) {
        this.year = photoYear;
    }

    public Set<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(Set<String> keywords) {
        this.keywords = keywords;
    }

    @Override
    public String toString() {
        return "NasaSirsImage [" + (id != null ? "id=" + id + ", " : "")
                + (title != null ? "title=" + title + ", " : "")
                + (category != null ? "category=" + category + ", " : "")
                + (date != null ? "photoDate=" + date + ", " : "")
                + (year != null ? "photoYear=" + year + ", " : "")
                + (description != null ? "description=" + description + ", " : "")
                + (keywords != null ? "keywords=" + keywords + ", " : "")
                + "metadata=" + metadata + "]";
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hash(id);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj) || getClass() != obj.getClass())
            return false;
        NasaSirsImage other = (NasaSirsImage) obj;
        return Objects.equals(id, other.id);
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
