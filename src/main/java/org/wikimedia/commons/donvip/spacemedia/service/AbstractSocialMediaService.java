package org.wikimedia.commons.donvip.spacemedia.service;

import static java.util.stream.Collectors.joining;
import static org.wikimedia.commons.donvip.spacemedia.service.wikimedia.CommonsService.timestampFormatter;
import static org.wikimedia.commons.donvip.spacemedia.utils.HashHelper.similarityScore;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.apache.logging.log4j.util.TriConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.wikimedia.commons.donvip.spacemedia.data.commons.CommonsImageRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.Media;
import org.wikimedia.commons.donvip.spacemedia.data.domain.Metadata;
import org.wikimedia.commons.donvip.spacemedia.service.wikimedia.CommonsService;

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
    protected CommonsImageRepository imageRepo;

    @Value("${perceptual.threshold}")
    private double perceptualThreshold;

    @Value("${commons.api.account}")
    private String commonsAccount;

    public abstract void postStatus(Collection<? extends Media<?, ?>> uploadedMedia,
            Collection<Metadata> uploadedMetadata, Set<String> emojis, Set<String> accounts) throws IOException;

    protected abstract S getOAuthService();

    protected abstract T getAccessToken();

    protected abstract TriConsumer<S, T, OAuthRequest> getSignMethod();

    private void checkInitialization() throws IOException {
        if (getOAuthService() == null || getAccessToken() == null) {
            throw new IOException("API not initialized correctly");
        }
    }

    protected abstract OAuthRequest buildStatusRequest(Collection<? extends Media<?, ?>> uploadedMedia,
            Collection<Metadata> uploadedMetadata, Set<String> emojis, Set<String> accounts) throws IOException;

    protected OAuthRequest postRequest(String endpoint, String contentType, Object payload)
            throws IOException {
        return request(Verb.POST, endpoint, contentType, payload, Map.of());
    }

    protected OAuthRequest request(Verb verb, String endpoint, String contentType, Object payload,
            Map<String, Object> params) throws IOException {
        checkInitialization();
        OAuthRequest request = new OAuthRequest(verb, endpoint);
        if (verb.isPermitBody()) {
            request.addHeader("Content-Type", contentType);
        }
        request.setCharset(StandardCharsets.UTF_8.name());
        if ("application/json".equals(contentType)) {
            params.forEach((k, v) -> request.addParameter(k, v.toString()));
            request.setPayload(jackson.writeValueAsString(payload));
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

    protected String createStatusText(Set<String> emojis, Set<String> accounts, int size,
            Collection<Metadata> uploadedMetadata) {
        String text = String.format("%s %d new picture%s", emojis.stream().sorted().collect(joining()), size,
                size >= 2 ? "s" : "");
        if (!accounts.isEmpty()) {
            text += " from " + accounts.stream().sorted().collect(joining(" "));
        }
        text += "\n\nList → https://commons.wikimedia.org/wiki/Special:ListFiles?limit=" + uploadedMetadata.size()
                + "&user=" + commonsAccount + "&ilshowall=1&offset="
                + timestampFormatter.format(timestampFormatter
                        .parse(imageRepo.findMaxTimestampBySha1In(uploadedMetadata.parallelStream()
                                .map(Metadata::getSha1).map(CommonsService::base36Sha1).toList()), LocalDateTime::from)
                        .plusSeconds(1));
        return text.strip();
    }

    protected Collection<Metadata> determineMediaToUploadToSocialMedia(Collection<Metadata> uploadedMedia) {
        // https://developer.twitter.com/en/docs/twitter-api/v1/media/upload-media/uploading-media/media-best-practices
        List<Metadata> imgs = uploadedMedia.stream().filter(x -> x.isReadableImage() == Boolean.TRUE
                && x.getPhash() != null & x.getMime() != null && x.getMime().startsWith("image/")).toList();
        Optional<Metadata> gif = imgs.stream().filter(x -> "image/gif".equals(x.getMime())).findAny();
        // attach up to 4 photos, 1 animated GIF or 1 video in a Tweet
        if (gif.isPresent()) {
            return List.of(gif.get());
        } else {
            return determineAtMost(4, imgs);
        }
    }

    private List<Metadata> determineAtMost(int max, List<Metadata> imgs) {
        List<Metadata> result = new ArrayList<>(max);
        for (Metadata img : imgs) {
            if (result.isEmpty()) {
                // Start by the first random image
                result.add(img);
            } else {
                // Include at most three other images different enough
                if (result.stream()
                        .noneMatch(x -> similarityScore(x.getPhash(), img.getPhash()) <= perceptualThreshold)) {
                    result.add(img);
                }
                if (result.size() == max) {
                    break;
                }
            }
        }
        return result;
    }

    public static Set<String> getEmojis(Set<String> keywords) {
        Set<String> result = new HashSet<>();
        for (String keyword : keywords) {
            switch (keyword.toLowerCase(Locale.ENGLISH)) {
            case "astronaut":
                result.add("🧑");
                break;
            case "astronaut snoopy", "snoopy":
                result.add("🐶");
                break;
            case "artemis":
                result.add("🧑");
                result.add("🌑");
                break;
            case "moon":
                result.add("🌑");
                break;
            case "launch vehicle", "rocket", "rocket science", "sls", "space launch system", "falcon", "starship",
                    "ariane":
                result.add("🚀");
                break;
            case "satellite", "spacecraft", "crew dragon", "starliner", "progress", "soyuz":
                result.add("🛰️");
                break;
            case "planet", "mercury", "venus", "mars", "jupiter", "saturn", "uranus", "neptune":
                result.add("🪐");
                break;
            case "plants", "plant production area":
                result.add("🌱");
                break;
            case "earth":
                result.add("🌎");
                break;
            case "sun":
                result.add("☀️");
                break;
            case "star", "galaxy":
                result.add("✨");
                break;
            case "comet":
                result.add("☄️");
                break;
            }
        }
        return result;
    }
}
