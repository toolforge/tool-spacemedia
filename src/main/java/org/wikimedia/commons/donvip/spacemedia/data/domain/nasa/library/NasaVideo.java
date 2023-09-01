package org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.library;

import javax.persistence.Entity;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

@Entity
@Indexed
public class NasaVideo extends NasaMedia {

    @Override
    public String toString() {
        return "NasaVideo ["
                + (getId() != null ? "nasaId=" + getId() + ", " : "")
                + (getTitle() != null ? "title=" + getTitle() + ", " : "")
                + (getDescription() != null ? "description=" + getDescription() : "") + ']';
    }
}
