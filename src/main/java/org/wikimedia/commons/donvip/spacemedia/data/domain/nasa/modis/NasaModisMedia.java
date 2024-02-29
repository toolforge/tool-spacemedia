package org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.modis;

import java.util.Objects;

import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.SingleFileMedia;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;

@Entity
public class NasaModisMedia extends SingleFileMedia {

    @Column(nullable = false, length = 8)
    private String satellite;

    @Column(nullable = true, length = 15)
    private String bands;

    public String getSatellite() {
        return satellite;
    }

    public void setSatellite(String satellite) {
        this.satellite = satellite;
    }

    public String getBands() {
        return bands;
    }

    public void setBands(String bands) {
        this.bands = bands;
    }

    @Override
    public boolean isImage() {
        return true;
    }

    @Override
    public boolean isVideo() {
        return false;
    }

    @Override
    public String getUploadId(FileMetadata fileMetadata) {
        return "MODIS " + getPublicationDate();
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hash(satellite, bands);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj) || getClass() != obj.getClass())
            return false;
        NasaModisMedia other = (NasaModisMedia) obj;
        return Objects.equals(satellite, other.satellite)
                && Objects.equals(bands, other.bands);
    }

    public NasaModisMedia copyDataFrom(NasaModisMedia other) {
        super.copyDataFrom(other);
        this.satellite = other.satellite;
        this.bands = other.bands;
        return this;
    }

    @Override
    public String toString() {
        return "NasaModisMedia [id=" + getId() + ", satellite=" + satellite + ", bands=" + bands + ']';
    }
}
