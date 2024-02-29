package org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.library;

import jakarta.persistence.Entity;

@Entity
public class NasaVideo extends NasaMedia {

    @Override
    public String toString() {
        return "NasaVideo ["
                + (getId() != null ? "nasaId=" + getId() + ", " : "")
                + (getTitle() != null ? "title=" + getTitle() + ", " : "")
                + (getDescription() != null ? "description=" + getDescription() : "") + ']';
    }
}
