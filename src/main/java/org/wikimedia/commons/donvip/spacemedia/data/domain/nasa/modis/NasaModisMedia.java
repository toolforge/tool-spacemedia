package org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.modis;

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.SingleFileMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.service.wikimedia.CommonsService;
import org.wikimedia.commons.donvip.spacemedia.utils.Utils;

@Entity
@Indexed
public class NasaModisMedia extends SingleFileMedia {

    @Column(nullable = false, length = 8)
    private String satellite;

    @Column(nullable = true, length = 8)
    private String bands;

    @Column(nullable = false, length = 200)
    private String credit;

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

    public String getCredit() {
        return credit;
    }

    public void setCredit(String credit) {
        this.credit = credit;
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
    public String getUploadTitle(FileMetadata fileMetadata) {
        return (getMetadataCount() < 2 ? CommonsService.normalizeFilename(title)
                : Utils.getFilename(fileMetadata.getAssetUrl())) + " (MODIS)";
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hash(satellite, bands, credit);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj) || getClass() != obj.getClass())
            return false;
        NasaModisMedia other = (NasaModisMedia) obj;
        return Objects.equals(satellite, other.satellite)
                && Objects.equals(bands, other.bands) && Objects.equals(credit, other.credit);
    }

    public NasaModisMedia copyDataFrom(NasaModisMedia other) {
        super.copyDataFrom(other);
        this.satellite = other.satellite;
        this.bands = other.bands;
        this.credit = other.credit;
        return this;
    }

    @Override
    public String toString() {
        return "NasaModisMedia [id=" + getId()
                + ", satellite=" + satellite + ", bands=" + bands + ", credit=" + credit + ']';
    }
}
