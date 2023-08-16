package org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.aster;

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.wikidata.wdtk.datamodel.interfaces.GlobeCoordinatesValue;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.Media;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.WithLatLon;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.library.NasaMediaType;
import org.wikimedia.commons.donvip.spacemedia.service.wikimedia.CommonsService;
import org.wikimedia.commons.donvip.spacemedia.utils.Utils;

@Entity
@Indexed
public class NasaAsterMedia extends Media<String> implements WithLatLon {

    @Id
    @Column(nullable = false, length = 32)
    private String id;

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
    public String getUploadTitle(FileMetadata fileMetadata) {
        return (getMetadataCount() < 2 ? CommonsService.normalizeFilename(title)
                : Utils.getFilename(fileMetadata.getAssetUrl())) + " (ASTER)";
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode()
                + Objects.hash(id, latitude, longitude, longName, category, icon, mediaType);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj) || getClass() != obj.getClass())
            return false;
        NasaAsterMedia other = (NasaAsterMedia) obj;
        return Objects.equals(id, other.id) && latitude == other.latitude && longitude == other.longitude
                && Objects.equals(longName, other.longName) && Objects.equals(category, other.category)
                && Objects.equals(icon, other.icon) && mediaType == other.mediaType;
    }

    @Override
    public String toString() {
        return "NasaAsterMedia [id=" + id + ", latitude="
                + latitude + ", longitude=" + longitude + ", longName=" + longName
                + ", category=" + category + ", icon=" + icon + ", mediaType=" + mediaType + ']';
    }
}
