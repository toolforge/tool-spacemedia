package org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.library;

import javax.persistence.Column;
import javax.persistence.Entity;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
@Indexed
@JsonIgnoreProperties(ignoreUnknown = true)
public class NasaImage extends NasaMedia {

    @Column(length = 300, nullable = true)
    private String photographer;

    @JsonProperty("secondary_creator")
    @Column(length = 300, nullable = true)
    private String secondaryCreator;

    public String getPhotographer() {
        return photographer;
    }

    public void setPhotographer(String photographer) {
        this.photographer = photographer;
    }

    public String getSecondaryCreator() {
        return secondaryCreator;
    }

    public void setSecondaryCreator(String secondaryCreator) {
        this.secondaryCreator = secondaryCreator;
    }

    @Override
    public String toString() {
        return "NasaImage ["
                + (getId() != null ? "nasaId=" + getId() + ", " : "")
                + (getTitle() != null ? "title=" + getTitle() + ", " : "")
                + (getDescription() != null ? "description=" + getDescription() + ", " : "")
                + (photographer != null ? "photographer=" + photographer : "")
                + (secondaryCreator != null ? "secondaryCreator=" + secondaryCreator : "") + ']';
    }

    NasaImage copyDataFrom(NasaImage mediaFromApi) {
        super.copyDataFrom(mediaFromApi);
        setPhotographer(mediaFromApi.getPhotographer());
        setSecondaryCreator(mediaFromApi.getSecondaryCreator());
        return this;
    }
}
