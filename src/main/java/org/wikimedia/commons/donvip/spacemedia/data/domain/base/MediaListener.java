package org.wikimedia.commons.donvip.spacemedia.data.domain.base;

import java.time.LocalDateTime;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

public class MediaListener {

    @PrePersist
    @PreUpdate
    public void methodExecuteBeforeSave(Media media) {
        media.setLastUpdate(LocalDateTime.now());
    }
}
