package org.wikimedia.commons.donvip.spacemedia.data.domain.base;

import java.time.temporal.Temporal;

import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;

@MappedSuperclass
public abstract class SingleFileMedia<ID, D extends Temporal> extends Media<ID, D> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SingleFileMedia.class);

    @Transient
    @JsonIgnore
    public FileMetadata getUniqueMetadata() {
        if (getMetadata().isEmpty()) {
            LOGGER.debug("Adding initial metadata for single file media {}", this);
            addMetadata(new FileMetadata());
        }
        if (getMetadata().size() != 1) {
            LOGGER.error("Single file media {} contains not exactly one metadata: {}", this, getMetadata());
        }
        return getMetadata().iterator().next();
    }
}
