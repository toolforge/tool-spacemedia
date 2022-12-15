package org.wikimedia.commons.donvip.spacemedia.service;

import java.net.URI;

import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private RestTemplate restTemplate;

    public String getHashLastTimestamp() {
        return restTemplate.getForObject(remoteApplication + "/hashLastTimestamp", String.class);
    }

    public void putHashAssociation(HashAssociation hash) {
        restTemplate.put(remoteApplication + "/hashAssociation", hash);
    }

    public void saveMedia(String agencyId, Media<?, ?> media) {
        restTemplate.put(
                String.join("/", remoteApplication.toString(), agencyId, "rest/media", media.getId().toString()),
                media);
    }

    public <T extends Media<?, ?>> T getMedia(String agencyId, String mediaId, Class<T> mediaClass) {
        try {
            return restTemplate.getForObject(
                    String.join("/", remoteApplication.toString(), agencyId, "rest/media", mediaId), mediaClass);
        } catch (NotFound e) {
            return null;
        }
    }
}
