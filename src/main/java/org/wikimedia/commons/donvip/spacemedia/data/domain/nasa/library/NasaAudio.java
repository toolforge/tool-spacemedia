package org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.library;

import jakarta.persistence.Entity;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

@Entity
@Indexed
public class NasaAudio extends NasaMedia {

    @Override
    public String toString() {
        return "NasaAudio ["
                + (getId() != null ? "nasaId=" + getId() + ", " : "")
                + (getTitle() != null ? "title=" + getTitle() + ", " : "")
                + (getDescription() != null ? "description=" + getDescription() : "") + ']';
    }
}
