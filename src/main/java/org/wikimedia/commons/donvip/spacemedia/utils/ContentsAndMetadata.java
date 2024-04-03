package org.wikimedia.commons.donvip.spacemedia.utils;

public record ContentsAndMetadata<T>(T contents, Long contentLength, String filename, String extension,
        int numImagesOrPages) {
}
