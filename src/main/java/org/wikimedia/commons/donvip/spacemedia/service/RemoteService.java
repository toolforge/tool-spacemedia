package org.wikimedia.commons.donvip.spacemedia.service;

import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException.NotFound;
import org.springframework.web.client.HttpServerErrorException.BadGateway;
import org.springframework.web.client.HttpServerErrorException.ServiceUnavailable;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.HashAssociation;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.Media;

@Service
public class RemoteService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RemoteService.class);

    @Value("${remote.application.uri}")
    private URI remoteApplication;

    @Autowired
    private RestTemplate restTemplate;

    public String getHashLastTimestamp() {
        return restTemplate.getForObject(remoteApplication + "/hashLastTimestamp", String.class);
    }

    public void putHashAssociation(HashAssociation hash) {
        boolean ok = false;
        do {
            try {
                restTemplate.put(remoteApplication + "/hashAssociation", hash);
                ok = true;
            } catch (BadGateway | ServiceUnavailable | ResourceAccessException e) {
                // Tool is restarting... loop until it comes back
                LOGGER.debug("{}", e.getMessage(), e);
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ex) {
                    LOGGER.error("{}", ex.getMessage(), ex);
                    Thread.currentThread().interrupt();
                }
            }
        } while (!ok);
    }

    public void saveMedia(String orgId, Media<?> media) {
        restTemplate.put(
                String.join("/", restOrgEndpoint(orgId), "media", media.getIdUsedInOrg()), media);
    }

    public <T extends Media<?>> T getMedia(String orgId, String mediaId, Class<T> mediaClass) {
        try {
            return restTemplate.getForObject(
                    String.join("/", restOrgEndpoint(orgId), "media", mediaId), mediaClass);
        } catch (NotFound e) {
            return null;
        }
    }

    public void evictCaches(String orgId) {
        restTemplate.getForObject(restOrgEndpoint(orgId) + "/evictcaches", String.class);
    }

    private String restOrgEndpoint(String orgId) {
        return String.join("/", remoteApplication.toString(), orgId, "rest");
    }
}
