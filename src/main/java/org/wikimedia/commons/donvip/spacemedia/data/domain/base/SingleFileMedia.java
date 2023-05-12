package org.wikimedia.commons.donvip.spacemedia.data.domain.base;

import java.time.temporal.Temporal;

import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import com.fasterxml.jackson.annotation.JsonIgnore;

@MappedSuperclass
public abstract class SingleFileMedia<ID, D extends Temporal> extends Media<ID, D> {

    @Transient
    @JsonIgnore
    public FileMetadata getUniqueMetadata() {
        if (getMetadata().isEmpty()) {
            addMetadata(new FileMetadata());
        }
        return getMetadata().get(0);
    }
}
