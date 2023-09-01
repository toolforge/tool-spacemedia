package org.wikimedia.commons.donvip.spacemedia.service.dvids;

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javax.transaction.Transactional;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.dvids.DvidsAudio;
import org.wikimedia.commons.donvip.spacemedia.data.domain.dvids.DvidsAudioRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.dvids.DvidsCreditRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.dvids.DvidsGraphic;
import org.wikimedia.commons.donvip.spacemedia.data.domain.dvids.DvidsGraphicRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.dvids.DvidsImage;
import org.wikimedia.commons.donvip.spacemedia.data.domain.dvids.DvidsImageRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.dvids.DvidsMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.dvids.DvidsNews;
import org.wikimedia.commons.donvip.spacemedia.data.domain.dvids.DvidsNewsRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.dvids.DvidsPublication;
import org.wikimedia.commons.donvip.spacemedia.data.domain.dvids.DvidsPublicationRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.dvids.DvidsVideo;
import org.wikimedia.commons.donvip.spacemedia.data.domain.dvids.DvidsVideoRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.dvids.DvidsWebcast;
import org.wikimedia.commons.donvip.spacemedia.data.domain.dvids.DvidsWebcastRepository;

@Service
public class DvidsMediaProcessorService {

    private static final String OLD_DVIDS_CDN = "https://cdn.dvidshub.net/";

    private static final Set<Class<? extends DvidsMedia>> MEDIA_WITH_CATEGORIES = Set.of(DvidsVideo.class,
            DvidsNews.class, DvidsAudio.class);

    @Autowired
    private DvidsAudioRepository audioRepository;

    @Autowired
    private DvidsImageRepository imageRepository;

    @Autowired
    private DvidsGraphicRepository graphicRepository;

    @Autowired
    private DvidsVideoRepository videoRepository;

    @Autowired
    private DvidsNewsRepository newsRepository;

    @Autowired
    private DvidsPublicationRepository publiRepository;

    @Autowired
    private DvidsWebcastRepository webcastRepository;

    @Autowired
    private DvidsCreditRepository creditRepository;

    @Transactional
    public Pair<DvidsMedia, Integer> processDvidsMedia(Supplier<Optional<DvidsMedia>> dbFetcher,
            Supplier<DvidsMedia> apiFetcher, Predicate<DvidsMedia> processDvidsMediaUpdater,
            BiPredicate<DvidsMedia, Boolean> shouldUploadAuto,
            Function<DvidsMedia, Triple<DvidsMedia, Collection<FileMetadata>, Integer>> uploader) {
        DvidsMedia media = null;
        boolean save = false;
        Optional<DvidsMedia> mediaInDb = dbFetcher.get();
        if (mediaInDb.isPresent()) {
            media = mediaInDb.get();
            save = updateCategoryAndCdnUrls(media, apiFetcher);
        } else {
            media = apiFetcher.get();
            save = true;
        }
        save |= processDvidsMediaUpdater.test(media);
        int uploadCount = 0;
        if (shouldUploadAuto.test(media, false)) {
            Triple<DvidsMedia, Collection<FileMetadata>, Integer> upload = uploader.apply(media);
            uploadCount = upload.getRight();
            media = upload.getLeft();
            save = true;
        }
        if (save) {
            if (isNotEmpty(media.getCredit())) {
                creditRepository.saveAll(media.getCredit());
            }
            save(media);
        }
        return Pair.of(media, uploadCount);
    }

    protected boolean updateCategoryAndCdnUrls(DvidsMedia media, Supplier<DvidsMedia> apiFetcher) {
        // DVIDS changed its CDN around 2021/2022. Example:
        // old: https://cdn.dvidshub.net/media/photos/2104/6622429.jpg
        // new: https://d34w7g4gy10iej.cloudfront.net/photos/2104/6622429.jpg
        if ((MEDIA_WITH_CATEGORIES.contains(media.getClass()) && media.getCategory() == null)
                || media.getUniqueMetadata().getAssetUrl().toExternalForm().startsWith(OLD_DVIDS_CDN)
                || media.getThumbnailUrl().toExternalForm().startsWith(OLD_DVIDS_CDN)
                || (media instanceof DvidsVideo videoMedia
                        && videoMedia.getImage().toExternalForm().startsWith(OLD_DVIDS_CDN))) {
            DvidsMedia mediaFromApi = apiFetcher.get();
            media.setCategory(mediaFromApi.getCategory());
            media.getUniqueMetadata().setAssetUrl(mediaFromApi.getUniqueMetadata().getAssetUrl());
            media.setThumbnailUrl(mediaFromApi.getThumbnailUrl());
            if (media instanceof DvidsVideo videoMedia && mediaFromApi instanceof DvidsVideo videoMediaFromApi) {
                videoMedia.setImage(videoMediaFromApi.getImage());
            }
            return true;
        }
        return false;
    }

    private DvidsMedia save(DvidsMedia media) {
        switch (media.getMediaType()) {
        case image:
            return imageRepository.save((DvidsImage) media);
        case graphic:
            return graphicRepository.save((DvidsGraphic) media);
        case video:
            return videoRepository.save((DvidsVideo) media);
        case audio:
            return audioRepository.save((DvidsAudio) media);
        case news:
            return newsRepository.save((DvidsNews) media);
        case publication_issue:
            return publiRepository.save((DvidsPublication) media);
        case webcast:
            return webcastRepository.save((DvidsWebcast) media);
        }
        throw new IllegalArgumentException(media.toString());
    }
}
