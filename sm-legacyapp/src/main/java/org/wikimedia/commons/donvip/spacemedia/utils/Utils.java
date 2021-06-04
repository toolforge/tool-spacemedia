package org.wikimedia.commons.donvip.spacemedia.utils;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wikimedia.commons.donvip.spacemedia.downloader.ImageHelper;
import org.wikimedia.commons.donvip.spacemedia.exception.ImageDecodingException;

public final class Utils {

    private static final Logger LOGGER = LoggerFactory.getLogger(Utils.class);

    private static final Set<String> IMAGE_EXTENSIONS = Set.of("bmp", "gif", "jpe", "jpg", "jpeg", "png", "tif",
            "tiff");

    private Utils() {
        // Hide default constructor
    }

    public static String findExtension(String path) {
        return path.substring(path.lastIndexOf('.') + 1).toLowerCase(Locale.ENGLISH);
    }

    public static BufferedImage readImage(URL url, boolean readMetadata)
            throws IOException, URISyntaxException, ImageDecodingException {
        URI uri = UriUrlUtils.urlToUri(url);
        LOGGER.info("Reading image {}", uri);
        String extension = findExtension(uri.toString());
        try (CloseableHttpClient httpclient = HttpClients.createDefault();
                CloseableHttpResponse response = httpclient.execute(new HttpGet(uri));
                InputStream in = response.getEntity().getContent()) {
            boolean ok = IMAGE_EXTENSIONS.contains(extension);
            if (!ok) {
                Header[] disposition = response.getHeaders("Content-Disposition");
                if (ArrayUtils.isNotEmpty(disposition)) {
                    extension = findExtension(disposition[0].getValue());
                    ok = IMAGE_EXTENSIONS.contains(extension);
                }
            }
            if (ok) {
                try {
                    return ImageHelper.readImage(ImageIO.createImageInputStream(in), readMetadata);
                } catch (IOException | RuntimeException e) {
                    throw new ImageDecodingException(e);
                }
            } else {
                throw new ImageDecodingException(
                        "Unsupported format: " + extension + " / headers:" + response.getAllHeaders());
            }
        }
    }

    public static Element newElement(String tag, String text, Map<String, String> attrs) {
        Attributes attributes = new Attributes();
        attrs.entrySet().forEach(e -> attributes.put(e.getKey(), e.getValue()));
        Element elem = new Element(Tag.valueOf(tag), "", attributes);
        if (text != null) {
            elem.text(text);
        }
        return elem;
    }

    public static Element prependChildElement(Element base, String tag, String text, Map<String, String> attrs) {
        Element elem = newElement(tag, text, attrs);
        base.insertChildren(0, elem);
        return elem;
    }

    public static Element appendChildElement(Element base, String tag, String text, Map<String, String> attrs) {
        Element elem = newElement(tag, text, attrs);
        base.appendChild(elem);
        return elem;
    }

    public static boolean isTextFound(String fullText, String textToFind) {
        if (fullText != null) {
            return isTextFoundLowercase(fullText.toLowerCase(Locale.ENGLISH), textToFind.toLowerCase(Locale.ENGLISH));
        }
        return false;
    }

    public static boolean isTextFoundLowercase(String fullTextLc, String textToFindLc) {
        if (fullTextLc != null) {
            if (textToFindLc.contains("+")) {
                for (String word : textToFindLc.split("\\+")) {
                    if (!isTextFound(fullTextLc, word)) {
                        return false;
                    }
                }
                return true;
            } else {
                int index = fullTextLc.indexOf(textToFindLc);
                if (index > -1) {
                    if (index > 0 && isTokenPart(fullTextLc.charAt(index - 1))) {
                        return false;
                    }
                    if (index + textToFindLc.length() < fullTextLc.length() - 1
                            && isTokenPart(fullTextLc.charAt(index + textToFindLc.length()))) {
                        return false;
                    }
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isTokenPart(char c) {
        return Character.isAlphabetic(c) || Character.isDigit(c) || c == '-' || c == '_';
    }

    public static void addCertificate(String resource) throws GeneralSecurityException, IOException {
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        try (InputStream is = Files.newInputStream(Paths.get(System.getProperty("java.home"), "lib", "security", "cacerts"))) {
            keyStore.load(is, "changeit".toCharArray());
        }
        keyStore.setCertificateEntry(resource,
                CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(
                        IOUtils.toByteArray(Utils.class.getClassLoader().getResourceAsStream(resource)))));
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(keyStore);
        SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
        sslContext.init(null, tmf.getTrustManagers(), null);
        SSLContext.setDefault(sslContext);
    }

    /**
     * Runs an external command and returns the standard output. Waits at most the specified time.
     *
     * @param command the command with arguments
     * @param timeout the maximum time to wait
     * @param unit    the time unit of the {@code timeout} argument. Must not be null
     * @return the output
     * @throws IOException          when there was an error, e.g. command does not exist
     * @throws ExecutionException   when the return code is != 0. The output is can be retrieved in the exception message
     * @throws InterruptedException if the current thread is {@linkplain Thread#interrupt() interrupted} by another thread while waiting
     */
    public static String execOutput(List<String> command, long timeout, TimeUnit unit)
            throws IOException, ExecutionException, InterruptedException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.join(" ", command));
        }
        Path out = Files.createTempFile("spacemedia_exec_" + command.get(0) + "_", ".txt");
        try {
            Process p = new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(out.toFile()).start();
            if (!p.waitFor(timeout, unit) || p.exitValue() != 0) {
                throw new ExecutionException(command.toString(), null);
            }
            return String.join("\n", Files.readAllLines(out)).trim();
        } finally {
            try {
                Files.delete(out);
            } catch (IOException e) {
                LOGGER.warn("Error while deleting file", e);
            }
        }
    }
}
