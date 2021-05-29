package org.wikimedia.commons.donvip.spacemedia.data.jpa;

import java.net.URL;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;

@Entity
public class Organization extends Person {

    /**
     * Organization website URL.
     */
    @Column(nullable = true)
    private URL website;

    public URL getWebsite() {
        return website;
    }

    public void setWebsite(URL website) {
        this.website = website;
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hash(website);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj) || getClass() != obj.getClass())
            return false;
        Organization other = (Organization) obj;
        return Objects.equals(website, other.website);
    }
}
