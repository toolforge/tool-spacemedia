package org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.svs.api;

import java.util.Arrays;
import java.util.List;

public enum NasaSvsMediaType {
    /** An image file. */
    Image,
    /** A video file. */
    Movie,
    /**
     * A set of images that are individual frames from a particular video. Note:
     * despite being treated as individual media items, Frames are actually groups
     * of individual images.
     */
    Frames,
    /** A set of captions for a particular video. */
    Captions,
    /** An audio file. */
    Audio,
    /** A presentation file (usually PowerPoint or .pdf). */
    Presentation,
    /** A file that can't be categorized into one of the other media types. */
    File;

    public boolean shouldBeOkForCommons() {
        return this != Frames && this != Captions;
    }

    public static List<NasaSvsMediaType> typesOkForCommons() {
        return Arrays.stream(values()).filter(NasaSvsMediaType::shouldBeOkForCommons).toList();
    }
}