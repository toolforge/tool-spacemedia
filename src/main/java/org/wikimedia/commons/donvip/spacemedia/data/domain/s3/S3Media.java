package org.wikimedia.commons.donvip.spacemedia.data.domain.s3;

import javax.persistence.Entity;
import javax.persistence.Table;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.wikidata.wdtk.datamodel.interfaces.GlobeCoordinatesValue;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.CompositeMediaId;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.SingleFileMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.WithLatLon;

@Entity
@Indexed
@Table(name = "s3_media")
public class S3Media extends SingleFileMedia implements WithLatLon {

    private double latitude;

    private double longitude;

    public S3Media() {

    }

    public S3Media(String bucketName, String key) {
        setId(new CompositeMediaId(bucketName, key));
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
    protected String getUploadId(FileMetadata fileMetadata) {
        return getUploadTitle();
    }

    public S3Media copyDataFrom(S3Media other) {
        super.copyDataFrom(other);
        return this;
    }

    @Override
    public String toString() {
        return "S3Media [" + (getId() != null ? "id=" + getId() + ", " : "")
                + (title != null ? "title=" + title + ", " : "") + "metadata=" + getMetadata() + ']';
    }
}
