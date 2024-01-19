package org.wikimedia.commons.donvip.spacemedia.utils;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

public final class Utils {

    private static final Logger LOGGER = LoggerFactory.getLogger(Utils.class);

    private Utils() {
        // Hide default constructor
    }

    public static URL newURL(String url) {
        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static URL newURL(String protocol, String host, String file) {
        try {
            return new URL(protocol, host, file);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static URI urlToUriUnchecked(URL url) {
        try {
            return urlToUri(url);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static URI urlToUri(URL url) throws URISyntaxException {
        if (url == null) {
            return null;
        }
        URI uri = null;
        try {
            uri = url.toURI();
        } catch (URISyntaxException e) {
            uri = new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(), url.getPath(), url.getQuery(), null);
        }
        return new URI(uri.toASCIIString());
    }

    public static HttpGet newHttpGet(String uri) {
        return newHttpGet(URI.create(uri));
    }

    public static HttpGet newHttpGet(URL url) {
        return newHttpGet(urlToUriUnchecked(url));
    }

    public static HttpGet newHttpGet(URI uri) {
        HttpGet request = new HttpGet(uri);
        RequestConfig.Builder requestConfig = RequestConfig.custom();
        requestConfig.setConnectTimeout(30 * 1000);
        requestConfig.setConnectionRequestTimeout(30 * 1000);
        requestConfig.setSocketTimeout(30 * 1000);
        request.setConfig(requestConfig.build());
        return request;
    }

    public static HttpPost newHttpPost(Element form, BiConsumer<Element, List<NameValuePair>> consumer)
            throws IOException {
        return newHttpPost(form, x -> x, consumer, null);
    }

    public static HttpPost newHttpPost(Element form, UnaryOperator<String> actionFunction,
            BiConsumer<Element, List<NameValuePair>> inputConsumer,
            BiConsumer<Element, List<NameValuePair>> buttonConsumer) throws IOException {
        if (form == null) {
            throw new IOException("Form not found");
        }
        List<NameValuePair> params = new ArrayList<>();
        buildPostParams(form, "input", inputConsumer, params);
        buildPostParams(form, "button", buttonConsumer, params);
        return newHttpPost(actionFunction.apply(form.attr("action")), params);
    }

    public static HttpPost newHttpPost(String uri, Map<String, Object> params) throws UnsupportedEncodingException {
        return newHttpPost(uri, params.entrySet().stream()
                .map(e -> new BasicNameValuePair(e.getKey(), Objects.toString(e.getValue()))).toList());
    }

    public static HttpPost newHttpPost(String uri, List<? extends NameValuePair> params)
            throws UnsupportedEncodingException {
        HttpPost post = new HttpPost(uri);
        post.setEntity(new UrlEncodedFormEntity(params));
        return post;
    }

    private static void buildPostParams(Element form, String tag, BiConsumer<Element, List<NameValuePair>> consumer,
            List<NameValuePair> params) {
        if (consumer != null) {
            form.getElementsByTag(tag).forEach(elem -> consumer.accept(elem, params));
        }
    }

    public static <T extends HttpResponse> T checkResponse(HttpRequest request, T response) throws IOException {
        if (response.getStatusLine().getStatusCode() >= 400) {
            throw new IOException(request + " => " + response.getStatusLine().toString());
        }
        return response;
    }

    public static Document getWithJsoup(String pageUrl, int timeout, int nRetries) throws IOException {
        return getWithJsoup(pageUrl, timeout, nRetries, true);
    }

    public static Document getWithJsoup(String pageUrl, int timeout, int nRetries, boolean followRedirects)
            throws IOException {
        for (int i = 0; i < nRetries; i++) {
            try {
                LOGGER.debug("Scrapping {}", pageUrl);
                return Jsoup.connect(pageUrl).timeout(timeout).followRedirects(followRedirects).get();
            } catch (SocketTimeoutException e) {
                LOGGER.error("Timeout when scrapping {} => {}", pageUrl, e.getMessage());
            }
        }
        throw new IOException("Unable to scrap " + pageUrl);
    }

    public static String findExtension(String path) {
        int idx = path.lastIndexOf('.');
        if (idx < 0) {
            return null;
        }
        String ext = path.substring(idx + 1).toLowerCase(Locale.ENGLISH);
        if ("gne".equals(ext)) {
            return null;
        }
        int len = ext.length();
        if (len < 3 || len > 4) {
            return null;
        }
        return getNormalizedExtension(ext);
    }

    public static String getNormalizedExtension(String ext) {
        return ext != null ? switch (ext) {
        case "apng" -> "png";
        case "djv" -> "djvu";
        case "jpe", "jpeg", "jps" -> "jpg"; // Use the same extension as flickr2commons as it solely relies on filenames
        case "tif" -> "tiff";
        case "mid", "kar" -> "midi";
        case "mpe", "mpg" -> "mpeg";
        default -> ext;
        } : null;
    }

    public static String getFilename(URL url) {
        String file = url.getFile();
        return file.substring(file.lastIndexOf('/') + 1, file.lastIndexOf('.')).trim();
    }

    public static Pair<Path, Long> downloadFile(URI uri, String fileName) throws IOException {
        return downloadFile(uri.toURL(), fileName);
    }

    public static Pair<Path, Long> downloadFile(URL url, String fileName) throws IOException {
        Path output = Files.createDirectories(Path.of("files")).resolve(fileName);
        Long size = 0L;
        LOGGER.info("Downloading file {}", url);
        try (ReadableByteChannel rbc = Channels.newChannel(url.openStream());
                FileOutputStream fos = new FileOutputStream(output.toString())) {
            size = fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            if (size == 0) {
                throw new IOException("No data transferred from " + url);
            }
        }
        return Pair.of(output, size);
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
        return fullText != null
                && isTextFoundLowercase(fullText.toLowerCase(Locale.ENGLISH), textToFind.toLowerCase(Locale.ENGLISH));
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

    public static Duration durationInSec(Temporal start) {
        return durationInSec(start, LocalDateTime.now());
    }

    public static Duration durationInSec(Temporal start, Temporal end) {
        return Duration.between(start, end).truncatedTo(ChronoUnit.SECONDS);
    }

    public static ZonedDateTime toZonedDateTime(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault());
    }

    public static RestTemplate restTemplateSupportingAll(Charset charset) {
        return new RestTemplate(List.of(new StringHttpMessageConverter(charset)));
    }

    public static RestTemplate restTemplateSupportingAll(ObjectMapper jackson) {
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(jackson);
        converter.setSupportedMediaTypes(List.of(MediaType.ALL));
        return new RestTemplate(List.of(converter));
    }

    public static <T> boolean replace(Collection<T> collection, T oldValue, T newValue) {
        return collection.remove(oldValue) && collection.add(newValue);
    }

    public static String getFirstSentence(String desc) {
        if (desc == null) {
            return "";
        }
        int idxDotInDesc = desc.indexOf('.');
        return idxDotInDesc > 0 ? desc.substring(0, idxDotInDesc) : desc;
    }

    public static boolean uriExists(String uri) {
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().disableRedirectHandling().build()) {
            return httpClient.execute(new HttpHead(uri)).getStatusLine().getStatusCode() == 200;
        } catch (IOException e) {
            return false;
        }
    }

    public static Optional<ZonedDateTime> extractDate(String desc, Map<DateTimeFormatter, Pattern> patterns) {
        for (Entry<DateTimeFormatter, Pattern> e : patterns.entrySet()) {
            Matcher m = e.getValue().matcher(desc);
            if (m.matches()) {
                String text = m.group(1);
                DateTimeFormatter format = e.getKey();
                return Optional.of(format.toString().contains("Value(MinuteOfHour)")
                        ? LocalDateTime.parse(text, format).atZone(ZoneId.systemDefault())
                        : LocalDate.parse(text, format).atStartOfDay(ZoneId.systemDefault()));
            }
        }
        return Optional.empty();
    }
}
