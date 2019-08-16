package org.wikimedia.commons.donvip.spacemedia.service.agencies;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
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
import org.springframework.beans.factory.annotation.Value;
import org.wikimedia.commons.donvip.spacemedia.data.local.ProblemRepository;
import org.wikimedia.commons.donvip.spacemedia.data.local.Statistics;
import org.wikimedia.commons.donvip.spacemedia.data.local.flickr.FlickrMedia;
import org.wikimedia.commons.donvip.spacemedia.data.local.flickr.FlickrMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.service.CommonsService;
import org.wikimedia.commons.donvip.spacemedia.service.FlickrService;
import org.wikimedia.commons.donvip.spacemedia.service.MediaService;

import com.flickr4java.flickr.FlickrException;
import com.flickr4java.flickr.people.User;
import com.github.dozermapper.core.Mapper;

public abstract class SpaceAgencyFlickrService extends SpaceAgencyService<FlickrMedia, Long> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpaceAgencyFlickrService.class);

    protected final FlickrMediaRepository flickrRepository;
    protected final FlickrService flickrService;
    protected final Mapper dozerMapper;
    protected final Set<String> flickrAccounts;

    @Value("${flickr.video.download.url}")
    private String flickrVideoDownloadUrl;

    public SpaceAgencyFlickrService(FlickrMediaRepository repository, ProblemRepository problemrepository,
            MediaService mediaService, CommonsService commonsService, FlickrService flickrService, Mapper dozerMapper,
            Set<String> flickrAccounts) {
        super(repository, problemrepository, mediaService, commonsService);
        this.flickrRepository = repository;
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

    @Override
    protected final String getDescription(FlickrMedia media) {
        return media.getDescription();
    }

    @Override
    protected final String getSource(FlickrMedia media) throws MalformedURLException {
        return wikiLink(getPhotoUrl(media), media.getTitle());
    }

    @Override
    protected final String getAuthor(FlickrMedia media) throws MalformedURLException {
        try {
            User user = flickrService.findUser(getUserPhotosUrl(media));
            URL profileUrl = flickrService.findUserProfileUrl(user.getId());
            return wikiLink(profileUrl, user.getUsername());
        } catch (FlickrException e) {
            throw new RuntimeException(e);
        }
    }

    private static final URL getUserPhotosUrl(FlickrMedia media) throws MalformedURLException {
        return new URL("https://www.flickr.com/photos/" + media.getPathAlias());
    }

    private static final URL getPhotoUrl(FlickrMedia media) throws MalformedURLException {
        return new URL(getUserPhotosUrl(media).toExternalForm() + "/" + media.getId());
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
                        problem(getPhotoUrl(media), e);
                    }
                }
            } catch (FlickrException | MalformedURLException e) {
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
        if ("video".equals(media.getMedia())) {
            String videoUrl = flickrVideoDownloadUrl.replace("<id>", media.getId().toString());
            if (!videoUrl.equals(media.getAssetUrl().toExternalForm())) {
                media.setAssetUrl(new URL(videoUrl));
                media.setCommonsFileNames(null);
                media.setSha1(null);
                save = true;
            }
        }
        if (mediaService.computeSha1(media)) {
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
