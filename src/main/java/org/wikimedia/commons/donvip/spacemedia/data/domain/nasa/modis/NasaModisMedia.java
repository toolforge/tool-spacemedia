package org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.modis;

import java.time.LocalDate;
import java.time.Year;
import java.time.temporal.ChronoField;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.SingleFileMedia;
import org.wikimedia.commons.donvip.spacemedia.service.wikimedia.CommonsService;
import org.wikimedia.commons.donvip.spacemedia.utils.Utils;

@Entity
@Indexed
public class NasaModisMedia extends SingleFileMedia<String, LocalDate> {

    @Id
    @Column(nullable = false, length = 32)
    private String id;

    /**
     * Acquisition date
     */
    @Column(nullable = true)
    private LocalDate date;

    @Column(nullable = false)
    private LocalDate publicationDate;

    @Column(nullable = false, length = 8)
    private String satellite;

    @Column(nullable = true, length = 8)
    private String bands;

    @Column(nullable = false, length = 200)
    private String credit;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public LocalDate getDate() {
        return date;
    }

    @Override
    public void setDate(LocalDate date) {
        this.date = date;
    }

    public LocalDate getPublicationDate() {
        return publicationDate;
    }

    public void setPublicationDate(LocalDate publicationDate) {
        this.publicationDate = publicationDate;
    }

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
        return (getMetadata().size() < 2 ? CommonsService.normalizeFilename(title)
                : Utils.getFilename(fileMetadata.getAssetUrl())) + " (MODIS)";
    }

    @Override
    public Year getYear() {
        return Year.of(getDate() != null ? getDate().get(ChronoField.YEAR) : getPublicationDate().getYear());
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hash(id, date, publicationDate, satellite, bands, credit);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj) || getClass() != obj.getClass())
            return false;
        NasaModisMedia other = (NasaModisMedia) obj;
        return Objects.equals(date, other.date) && Objects.equals(publicationDate, other.publicationDate)
                && Objects.equals(id, other.id) && Objects.equals(satellite, other.satellite)
                && Objects.equals(bands, other.bands) && Objects.equals(credit, other.credit);
    }

    @Override
    public String toString() {
        return "NasaModisMedia [id=" + id + ", date=" + date + ", publicationDate=" + publicationDate
                + ", satellite=" + satellite + ", bands=" + bands + ", credit=" + credit + ']';
    }
}
