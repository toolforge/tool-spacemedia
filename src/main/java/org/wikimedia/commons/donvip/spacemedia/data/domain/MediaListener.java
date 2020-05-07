package org.wikimedia.commons.donvip.spacemedia.data.domain;

import java.time.LocalDateTime;

import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;

public class MediaListener {

    @PrePersist
    @PreUpdate
    public void methodExecuteBeforeSave(Media<?, ?> media) {
        media.setLastUpdate(LocalDateTime.now());
    }
}
