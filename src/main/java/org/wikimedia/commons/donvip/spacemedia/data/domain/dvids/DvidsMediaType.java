package org.wikimedia.commons.donvip.spacemedia.data.domain.dvids;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;

@JsonFormat(shape = Shape.STRING)
public enum DvidsMediaType {
    audio,
    graphic,
    image,
    news,
    publication_issue,
    video,
    webcast;

    static Set<String> images() {
        return Set.of(image.name());
    }

    static Set<String> videos() {
        return Set.of(video.name(), graphic.name());
    }
}
