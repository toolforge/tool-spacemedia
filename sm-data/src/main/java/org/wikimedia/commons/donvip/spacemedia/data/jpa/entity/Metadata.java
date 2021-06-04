package org.wikimedia.commons.donvip.spacemedia.data.jpa.entity;

import java.io.Serializable;
import java.util.Objects;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.Transient;

@Entity
public class Metadata implements Serializable {

    private static final long serialVersionUID = 1L;

    @EmbeddedId
    private MetadataKey mkey;

    @Lob
    private String value;

    public Metadata() {

    }

    public Metadata(MetadataKey mkey, String value) {
        this.mkey = mkey;
        this.value = value;
    }

    public Metadata(String context, String key, String value) {
        this(MetadataKey.from(context, key, value), value);
    }

    public MetadataKey getMkey() {
        return mkey;
    }

    public void setMkey(MetadataKey mkey) {
        this.mkey = mkey;
    }

    @Transient
    public String getKey() {
        return mkey.getKey();
    }

    @Transient
    public String getContext() {
        return mkey.getContext();
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public int hashCode() {
        return mkey.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        Metadata other = (Metadata) obj;
        return Objects.equals(mkey, other.mkey);
    }
}
