package org.wikimedia.commons.donvip.spacemedia.service.flickr;

import static java.util.stream.Collectors.toSet;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.wikimedia.commons.donvip.spacemedia.service.MediaService.ignoreMetadata;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.newURL;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.function.TriFunction;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.CompositeMediaId;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.Media;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrLicense;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrPhotoSet;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrPhotoSetRepository;
import org.wikimedia.commons.donvip.spacemedia.service.MediaService;
import org.wikimedia.commons.donvip.spacemedia.service.MediaService.MediaUpdateContext;
import org.wikimedia.commons.donvip.spacemedia.service.UrlResolver;
import org.wikimedia.commons.donvip.spacemedia.service.wikimedia.GlitchTip;
import org.wikimedia.commons.donvip.spacemedia.utils.CsvHelper;
import org.wikimedia.commons.donvip.spacemedia.utils.UnitedStates;

import com.flickr4java.flickr.FlickrException;
import com.flickr4java.flickr.photos.PhotoSet;

@Lazy
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
        ignoredPhotoAlbums = CsvHelper.loadSet(getClass().getResource("/lists/blocklist.ignored.flickr.albums.csv")).stream()
                .mapToLong(Long::parseLong).boxed().collect(toSet());
    }

    public URL getVideoUrl(FlickrMedia media) {
        return getVideoUrl(media.getIdUsedInOrg());
    }

    public URL getVideoUrl(String id) {
        return newURL(flickrVideoDownloadUrl.replace("<id>", id));
    }

    @Transactional
    public Pair<FlickrMedia, Integer> processFlickrMedia(FlickrMedia media, String flickrAccount,
            Supplier<Collection<Pattern>> patternsToRemove, Supplier<Collection<String>> stringsToRemove,
            BiPredicate<FlickrMedia, Boolean> shouldUploadAuto,
            Function<FlickrMedia, Triple<FlickrMedia, Collection<FileMetadata>, Integer>> uploader,
            UrlResolver<FlickrMedia> urlResolver,
            TriFunction<FlickrMedia, LocalDate, Integer, List<? extends Media>> similarCandidateMedia,
            boolean checkAllowlist, boolean checkBlocklist,
            UnaryOperator<FlickrMedia> saver, List<IgnoreCriteria> ignoreCriterias) throws IOException {
        boolean save = false;
        boolean savePhotoSets = false;
        final Optional<FlickrMedia> optMediaInRepo = flickrRepository.findById(media.getId());
        final boolean isPresentInDb = optMediaInRepo.isPresent();
        boolean saveMetadata = false;
        if (isPresentInDb) {
            FlickrMedia mediaInRepo = optMediaInRepo.get();
            if (mediaInRepo.getLicense() != media.getLicense()) {
                save = handleLicenseChange(media, flickrAccount, mediaInRepo);
            }
            media = mediaInRepo;
        } else {
            save = true;
            try {
                FlickrLicense license = FlickrLicense.of(media.getLicense());
                if (license == FlickrLicense.Public_Domain_Mark && !media.isIgnored()
                        && !UnitedStates.isClearPublicDomain(media.getDescription())) {
                    for (FileMetadata metadata : media.getMetadata()) {
                        saveMetadata = ignoreMetadata(metadata, "Public Domain Mark is not a legal license");
                    }
                } else if (!license.isFree()) {
                    LOGGER.debug("Non-free Flickr licence for media {}: {}", media, license);
                }
            } catch (IllegalArgumentException e) {
                LOGGER.debug("Unknown Flickr licence for media {}: {}", media, e.getMessage());
            }
        }
        if (isEmpty(media.getPhotosets())) {
            try {
                Set<FlickrPhotoSet> sets = getPhotoSets(media, flickrAccount);
                if (isNotEmpty(sets)) {
                    sets.forEach(media::addPhotoSet);
                    LOGGER.info("Saving media {} to add photosets {}", media, sets);
                    savePhotoSets = true;
                    save = true;
                }
            } catch (FlickrException e) {
                LOGGER.error("Failed to retrieve photosets of image {}: {}", media.getId(), e.getMessage());
                GlitchTip.capture(e);
            }
        }
        if ((!isPresentInDb || isEmpty(media.getAllCommonsFileNames())) && media.getPhotosets() != null) {
            for (FlickrPhotoSet photoSet : media.getPhotosets()) {
                if (StringUtils.isBlank(photoSet.getPathAlias())) {
                    photoSet.setPathAlias(flickrAccount);
                    savePhotoSets = true;
                }
            }
        }
        saveMetadata |= checkIgnoredCriteria(media, ignoreCriterias);
        if (saveMetadata) {
            for (FileMetadata metadata : media.getMetadata()) {
                mediaService.saveMetadata(metadata);
            }
        }
        media = saveMediaAndPhotosetsIfNeeded(media, save, savePhotoSets, isPresentInDb, saver);
        savePhotoSets = false;
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            save = mediaService
                    .updateMedia(new MediaUpdateContext<>(media, null, urlResolver, httpClient, null, false, false),
                            patternsToRemove.get(), stringsToRemove.get(), similarCandidateMedia, checkAllowlist, checkBlocklist)
                    .result();
        }
        int uploadCount = 0;
        if (shouldUploadAuto.test(media, false) && (videosEnabled || !media.isVideo())) {
            Triple<FlickrMedia, Collection<FileMetadata>, Integer> upload = uploader.apply(media);
            media = upload.getLeft();
            uploadCount = upload.getRight();
            save = true;
        }
        return Pair.of(saveMediaAndPhotosetsIfNeeded(media, save, savePhotoSets, isPresentInDb, saver), uploadCount);
    }

    public boolean checkIgnoredCriteria(FlickrMedia media, List<IgnoreCriteria> ignoreCriterias) {
        boolean result = false;
        for (FileMetadata fm : media.getMetadata()) {
            if (Boolean.TRUE != fm.isIgnored()) {
                for (IgnoreCriteria c : ignoreCriterias) {
                    if (c.match(media)) {
                        result |= ignoreMetadata(fm, "Ignored criteria: " + c);
                    }
                }
                if (!result && media.getPhotosets() != null) {
                    for (FlickrPhotoSet photoSet : media.getPhotosets()) {
                        if (ignoredPhotoAlbums.contains(photoSet.getId())) {
                            result |= ignoreMetadata(fm, "Photoset ignored: " + photoSet.getTitle());
                            break;
                        }
                    }
                }
            }
        }
        return result;
    }

    private FlickrMedia saveMediaAndPhotosetsIfNeeded(FlickrMedia media, boolean save, boolean savePhotoSets,
            boolean wasPresent, UnaryOperator<FlickrMedia> saver) {
        if (save) {
            LOGGER.info("Saving {}", media);
            media = saver.apply(media);
        }
        if (savePhotoSets) {
            if (wasPresent) {
                LOGGER.debug("Saving photosets for existing media {}", media);
            }
            media.getPhotosets().forEach(flickrPhotoSetRepository::save);
        }
        return media;
    }

    private Set<FlickrPhotoSet> getPhotoSets(FlickrMedia media, String flickrAccount) throws FlickrException {
        return flickrService.findPhotoSets(media.getId().getMediaId()).stream()
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
            FlickrLicense license = FlickrLicense.of(media.getLicense());
            if (!license.isFree() && !mediaInRepo.isIgnored()) {
                String message = String.format("Flickr license for picture %s of %s is no longer free!", media.getId(),
                        flickrAccount);
                mediaService.ignoreMedia(mediaInRepo, message);
                LOGGER.warn(message);
            } else if (license.isFree()) {
                LOGGER.info("Flickr license for picture {} of {} is free again!", media.getId(), flickrAccount);
                for (FileMetadata metadata : media.getMetadata()) {
                    if (Boolean.TRUE == metadata.isIgnored() && metadata.getIgnoredReason() != null
                            && metadata.getIgnoredReason().endsWith("is no longer free!")) {
                        metadata.setIgnored(Boolean.FALSE);
                        metadata.setIgnoredReason(null);
                        mediaService.saveMetadata(metadata);
                    }
                }
            }
        } catch (IllegalArgumentException e) {
            String message = String.format("Flickr license for picture %s of %s is unknown!",
                    media.getId(), flickrAccount);
            mediaService.ignoreMedia(mediaInRepo, message);
            LOGGER.warn(message);
        }
        mediaInRepo.setLicense(media.getLicense());
        return true;
    }

    @Transactional
    public void deleteFlickrMedia(CompositeMediaId id) {
        flickrRepository.findById(id).ifPresent(media -> {
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
