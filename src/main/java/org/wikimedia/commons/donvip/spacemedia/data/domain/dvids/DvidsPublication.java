package org.wikimedia.commons.donvip.spacemedia.data.domain.dvids;

import jakarta.persistence.Entity;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

@Entity
@Indexed
public class DvidsPublication extends DvidsMedia {

    @Override
    public String toString() {
        return "DvidsPublication ["
                + (getId() != null ? "id=" + getId() + ", " : "")
                + (getTitle() != null ? "title=" + getTitle() + ", " : "")
                + (getDescription() != null ? "description=" + getDescription() : "") + "]";
    }
}
