package org.wikimedia.commons.donvip.spacemedia.utils;

import java.awt.image.BufferedImage;

public record ImageAndMetadata(BufferedImage image, Long contentLength, String filename, String extension) {

}
