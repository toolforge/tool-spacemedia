package org.wikimedia.commons.donvip.spacemedia.service;

import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.wikimedia.commons.donvip.spacemedia.data.domain.Media;
import org.wikimedia.commons.donvip.spacemedia.data.domain.MediaRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.UploadMode;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrFreeLicense;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrMediaType;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrPhotoSet;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrPhotoSetRepository;
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

    public URL getVideoUrl(FlickrMedia media) throws MalformedURLException {
        return new URL(flickrVideoDownloadUrl.replace("<id>", media.getId().toString()));
    }

    public boolean isBadVideoEntry(FlickrMedia media) throws MalformedURLException {
        return FlickrMediaType.video == media.getMedia()
                && !getVideoUrl(media).equals(media.getMetadata().getAssetUrl());
    }

    @Transactional
    public FlickrMedia processFlickrMedia(FlickrMedia media, String flickrAccount,
            MediaRepository<? extends Media<?, ?>, ?, ?> originalRepo,
            Predicate<FlickrMedia> customProcessor, UploadMode uploadMode,
            Function<FlickrMedia, FlickrMedia> uploader)
            throws IOException {
        boolean save = false;
        boolean savePhotoSets = false;
        Optional<FlickrMedia> optMediaInRepo = flickrRepository.findById(media.getId());
        if (optMediaInRepo.isPresent()) {
            FlickrMedia mediaInRepo = optMediaInRepo.get();
            if (mediaInRepo.getLicense() != media.getLicense()) {
                save = handleLicenseChange(media, flickrAccount, mediaInRepo);
            }
            media = mediaInRepo;
        } else {
            save = true;
        }
        if (isEmpty(media.getPhotosets())) {
            try {
                Set<FlickrPhotoSet> sets = getPhotoSets(media, flickrAccount);
                if (isNotEmpty(sets)) {
                    sets.forEach(media::addPhotoSet);
                    savePhotoSets = true;
                    save = true;
                }
            } catch (FlickrException e) {
                LOGGER.error("Failed to retrieve photosets of image " + media.getId(), e);
            }
        }
        if (isBadVideoEntry(media)) {
            save = handleBadVideo(media);
        }
        if (StringUtils.isEmpty(media.getPathAlias())) {
            media.setPathAlias(flickrAccount);
            save = true;
        }
        if (media.getPhotosets() != null) {
            for (FlickrPhotoSet photoSet : media.getPhotosets()) {
                if (StringUtils.isBlank(photoSet.getPathAlias())) {
                    photoSet.setPathAlias(flickrAccount);
                    savePhotoSets = true;
                }
            }
        }
        try {
            if (FlickrFreeLicense.of(media.getLicense()) == FlickrFreeLicense.Public_Domain_Mark
                    && !Boolean.TRUE.equals(media.isIgnored())
                    && !UnitedStates.isClearPublicDomain(media.getDescription())) {
                media.setIgnored(true);
                media.setIgnoredReason("Public Domain Mark is not a legal license");
                save = true;
            }
        } catch (IllegalArgumentException e) {
            LOGGER.debug("Non-free Flickr licence for media {}: {}", media, e.getMessage());
        }
        if (mediaService.updateMedia(media, originalRepo)) {
            save = true;
        }
        if (customProcessor.test(media)) {
            save = true;
        }
        if (uploadMode == UploadMode.AUTO && !Boolean.TRUE.equals(media.isIgnored())
                && isEmpty(media.getCommonsFileNames())) {
            media = uploader.apply(media);
            save = true;
        }
        if (save) {
            media = flickrRepository.save(media);
        }
        if (savePhotoSets) {
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
                .collect(Collectors.toSet());
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
}
