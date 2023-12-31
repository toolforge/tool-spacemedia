package org.wikimedia.commons.donvip.spacemedia.service;

import static java.util.stream.Collectors.joining;
import static org.wikimedia.commons.donvip.spacemedia.service.wikimedia.CommonsService.timestampFormatter;
import static org.wikimedia.commons.donvip.spacemedia.utils.HashHelper.similarityScore;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.newHttpGet;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.newURL;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.BiFunction;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.logging.log4j.util.TriConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.wikimedia.commons.donvip.spacemedia.data.commons.CommonsImageProjection;
import org.wikimedia.commons.donvip.spacemedia.data.commons.CommonsImageRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.Media;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.WithKeywords;
import org.wikimedia.commons.donvip.spacemedia.service.wikimedia.CommonsService;
import org.wikimedia.commons.donvip.spacemedia.utils.Emojis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.scribejava.core.httpclient.multipart.BodyPartPayload;
import com.github.scribejava.core.httpclient.multipart.FileByteArrayBodyPartPayload;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Token;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuthService;

public abstract class AbstractSocialMediaService<S extends OAuthService, T extends Token> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractSocialMediaService.class);

    @Autowired
    protected ObjectMapper jackson;

    @Autowired
    protected CommonsService commonsService;

    @Autowired
    protected CommonsImageRepository imageRepo;

    @Value("${perceptual.threshold}")
    private double perceptualThreshold;

    @Value("${commons.api.account}")
    private String commonsAccount;

    public abstract void postStatus(String text) throws IOException;

    public abstract void postStatus(Collection<? extends Media> uploadedMedia,
            Collection<FileMetadata> uploadedMetadata, Set<String> emojis, Set<String> accounts) throws IOException;

    protected abstract S getOAuthService();

    protected abstract T getAccessToken();

    protected abstract TriConsumer<S, T, OAuthRequest> getSignMethod();

    private void checkInitialization() throws IOException {
        if (getOAuthService() == null || getAccessToken() == null) {
            throw new IOException("API not initialized correctly");
        }
    }

    protected abstract OAuthRequest buildStatusRequest(String text) throws IOException;

    protected abstract OAuthRequest buildStatusRequest(Collection<? extends Media> uploadedMedia,
            Collection<FileMetadata> uploadedMetadata, Set<String> emojis, Set<String> accounts) throws IOException;

    protected OAuthRequest postRequest(String endpoint, String contentType, Object payload)
            throws IOException {
        return request(Verb.POST, endpoint, contentType, payload, Map.of());
    }

    protected OAuthRequest request(Verb verb, String endpoint, String contentType, Object payload,
            Map<String, Object> params) throws IOException {
        checkInitialization();
        OAuthRequest request = new OAuthRequest(verb, endpoint);
        if (verb.isPermitBody() && contentType != null) {
            request.addHeader("Content-Type", contentType);
        }
        request.setCharset(StandardCharsets.UTF_8.name());
        if ("application/json".equals(contentType)) {
            params.forEach((k, v) -> request.addParameter(k, v.toString()));
            String json = jackson.writeValueAsString(payload);
            LOGGER.info("JSON payload: {}", json);
            request.setPayload(json);
        } else if (payload instanceof BodyPartPayload bodyPartPayload) {
            request.initMultipartPayload();
            if (params != null) {
                params.forEach((k, v) -> request.addBodyPartPayloadInMultipartPayload(
                        new FileByteArrayBodyPartPayload(v.toString().getBytes(), k)));
            }
            request.addBodyPartPayloadInMultipartPayload(bodyPartPayload);
        }
        getSignMethod().accept(getOAuthService(), getAccessToken(), request);
        return request;
    }

    protected <R> R callApi(OAuthRequest request, Class<R> responseClass) throws IOException {
        checkInitialization();
        try {
            LOGGER.info("Calling API with request {}", request);
            Response response = getOAuthService().execute(request);
            if (response.getCode() >= 400) {
                LOGGER.error("Error: {}", response);
                throw new IOException(response.toString());
            }
            LOGGER.info("Response: {}", response);
            return jackson.readValue(response.getBody(), responseClass);
        } catch (ExecutionException e) {
            throw new IOException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        }
    }

    protected String createStatusText(Set<String> emojis, Set<String> accounts, long imgCount, long vidCount,
            Collection<? extends Media> uploadedMedia, Collection<FileMetadata> uploadedMetadata, int maxKeywords) {
        StringBuilder sb = new StringBuilder();
        getLongestTitle(uploadedMedia).ifPresent(sb::append);
        sb.append(' ').append(emojis.stream().sorted().collect(joining()));
        appendKeywords(uploadedMedia, sb, maxKeywords);
        String mediaFrom = getMediaFrom(accounts, imgCount, vidCount);
        if (imgCount + vidCount > 1) {
            sb.append("\n\n⏩ ").append(mediaFrom);
            String maxTimestamp = imageRepo.findMaxTimestampBySha1In(uploadedMetadata.parallelStream()
                    .map(FileMetadata::getSha1).filter(Objects::nonNull).map(CommonsService::base36Sha1).toList());
            if (maxTimestamp != null) {
                sb.append(" https://commons.wikimedia.org/wiki/Special:ListFiles?limit=" + uploadedMetadata.size()
                        + "&user=" + commonsAccount + "&ilshowall=1&offset=" + timestampFormatter
                                .format(timestampFormatter.parse(maxTimestamp, LocalDateTime::from).plusSeconds(1)));
            } else {
                LOGGER.error(
                        "No timestamp found! Is there replag right now? Check commonswiki.{analytics,web}.db.svc.wikimedia.cloud at https://replag.toolforge.org/");
            }
        } else {
            try {
                sb.append("\n\n▶️ ").append(mediaFrom).append(" https://commons.wikimedia.org/wiki/File:"
                        + URLEncoder.encode(uploadedMetadata.iterator().next().getCommonsFileNames().iterator().next(),
                                StandardCharsets.UTF_8));
            } catch (NoSuchElementException e) {
                LOGGER.error("No commons file name for uploaded metadata ?! {}", uploadedMetadata);
            }
        }
        return sb.toString().strip();
    }

    private static String getMediaFrom(Set<String> accounts, long imgCount, long vidCount) {
        StringBuilder mediaFrom = new StringBuilder();
        if (imgCount > 0) {
            mediaFrom.append(String.format("%d new picture%s", imgCount, imgCount > 1 ? "s" : ""));
            if (vidCount > 0) {
                mediaFrom.append(" and");
            }
        }
        if (vidCount > 0) {
            mediaFrom.append(String.format("%d new video%s", vidCount, vidCount > 1 ? "s" : ""));
        }
        if (!accounts.isEmpty()) {
            mediaFrom.append(" from " + accounts.stream().sorted().collect(joining(" ")));
        }
        return mediaFrom.toString();
    }

    private static Optional<String> getLongestTitle(Collection<? extends Media> uploadedMedia) {
        return uploadedMedia.stream().map(Media::getTitle)
                .filter(x -> x != null && x.length() > 3 && !x.matches("[a-z0-9]+"))
                .map(x -> " " + x.replace(" (annotated)", "").replace(" (labeled)", "")).distinct()
                .sorted(Comparator.comparingInt(String::length).reversed()).findFirst();
    }

    private static void appendKeywords(Collection<? extends Media> uploadedMedia, StringBuilder sb, int max) {
        List<String> keywords = uploadedMedia.stream().filter(WithKeywords.class::isInstance)
                .flatMap(x -> ((WithKeywords) x).getKeywordStream())
                .map(kw -> kw.replace(" ", "").replace("-", "").replace(".", ""))
                .distinct().sorted().limit(max).toList();
        if (!keywords.isEmpty()) {
            sb.append("\n\n");
            keywords.forEach(kw -> sb.append('#').append(kw).append(' '));
        }
    }

    protected Collection<FileMetadata> determineMediaToUploadToSocialMedia(Collection<FileMetadata> uploadedMedia) {
        // https://developer.twitter.com/en/docs/twitter-api/v1/media/upload-media/uploading-media/media-best-practices
        // attach up to 4 photos
        return determineAtMost(4, uploadedMedia.stream().filter(x -> x.isReadableImage() == Boolean.TRUE
                && x.hasPhash() && x.getMime() != null && x.getMime().startsWith("image/")
                && !"image/gif".equals(x.getMime())).toList());
    }

    private List<FileMetadata> determineAtMost(int max, List<FileMetadata> imgs) {
        List<FileMetadata> result = new ArrayList<>(max);
        for (FileMetadata img : imgs) {
            if (result.isEmpty()) {
                // Start by the first random image
                result.add(img);
            } else {
                // Include at most three other images different enough
                if (result.stream().noneMatch(x -> sameImages(x, img))) {
                    result.add(img);
                }
                if (result.size() == max) {
                    break;
                }
            }
        }
        // Replace TIFF by JPEG/PNG version when possible, as thumb server sucks
        return result.stream().map(x -> {
            if ("image/tiff".equals(x.getMime())) {
                return imgs.stream().filter(o -> !"image/tiff".equals(o.getMime()) && sameImages(o, x)).findAny()
                        .orElse(x);
            }
            return x;
        }).toList();
    }

    boolean sameImages(FileMetadata a, FileMetadata b) {
        return similarityScore(a.getPhash(), b.getPhash()) <= perceptualThreshold;
    }

    protected static URL getImageUrl(URL url, int width, String fileName) {
        return width > 2560
                ? newURL(String.format(CommonsService.BASE_URL + "/w/thumb.php?f=%s&w=%d", fileName, 2560))
                : url;
    }

    protected <M> M postMedia(MediaUploadContext muc, BiFunction<MediaUploadContext, byte[], M> poster)
            throws IOException, URISyntaxException {
        try {
            return postMedia(muc, poster, 10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        }
    }

    private <M> M postMedia(MediaUploadContext muc, BiFunction<MediaUploadContext, byte[], M> poster, int retryCount)
            throws IOException, URISyntaxException, InterruptedException {
        LOGGER.info("Uploading media for file {} resolved to URL {}", muc.filename, muc.url);
        try (CloseableHttpClient httpclient = HttpClients.custom().setUserAgent(commonsService.getUserAgent()).build();
                CloseableHttpResponse response = httpclient.execute(newHttpGet(muc.url.toURI()));
                InputStream in = response.getEntity().getContent()) {
            if (response.getStatusLine().getStatusCode() >= 400) {
                String message = muc.url.toURI().toString() + " -> " + response.getStatusLine().toString();
                if (retryCount > 0) {
                    LOGGER.warn("{}, {} retry attempts remaining", message, retryCount);
                    Thread.sleep(2000);
                    return postMedia(muc, poster, retryCount - 1);
                }
                throw new IOException(message);
            }
            Thread.sleep(500);
            return poster.apply(muc, in.readAllBytes());
        }
    }

    protected static class MediaUploadContext {
        public final String filename;
        public final String mime;
        public final String cat;
        public final URL url;

        public MediaUploadContext(CommonsImageProjection file, String mime) {
            final URL fileUrl = CommonsService.getImageUrl(file.getName());
            this.filename = file.getName();
            if ("image/gif".equals(mime)) {
                this.mime = mime;
                this.cat = "tweet_gif";
                this.url = fileUrl;
            } else {
                this.mime = "image/jpeg";
                this.cat = "tweet_image";
                this.url = getImageUrl(fileUrl, file.getWidth(), file.getName());
            }
        }
    }

    public static Set<String> getEmojis(Set<String> keywords) {
        Set<String> result = new HashSet<>();
        for (String keyword : keywords) {
            switch (keyword.toLowerCase(Locale.ENGLISH)) {
            case "astronaut":
                result.add(Emojis.ASTRONAUT);
                break;
            case "astronaut snoopy", "snoopy":
                result.add(Emojis.DOG_HEAD);
                break;
            case "artemis":
                result.add(Emojis.ASTRONAUT);
                result.add(Emojis.MOON);
                break;
            case "moon":
                result.add(Emojis.MOON);
                break;
            case "launch vehicle", "rocket", "rocket science", "sls", "space launch system", "falcon", "starship",
                    "ariane":
                result.add(Emojis.ROCKET);
                break;
            case "satellite", "spacecraft", "crew dragon", "starliner", "progress", "soyuz":
                result.add(Emojis.SATELLITE);
                break;
            case "planet", "mercury", "venus", "mars", "jupiter", "saturn", "uranus", "neptune":
                result.add(Emojis.PLANET_WITH_RINGS);
                break;
            case "plants", "plant production area":
                result.add(Emojis.PLANT);
                break;
            case "earth":
                result.add(Emojis.EARTH_AMERICA);
                break;
            case "sun":
                result.add(Emojis.SUN);
                break;
            case "star", "galaxy":
                result.add(Emojis.STARS);
                break;
            case "comet":
                result.add(Emojis.COMET);
                break;
            case "fire", "wildfire":
                result.add(Emojis.FIRE);
                break;
            }
        }
        return result;
    }
}
