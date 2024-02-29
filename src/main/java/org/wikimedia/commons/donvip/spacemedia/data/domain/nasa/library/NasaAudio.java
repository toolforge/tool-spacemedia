package org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.library;

import jakarta.persistence.Entity;

@Entity
public class NasaAudio extends NasaMedia {

    @Override
    public String toString() {
        return "NasaAudio ["
                + (getId() != null ? "nasaId=" + getId() + ", " : "")
                + (getTitle() != null ? "title=" + getTitle() + ", " : "")
                + (getDescription() != null ? "description=" + getDescription() : "") + ']';
    }
}
