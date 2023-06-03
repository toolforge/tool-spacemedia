package org.wikimedia.commons.donvip.spacemedia.data.domain.djangoplicity;

import java.io.Serializable;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class DjangoplicityMediaId implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(length = 16)
    private String website;

    @Column(length = 127)
    private String id;

    public DjangoplicityMediaId() {

    }

    public DjangoplicityMediaId(String jsonId) {
        String[] tab = jsonId.split(":");
        this.website = tab[0];
        this.id = tab[1];
    }

    public DjangoplicityMediaId(String website, String id) {
        this.website = website;
        this.id = id;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, website);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        DjangoplicityMediaId other = (DjangoplicityMediaId) obj;
        return Objects.equals(id, other.id) && Objects.equals(website, other.website);
    }

    @Override
    public String toString() {
        return "[website=" + website + ", id=" + id + ']';
    }
}
