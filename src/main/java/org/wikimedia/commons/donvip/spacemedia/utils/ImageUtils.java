package org.wikimedia.commons.donvip.spacemedia.utils;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.newHttpGet;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wikimedia.commons.donvip.spacemedia.exception.ImageDecodingException;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;

public class ImageUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageUtils.class);

    private static final Set<String> IMAGE_EXTENSIONS = Set.of("bmp", "gif", "jpe", "jpg", "jpeg", "png", "svg", "tif",
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
     *         input.
     *
     * @throws IllegalArgumentException if <code>stream</code> is <code>null</code>.
     * @throws IOException              if an error occurs during reading.
     */
    static ImageAndMetadata readImage(ImageInputStream stream, boolean readMetadata) throws IOException {
        Iterator<ImageReader> iter = ImageIO.getImageReaders(stream);
        if (!iter.hasNext()) {
            throw new IOException("No image reader found");
        }

        ImageReader reader = iter.next();
        if (iter.hasNext()) {
            LOGGER.debug("At least another image reader is available and ignored: {}", iter.next());
        }
        reader.setInput(stream, true, !readMetadata);
        try {
            return new ImageAndMetadata(reader.read(0, reader.getDefaultReadParam()), null, null,
                    switch(reader.getClass().getSimpleName()) {
                    case "BMPImageReader" -> "bmp";
                    case "GIFImageReader" -> "gif";
                    case "JPEGImageReader" -> "jpg";
                    case "PNGImageReader" -> "png";
                    case "SVGImageReader" -> "svg";
                    case "TIFFImageReader" -> "tiff";
                    default -> null;
                    }, reader.getNumImages(false));
        } finally {
            reader.dispose();
            stream.close();
        }
    }

    public static ImageAndMetadata readImage(URL url, boolean readMetadata, boolean log)
            throws IOException, ImageDecodingException {
        return readImage(Utils.urlToUriUnchecked(url), readMetadata, log);
    }

    public static ImageAndMetadata readImage(URI uri, boolean readMetadata, boolean log)
            throws IOException, ImageDecodingException {
        if (log) {
            LOGGER.info("Reading image {}", uri);
        }
        String extension = Utils.findExtension(uri.toString());
        try (CloseableHttpClient httpclient = HttpClients.createDefault();
                CloseableHttpResponse response = httpclient.execute(newHttpGet(uri));
                InputStream in = response.getEntity().getContent()) {
            boolean ok = extension != null && IMAGE_EXTENSIONS.contains(extension);
            String filename = null;
            if (!ok) {
                Header[] disposition = response.getHeaders("Content-Disposition");
                if (ArrayUtils.isNotEmpty(disposition)) {
                    String value = disposition[0].getValue();
                    if (value.startsWith("attachment;filename=")) {
                        filename = URLDecoder.decode(value.split("=")[1], "UTF-8").replace(";filename*", "")
                                .replace("\"", "");
                    }
                    extension = Utils.findExtension(value);
                    ok = extension != null && IMAGE_EXTENSIONS.contains(extension);
                }
            }
            long contentLength = response.getEntity().getContentLength();
            if (ok || isBlank(extension)) {
                ImageAndMetadata result = readImage(in, readMetadata);
                return new ImageAndMetadata(result.image(), contentLength, filename,
                        isBlank(extension) ? result.extension() : extension, result.numImages());
            } else if ("webp".equals(extension)) {
                return new ImageAndMetadata(readWebp(uri, readMetadata).image(), contentLength, filename, extension, 1);
            } else {
                throw new ImageDecodingException(
                        "Unsupported format: " + extension + " / headers:"
                                + Arrays.stream(response.getAllHeaders()).map(h -> h.getName() + ": " + h.getValue())
                                        .sorted().toList());
            }
        }
    }

    private static ImageAndMetadata readWebp(URI uri, boolean readMetadata)
            throws IOException, ImageDecodingException {
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

    private static ImageAndMetadata readImage(InputStream in, boolean readMetadata) throws ImageDecodingException {
        try {
            return readImage(ImageIO.createImageInputStream(in), readMetadata);
        } catch (IOException | RuntimeException e) {
            throw new ImageDecodingException(e);
        }
    }

    public static Metadata readImageMetadata(URL url) throws IOException {
        return readImageMetadata(Utils.urlToUriUnchecked(url));
    }

    public static Metadata readImageMetadata(URI uri) throws IOException {
        LOGGER.info("Reading EXIF metadata for {}...", uri);
        try (CloseableHttpClient httpclient = HttpClients.createDefault();
                CloseableHttpResponse response = httpclient.execute(newHttpGet(uri));
                InputStream in = response.getEntity().getContent()) {
            return ImageMetadataReader.readMetadata(in);
        } catch (ImageProcessingException e) {
            throw new IOException(e);
        }
    }
}
