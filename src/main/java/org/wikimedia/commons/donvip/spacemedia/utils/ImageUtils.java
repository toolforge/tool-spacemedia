package org.wikimedia.commons.donvip.spacemedia.utils;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wikimedia.commons.donvip.spacemedia.exception.ImageDecodingException;

import com.fasterxml.jackson.datatype.jdk8.WrappedIOException;

public class ImageUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageUtils.class);

    private static final Set<String> IMAGE_EXTENSIONS = Set.of("bmp", "gif", "jpe", "jpg", "jpeg", "png", "tif",
            "tiff");

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
     *         input, or <code>null</code>.
     *
     * @throws IllegalArgumentException if <code>stream</code> is <code>null</code>.
     * @throws IOException              if an error occurs during reading.
     */
    public static BufferedImage readImage(ImageInputStream stream, boolean readMetadata) throws IOException {
        return doReadImageOrMetadata(stream, readMetadata, reader -> {
            try {
                return reader.read(0, reader.getDefaultReadParam());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    public static Map<String, String> readImageMetadata(ImageInputStream stream) throws IOException {
        return doReadImageOrMetadata(stream, false, reader -> {
            try {
                Map<String, String> result = new TreeMap<>();
                IIOMetadata metadata = reader.getStreamMetadata();
                if (metadata != null) {
                    LOGGER.info("Stream: {}", metadata);
                    // TODO
                }
                for (int index = 0; index < reader.getNumImages(true); index++) {
                    metadata = reader.getImageMetadata(index);
                    if (metadata != null) {
                        LOGGER.info("Image {}: {}", index, metadata);
                        // TODO
                    }
                }
                return result;
            } catch (IOException e) {
                throw new WrappedIOException(e);
            }
        });
    }

    private static <T> T doReadImageOrMetadata(ImageInputStream stream, boolean readMetadata, Function<ImageReader, T> function)
            throws IOException {
        Iterator<ImageReader> iter = ImageIO.getImageReaders(stream);
        if (!iter.hasNext()) {
            LOGGER.warn("No image reader found");
            return null;
        }

        ImageReader reader = iter.next();
        if (iter.hasNext()) {
            LOGGER.debug("At least another image reader is available and ignored: {}", iter.next());
        }
        reader.setInput(stream, true, !readMetadata);
        try {
            return function.apply(reader);
        } catch (WrappedIOException e) {
            throw e.getCause();
        } finally {
            reader.dispose();
            stream.close();
        }
    }

    public static Pair<BufferedImage, Long> readImage(URL url, boolean readMetadata, boolean log)
            throws IOException, URISyntaxException, ImageDecodingException {
        URI uri = Utils.urlToUri(url);
        if (log) {
            LOGGER.info("Reading image {}", uri);
        }
        String extension = Utils.findExtension(uri.toString());
        try (CloseableHttpClient httpclient = HttpClients.createDefault();
                CloseableHttpResponse response = httpclient.execute(new HttpGet(uri));
                InputStream in = response.getEntity().getContent()) {
            boolean ok = IMAGE_EXTENSIONS.contains(extension);
            if (!ok) {
                Header[] disposition = response.getHeaders("Content-Disposition");
                if (ArrayUtils.isNotEmpty(disposition)) {
                    extension = Utils.findExtension(disposition[0].getValue());
                    ok = IMAGE_EXTENSIONS.contains(extension);
                }
            }
            long contentLength = response.getEntity().getContentLength();
            if (ok) {
                return Pair.of(readImage(in, readMetadata), contentLength);
            } else if ("webp".equals(extension)) {
                return Pair.of(readWebp(url, uri, readMetadata), contentLength);
            } else {
                throw new ImageDecodingException(
                        "Unsupported format: " + extension + " / headers:" + response.getAllHeaders());
            }
        }
    }

    private static BufferedImage readWebp(URL url, URI uri, boolean readMetadata)
            throws IOException, ImageDecodingException {
        Path file = Utils.downloadFile(url, uri.getPath().replace('/', '_')).getKey();
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

    private static BufferedImage readImage(InputStream in, boolean readMetadata) throws ImageDecodingException {
        try {
            return readImage(ImageIO.createImageInputStream(in), readMetadata);
        } catch (IOException | RuntimeException e) {
            throw new ImageDecodingException(e);
        }
    }
}
