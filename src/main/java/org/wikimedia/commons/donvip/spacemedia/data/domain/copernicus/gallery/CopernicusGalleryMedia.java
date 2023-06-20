package org.wikimedia.commons.donvip.spacemedia.data.domain.copernicus.gallery;

import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Lob;

import org.wikimedia.commons.donvip.spacemedia.data.domain.base.SingleFileMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.WithKeywords;

@Entity
public class CopernicusGalleryMedia extends SingleFileMedia<String, ZonedDateTime> implements WithKeywords {

    @Id
    private String id;

    @Column(nullable = false)
    private ZonedDateTime date;

    @Lob
    @Column(name = "credit", nullable = false, columnDefinition = "TEXT")
    private String credit;

    @Column(nullable = false)
    private String location;

    @Column
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

    @Override
    public ZonedDateTime getDate() {
        return date;
    }

    @Override
    public void setDate(ZonedDateTime date) {
        this.date = date;
    }

    @Override
    public Set<String> getKeywords() {
        return keywords;
    }

    @Override
    public void setKeywords(Set<String> keywords) {
        this.keywords = keywords;
    }

    public String getCredit() {
        return credit;
    }

    public void setCredit(String credit) {
        this.credit = credit;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    @Override
    public final boolean isImage() {
        return true;
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hash(date, id, credit, location, keywords);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj) || getClass() != obj.getClass())
            return false;
        CopernicusGalleryMedia other = (CopernicusGalleryMedia) obj;
        return Objects.equals(date, other.date) && Objects.equals(id, other.id)
                && Objects.equals(credit, other.credit)
                && Objects.equals(location, other.location)
                && Objects.equals(keywords, other.keywords);
    }

    @Override
    public String toString() {
        return "CopernicusGalleryMedia [id=" + id + ", date=" + date + ", title=" + title + "]";
    }

    public CopernicusGalleryMedia copyDataFrom(CopernicusGalleryMedia mediaFromApi) {
        super.copyDataFrom(mediaFromApi);
        this.credit = mediaFromApi.credit;
        this.location = mediaFromApi.location;
        this.keywords = mediaFromApi.keywords;
        return this;
    }
}