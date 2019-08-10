package org.wikimedia.commons.donvip.spacemedia.service;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.data.local.flickr.FlickrMedia;
import org.wikimedia.commons.donvip.spacemedia.data.local.flickr.FlickrMediaRepository;

import com.flickr4java.flickr.FlickrException;
import com.github.dozermapper.core.Mapper;

@Service
public class SpaceXService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpaceXService.class);

    @Value("${spacex.flickr.accounts}")
    private Set<String> flickrAccounts;

    @Autowired
    private FlickrMediaRepository repository;

    @Autowired
    private MediaService mediaService;

    @Autowired
    private FlickrService flickrService;

    @Autowired
    private Mapper dozerMapper;

    @Scheduled(fixedRateString = "${spacex.update.rate}")
    public List<FlickrMedia> updateMedia() {
        LocalDateTime start = LocalDateTime.now();
        LOGGER.info("Starting SpaceX medias update...");
        final List<FlickrMedia> medias = new ArrayList<>();
        for (String flickrAccount : flickrAccounts) {
            try {
                LOGGER.info("Fetching Flickr images from account '{}'...", flickrAccount);
                for (FlickrMedia media : flickrService.findFreePhotos(flickrAccount).stream()
                        .map(p -> dozerMapper.map(p, FlickrMedia.class)).collect(Collectors.toList())) {
                    try {
                        medias.add(processMedia(media));
                    } catch (IOException | URISyntaxException e) {
                        LOGGER.error("Error while processing media " + media, e);
                    }
                }
            } catch (FlickrException e) {
                LOGGER.error("Error while fetching Flickr images from account " + flickrAccount, e);
            }
        }
        LOGGER.info("SpaceX medias update completed: {} medias in {}", medias.size(),
                Duration.between(LocalDateTime.now(), start));
        return medias;
    }

    private FlickrMedia processMedia(FlickrMedia media) throws IOException, URISyntaxException {
        boolean save = false;
        Optional<FlickrMedia> mediaInRepo = repository.findById(media.getId());
        if (mediaInRepo.isPresent()) {
            media = mediaInRepo.get();
        } else {
            save = true;
        }
        if (mediaService.computeSha1(media, media.getOriginalUrl())) {
            save = true;
        }
        if (mediaService.findCommonsFilesWithSha1(media)) {
            save = true;
        }
        if (save) {
            media = repository.save(media);
        }
        return media;
    }
}
