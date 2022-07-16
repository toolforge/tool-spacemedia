package org.wikimedia.commons.donvip.spacemedia.data.domain.dvids;

import java.io.Serializable;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Embeddable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;

@Embeddable
public class DvidsMediaTypedId implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonIgnore
    @Column(name = "`type`")
    private DvidsMediaType type;

    @JsonIgnore
    private Long id;

    public DvidsMediaTypedId() {

    }

    public DvidsMediaTypedId(String jsonId) {
        String[] tab = jsonId.split(":");
        this.type = DvidsMediaType.valueOf(tab[0]);
        this.id = Long.parseLong(tab[1]);
    }

    public DvidsMediaType getType() {
        return type;
    }

    public void setType(DvidsMediaType type) {
        this.type = type;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, type);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        DvidsMediaTypedId other = (DvidsMediaTypedId) obj;
        return Objects.equals(id, other.id) && type == other.type;
    }

    @Override
    @JsonValue
    public String toString() {
        return type + ":" + id;
    }
}
