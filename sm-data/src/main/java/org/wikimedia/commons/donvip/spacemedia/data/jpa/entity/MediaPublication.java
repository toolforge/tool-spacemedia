package org.wikimedia.commons.donvip.spacemedia.data.jpa.entity;

import java.net.URL;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.ManyToMany;

@Entity
public class MediaPublication extends ContentPublication {

    @ManyToMany(mappedBy = "mediaPublications", cascade = CascadeType.REMOVE)
    private Set<WebPublication> webPublications;

    public MediaPublication() {

    }

    public MediaPublication(Depot depot, PublicationKey key, URL url) {
        super(depot, key, url);
    }

    public MediaPublication(Depot depot, String id, URL url) {
        this(depot, new PublicationKey(depot.getId(), id), url);
    }

    public Set<WebPublication> getWebPublications() {
        return webPublications;
    }

    public void setWebPublications(Set<WebPublication> webPublications) {
        this.webPublications = webPublications;
    }
}
