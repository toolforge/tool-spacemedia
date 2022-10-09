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

    static Set<DvidsMediaType> images() {
        return Set.of(image);
    }

    static Set<DvidsMediaType> videos() {
        return Set.of(video, graphic);
    }
}
