package org.wikimedia.commons.donvip.spacemedia.utils;

import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.executeRequestStream;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.newHttpGet;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wikimedia.commons.donvip.spacemedia.exception.FileDecodingException;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;

public class ImageUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageUtils.class);

    private ImageUtils() {
        // Hide default constructor
    }

    /**
     * Returns a <code>BufferedImage</code> as the result of decoding a supplied
     * <code>ImageInputStream</code> with an <code>ImageReader</code> chosen
     * automatically from among those currently registered. If no registered
     * <code>ImageReader</code> claims to be able to read the stream,
     * <code>null</code> is returned.
     *
     * <p>
     * This method <em>does</em> close the provided <code>ImageInputStream</code>
     * after the read operation has completed, unless <code>null</code> is returned,
     * in which case this method <em>does not</em> close the stream.
     *
     * @param stream       an <code>ImageInputStream</code> to read from.
     * @param readMetadata if {@code true}, makes sure to read image metadata
     *
     * @return a <code>BufferedImage</code> containing the decoded contents of the
     *         input.
     *
     * @throws IllegalArgumentException if <code>stream</code> is <code>null</code>.
     * @throws IOException              if an error occurs during reading.
     */
    static ContentsAndMetadata<BufferedImage> readImage(ImageInputStream stream, boolean readMetadata)
            throws IOException {
        return readImage(stream, true, readMetadata, reader -> {
            try {
                return new ContentsAndMetadata<>(reader.read(0, reader.getDefaultReadParam()),
                        stream.getStreamPosition(), null,
                        switch (reader.getClass().getSimpleName()) {
                        case "BMPImageReader" -> "bmp";
                        case "GIFImageReader" -> "gif";
                        case "JPEGImageReader" -> "jpg";
                        case "PNGImageReader" -> "png";
                        case "SVGImageReader" -> "svg";
                        case "TIFFImageReader" -> "tiff";
                        default -> null;
                        }, reader.getNumImages(false), null);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    private static <T> T readImage(ImageInputStream stream, boolean seekForwardOnly, boolean readMetadata,
            Function<ImageReader, T> readFunction) throws IOException {
        Iterator<ImageReader> iter = ImageIO.getImageReaders(stream);
        if (!iter.hasNext()) {
            throw new IOException("No image reader found");
        }

        ImageReader reader = iter.next();
        if (iter.hasNext()) {
            LOGGER.debug("At least another image reader is available and ignored: {}", iter.next());
        }
        reader.setInput(stream, seekForwardOnly, !readMetadata);
        try {
            return readFunction.apply(reader);
        } catch (UncheckedIOException e) {
            throw e.getCause();
        } finally {
            reader.dispose();
            stream.close();
        }
    }

    public static int readNumberOfImages(URI uri, boolean log) throws IOException {
        if (log) {
            LOGGER.info("Reading number of images in {}", uri);
        }
        try (CloseableHttpClient httpclient = HttpClients.createDefault();
                InputStream in = executeRequestStream(newHttpGet(uri), httpclient, null)) {
            return readNumberOfImages(ImageIO.createImageInputStream(in));
        }
    }

    private static int readNumberOfImages(ImageInputStream stream) throws IOException {
        return readImage(stream, false, true, reader -> {
            try {
                return reader.getNumImages(true);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    static ContentsAndMetadata<BufferedImage> readWebp(URI uri, boolean readMetadata)
            throws IOException, FileDecodingException {
        Path file = Utils.downloadFile(uri, uri.getPath().replace('/', '_')).getKey();
        try {
            Path pngFile = Path.of(file.toString().replace(".webp", ".png"));
            Utils.execOutput(List.of("dwebp", file.toString(), "-o", pngFile.toString()), 1, TimeUnit.MINUTES);
            try (InputStream inPng = Files.newInputStream(pngFile)) {
                return readImage(inPng, readMetadata);
            } finally {
                Files.delete(pngFile);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        } catch (ExecutionException e) {
            throw new IOException(e);
        } finally {
            Files.delete(file);
        }
    }

    static ContentsAndMetadata<BufferedImage> readImage(InputStream in, boolean readMetadata)
            throws FileDecodingException, IIOException {
        try {
            return readImage(ImageIO.createImageInputStream(in), readMetadata);
        } catch (IIOException e) {
            throw e;
        } catch (IOException | RuntimeException e) {
            throw new FileDecodingException(e);
        }
    }

    public static Metadata readImageMetadata(URL url, HttpClient httpClient, HttpClientContext context)
            throws IOException {
        return readImageMetadata(Utils.urlToUriUnchecked(url), httpClient, context);
    }

    public static Metadata readImageMetadata(URI uri, HttpClient httpClient, HttpClientContext context)
            throws IOException {
        LOGGER.info("Reading EXIF metadata for {}...", uri);
        try (InputStream in = executeRequestStream(newHttpGet(uri), httpClient, context)) {
            return ImageMetadataReader.readMetadata(in);
        } catch (ImageProcessingException e) {
            throw new IOException(e);
        }
    }
}
