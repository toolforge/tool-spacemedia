package org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.svs.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum NasaSvsPageType {
    Visualization,
    @JsonProperty("Produced Video")
    Produced_Video,
    @JsonProperty("Hyperwall Visual")
    Hyperwall_Visual
}
