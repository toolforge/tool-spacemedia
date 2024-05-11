package org.wikimedia.commons.donvip.spacemedia.utils;

import javax.imageio.IIOException;

public record ContentsAndMetadata<T>(T contents, Long contentLength, String filename, String extension,
        int numImagesOrPages, IIOException iioException) {
}
