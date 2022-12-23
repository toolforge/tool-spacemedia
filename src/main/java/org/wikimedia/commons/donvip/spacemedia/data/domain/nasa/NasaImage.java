package org.wikimedia.commons.donvip.spacemedia.data.domain.nasa;

import javax.persistence.Column;
import javax.persistence.Entity;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

@Entity
@Indexed
public class NasaImage extends NasaMedia {

    @Column(length = 300)
    private String photographer;

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
                + (getDate() != null ? "date=" + getDate() + ", " : "")
                + (getDescription() != null ? "description=" + getDescription() + ", " : "")
                + (photographer != null ? "photographer=" + photographer : "") + ']';
    }

    NasaImage copyDataFrom(NasaImage mediaFromApi) {
        super.copyDataFrom(mediaFromApi);
        setPhotographer(mediaFromApi.getPhotographer());
        return this;
    }
}
