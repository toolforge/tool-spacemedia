package org.wikimedia.commons.donvip.spacemedia.service.mastodon;

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
            LOGGER.error("Unable to setup Mastodon API: {}", e.getMessage(), e);
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
    public void postStatus(Collection<? extends Media<?, ?>> uploadedMedia, Collection<Metadata> uploadedMetadata,
            Set<String> accounts) throws IOException {
        callApi(buildStatusRequest(uploadedMedia, uploadedMetadata, accounts), Status.class);
    }

    @Override
    protected OAuthRequest buildStatusRequest(Collection<? extends Media<?, ?>> uploadedMedia,
            Collection<Metadata> uploadedMetadata, Set<String> accounts) throws IOException {
        return postRequest(api.getStatusUrl(), "application/json",
                new StatusRequest(createStatusText(accounts, uploadedMedia.size(), uploadedMetadata),
                        postMedia(uploadedMetadata)));
    }

    private List<String> postMedia(Collection<Metadata> uploadedMetadata) {
        List<String> mediaIds = new ArrayList<>();
        for (Metadata metadata : determineMediaToUploadToSocialMedia(uploadedMetadata)) {
            try {
                LOGGER.info("Start uploading of media to Mastodon: {}", metadata);
                List<CommonsImageProjection> files = imageRepo
                        .findBySha1OrderByTimestamp(CommonsService.base36Sha1(metadata.getSha1()));
                if (!files.isEmpty()) {
                    final CommonsImageProjection file = files.get(0);
                    URL url = CommonsService.getImageUrl(file.getName());
                    String mime = metadata.getMime();
                    if (!"image/gif".equals(mime)) {
                        url = new URL(String.format("%s/%dpx-%s.jpg", url.toExternalForm(),
                                Math.max(4096, file.getWidth()), file.getName()));
                        mime = "image/jpeg";
                    }
                    LOGGER.info("File and URL resolved to: {} - {}", file, url);
                    try (CloseableHttpClient httpclient = HttpClients.createDefault();
                            CloseableHttpResponse response = httpclient.execute(new HttpGet(url.toURI()));
                            InputStream in = response.getEntity().getContent()) {
                        mediaIds.add(callApi(request(Verb.POST, api.getMediaUrl(), "multipart/form-data",
                                new FileByteArrayBodyPartPayload(in.readAllBytes(), "file"),
                                Map.of("media_type", mime, "description", file.getName())), MediaAttachment.class)
                                .getId());
                    }
                } else {
                    LOGGER.error("Couldn't find by its SHA1 a file we've just uploaded: {}", metadata.getSha1());
                }
            } catch (IOException | RuntimeException | URISyntaxException e) {
                LOGGER.error("Unable to retrieve JPEG from Commons or upload it to Mastodon: {}", e.getMessage(), e);
            }
        }
        return mediaIds.isEmpty() ? null : mediaIds;
    }
}
