package org.wikimedia.commons.donvip.spacemedia.data.jpa;

import java.util.Objects;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.ManyToMany;

@Entity
public class MediaPublication extends ContentPublication {

    @ManyToMany(mappedBy = "mediaPublications")
    private Set<WebPublication> webPublications;

    public Set<WebPublication> getWebPublications() {
        return webPublications;
    }

    public void setWebPublications(Set<WebPublication> webPublications) {
        this.webPublications = webPublications;
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hash(webPublications);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj) || getClass() != obj.getClass())
            return false;
        MediaPublication other = (MediaPublication) obj;
        return Objects.equals(webPublications, other.webPublications);
    }
}
