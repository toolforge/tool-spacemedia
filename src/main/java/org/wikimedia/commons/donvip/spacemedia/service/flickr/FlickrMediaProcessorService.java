package org.wikimedia.commons.donvip.spacemedia.service.flickr;

import static java.util.stream.Collectors.toSet;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.newURL;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Supplier;

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
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrFreeLicense;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrMediaType;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrPhotoSet;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrPhotoSetRepository;
import org.wikimedia.commons.donvip.spacemedia.service.MediaService;
import org.wikimedia.commons.donvip.spacemedia.service.UrlResolver;
import org.wikimedia.commons.donvip.spacemedia.utils.CsvHelper;
import org.wikimedia.commons.donvip.spacemedia.utils.UnitedStates;

import com.flickr4java.flickr.FlickrException;
import com.flickr4java.flickr.photos.PhotoSet;

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
    protected MediaService mediaService;

    @Value("${flickr.video.download.url}")
    private String flickrVideoDownloadUrl;
    @Value("${videos.enabled}")
    private boolean videosEnabled;

    private Set<Long> ignoredPhotoAlbums;

    @PostConstruct
    public void init() throws IOException {
        ignoredPhotoAlbums = CsvHelper.loadSet(getClass().getResource("/blocklist.ignored.flickr.albums.csv")).stream()
                .mapToLong(Long::parseLong).boxed().collect(toSet());
    }

    public URL getVideoUrl(FlickrMedia media) {
        return newURL(flickrVideoDownloadUrl.replace("<id>", media.getId().toString()));
    }

    public boolean isBadVideoEntry(FlickrMedia media) throws URISyntaxException {
        return FlickrMediaType.video == media.getMedia()
                && !getVideoUrl(media).toURI().equals(media.getMetadata().get(0).getAssetUrl().toURI());
    }

    @Transactional
    public Pair<FlickrMedia, Integer> processFlickrMedia(FlickrMedia media, String flickrAccount,
            Supplier<Collection<String>> stringsToRemove, BiPredicate<FlickrMedia, Boolean> shouldUploadAuto,
            Function<FlickrMedia, Triple<FlickrMedia, Collection<FileMetadata>, Integer>> uploader,
            UrlResolver<FlickrMedia> urlResolver) throws IOException {
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
        if ((!isPresentInDb || isEmpty(media.getAllCommonsFileNames())) && media.getPhotosets() != null) {
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
        save = mediaService.updateMedia(media, stringsToRemove.get(), false, urlResolver).getResult();
        int uploadCount = 0;
        if (shouldUploadAuto.test(media, false) && (videosEnabled || !media.isVideo())) {
            Triple<FlickrMedia, Collection<FileMetadata>, Integer> upload = uploader.apply(media);
            media = upload.getLeft();
            uploadCount = upload.getRight();
            save = true;
        }
        return Pair.of(saveMediaAndPhotosetsIfNeeded(media, save, savePhotoSets, isPresentInDb), uploadCount);
    }

    private FlickrMedia saveMediaAndPhotosetsIfNeeded(FlickrMedia media, boolean save, boolean savePhotoSets,
            boolean wasPresent) {
        if (save) {
            LOGGER.info("Saving {}", media);
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

    private boolean handleBadVideo(FlickrMedia media) {
        FileMetadata metadata = media.getUniqueMetadata();
        metadata.setCommonsFileNames(null);
        metadata.setSha1(null);
        metadata.setAssetUrl(getVideoUrl(media));
        return true;
    }

    private Set<FlickrPhotoSet> getPhotoSets(FlickrMedia media, String flickrAccount) throws FlickrException {
        return flickrService.findPhotoSets(media.getId().toString()).stream()
                .map(ps -> flickrPhotoSetRepository.findById(Long.valueOf(ps.getId()))
                        .orElseGet(() -> flickrPhotoSetRepository.save(mapPhotoSet(ps, flickrAccount))))
                .collect(toSet());
    }

    private static FlickrPhotoSet mapPhotoSet(PhotoSet ps, String flickrAccount) {
        FlickrPhotoSet set = new FlickrPhotoSet();
        set.setId(Long.parseLong(ps.getId()));
        set.setTitle(ps.getTitle());
        set.setPathAlias(flickrAccount);
        return set;
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
            LOGGER.warn("Deleteing {}", media);
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
