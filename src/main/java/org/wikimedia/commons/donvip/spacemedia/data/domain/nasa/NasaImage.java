package org.wikimedia.commons.donvip.spacemedia.data.domain.nasa;

import javax.persistence.Entity;

@Entity
public class NasaImage extends NasaMedia {

    private String photographer;

    public NasaImage() {
        super();
    }

    public String getPhotographer() {
        return photographer;
    }

    public void setPhotographer(String photographer) {
        this.photographer = photographer;
    }

    @Override
    public String toString() {
        return "NasaImage ["
                + (getId() != null ? "nasaId=" + getId() + ", " : "")
                + (getTitle() != null ? "title=" + getTitle() + ", " : "")
                + (getCenter() != null ? "center=" + getCenter() + ", " : "")
                + (getDateCreated() != null ? "dateCreated=" + getDateCreated() + ", " : "")
                + (getDescription() != null ? "description=" + getDescription() + ", " : "")
                + (photographer != null ? "photographer=" + photographer : "") + "]";
    }
}
