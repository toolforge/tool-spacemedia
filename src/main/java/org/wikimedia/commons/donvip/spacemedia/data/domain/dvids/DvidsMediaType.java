package org.wikimedia.commons.donvip.spacemedia.data.domain.dvids;

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
}
