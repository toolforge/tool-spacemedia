package org.wikimedia.commons.donvip.spacemedia.data.domain.copernicus.gallery;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;

import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.SingleFileMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.WithKeywords;

@Entity
public class CopernicusGalleryMedia extends SingleFileMedia implements WithKeywords {

    @Column(nullable = false)
    private String location;

    @Column
    @ElementCollection(fetch = FetchType.EAGER)
    private Set<String> keywords = new HashSet<>();

    @Override
    public Set<String> getKeywords() {
        return keywords;
    }

    @Override
    public void setKeywords(Set<String> keywords) {
        this.keywords = keywords;
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
    protected String getUploadId(FileMetadata fileMetadata) {
        return "Copernicus";
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hash(location, keywords);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj) || getClass() != obj.getClass())
            return false;
        CopernicusGalleryMedia other = (CopernicusGalleryMedia) obj;
        return Objects.equals(location, other.location)
                && Objects.equals(keywords, other.keywords);
    }

    @Override
    public String toString() {
        return "CopernicusGalleryMedia [id=" + getId() + ", title=" + title + "]";
    }

    public CopernicusGalleryMedia copyDataFrom(CopernicusGalleryMedia mediaFromApi) {
        super.copyDataFrom(mediaFromApi);
        this.location = mediaFromApi.location;
        return this;
    }
}
