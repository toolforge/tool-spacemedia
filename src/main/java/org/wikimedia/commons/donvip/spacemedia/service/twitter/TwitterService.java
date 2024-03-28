package org.wikimedia.commons.donvip.spacemedia.service.twitter;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.apache.logging.log4j.util.TriConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.data.commons.CommonsImageProjection;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.Media;
import org.wikimedia.commons.donvip.spacemedia.service.AbstractSocialMediaService;
import org.wikimedia.commons.donvip.spacemedia.service.twitter.TweetRequest.TweetMedia;
import org.wikimedia.commons.donvip.spacemedia.service.wikimedia.CommonsService;

import com.github.scribejava.apis.TwitterApi;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.httpclient.multipart.FileByteArrayBodyPartPayload;
import com.github.scribejava.core.model.OAuth1AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth10aService;
import com.twitter.twittertext.TwitterTextParser;

import io.github.redouane59.twitter.TwitterClient;
import io.github.redouane59.twitter.dto.tweet.MediaCategory;
import io.github.redouane59.twitter.dto.tweet.TweetParameters;
import io.github.redouane59.twitter.dto.tweet.TweetParameters.TweetParametersBuilder;
import io.github.redouane59.twitter.dto.tweet.UploadMediaResponse;
import io.github.redouane59.twitter.signature.TwitterCredentials;

@Lazy
@Service
public class TwitterService extends AbstractSocialMediaService<OAuth10aService, OAuth1AccessToken> {

    private static final Logger LOGGER = LoggerFactory.getLogger(TwitterService.class);

    private static final String V1_UPLOAD = "https://upload.twitter.com/1.1/media/upload.json?media_category=";

    private static final String V2_TWEET = "https://api.twitter.com/2/tweets";

    private OAuth10aService oAuthService;
    private OAuth1AccessToken oAuthAccessToken;

    private TwitterClient twitterClient;

    public TwitterService(
            @Value("${twitter.api.oauth1.consumer-token}") String consumerToken,
            @Value("${twitter.api.oauth1.consumer-secret}") String consumerSecret,
            @Value("${twitter.api.oauth1.access-token}") String accessToken,
            @Value("${twitter.api.oauth1.access-secret}") String accessSecret) {
        try {
            oAuthService = new ServiceBuilder(consumerToken).apiSecret(consumerSecret).build(TwitterApi.instance());
            oAuthAccessToken = new OAuth1AccessToken(accessToken, accessSecret);
            twitterClient = new TwitterClient(TwitterCredentials.builder().accessToken(accessToken)
                    .accessTokenSecret(accessSecret).apiKey(consumerToken).apiSecretKey(consumerSecret).build());
        } catch (IllegalArgumentException e) {
            LOGGER.error("Unable to setup Twitter API: {}", e.getMessage());
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
    public void postStatus(String text) throws IOException {
        callApi(buildStatusRequest(text), TweetResponse.class);
    }

    @Override
    public void postStatus(Collection<? extends Media> uploadedMedia, Collection<FileMetadata> uploadedMetadata,
            Set<String> emojis, Set<String> accounts) throws IOException {
        OAuthRequest request = buildStatusRequest(uploadedMedia, uploadedMetadata, emojis, accounts);
        try {
            callApi(request, TweetResponse.class);
        } catch (IOException e) {
            LOGGER.error("Unable to post status with own code: {}", e.getMessage());
            LOGGER.debug("Unable to post status with own code: {}", e.getMessage(), e);
            LOGGER.info("Fallback to twittered client call...");
            TweetRequest tweet = jackson.readValue(request.getStringPayload(), TweetRequest.class);
            TweetParametersBuilder builder = TweetParameters.builder().text(tweet.text());
            if (tweet.media() != null) {
                builder.media(TweetParameters.Media.builder().mediaIds(tweet.media().mediaIds()).build());
            }
            TweetParameters params = builder.build();
            LOGGER.info("Twittered JSON payload: {}", jackson.writeValueAsString(params));
            twitterClient.postTweet(params);
        }
    }

    @Override
    protected OAuthRequest buildStatusRequest(String text) throws IOException {
        return postRequest(V2_TWEET, "application/json", new TweetRequest(null, text));
    }

    @Override
    protected OAuthRequest buildStatusRequest(Collection<? extends Media> uploadedMedia,
            Collection<FileMetadata> uploadedMetadata, Set<String> emojis, Set<String> accounts) throws IOException {
        final long imagesCount = uploadedMedia.stream().filter(Media::isImage).count();
        final long videosCount = uploadedMedia.stream().filter(Media::isVideo).count();
        int maxKeywords = 8;
        String text = createStatusText(emojis, accounts, imagesCount, videosCount, uploadedMedia, uploadedMetadata,
                maxKeywords);
        while (!isTweetValid(text) && maxKeywords > 1) {
            text = createStatusText(emojis, accounts, imagesCount, videosCount, uploadedMedia, uploadedMetadata,
                    --maxKeywords);
        }
        return postRequest(V2_TWEET, "application/json", new TweetRequest(createTweetMedia(uploadedMetadata), text));
    }

    private static boolean isTweetValid(String text) {
        return TwitterTextParser.parseTweet(text).isValid;
    }

    private TweetMedia createTweetMedia(Collection<FileMetadata> uploadedMetadata) {
        List<String> mediaIds = new ArrayList<>();
        for (FileMetadata metadata : determineMediaToUploadToSocialMedia(uploadedMetadata)) {
            try {
                LOGGER.info("Start uploading of media to Twitter: {}", metadata);
                List<CommonsImageProjection> files = imageRepo
                        .findBySha1OrderByTimestamp(CommonsService.base36Sha1(metadata.getSha1()));
                if (!files.isEmpty()) {
                    mediaIds.add(postMedia(new MediaUploadContext(files.get(0), metadata.getMime()), this::postMedia));
                } else {
                    LOGGER.error("Couldn't find by its SHA1 a file we've just uploaded: {}", metadata.getSha1());
                }
            } catch (IOException | RuntimeException | URISyntaxException e) {
                LOGGER.error("Unable to retrieve JPEG from Commons or upload it to Twitter: {}", e.getMessage());
                LOGGER.debug("Unable to retrieve JPEG from Commons or upload it to Twitter: {}", e.getMessage(), e);
            }
        }
        // Don't return empty media object as it causes bad request in v2/tweet endpoint
        return mediaIds.isEmpty() ? null : new TweetMedia(mediaIds);
    }

    private String postMedia(MediaUploadContext muc, byte[] data) {
        try {
            return callApi(request(Verb.POST, V1_UPLOAD + muc.cat, null,
                    new FileByteArrayBodyPartPayload("application/octet-stream", data, "media", muc.filename), null),
                    UploadResponse.class).getMediaIdString();
        } catch (Exception e) {
            LOGGER.error("Unable to post media with own code: {}", e.getMessage(), e);
            LOGGER.info("Fallback to twittered client call...");
            UploadMediaResponse r = twitterClient.uploadMedia(muc.filename, data,
                    MediaCategory.valueOf(muc.cat.toUpperCase(Locale.ENGLISH)));
            if (r.getMediaId() == null) {
                throw new UncheckedIOException(new IOException("Twitter response without media id: " + r, e));
            }
            return r.getMediaId();
        }
    }
}
