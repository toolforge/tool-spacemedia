package org.wikimedia.commons.donvip.spacemedia.utils;

import static java.util.Objects.requireNonNull;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.chrono.ChronoLocalDate;
import java.time.chrono.ChronoLocalDateTime;
import java.time.chrono.ChronoZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalQuery;
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
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpHead;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.cookie.CookieStore;
import org.apache.hc.client5.http.cookie.StandardCookieSpec;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.hc.core5.util.Timeout;
import org.jsoup.HttpStatusException;
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
            return new URL(url.replace("//", "/").replace(":/", "://"));
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(url, e);
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
        requestConfig.setConnectionRequestTimeout(Timeout.ofSeconds(30));
        requestConfig.setResponseTimeout(Timeout.ofSeconds(30));
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

    public static HttpPost newHttpPost(String uri, Map<String, Object> params) {
        return newHttpPost(uri, params.entrySet().stream()
                .map(e -> new BasicNameValuePair(e.getKey(), Objects.toString(e.getValue()))).toList());
    }

    public static HttpPost newHttpPost(String uri, List<? extends NameValuePair> params) {
        HttpPost post = new HttpPost(uri);
        post.setEntity(new UrlEncodedFormEntity(params, StandardCharsets.UTF_8));
        return post;
    }

    private static void buildPostParams(Element form, String tag, BiConsumer<Element, List<NameValuePair>> consumer,
            List<NameValuePair> params) {
        if (consumer != null) {
            form.getElementsByTag(tag).forEach(elem -> consumer.accept(elem, params));
        }
    }

    public static InputStream executeRequestStream(HttpUriRequestBase request, HttpClient httpclient,
            HttpClientContext context) throws IOException {
        return requireNonNull(executeRequest(request, httpclient, context).getEntity(), "entity").getContent();
    }

    public static ClassicHttpResponse executeRequest(HttpUriRequestBase request, HttpClient httpclient,
            HttpClientContext context) throws IOException {
        return checkResponse(request, httpclient.executeOpen(null, request, context));
    }

    public static <T extends HttpResponse> T checkResponse(HttpRequest request, T response) throws IOException {
        if (response.getCode() >= 400) {
            throw new IOException(request + " => " + response);
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
            } catch (HttpStatusException e) {
                LOGGER.warn("{} when scrapping {} => {}", e.getClass().getSimpleName(), pageUrl, e.getMessage());
                if (e.getStatusCode() >= 400 && e.getStatusCode() < 500) {
                    throw e;
                }
            } catch (UnknownHostException e) {
                throw e;
            } catch (IOException e) {
                LOGGER.warn("{} when scrapping {} => {}", e.getClass().getSimpleName(), pageUrl, e.getMessage());
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

    public static String getFilename(String url) {
        return getFilename(newURL(url));
    }

    public static String getFilename(URL url) {
        String file = url.getFile();
        String result = file.substring(file.lastIndexOf('/') + 1, file.lastIndexOf('.')).trim();
        return result.contains("=") ? result.substring(result.lastIndexOf('=') + 1).trim() : result;
    }

    public static Pair<Path, Long> downloadFile(URI uri, String fileName) throws IOException {
        return downloadFile(uri.toURL(), fileName);
    }

    public static Pair<Path, Long> downloadFile(URL url, String fileName) throws IOException {
        Path output = Files.createDirectories(Path.of("files")).resolve(fileName);
        Long size = 0L;
        LOGGER.info("Downloading file {} as {}", url, fileName);
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
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(String.join(" ", command));
        }
        Path out = Files.createTempFile("spacemedia_exec_" + command.get(0) + "_", ".txt");
        try {
            Process p = new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(out.toFile()).start();
            boolean error = !p.waitFor(timeout, unit) || p.exitValue() != 0;
            String output = String.join("\n", Files.readAllLines(out)).trim();
            LOGGER.info(output);
            if (error) {
                throw new ExecutionException(command.toString() + " => " + output, null);
            }
            return output;
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
            return httpClient.executeOpen(null, new HttpHead(uri), null).getCode() == 200;
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

    public static Optional<Temporal> extractDate(String text, List<DateTimeFormatter> dtfs) {
        if (!text.isEmpty()) {
            String cleanText = text.replace(", ", " ").replace("  ", " ");
            for (DateTimeFormatter dtf : dtfs) {
                try {
                    TemporalAccessor accessor = dtf.parse(cleanText);
                    for (TemporalQuery<Temporal> query : List.<TemporalQuery<Temporal>>of(ZonedDateTime::from,
                            LocalDateTime::from, LocalDate::from, YearMonth::from, Year::from)) {
                        try {
                            Temporal temporal = accessor.query(query);
                            if (temporal != null) {
                                return Optional.of(temporal);
                            }
                        } catch (DateTimeException | ArithmeticException e) {
                            LOGGER.trace(e.getMessage());
                        }
                    }
                } catch (DateTimeParseException e) {
                    LOGGER.trace(e.getMessage());
                }
            }
        }
        return Optional.empty();
    }

    public static boolean isTemporalBefore(Temporal t, LocalDate date) {
        if (t instanceof ChronoLocalDate cld) {
            return cld.isBefore(date);
        } else if (t instanceof ChronoLocalDateTime<?> cldt) {
            return cldt.toLocalDate().isBefore(date);
        } else if (t instanceof ChronoZonedDateTime<?> czdt) {
            return czdt.toLocalDate().isBefore(date);
        } else if (t instanceof YearMonth ym) {
            return ym.isBefore(YearMonth.of(date.getYear(), date.getMonth()));
        } else if (t instanceof Year y) {
            return y.isBefore(Year.of(date.getYear()));
        }
        throw new IllegalArgumentException("Unsupported temporal: " + t);
    }

    public static Optional<Integer> extractFileSize(Pattern pattern, String text) {
        Matcher m = pattern.matcher(text);
        if (m.matches()) {
            double size = Double.parseDouble(m.group(1));
            switch (m.group(2)) {
            case "KB":
                size *= 1024;
                break;
            case "MB":
                size *= 1024 * 1024;
                break;
            case "GB":
                size *= 1024 * 1024 * 1024;
                break;
            default:
                throw new IllegalArgumentException("Unsupported file size unit: '" + m.group(2) + "'");
            }
            return Optional.of((int) size);
        }
        return Optional.empty();
    }

    public static HttpClientContext getHttpClientContext(CookieStore cookieStore) {
        HttpClientContext context = new HttpClientContext();
        context.setCookieStore(cookieStore);
        context.setRequestConfig(RequestConfig.custom().setCookieSpec(StandardCookieSpec.STRICT).build());
        return context;
    }
}
