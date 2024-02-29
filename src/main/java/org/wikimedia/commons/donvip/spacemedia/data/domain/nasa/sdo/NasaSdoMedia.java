package org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.sdo;

import java.util.Objects;

import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.SingleFileMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.library.NasaMediaType;
import org.wikimedia.commons.donvip.spacemedia.service.wikimedia.CommonsService;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Entity
public class NasaSdoMedia extends SingleFileMedia {

    @Enumerated(EnumType.ORDINAL)
    @Column(nullable = false, columnDefinition = "TINYINT default 0")
    private NasaMediaType mediaType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 6)
    private NasaSdoDataType dataType;

    @Embedded
    private NasaSdoKeywords keywords = new NasaSdoKeywords();

    public NasaMediaType getMediaType() {
        return mediaType;
    }

    public void setMediaType(NasaMediaType mediaType) {
        this.mediaType = mediaType;
    }

    public NasaSdoDataType getDataType() {
        return dataType;
    }

    public void setDataType(NasaSdoDataType dataType) {
        this.dataType = dataType;
    }

    public NasaSdoKeywords getKeywords() {
        return keywords;
    }

    public void setKeywords(NasaSdoKeywords keywords) {
        this.keywords = keywords;
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
        return String.format("SDO_%s (%s)", CommonsService.normalizeFilename(title), dataType.getInstrument().name());
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hash(dataType, mediaType, keywords);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj) || getClass() != obj.getClass())
            return false;
        NasaSdoMedia other = (NasaSdoMedia) obj;
        return dataType == other.dataType && mediaType == other.mediaType
                && Objects.equals(keywords, other.keywords);
    }

    @Override
    public String toString() {
        return "NasaSdoMedia [id=" + getId() + ", mediaType=" + mediaType + ", dataType=" + dataType + ']';
    }
}
