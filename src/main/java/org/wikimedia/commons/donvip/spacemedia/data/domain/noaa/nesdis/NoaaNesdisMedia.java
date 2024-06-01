package org.wikimedia.commons.donvip.spacemedia.data.domain.noaa.nesdis;

import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.Media;

import jakarta.persistence.Entity;

@Entity
public class NoaaNesdisMedia extends Media {

    @Override
    public String toString() {
        return "NoaaNesdisMedia [publicationDate=" + getPublicationDate() + ", id=" + getId() + ']';
    }

    @Override
    public String getUploadId(FileMetadata fileMetadata) {
        return "NESDIS " + getPublicationDate()
                + (getMetadataCount() < 2 ? "" : " " + fileMetadata.getFileName().split("\\.")[0]);
    }

    public NoaaNesdisMedia copyDataFrom(NoaaNesdisMedia other) {
        super.copyDataFrom(other);
        return this;
    }
}
