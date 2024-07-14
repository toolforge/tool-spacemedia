package org.wikimedia.commons.donvip.spacemedia.data.domain.dinamis;

import java.util.Objects;

import org.apache.commons.lang3.ArrayUtils;
import org.locationtech.jts.algorithm.Centroid;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.impl.PackedCoordinateSequence;
import org.springframework.data.geo.Point;
import org.springframework.data.geo.Polygon;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.Media;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.WithLatLon;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Transient;

@Entity
public class DinamisMedia extends Media implements WithLatLon {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 8)
    private Mode mode;

    @Column(nullable = false)
    private double resolution;

    @Column(nullable = false, length = 512)
    private Polygon polygon;

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public double getResolution() {
        return resolution;
    }

    public void setResolution(double resolution) {
        this.resolution = resolution;
    }

    @Override
    @Transient
    public double getLatitude() {
        return getCentroid().getY();
    }

    @Override
    @Transient
    public void setLatitude(double latitude) {
        // Do nothing
    }

    @Override
    @Transient
    public double getLongitude() {
        return getCentroid().getX();
    }

    @Override
    @Transient
    public void setLongitude(double longitude) {
        // Do nothing
    }

    @Override
    @Transient
    public double getPrecision() {
        return 0;
    }

    public Polygon getPolygon() {
        return polygon;
    }

    public void setPolygon(Polygon polygon) {
        this.polygon = polygon;
    }

    @Transient
    @JsonIgnore
    public Point getCentroid() {
        Coordinate centroid = Centroid
                .getCentroid(new GeometryFactory().createPolygon(new PackedCoordinateSequence.Double(
                        ArrayUtils.toPrimitive(polygon.getPoints().toArray(new Double[] {})), 2, 0)));
        return new Point(centroid.x, centroid.y);
    }

    @Override
    public String getUploadId(FileMetadata fileMetadata) {
        return "Dinamis " + super.getUploadId(fileMetadata);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hash(mode, resolution);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj) || getClass() != obj.getClass())
            return false;
        DinamisMedia other = (DinamisMedia) obj;
        return mode == other.mode && Double.doubleToLongBits(resolution) == Double.doubleToLongBits(other.resolution);
    }

    @Override
    public String toString() {
        return "DinamisMedia [id=" + getId() + ", " + (mode != null ? "mode=" + mode + ", " : "") + "resolution="
                + resolution + "]";
    }

    public DinamisMedia copyDataFrom(DinamisMedia media) {
        super.copyDataFrom(media);
        setMode(media.getMode());
        setPolygon(media.getPolygon());
        setResolution(media.getResolution());
        return this;
    }

    public enum Mode {
        MS, PAN, PMS
    }
}
