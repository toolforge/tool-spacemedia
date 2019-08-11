package org.wikimedia.commons.donvip.spacemedia.service.agencies;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wikimedia.commons.donvip.spacemedia.data.local.ProblemRepository;
import org.wikimedia.commons.donvip.spacemedia.data.local.Statistics;
import org.wikimedia.commons.donvip.spacemedia.data.local.flickr.FlickrMedia;
import org.wikimedia.commons.donvip.spacemedia.data.local.flickr.FlickrMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.service.FlickrService;
import org.wikimedia.commons.donvip.spacemedia.service.MediaService;

import com.flickr4java.flickr.FlickrException;
import com.github.dozermapper.core.Mapper;

public abstract class SpaceAgencyFlickrService extends SpaceAgencyService<FlickrMedia, Long> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpaceAgencyFlickrService.class);

    protected final FlickrMediaRepository flickrRepository;
    protected final MediaService mediaService;
    protected final FlickrService flickrService;
    protected final Mapper dozerMapper;
    protected final Set<String> flickrAccounts;

    public SpaceAgencyFlickrService(FlickrMediaRepository repository, ProblemRepository problemrepository,
            MediaService mediaService, FlickrService flickrService, Mapper dozerMapper, Set<String> flickrAccounts) {
        super(repository, problemrepository);
        this.flickrRepository = repository;
        this.mediaService = Objects.requireNonNull(mediaService);
        this.flickrService = Objects.requireNonNull(flickrService);
        this.dozerMapper = Objects.requireNonNull(dozerMapper);
        this.flickrAccounts = Objects.requireNonNull(flickrAccounts);
    }

    @Override
    public final long countAllMedia() {
        return flickrRepository.count(flickrAccounts);
    }

    @Override
    public final long countMissingMedia() {
        return flickrRepository.countMissingInCommons(flickrAccounts);
    }

    @Override
    public final Iterable<FlickrMedia> listAllMedia() {
        return flickrRepository.findAll(flickrAccounts);
    }

    @Override
    public final List<FlickrMedia> listMissingMedia() {
        return flickrRepository.findMissingInCommons(flickrAccounts);
    }

    @Override
    public final List<FlickrMedia> listDuplicateMedia() {
        return flickrRepository.findDuplicateInCommons(flickrAccounts);
    }

    @Override
    public Statistics getStatistics() {
        Statistics stats = super.getStatistics();
        if (flickrAccounts.size() > 1) {
            stats.setDetails(flickrAccounts.stream()
                    .map(a -> new Statistics(a, flickrRepository.count(Collections.singleton(a)),
                            flickrRepository.countMissingInCommons(Collections.singleton(a)), null))
                    .sorted().collect(Collectors.toList()));
        }
        return stats;
    }

    protected List<FlickrMedia> updateFlickrMedia() {
        LocalDateTime start = LocalDateTime.now();
        LOGGER.info("Starting {} medias update...", getName());
        final List<FlickrMedia> medias = new ArrayList<>();
        for (String flickrAccount : flickrAccounts) {
            try {
                LOGGER.info("Fetching Flickr images from account '{}'...", flickrAccount);
                for (FlickrMedia media : flickrService.findFreePhotos(flickrAccount).stream()
                        .map(p -> dozerMapper.map(p, FlickrMedia.class)).collect(Collectors.toList())) {
                    try {
                        medias.add(processFlickrMedia(media));
                    } catch (IOException | URISyntaxException e) {
                        LOGGER.error("Error while processing media " + media, e);
                    }
                }
            } catch (FlickrException e) {
                LOGGER.error("Error while fetching Flickr images from account " + flickrAccount, e);
            }
        }
        LOGGER.info("{} medias update completed: {} medias in {}", getName(), medias.size(),
                Duration.between(LocalDateTime.now(), start));
        return medias;
    }

    private FlickrMedia processFlickrMedia(FlickrMedia media) throws IOException, URISyntaxException {
        boolean save = false;
        Optional<FlickrMedia> mediaInRepo = flickrRepository.findById(media.getId());
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
            media = flickrRepository.save(media);
        }
        return media;
    }
}
