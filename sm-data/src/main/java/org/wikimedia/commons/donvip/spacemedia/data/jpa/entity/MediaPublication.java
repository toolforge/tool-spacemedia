package org.wikimedia.commons.donvip.spacemedia.data.jpa.entity;

import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.ManyToMany;

@Entity
public class MediaPublication extends ContentPublication {

    @ManyToMany(mappedBy = "mediaPublications", cascade = CascadeType.REMOVE)
    private Set<WebPublication> webPublications;

    public Set<WebPublication> getWebPublications() {
        return webPublications;
    }

    public void setWebPublications(Set<WebPublication> webPublications) {
        this.webPublications = webPublications;
    }
}
