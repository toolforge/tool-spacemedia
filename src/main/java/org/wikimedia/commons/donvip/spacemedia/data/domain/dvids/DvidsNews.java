package org.wikimedia.commons.donvip.spacemedia.data.domain.dvids;

import javax.persistence.Entity;

import org.hibernate.search.annotations.Indexed;

@Entity
@Indexed
public class DvidsNews extends DvidsMedia {

    @Override
    public String toString() {
        return "DvidsNews ["
                + (getId() != null ? "id=" + getId() + ", " : "")
                + (getTitle() != null ? "title=" + getTitle() + ", " : "")
                + (getDatePublished() != null ? "datePublished=" + getDatePublished() + ", " : "")
                + (getDate() != null ? "date=" + getDate() + ", " : "")
                + (getDescription() != null ? "description=" + getDescription() : "") + "]";
    }
}
