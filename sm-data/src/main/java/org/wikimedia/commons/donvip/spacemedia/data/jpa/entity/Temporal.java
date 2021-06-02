package org.wikimedia.commons.donvip.spacemedia.data.jpa.entity;

import java.time.Duration;

public interface Temporal {

    Duration getDuration();

    void setDuration(Duration duration);
}
