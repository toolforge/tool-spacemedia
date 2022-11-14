package org.wikimedia.commons.donvip.spacemedia.service;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException.NotFound;
import org.springframework.web.client.RestTemplate;
import org.wikimedia.commons.donvip.spacemedia.data.domain.HashAssociation;
import org.wikimedia.commons.donvip.spacemedia.data.domain.Media;

@Service
public class RemoteService {
    @Value("${remote.application.uri}")
    private URI remoteApplication;

    public String getHashLastTimestamp() {
        return new RestTemplate().getForObject(remoteApplication + "/hashLastTimestamp", String.class);
    }

    public void putHashAssociation(HashAssociation hash) {
        new RestTemplate().put(remoteApplication + "/hashAssociation", hash);
    }

    public void saveMedia(String agencyId, Media<?, ?> media) {
        new RestTemplate().put(
                String.join("/", remoteApplication.toString(), agencyId, "rest/media", media.getId().toString()),
                media);
    }

    public <T extends Media<?, ?>> T getMedia(String agencyId, String mediaId, Class<T> mediaClass) {
        try {
            return new RestTemplate().getForObject(
                    String.join("/", remoteApplication.toString(), agencyId, "rest/media", mediaId), mediaClass);
        } catch (NotFound e) {
            return null;
        }
    }
}
