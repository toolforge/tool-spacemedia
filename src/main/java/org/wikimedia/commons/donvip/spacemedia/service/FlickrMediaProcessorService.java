package org.wikimedia.commons.donvip.spacemedia.service;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrFreeLicense;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrPhotoSet;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrPhotoSetRepository;

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
		return "video".equals(media.getMedia()) && !getVideoUrl(media).equals(media.getAssetUrl());
	}

	@Transactional
	public FlickrMedia processFlickrMedia(FlickrMedia media, String flickrAccount)
			throws IOException, URISyntaxException {
		boolean save = false;
        boolean savePhotoSets = false;
		Optional<FlickrMedia> optMediaInRepo = flickrRepository.findById(media.getId());
		if (optMediaInRepo.isPresent()) {
            FlickrMedia mediaInRepo = optMediaInRepo.get();
            if (mediaInRepo.getLicense() != media.getLicense()) {
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
                save = true;
            }
            media = mediaInRepo;
		} else {
			save = true;
		}
		if (CollectionUtils.isEmpty(media.getPhotosets())) {
			try {
				Set<FlickrPhotoSet> sets = flickrService.findPhotoSets(media.getId().toString()).stream()
						.map(ps -> flickrPhotoSetRepository.findById(Long.valueOf(ps.getId()))
                                .orElseGet(() -> {
                                    FlickrPhotoSet set = dozerMapper.map(ps, FlickrPhotoSet.class);
                                    set.setPathAlias(flickrAccount);
                                    return flickrPhotoSetRepository.save(set);
                                }))
						.collect(Collectors.toSet());
				if (CollectionUtils.isNotEmpty(sets)) {
					sets.forEach(media::addPhotoSet);
                    savePhotoSets = true;
					save = true;
				}
			} catch (FlickrException e) {
				LOGGER.error("Failed to retrieve photosets of image " + media.getId(), e);
			}
		}
		if (isBadVideoEntry(media)) {
			media.setAssetUrl(getVideoUrl(media));
			media.setCommonsFileNames(null);
			media.setSha1(null);
			save = true;
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
		if (mediaService.updateMedia(media)) {
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
}
