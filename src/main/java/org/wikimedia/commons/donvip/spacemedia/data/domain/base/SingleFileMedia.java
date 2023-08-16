package org.wikimedia.commons.donvip.spacemedia.data.domain.base;

import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import com.fasterxml.jackson.annotation.JsonIgnore;

@MappedSuperclass
public abstract class SingleFileMedia<ID> extends Media<ID> {

    @Transient
    @JsonIgnore
    public FileMetadata getUniqueMetadata() {
        if (getMetadataCount() != 1) {
            throw new IllegalStateException(
                    "Single file media " + this + " contains not exactly one metadata: " + getMetadata());
        }
        return getMetadata().iterator().next();
    }
}
