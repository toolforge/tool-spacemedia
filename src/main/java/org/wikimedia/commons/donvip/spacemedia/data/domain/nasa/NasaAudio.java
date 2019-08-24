package org.wikimedia.commons.donvip.spacemedia.data.domain.nasa;

import javax.persistence.Entity;

@Entity
public class NasaAudio extends NasaMedia {

    @Override
    public String toString() {
        return "NasaAudio ["
                + (getId() != null ? "nasaId=" + getId() + ", " : "")
                + (getTitle() != null ? "title=" + getTitle() + ", " : "")
                + (getCenter() != null ? "center=" + getCenter() + ", " : "")
                + (getDateCreated() != null ? "dateCreated=" + getDateCreated() + ", " : "")
                + (getDescription() != null ? "description=" + getDescription() : "") + "]";
    }

}
