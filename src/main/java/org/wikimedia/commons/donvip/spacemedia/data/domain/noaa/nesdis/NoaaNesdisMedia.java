package org.wikimedia.commons.donvip.spacemedia.data.domain.noaa.nesdis;

import org.wikimedia.commons.donvip.spacemedia.data.domain.base.Media;

import jakarta.persistence.Entity;

@Entity
public class NoaaNesdisMedia extends Media {

    @Override
    public String toString() {
        return "NoaaNesdisMedia [publicationDate=" + getPublicationDate() + ", id=" + getId() + ']';
    }

    public NoaaNesdisMedia copyDataFrom(NoaaNesdisMedia other) {
        super.copyDataFrom(other);
        return this;
    }
}
