package org.wikimedia.commons.donvip.spacemedia.data.domain.dvids;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum DvidsAspectRatio {

    @JsonProperty("4:3")
    four_by_three,
    @JsonProperty("16:9")
    sixteen_by_nine,
    landscape,
    portrait,
    square;
}
