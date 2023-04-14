package org.wikimedia.commons.donvip.spacemedia.service.twitter;

import static java.util.stream.Collectors.joining;
import static org.wikimedia.commons.donvip.spacemedia.service.wikimedia.CommonsService.timestampFormatter;
import static org.wikimedia.commons.donvip.spacemedia.utils.HashHelper.similarityScore;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.data.commons.CommonsImageProjection;
import org.wikimedia.commons.donvip.spacemedia.data.commons.CommonsImageRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.Media;
import org.wikimedia.commons.donvip.spacemedia.data.domain.Metadata;
import org.wikimedia.commons.donvip.spacemedia.service.twitter.TweetRequest.TweetMedia;
import org.wikimedia.commons.donvip.spacemedia.service.twitter.UploadResponse.ProcessingInfo;
import org.wikimedia.commons.donvip.spacemedia.service.wikimedia.CommonsService;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.scribejava.apis.TwitterApi;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.httpclient.multipart.BodyPartPayload;
import com.github.scribejava.core.httpclient.multipart.FileByteArrayBodyPartPayload;
import com.github.scribejava.core.model.OAuth1AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth10aService;

@Service
public class TwitterService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TwitterService.class);

    private static final String V1_UPLOAD = "https://upload.twitter.com/1.1/media/upload.json";

    private static final String V2_TWEET = "https://api.twitter.com/2/tweets";

    @Autowired
    private ObjectMapper jackson;

    @Autowired
    private CommonsImageRepository imageRepo;

    @Value("${perceptual.threshold}")
    private double perceptualThreshold;

    @Value("${commons.api.account}")
    private String commonsAccount;

    private OAuth10aService oAuthService;
    private OAuth1AccessToken oAuthAccessToken;

    public TwitterService(
            @Value("${twitter.api.oauth1.consumer-token}") String consumerToken,
            @Value("${twitter.api.oauth1.consumer-secret}") String consumerSecret,
            @Value("${twitter.api.oauth1.access-token}") String accessToken,
            @Value("${twitter.api.oauth1.access-secret}") String accessSecret) {
        try {
            oAuthService = new ServiceBuilder(consumerToken).apiSecret(consumerSecret).build(TwitterApi.instance());
            oAuthAccessToken = new OAuth1AccessToken(accessToken, accessSecret);
        } catch (IllegalArgumentException e) {
            LOGGER.error("Unable to setup Twitter API: {}", e.getMessage(), e);
        }
    }

    public TweetResponse tweet(Collection<? extends Media<?, ?>> uploadedMedia, Collection<Metadata> uploadedMetadata,
            Set<String> twitterAccounts) throws IOException {
        return callTwitterApi(buildTweetRequest(uploadedMedia, uploadedMetadata, twitterAccounts), TweetResponse.class);
    }

    private <T> T callTwitterApi(OAuthRequest request, Class<T> responseClass) throws IOException {
        if (oAuthService == null || oAuthAccessToken == null) {
            throw new IOException("Twitter API not initialized correctly");
        }
        try {
            LOGGER.info("Calling Twitter API with request {}", request);
            Response response = oAuthService.execute(request);
            if (response.getCode() >= 400) {
                LOGGER.error("Twitter error: {}", response);
            } else {
                LOGGER.info("Twitter response: {}", response);
            }
            return jackson.readValue(response.getBody(), responseClass);
        } catch (ExecutionException e) {
            throw new IOException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        }
    }

    private OAuthRequest postRequest(String endpoint, String contentType, Object payload)
            throws JsonProcessingException {
        return request(Verb.POST, endpoint, contentType, payload, Map.of());
    }

    private OAuthRequest request(Verb verb, String endpoint, String contentType, Object payload,
            Map<String, Object> params) throws JsonProcessingException {
        OAuthRequest request = new OAuthRequest(verb, endpoint);
        if (verb == Verb.POST || verb == Verb.PUT) {
            request.addHeader("Content-Type", contentType);
        }
        request.setCharset(StandardCharsets.UTF_8.name());
        params.forEach((k, v) -> request.addParameter(k, v.toString()));
        oAuthService.signRequest(oAuthAccessToken, request);
        if ("application/json".equals(contentType)) {
            request.setPayload(jackson.writeValueAsString(payload));
        } else if (payload instanceof BodyPartPayload bodyPartPayload) {
            request.setBodyPartPayloadInMultipartPayload(bodyPartPayload);
        } else if (payload instanceof byte[] bytes) {
            request.setPayload(bytes);
        }
        return request;
    }

    OAuthRequest buildTweetRequest(Collection<? extends Media<?, ?>> uploadedMedia,
            Collection<Metadata> uploadedMetadata, Set<String> twitterAccounts) throws JsonProcessingException {
        return postRequest(V2_TWEET, "application/json",
                new TweetRequest(createTweetMedia(uploadedMetadata),
                        createTweetText(twitterAccounts, uploadedMedia.size(), uploadedMetadata)));
    }

    private String createTweetText(Set<String> twitterAccounts, int size, Collection<Metadata> uploadedMetadata) {
        String text = String.format("%d new picture%s", size, size >= 2 ? "s" : "");
        if (!twitterAccounts.isEmpty()) {
            text += " from " + twitterAccounts.stream().sorted().map(account -> "@" + account).collect(joining(" "));
        }
        text += " https://commons.wikimedia.org/wiki/Special:ListFiles?limit=" + uploadedMetadata.size()
                + "&user=" + commonsAccount + "&ilshowall=1&offset="
                + timestampFormatter.format(timestampFormatter
                        .parse(imageRepo.findMaxTimestampBySha1In(uploadedMetadata.parallelStream()
                                .map(Metadata::getSha1).map(CommonsService::base36Sha1).toList()), LocalDateTime::from)
                        .plusSeconds(1));
        return text;
    }

    private TweetMedia createTweetMedia(Collection<Metadata> uploadedMetadata) {
        List<Long> mediaIds = new ArrayList<>();
        for (Metadata metadata : determineMediaToUploadToTwitter(uploadedMetadata)) {
            try {
                LOGGER.info("Start uploading of media to Twitter: {}", metadata);
                List<CommonsImageProjection> files = imageRepo
                        .findBySha1OrderByTimestamp(CommonsService.base36Sha1(metadata.getSha1()));
                if (!files.isEmpty()) {
                    final CommonsImageProjection file = files.get(0);
                    URL url = CommonsService.getImageUrl(file.getName());
                    String mime = metadata.getMime();
                    String cat = "tweet_gif";
                    if (!"image/gif".equals(mime)) {
                        url = new URL(String.format("%s/%dpx-%s.jpg", url.toExternalForm(),
                                Math.max(4096, file.getWidth()), file.getName()));
                        mime = "image/jpeg";
                        cat = "tweet_image";
                    }
                    LOGGER.info("File and URL resolved to: {} - {}", file, url);
                    try (CloseableHttpClient httpclient = HttpClients.createDefault();
                            CloseableHttpResponse response = httpclient.execute(new HttpGet(url.toURI()));
                            InputStream in = response.getEntity().getContent()) {

                        final long mediaId = initializeUpload(mime, cat, response.getEntity().getContentLength());

                        performMultipartUpload(in, mediaId);

                        ProcessingInfo processingInfo = finalizeUpload(mediaId);

                        while (processingInfo != null) {
                            processingInfo = awaitUploadCompletion(mediaIds, mediaId, processingInfo);
                        }
                    }
                } else {
                    LOGGER.error("Couldn't find by its SHA1 a file we've just uploaded: {}", metadata.getSha1());
                }
            } catch (IOException | RuntimeException | URISyntaxException e) {
                LOGGER.error("Unable to retrieve JPEG from Commons or upload it to Twitter: {}", e.getMessage(), e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        // Don't return empty media object as it causes bad request in v2/tweet endpoint
        return mediaIds.isEmpty() ? null : new TweetMedia(mediaIds);
    }

    private long initializeUpload(String mime, String cat, final long contentLength) throws IOException {
        return callTwitterApi(request(Verb.POST, V1_UPLOAD, "multipart/form-data", null,
                Map.of("command", "INIT", "media_type", mime, "total_bytes", contentLength, "media_category", cat)),
                UploadResponse.class).getMediaId();
    }

    private void performMultipartUpload(InputStream in, final long mediaId) throws IOException {
        int idx = 0;
        byte[] bytes;
        do {
            bytes = in.readNBytes(1 * 1024 * 1024);
            callTwitterApi(request(Verb.POST, V1_UPLOAD, "multipart/form-data",
                    new FileByteArrayBodyPartPayload(bytes, "media"),
                    Map.of("command", "APPEND", "media_id", mediaId, "segment_index", idx++)), Object.class);
        } while (bytes.length > 0);
    }

    private ProcessingInfo finalizeUpload(final long mediaId) throws IOException {
        return callTwitterApi(request(Verb.POST, V1_UPLOAD, "multipart/form-data", null,
                Map.of("command", "FINALIZE", "media_id", mediaId)), UploadResponse.class).getProcessingInfo();
    }

    private ProcessingInfo awaitUploadCompletion(List<Long> mediaIds, final long mediaId, ProcessingInfo processingInfo)
            throws InterruptedException, IOException {
        int checkAfterSecs = processingInfo.getCheckAfterSecs();
        LOGGER.info("Waiting {} seconds for the upload to be complete...", checkAfterSecs);
        Thread.sleep(checkAfterSecs * 1000L);
        processingInfo = callTwitterApi(
                request(Verb.GET, V1_UPLOAD, null, null, Map.of("command", "STATUS", "media_id", mediaId)),
                UploadResponse.class).getProcessingInfo();

        switch (processingInfo.getState()) {
        case "succeeded":
            LOGGER.info("Upload of media id {} succeeded", mediaId);
            mediaIds.add(mediaId);
            processingInfo = null;
            break;
        case "failed":
            LOGGER.error("Upload of media id {} failed", mediaId);
            processingInfo = null;
            break;
        case "pending", "in_progress":
            LOGGER.info("Upload of media id {} still in progress", mediaId);
            break;
        default:
            LOGGER.error("Upload of media id {} is undetermined", mediaId);
            processingInfo = null;
        }
        return processingInfo;
    }

    private Collection<Metadata> determineMediaToUploadToTwitter(Collection<Metadata> uploadedMedia) {
        // https://developer.twitter.com/en/docs/twitter-api/v1/media/upload-media/uploading-media/media-best-practices
        List<Metadata> imgs = uploadedMedia.stream().filter(
                x -> x.isReadableImage() == Boolean.TRUE && x.getPhash() != null & x.getMime() != null
                        && x.getMime().startsWith("image/"))
                .toList();
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
}
