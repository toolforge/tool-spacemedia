package org.wikimedia.commons.donvip.spacemedia.service.flickr;

import static java.util.stream.Collectors.toSet;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.wikimedia.commons.donvip.spacemedia.data.domain.Media;
import org.wikimedia.commons.donvip.spacemedia.data.domain.MediaRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.Metadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrFreeLicense;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrMediaType;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrPhotoSet;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrPhotoSetRepository;
import org.wikimedia.commons.donvip.spacemedia.service.MediaService;
import org.wikimedia.commons.donvip.spacemedia.utils.CsvHelper;
import org.wikimedia.commons.donvip.spacemedia.utils.UnitedStates;

import com.flickr4java.flickr.FlickrException;
import com.github.dozermapper.core.Mapper;

@Service
public class FlickrMediaProcessorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlickrMediaProcessorService.class);

    @Autowired
    protected FlickrMediaRepository flickrRepository;
    @Autowired
    protected FlickrPhotoSetRepository flickrPhotoSetRepository;
    @Autowired
    protected FlickrService flickrService;
    @Autowired
    protected Mapper dozerMapper;
    @Autowired
    protected MediaService mediaService;

    @Value("${flickr.video.download.url}")
    private String flickrVideoDownloadUrl;

    private Set<Long> ignoredPhotoAlbums;

    @PostConstruct
    public void init() throws IOException {
        ignoredPhotoAlbums = CsvHelper.loadSet(getClass().getResource("/blocklist.ignored.flickr.albums.csv")).stream()
                .mapToLong(Long::parseLong).boxed().collect(toSet());
    }

    public URL getVideoUrl(FlickrMedia media) throws MalformedURLException {
        return new URL(flickrVideoDownloadUrl.replace("<id>", media.getId().toString()));
    }

    public boolean isBadVideoEntry(FlickrMedia media) throws MalformedURLException, URISyntaxException {
        return FlickrMediaType.video == media.getMedia()
                && !getVideoUrl(media).toURI().equals(media.getMetadata().getAssetUrl().toURI());
    }

    @Transactional
    public Pair<FlickrMedia, Integer> processFlickrMedia(FlickrMedia media, String flickrAccount,
            MediaRepository<? extends Media<?, ?>, ?, ?> originalRepo, Collection<String> stringsToRemove,
            Predicate<FlickrMedia> customProcessor, BiPredicate<FlickrMedia, Boolean> shouldUploadAuto,
            Function<FlickrMedia, Triple<FlickrMedia, Collection<Metadata>, Integer>> uploader)
            throws IOException {
        boolean save = false;
        boolean savePhotoSets = false;
        final Optional<FlickrMedia> optMediaInRepo = flickrRepository.findById(media.getId());
        final boolean isPresentInDb = optMediaInRepo.isPresent();
        if (isPresentInDb) {
            FlickrMedia mediaInRepo = optMediaInRepo.get();
            if (mediaInRepo.getLicense() != media.getLicense()) {
                save = handleLicenseChange(media, flickrAccount, mediaInRepo);
            }
            media = mediaInRepo;
        } else {
            save = true;
            if (isEmpty(media.getPhotosets())) {
                try {
                    Set<FlickrPhotoSet> sets = getPhotoSets(media, flickrAccount);
                    if (isNotEmpty(sets)) {
                        sets.forEach(media::addPhotoSet);
                        savePhotoSets = true;
                    }
                } catch (FlickrException e) {
                    LOGGER.error("Failed to retrieve photosets of image " + media.getId(), e);
                }
            }
            if (StringUtils.isEmpty(media.getPathAlias())) {
                media.setPathAlias(flickrAccount);
            }
            try {
                if (FlickrFreeLicense.of(media.getLicense()) == FlickrFreeLicense.Public_Domain_Mark
                        && !Boolean.TRUE.equals(media.isIgnored())
                        && !UnitedStates.isClearPublicDomain(media.getDescription())) {
                    MediaService.ignoreMedia(media, "Public Domain Mark is not a legal license");
                }
            } catch (IllegalArgumentException e) {
                LOGGER.debug("Non-free Flickr licence for media {}: {}", media, e.getMessage());
            }
        }
        try {
            if (isBadVideoEntry(media)) {
                save = handleBadVideo(media);
            }
        } catch (URISyntaxException e) {
            LOGGER.error("URISyntaxException for video " + media.getId(), e);
        }
        if (StringUtils.isNotBlank(media.getDescription())) {
            for (String toRemove : stringsToRemove) {
                if (media.getDescription().contains(toRemove)) {
                    media.setDescription(media.getDescription().replace(toRemove, "").trim());
                    save = true;
                }
            }
        }
        if (media.getPhotosets() != null) {
            for (FlickrPhotoSet photoSet : media.getPhotosets()) {
                if (StringUtils.isBlank(photoSet.getPathAlias())) {
                    photoSet.setPathAlias(flickrAccount);
                    savePhotoSets = true;
                }
                if (!Boolean.TRUE.equals(media.isIgnored()) && ignoredPhotoAlbums.contains(photoSet.getId())) {
                    save = MediaService.ignoreMedia(media, "Photoset ignored: " + photoSet.getTitle());
                }
            }
        }
        media = saveMediaAndPhotosetsIfNeeded(media, save, savePhotoSets, isPresentInDb);
        savePhotoSets = false;
        save = false;
        if (mediaService.updateMedia(media, originalRepo, false).getResult()) {
            save = true;
        }
        if (customProcessor.test(media)) {
            save = true;
        }
        int uploadCount = 0;
        if (shouldUploadAuto.test(media, false)) {
            Triple<FlickrMedia, Collection<Metadata>, Integer> upload = uploader.apply(media);
            media = upload.getLeft();
            uploadCount = upload.getRight();
            save = true;
        }
        return Pair.of(saveMediaAndPhotosetsIfNeeded(media, save, savePhotoSets, isPresentInDb), uploadCount);
    }

    private FlickrMedia saveMediaAndPhotosetsIfNeeded(FlickrMedia media, boolean save, boolean savePhotoSets,
            boolean wasPresent) {
        if (save) {
            if (wasPresent) {
                LOGGER.debug("Saving existing media {}", media);
            }
            media = flickrRepository.save(media);
        }
        if (savePhotoSets) {
            if (wasPresent) {
                LOGGER.debug("Saving photosets for existing media {}", media);
            }
            media.getPhotosets().forEach(flickrPhotoSetRepository::save);
        }
        return media;
    }

    private boolean handleBadVideo(FlickrMedia media) throws MalformedURLException {
        URL videoUrl = getVideoUrl(media);
        media.setOriginalUrl(videoUrl);
        media.setCommonsFileNames(null);
        media.getMetadata().setSha1(null);
        media.getMetadata().setAssetUrl(videoUrl);
        return true;
    }

    private Set<FlickrPhotoSet> getPhotoSets(FlickrMedia media, String flickrAccount) throws FlickrException {
        return flickrService.findPhotoSets(media.getId().toString()).stream()
                .map(ps -> flickrPhotoSetRepository.findById(Long.valueOf(ps.getId()))
                        .orElseGet(() -> {
                            FlickrPhotoSet set = dozerMapper.map(ps, FlickrPhotoSet.class);
                            set.setPathAlias(flickrAccount);
                            return flickrPhotoSetRepository.save(set);
                        }))
                .collect(toSet());
    }

    private boolean handleLicenseChange(FlickrMedia media, String flickrAccount, FlickrMedia mediaInRepo) {
        LOGGER.warn("Flickr license has changed for picture {} of {} from {} to {}",
                media.getId(), flickrAccount, mediaInRepo.getLicense(), media.getLicense());
        try {
            if (FlickrFreeLicense.of(media.getLicense()) != null && mediaInRepo.isIgnored()
                    && mediaInRepo.getIgnoredReason() != null
                    && mediaInRepo.getIgnoredReason().endsWith("is no longer free!")) {
                LOGGER.info("Flickr license for picture {} of {} is free again!", media.getId(), flickrAccount);
                mediaInRepo.setIgnored(Boolean.FALSE);
                mediaInRepo.setIgnoredReason(null);
            }
        } catch (IllegalArgumentException e) {
            String message = String.format("Flickr license for picture %d of %s is no longer free!",
                    media.getId(), flickrAccount);
            mediaInRepo.setIgnored(Boolean.TRUE);
            mediaInRepo.setIgnoredReason(message);
            LOGGER.warn(message);
        }
        mediaInRepo.setLicense(media.getLicense());
        return true;
    }

    @Transactional
    public void deleteFlickrMedia(String id) {
        flickrRepository.findById(Long.valueOf(id)).ifPresent(media -> {
            media.getPhotosets().forEach(ps -> {
                if (ps.getMembers().remove(media)) {
                    flickrPhotoSetRepository.save(ps);
                }
            });
            media.getPhotosets().clear();
            flickrRepository.delete(flickrRepository.save(media));
        });
    }
}
