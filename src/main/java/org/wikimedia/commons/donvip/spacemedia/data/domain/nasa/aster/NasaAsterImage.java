package org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.aster;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.wikimedia.commons.donvip.spacemedia.data.domain.ImageDimensions;
import org.wikimedia.commons.donvip.spacemedia.data.domain.Media;

@Entity
@Indexed
@Table(indexes = { @Index(columnList = "sha1, phash") })
public class NasaAsterImage extends Media<String, LocalDate> {

    @Id
    @Column(nullable = false, length = 32)
    private String id;

    /**
     * Acquisition date
     */
    @Column(nullable = false)
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

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
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

    public ImageDimensions getDimensions() {
        return dimensions;
    }

    public void setDimensions(ImageDimensions dimensions) {
        this.dimensions = dimensions;
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode()
                + Objects.hash(id, date, publicationDate, latitude, longitude, longName, category, icon, dimensions);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj) || getClass() != obj.getClass())
            return false;
        NasaAsterImage other = (NasaAsterImage) obj;
        return Objects.equals(date, other.date) && Objects.equals(publicationDate, other.publicationDate)
                && Objects.equals(id, other.id) && latitude == other.latitude && longitude == other.longitude
                && Objects.equals(longName, other.longName) && Objects.equals(category, other.category)
                && Objects.equals(icon, other.icon) && Objects.equals(dimensions, other.dimensions);
    }

    @Override
    public String toString() {
        return "NasaAsterImage [id=" + id + ", date=" + date + ", publicationDate=" + publicationDate + ", dimensions="
                + dimensions + ", latitude=" + latitude + ", longitude=" + longitude + ", longName=" + longName
                + ", category=" + category + ", icon=" + icon + "]";
    }
}
