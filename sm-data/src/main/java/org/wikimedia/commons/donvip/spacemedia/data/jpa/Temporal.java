package org.wikimedia.commons.donvip.spacemedia.data.jpa;

import java.time.Duration;

public interface Temporal {

    Duration getDuration();

    void setDuration(Duration duration);
}
