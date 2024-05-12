package org.wikimedia.commons.donvip.spacemedia.data.domain.stac;

import java.net.URL;

import org.wikidata.wdtk.datamodel.interfaces.GlobeCoordinatesValue;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.Media;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.WithLatLon;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(indexes = { @Index(columnList = "url"), @Index(columnList = "productType"), @Index(columnList = "collectId") })
public class StacMedia extends Media implements WithLatLon {

    @Column(nullable = false, unique = true, length = 380)
    private URL url;

    private double latitude;

    private double longitude;

    @Column(nullable = true, length = 3)
    private String productType;

    @Column(nullable = true, length = 36)
    private String collectId;

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

    public String getProductType() {
        return productType;
    }

    public void setProductType(String productType) {
        this.productType = productType;
    }

    public String getCollectId() {
        return collectId;
    }

    public void setCollectId(String collectId) {
        this.collectId = collectId;
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
        this.productType = other.productType;
        this.collectId = other.collectId;
        return this;
    }

    @Override
    public String toString() {
        return "StacMedia [" + (getId() != null ? "id=" + getId() + ", " : "")
                + (title != null ? "title=" + title + ", " : "") + "metadata=" + getMetadata() + ']';
    }
}
