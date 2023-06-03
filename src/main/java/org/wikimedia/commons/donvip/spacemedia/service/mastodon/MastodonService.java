package org.wikimedia.commons.donvip.spacemedia.service.mastodon;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.util.TriConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.data.commons.CommonsImageProjection;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.Media;
import org.wikimedia.commons.donvip.spacemedia.service.AbstractSocialMediaService;
import org.wikimedia.commons.donvip.spacemedia.service.wikimedia.CommonsService;

import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.httpclient.multipart.FileByteArrayBodyPartPayload;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;

@Service
public class MastodonService extends AbstractSocialMediaService<OAuth20Service, OAuth2AccessToken> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MastodonService.class);

    private MastodonApi api;
    private OAuth20Service oAuthService;
    private OAuth2AccessToken oAuthAccessToken;

    public MastodonService(@Value("${mastodon.instance}") String instance,
            @Value("${mastodon.api.oauth2.client-id}") String clientId,
            @Value("${mastodon.api.oauth2.client-secret}") String clientSecret,
            @Value("${mastodon.api.oauth2.access-token}") String accessToken) {
        try {
            api = MastodonApi.instance(instance);
            oAuthService = new ServiceBuilder(clientId).apiSecret(clientSecret).build(api);
            oAuthAccessToken = new OAuth2AccessToken(accessToken);
        } catch (IllegalArgumentException e) {
            LOGGER.error("Unable to setup Mastodon API: {}", e.getMessage());
        }
    }

    @Override
    protected OAuth20Service getOAuthService() {
        return oAuthService;
    }

    @Override
    protected OAuth2AccessToken getAccessToken() {
        return oAuthAccessToken;
    }

    @Override
    protected TriConsumer<OAuth20Service, OAuth2AccessToken, OAuthRequest> getSignMethod() {
        return OAuth20Service::signRequest;
    }

    @Override
    public void postStatus(String text) throws IOException {
        callApi(buildStatusRequest(text), Status.class);
    }

    @Override
    public void postStatus(Collection<? extends Media<?, ?>> uploadedMedia, Collection<FileMetadata> uploadedMetadata,
            Set<String> emojis, Set<String> accounts) throws IOException {
        callApi(buildStatusRequest(uploadedMedia, uploadedMetadata, emojis, accounts), Status.class);
    }

    @Override
    protected OAuthRequest buildStatusRequest(String text) throws IOException {
        return postRequest(api.getStatusUrl(), "application/json", new StatusRequest(text, null));
    }

    @Override
    protected OAuthRequest buildStatusRequest(Collection<? extends Media<?, ?>> uploadedMedia,
            Collection<FileMetadata> uploadedMetadata, Set<String> emojis, Set<String> accounts) throws IOException {
        return postRequest(api.getStatusUrl(), "application/json",
                new StatusRequest(
                        createStatusText(emojis, accounts, uploadedMedia.stream().filter(Media::isImage).count(),
                                uploadedMedia.stream().filter(Media::isVideo).count(), uploadedMetadata),
                        postMedia(uploadedMetadata)));
    }

    private List<String> postMedia(Collection<FileMetadata> uploadedMetadata) {
        List<String> mediaIds = new ArrayList<>();
        for (FileMetadata metadata : determineMediaToUploadToSocialMedia(uploadedMetadata)) {
            try {
                LOGGER.info("Start uploading of media to Mastodon: {}", metadata);
                List<CommonsImageProjection> files = imageRepo
                        .findBySha1OrderByTimestamp(CommonsService.base36Sha1(metadata.getSha1()));
                if (!files.isEmpty()) {
                    mediaIds.add(postMedia(new MediaUploadContext(files.get(0), metadata.getMime()), this::postMedia));
                } else {
                    LOGGER.error("Couldn't find by its SHA1 a file we've just uploaded: {}", metadata.getSha1());
                }
            } catch (IOException | RuntimeException | URISyntaxException e) {
                LOGGER.error("Unable to retrieve JPEG from Commons or upload it to Mastodon: {}", e.getMessage(), e);
            }
        }
        return mediaIds.isEmpty() ? null : mediaIds;
    }

    private String postMedia(MediaUploadContext muc, byte[] data) {
        try {
            return callApi(request(Verb.POST, api.getMediaUrl(), "multipart/form-data",
                    new FileByteArrayBodyPartPayload("application/octet-stream", data, "file", muc.filename),
                    Map.of("media_type", muc.mime, "description", muc.filename)), MediaAttachment.class).getId();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
