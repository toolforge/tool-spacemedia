package org.wikimedia.commons.donvip.spacemedia.data.jpa;

import java.util.Objects;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.ManyToMany;

@Entity
public class WebPublication extends ContentPublication {

    @ManyToMany
    private Set<MediaPublication> mediaPublications;

    public Set<MediaPublication> getMediaPublications() {
        return mediaPublications;
    }

    public void setMediaPublications(Set<MediaPublication> mediaPublications) {
        this.mediaPublications = mediaPublications;
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hash(mediaPublications);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj) || getClass() != obj.getClass())
            return false;
        WebPublication other = (WebPublication) obj;
        return Objects.equals(mediaPublications, other.mediaPublications);
    }
}
