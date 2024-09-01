package org.wikimedia.commons.donvip.spacemedia.data.domain.dvids;

import java.net.URL;

import jakarta.persistence.Embeddable;

@Embeddable
public record DvidsVideoFile (
    URL src,
    String type,
    Short height,
    Short width,
    Long size,
    Short bitrate) {
}
