package org.wikimedia.commons.donvip.spacemedia.data.jpa.entity;

import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.ManyToMany;

@Entity
public class WebPublication extends ContentPublication {

    @ManyToMany(cascade = CascadeType.REMOVE)
    private Set<MediaPublication> mediaPublications;

    public Set<MediaPublication> getMediaPublications() {
        return mediaPublications;
    }

    public void setMediaPublications(Set<MediaPublication> mediaPublications) {
        this.mediaPublications = mediaPublications;
    }
}
