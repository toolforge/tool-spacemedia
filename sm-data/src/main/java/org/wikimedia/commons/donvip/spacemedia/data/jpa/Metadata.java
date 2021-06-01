package org.wikimedia.commons.donvip.spacemedia.data.jpa;

import java.io.Serializable;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class Metadata implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "k", length = 32)
    private String key;

    @Id
    @Column(name = "v", length = 255)
    private String value;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, value);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        Metadata other = (Metadata) obj;
        return Objects.equals(key, other.key) && Objects.equals(value, other.value);
    }
}
