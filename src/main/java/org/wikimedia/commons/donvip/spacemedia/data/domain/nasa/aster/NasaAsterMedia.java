package org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.aster;

import java.util.Objects;

import org.wikidata.wdtk.datamodel.interfaces.GlobeCoordinatesValue;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.Media;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.WithLatLon;
import org.wikimedia.commons.donvip.spacemedia.service.wikimedia.CommonsService;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;

@Entity
public class NasaAsterMedia extends Media implements WithLatLon {

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    @Column(nullable = false, length = 64)
    private String longName;

    @Column(nullable = false, length = 32)
    private String category;

    @Column(nullable = false, length = 16)
    private String icon;

    @Override
    public double getLatitude() {
        return latitude;
    }

    @Override
    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    @Override
    public double getLongitude() {
        return longitude;
    }

    @Override
    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    @Override
    public double getPrecision() {
        return GlobeCoordinatesValue.PREC_DECI_DEGREE;
    }

    public String getLongName() {
        return longName;
    }

    public void setLongName(String longName) {
        this.longName = longName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    @Override
    public String getUploadTitle(FileMetadata fileMetadata) {
        return (getMetadataCount() < 2 ? CommonsService.normalizeFilename(title) : fileMetadata.getFileName())
                + " (ASTER)";
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode()
                + Objects.hash(latitude, longitude, longName, category, icon);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj) || getClass() != obj.getClass())
            return false;
        NasaAsterMedia other = (NasaAsterMedia) obj;
        return latitude == other.latitude && longitude == other.longitude
                && Objects.equals(longName, other.longName) && Objects.equals(category, other.category)
                && Objects.equals(icon, other.icon);
    }

    @Override
    public String toString() {
        return "NasaAsterMedia [id=" + getId() + ", latitude="
                + latitude + ", longitude=" + longitude + ", longName=" + longName
                + ", category=" + category + ", icon=" + icon + ']';
    }

    public NasaAsterMedia copyDataFrom(NasaAsterMedia mediaFromApi) {
        super.copyDataFrom(mediaFromApi);
        this.longName = mediaFromApi.longName;
        this.category = mediaFromApi.category;
        this.icon = mediaFromApi.icon;
        return this;
    }
}
