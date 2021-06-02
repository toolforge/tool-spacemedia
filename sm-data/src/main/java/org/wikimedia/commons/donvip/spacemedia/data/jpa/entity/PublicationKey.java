package org.wikimedia.commons.donvip.spacemedia.data.jpa.entity;

import java.io.Serializable;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class PublicationKey implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(length = 32)
    private String depot_id;

    @Column(length = 255)
    private String id;

    public PublicationKey() {

    }

    public PublicationKey(String depot_id, String id) {
        this.depot_id = depot_id;
        this.id = id;
    }

    public String getDepotId() {
        return depot_id;
    }

    public void setDepotId(String depotId) {
        this.depot_id = depotId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(depot_id, id);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        PublicationKey other = (PublicationKey) obj;
        return Objects.equals(depot_id, other.depot_id) && Objects.equals(id, other.id);
    }

    @Override
    public String toString() {
        return depot_id + '-' + id;
    }
}
