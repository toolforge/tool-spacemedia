package org.wikimedia.commons.donvip.spacemedia.data.domain.stac;

import java.net.URL;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.wikidata.wdtk.datamodel.interfaces.GlobeCoordinatesValue;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.Media;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.WithLatLon;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Indexed
@Table(indexes = { @Index(columnList = "url") })
public class StacMedia extends Media implements WithLatLon {

    @Column(nullable = false, unique = true, length = 255)
    private URL url;

    private double latitude;

    private double longitude;

    public URL getUrl() {
        return url;
    }

    public void setUrl(URL url) {
        this.url = url;
    }

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

    @Override
    public boolean isCOG() {
        return latitude != 0 && longitude != 0
                && getMetadataStream().map(FileMetadata::getAssetUri).anyMatch(x -> x.toString().contains(".tif"));
    }

    public StacMedia copyDataFrom(StacMedia other) {
        super.copyDataFrom(other);
        this.url = other.url;
        return this;
    }

    @Override
    public String toString() {
        return "StacMedia [" + (getId() != null ? "id=" + getId() + ", " : "")
                + (title != null ? "title=" + title + ", " : "") + "metadata=" + getMetadata() + ']';
    }
}
