package org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.aster;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.temporal.ChronoField;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.wikidata.wdtk.datamodel.interfaces.GlobeCoordinatesValue;
import org.wikimedia.commons.donvip.spacemedia.data.domain.ImageDimensions;
import org.wikimedia.commons.donvip.spacemedia.data.domain.Media;
import org.wikimedia.commons.donvip.spacemedia.data.domain.WithDimensions;
import org.wikimedia.commons.donvip.spacemedia.data.domain.WithLatLon;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.NasaMediaType;
import org.wikimedia.commons.donvip.spacemedia.service.wikimedia.CommonsService;

@Entity
@Indexed
@Table(indexes = { @Index(columnList = "sha1, phash") })
public class NasaAsterMedia extends Media<String, LocalDate> implements WithLatLon, WithDimensions {

    @Id
    @Column(nullable = false, length = 32)
    private String id;

    /**
     * Acquisition date
     */
    @Column(nullable = true)
    private LocalDate date;

    @Column(nullable = false)
    private LocalDateTime publicationDate;

    @Embedded
    private ImageDimensions dimensions;

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

    @Enumerated(EnumType.ORDINAL)
    @Column(nullable = false, columnDefinition = "TINYINT default 0")
    private NasaMediaType mediaType;

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

    public LocalDateTime getPublicationDate() {
        return publicationDate;
    }

    public void setPublicationDate(LocalDateTime publicationDate) {
        this.publicationDate = publicationDate;
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
    public ImageDimensions getImageDimensions() {
        return dimensions;
    }

    @Override
    public void setImageDimensions(ImageDimensions dimensions) {
        this.dimensions = dimensions;
    }

    public NasaMediaType getMediaType() {
        return mediaType;
    }

    public void setMediaType(NasaMediaType mediaType) {
        this.mediaType = mediaType;
    }

    @Override
    public boolean isImage() {
        return mediaType == NasaMediaType.image;
    }

    @Override
    public boolean isVideo() {
        return mediaType == NasaMediaType.video;
    }

    @Override
    public String getUploadTitle() {
        return CommonsService.normalizeFilename(title) + " (ASTER)";
    }

    @Override
    public Year getYear() {
        return Year.of(getDate() != null ? getDate().get(ChronoField.YEAR) : getPublicationDate().getYear());
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode()
                + Objects.hash(id, date, publicationDate, latitude, longitude, longName, category, icon, dimensions,
                        mediaType);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj) || getClass() != obj.getClass())
            return false;
        NasaAsterMedia other = (NasaAsterMedia) obj;
        return Objects.equals(date, other.date) && Objects.equals(publicationDate, other.publicationDate)
                && Objects.equals(id, other.id) && latitude == other.latitude && longitude == other.longitude
                && Objects.equals(longName, other.longName) && Objects.equals(category, other.category)
                && Objects.equals(icon, other.icon) && Objects.equals(dimensions, other.dimensions)
                && mediaType == other.mediaType;
    }

    @Override
    public String toString() {
        return "NasaSdoMedia [id=" + id + ", date=" + date + ", publicationDate=" + publicationDate + ", dimensions="
                + dimensions + ", latitude=" + latitude + ", longitude=" + longitude + ", longName=" + longName
                + ", category=" + category + ", icon=" + icon + ", mediaType=" + mediaType + ']';
    }
}
