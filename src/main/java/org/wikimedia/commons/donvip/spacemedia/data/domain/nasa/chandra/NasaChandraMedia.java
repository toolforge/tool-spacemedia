package org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.chandra;

import org.wikimedia.commons.donvip.spacemedia.data.domain.base.Media;

import jakarta.persistence.Entity;

@Entity
public class NasaChandraMedia extends Media {

    @Override
    public String toString() {
        return "NasaChandraMedia [publicationDate=" + getPublicationDate() + ", id=" + getId() + ']';
    }

    public NasaChandraMedia copyDataFrom(NasaChandraMedia other) {
        super.copyDataFrom(other);
        return this;
    }
}
