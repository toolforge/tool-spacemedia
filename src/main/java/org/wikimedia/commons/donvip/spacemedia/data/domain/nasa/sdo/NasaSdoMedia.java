package org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.sdo;

import java.time.LocalDateTime;
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
import org.wikimedia.commons.donvip.spacemedia.data.domain.ImageDimensions;
import org.wikimedia.commons.donvip.spacemedia.data.domain.Media;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.NasaMediaType;
import org.wikimedia.commons.donvip.spacemedia.service.wikimedia.CommonsService;

@Entity
@Indexed
@Table(indexes = { @Index(columnList = "sha1, phash") })
public class NasaSdoMedia extends Media<String, LocalDateTime> {

    @Id
    @Column(nullable = false, length = 32)
    private String id;

    @Column(nullable = false)
    private LocalDateTime date;

    @Enumerated(EnumType.ORDINAL)
    @Column(nullable = false, columnDefinition = "TINYINT default 0")
    private NasaMediaType mediaType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private NasaSdoInstrument instrument;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 6)
    private NasaSdoDataType dataType;

    @Embedded
    private ImageDimensions dimensions;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public LocalDateTime getDate() {
        return date;
    }

    @Override
    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public NasaMediaType getMediaType() {
        return mediaType;
    }

    public void setMediaType(NasaMediaType mediaType) {
        this.mediaType = mediaType;
    }

    public NasaSdoInstrument getInstrument() {
        return instrument;
    }

    public void setInstrument(NasaSdoInstrument instrument) {
        this.instrument = instrument;
    }

    public ImageDimensions getDimensions() {
        return dimensions;
    }

    public NasaSdoDataType getDataType() {
        return dataType;
    }

    public void setDataType(NasaSdoDataType dataType) {
        this.dataType = dataType;
    }

    public void setDimensions(ImageDimensions dimensions) {
        this.dimensions = dimensions;
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
        return String.format("SDO_%s (%s %s)", CommonsService.normalizeFilename(title), instrument.name(),
                dataType.name().replace("_", ""));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + Objects.hash(dataType, date, dimensions, id, instrument, mediaType);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        NasaSdoMedia other = (NasaSdoMedia) obj;
        return dataType == other.dataType && Objects.equals(date, other.date)
                && Objects.equals(dimensions, other.dimensions) && Objects.equals(id, other.id)
                && instrument == other.instrument && mediaType == other.mediaType;
    }

    @Override
    public String toString() {
        return "NasaSdoMedia [id=" + id + ", date=" + date + ", mediaType=" + mediaType + ", instrument=" + instrument
                + ", dataType=" + dataType + ", dimensions=" + dimensions + "]";
    }
}
