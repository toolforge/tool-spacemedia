package org.wikimedia.commons.donvip.spacemedia.data.domain.base;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Transient;

@MappedSuperclass
public abstract class SingleFileMedia extends Media {

    @Transient
    @JsonIgnore
    public FileMetadata getUniqueMetadata() {
        if (getMetadataCount() != 1) {
            throw new IllegalStateException(
                    "Single file media " + this + " contains not exactly one metadata: " + getMetadata());
        }
        return getMetadata().iterator().next();
    }

    public void setIgnored(Boolean ignored) {
        getUniqueMetadata().setIgnored(ignored);
    }

    @JsonIgnore
    public String getIgnoredReason() {
        return getUniqueMetadata().getIgnoredReason();
    }

    public void setIgnoredReason(String ignoredReason) {
        getUniqueMetadata().setIgnoredReason(ignoredReason);
    }
}
