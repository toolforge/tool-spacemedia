package org.wikimedia.commons.donvip.spacemedia.data.domain.dvids;

import jakarta.persistence.Entity;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

@Entity
@Indexed
public class DvidsWebcast extends DvidsMedia {

    @Override
    public String toString() {
        return "DvidsWebcast ["
                + (getId() != null ? "id=" + getId() + ", " : "")
                + (getTitle() != null ? "title=" + getTitle() + ", " : "")
                + (getDescription() != null ? "description=" + getDescription() : "") + "]";
    }
}
