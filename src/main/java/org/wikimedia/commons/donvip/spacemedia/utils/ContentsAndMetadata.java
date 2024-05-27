package org.wikimedia.commons.donvip.spacemedia.utils;

import java.io.IOException;

public record ContentsAndMetadata<T>(T contents, long contentLength, String filename, String extension,
        int numImagesOrPages, IOException ioException) {
}
