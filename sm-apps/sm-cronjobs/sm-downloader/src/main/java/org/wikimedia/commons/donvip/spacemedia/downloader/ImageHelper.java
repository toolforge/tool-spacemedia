package org.wikimedia.commons.donvip.spacemedia.downloader;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImageHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageHelper.class);

    /**
     * Returns a <code>BufferedImage</code> as the result of decoding a supplied
     * <code>ImageInputStream</code> with an <code>ImageReader</code> chosen
     * automatically from among those currently registered. If no registered
     * <code>ImageReader</code> claims to be able to read the stream,
     * <code>null</code> is returned.
     *
     * <p>
     * This method <em>does not</em> close the provided
     * <code>ImageInputStream</code>.
     *
     * @param stream an <code>ImageInputStream</code> to read from.
     * @param readMetadata if {@code true}, makes sure to read image metadata
     *
     * @return a <code>BufferedImage</code> containing the decoded contents of the
     *         input, or <code>null</code>.
     *
     * @throws IllegalArgumentException if <code>stream</code> is <code>null</code>.
     * @throws IOException if an error occurs during reading.
     */
    public static BufferedImage readImage(ImageInputStream stream, boolean readMetadata) throws IOException {
        Iterator<ImageReader> iter = ImageIO.getImageReaders(stream);
        if (!iter.hasNext()) {
            return null;
        }

        ImageReader reader = iter.next();
        if (iter.hasNext()) {
            LOGGER.debug("At least another image reader is available and ignored: {}", iter.next());
        }
        reader.setInput(stream, true, !readMetadata);
        try {
            return reader.read(0, reader.getDefaultReadParam());
        } finally {
            reader.dispose();
        }
    }
}
