package org.wikimedia.commons.donvip.spacemedia.data.jpa;

import java.net.URL;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class Depot {

    @Id
    @Column(nullable = false, length = 32)
    private String id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false, unique = true)
    private URL website;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public URL getWebsite() {
        return website;
    }

    public void setWebsite(URL website) {
        this.website = website;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, website);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        Depot other = (Depot) obj;
        return Objects.equals(id, other.id) && Objects.equals(name, other.name)
                && Objects.equals(website, other.website);
    }
}
