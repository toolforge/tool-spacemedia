package org.wikimedia.commons.donvip.spacemedia.service;

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;

import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException.NotFound;
import org.springframework.web.client.HttpServerErrorException.BadGateway;
import org.springframework.web.client.HttpServerErrorException.ServiceUnavailable;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.Media;
import org.wikimedia.commons.donvip.spacemedia.data.hashes.HashAssociation;
import org.wikimedia.commons.donvip.spacemedia.service.wikimedia.GlitchTip;

@Lazy
@Service
public class RemoteService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RemoteService.class);

    @Value("${remote.application.uri}")
    private URI remoteApplication;

    @Value("${git.build.version}")
    private String buildVersion;

    @Value("${git.commit.id.full}")
    private String commitId;

    @Autowired
    private RestTemplate restTemplate;

    public String getHashLastTimestamp() {
        return exchange(remoteApplication + "/hashLastTimestamp", GET, String.class, null).getBody();
    }

    public void putHashAssociation(HashAssociation hash) {
        boolean ok = false;
        do {
            try {
                exchange(remoteApplication + "/hashAssociation", PUT, String.class, hash);
                ok = true;
            } catch (BadGateway | ServiceUnavailable | ResourceAccessException e) {
                // Tool is restarting... loop until it comes back
                LOGGER.debug("{}", e.getMessage(), e);
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ex) {
                    LOGGER.error("{}", ex.getMessage(), ex);
                    GlitchTip.capture(e);
                    Thread.currentThread().interrupt();
                }
            }
        } while (!ok);
    }

    public void saveMedia(String orgId, Media media) {
        exchange(String.join("/", restOrgEndpoint(orgId), "media", media.getIdUsedInOrg()), PUT, String.class, media);
    }

    public <T extends Media> T getMedia(String orgId, String mediaId, Class<T> mediaClass) {
        try {
            return exchange(String.join("/", restOrgEndpoint(orgId), "media", mediaId), GET, mediaClass, null)
                    .getBody();
        } catch (NotFound e) {
            return null;
        }
    }

    public void evictCaches(String orgId) {
        exchange(restOrgEndpoint(orgId) + "/evictcaches", GET, String.class, null);
    }

    public String getUserAgent() {
        return "Spacemedia " + buildVersion + '/' + commitId;
    }

    private String restOrgEndpoint(String orgId) {
        return String.join("/", remoteApplication.toString(), orgId, "rest");
    }

    private <T> ResponseEntity<T> exchange(String url, HttpMethod method, Class<T> responseType,
            Object requestBody, Object... uriVariables) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", getUserAgent());
        return restTemplate.exchange(url, method,
                method == POST || method == PUT || method == PATCH ? new HttpEntity<>(requestBody, headers)
                        : new HttpEntity<>(headers),
                responseType, uriVariables);
    }
}
