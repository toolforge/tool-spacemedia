package org.wikimedia.commons.donvip.spacemedia.data.local.nasa;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;

@JsonFormat(shape = Shape.STRING)
public enum NasaMediaType {
    image,
    audio,
    video
}
