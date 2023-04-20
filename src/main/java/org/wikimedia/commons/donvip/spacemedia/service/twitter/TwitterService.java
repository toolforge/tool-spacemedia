package org.wikimedia.commons.donvip.spacemedia.service.twitter;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.logging.log4j.util.TriConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.data.commons.CommonsImageProjection;
import org.wikimedia.commons.donvip.spacemedia.data.domain.Media;
import org.wikimedia.commons.donvip.spacemedia.data.domain.Metadata;
import org.wikimedia.commons.donvip.spacemedia.service.AbstractSocialMediaService;
import org.wikimedia.commons.donvip.spacemedia.service.twitter.TweetRequest.TweetMedia;
import org.wikimedia.commons.donvip.spacemedia.service.twitter.UploadResponse.ProcessingInfo;
import org.wikimedia.commons.donvip.spacemedia.service.wikimedia.CommonsService;

import com.github.scribejava.apis.TwitterApi;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.httpclient.multipart.FileByteArrayBodyPartPayload;
import com.github.scribejava.core.model.OAuth1AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth10aService;

@Service
public class TwitterService extends AbstractSocialMediaService<OAuth10aService, OAuth1AccessToken> {

    private static final Logger LOGGER = LoggerFactory.getLogger(TwitterService.class);

    private static final String V1_UPLOAD = "https://upload.twitter.com/1.1/media/upload.json";

    private static final String V2_TWEET = "https://api.twitter.com/2/tweets";

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

    @Override
    protected OAuth10aService getOAuthService() {
        return oAuthService;
    }

    @Override
    protected OAuth1AccessToken getAccessToken() {
        return oAuthAccessToken;
    }

    @Override
    protected TriConsumer<OAuth10aService, OAuth1AccessToken, OAuthRequest> getSignMethod() {
        return OAuth10aService::signRequest;
    }

    @Override
    public void postStatus(Collection<? extends Media<?, ?>> uploadedMedia, Collection<Metadata> uploadedMetadata,
            Set<String> emojis, Set<String> accounts) throws IOException {
        callApi(buildStatusRequest(uploadedMedia, uploadedMetadata, emojis, accounts), TweetResponse.class);
    }

    @Override
    protected OAuthRequest buildStatusRequest(Collection<? extends Media<?, ?>> uploadedMedia,
            Collection<Metadata> uploadedMetadata, Set<String> emojis, Set<String> accounts) throws IOException {
        return postRequest(V2_TWEET, "application/json",
                new TweetRequest(createTweetMedia(uploadedMetadata),
                        createStatusText(emojis, accounts, uploadedMedia.size(), uploadedMetadata)));
    }

    private TweetMedia createTweetMedia(Collection<Metadata> uploadedMetadata) {
        List<Long> mediaIds = new ArrayList<>();
        for (Metadata metadata : determineMediaToUploadToSocialMedia(uploadedMetadata)) {
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

                        performMultipartUpload(in, mime, mediaId, file.getName());

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
        return callApi(request(Verb.POST, V1_UPLOAD, "multipart/form-data", null,
                Map.of("command", "INIT", "media_type", mime, "total_bytes", contentLength, "media_category", cat)),
                UploadResponse.class).getMediaId();
    }

    private void performMultipartUpload(InputStream in, String mime, long mediaId, String fileName) throws IOException {
        int idx = 0;
        byte[] bytes;
        do {
            bytes = in.readNBytes(1 * 1024 * 1024);
            callApi(request(Verb.POST, V1_UPLOAD, "multipart/form-data",
                    new FileByteArrayBodyPartPayload(mime, bytes, "media", fileName),
                    Map.of("command", "APPEND", "media_id", mediaId, "segment_index", idx++)), Object.class);
        } while (bytes.length > 0);
    }

    private ProcessingInfo finalizeUpload(final long mediaId) throws IOException {
        return callApi(request(Verb.POST, V1_UPLOAD, "multipart/form-data", null,
                Map.of("command", "FINALIZE", "media_id", mediaId)), UploadResponse.class).getProcessingInfo();
    }

    private ProcessingInfo awaitUploadCompletion(List<Long> mediaIds, final long mediaId, ProcessingInfo processingInfo)
            throws InterruptedException, IOException {
        int checkAfterSecs = processingInfo.getCheckAfterSecs();
        LOGGER.info("Waiting {} seconds for the upload to be complete...", checkAfterSecs);
        Thread.sleep(checkAfterSecs * 1000L);
        processingInfo = callApi(
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
}
