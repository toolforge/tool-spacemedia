package org.wikimedia.commons.donvip.spacemedia.data.local.nasa;

import javax.persistence.Entity;

@Entity
public class NasaVideo extends NasaMedia {

    @Override
    public String toString() {
        return "NasaVideo ["
                + (getNasaId() != null ? "nasaId=" + getNasaId() + ", " : "")
                + (getTitle() != null ? "title=" + getTitle() + ", " : "")
                + (getCenter() != null ? "center=" + getCenter() + ", " : "")
                + (getDateCreated() != null ? "dateCreated=" + getDateCreated() + ", " : "")
                + (getDescription() != null ? "description=" + getDescription() : "") + "]";
    }
}
