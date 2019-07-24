package org.wikimedia.commons.donvip.spacemedia.utils;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Iterator;
import java.util.Locale;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.wikimedia.commons.donvip.spacemedia.exception.ImageDecodingException;

public final class Utils {

    private Utils() {
        // Hide default constructor
    }

    public static URI urlToUri(URL url) throws URISyntaxException {
        // TODO: cannot access https://images-assets.nasa.gov/video/EarthKAM_español_V2/collection.json
        // The ñ character is not translated as expected by AWS and we receive a 403 error from AWS
        // See https://stackoverflow.com/a/49769155/2257172
        return new URI(
                url.getProtocol(),
                url.getUserInfo(),
                url.getHost(),
                url.getPort(),
                url.getPath(),
                url.getQuery(),
                null);
    }

    public static String computeSha1(URL url) throws IOException, URISyntaxException {
        try (CloseableHttpClient httpclient = HttpClients.createDefault();
                CloseableHttpResponse response = httpclient.execute(new HttpGet(urlToUri(url)));
                InputStream in = response.getEntity().getContent()) {
            return DigestUtils.sha1Hex(in);
        }
    }

    /**
     * Returns a <code>BufferedImage</code> as the result of decoding
     * a supplied <code>ImageInputStream</code> with an
     * <code>ImageReader</code> chosen automatically from among those
     * currently registered.  If no registered
     * <code>ImageReader</code> claims to be able to read the stream,
     * <code>null</code> is returned.
     *
     * <p>This method <em>does</em>
     * close the provided <code>ImageInputStream</code> after the read
     * operation has completed, unless <code>null</code> is returned,
     * in which case this method <em>does not</em> close the stream.
     *
     * @param stream an <code>ImageInputStream</code> to read from.
     * @param readMetadata if {@code true}, makes sure to read image metadata
     *
     * @return a <code>BufferedImage</code> containing the decoded
     * contents of the input, or <code>null</code>.
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
        reader.setInput(stream, true, !readMetadata);
        try {
            return reader.read(0, reader.getDefaultReadParam());
        } finally {
            reader.dispose();
            stream.close();
        }
    }

    public static BufferedImage readImage(URL url, boolean readMetadata) throws IOException, URISyntaxException, ImageDecodingException {
        URI uri = urlToUri(url);
        String extension = uri.toString().substring(uri.toString().lastIndexOf('.')+1).toLowerCase(Locale.ENGLISH);
        try (CloseableHttpClient httpclient = HttpClients.createDefault();
                CloseableHttpResponse response = httpclient.execute(new HttpGet(uri));
                InputStream in = response.getEntity().getContent()) {
            switch (extension) {
                case "bmp":
                case "gif":
                case "jpg":
                case "jpeg":
                case "png":
                case "tif":
                case "tiff":
                    try {
                        return readImage(ImageIO.createImageInputStream(in), readMetadata);
                    } catch (IOException | IllegalArgumentException e) {
                        throw new ImageDecodingException(e);
                    }
                default:
                    throw new ImageDecodingException("Unsupported format: " + extension);
            }
        }
    }
}
