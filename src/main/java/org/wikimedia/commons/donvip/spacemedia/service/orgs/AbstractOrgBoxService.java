package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import static java.util.stream.Collectors.toSet;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.newURL;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.box.BoxMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.box.BoxMediaId;
import org.wikimedia.commons.donvip.spacemedia.data.domain.box.BoxMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.exception.UploadException;
import org.wikimedia.commons.donvip.spacemedia.service.UrlResolver;
import org.wikimedia.commons.donvip.spacemedia.service.box.BoxService;

import com.box.sdk.BoxFile;

/**
 * Service fetching images from box.com
 */
public abstract class AbstractOrgBoxService
        extends AbstractOrgService<BoxMedia, BoxMediaId, ZonedDateTime> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractOrgBoxService.class);

    @Autowired
    private BoxMediaRepository mediaRepository;

    @Autowired
    private BoxService boxService;

    private final Set<String> appShares;

    protected AbstractOrgBoxService(BoxMediaRepository repository, String id, Set<String> appShares) {
        super(repository, id);
        this.appShares = appShares;
    }

    @Override
    protected Class<BoxMedia> getMediaClass() {
        return BoxMedia.class;
    }

    @Override
    protected Class<BoxMedia> getTopTermsMediaClass() {
        return BoxMedia.class;
    }

    @Override
    protected final BoxMediaId getMediaId(String id) {
        return new BoxMediaId(id);
    }

    @Override
    protected UrlResolver<BoxMedia> getUrlResolver() {
        return (media, metadata) -> boxService.getSharedFile(getSharedLink(media.getId()), media.getId().getId())
                .getDownloadURL();
    }

    private String getSharedLink(BoxMediaId id) {
        return boxService.getSharedLink(id.getApp(), id.getShare());
    }

    @Override
    public void updateMedia() {
        LocalDateTime start = startUpdateMedia();
        List<BoxMedia> uploadedMedia = new ArrayList<>();
        int count = 0;
        for (String appShare : appShares) {
            String[] as = appShare.split(":");
            Pair<Integer, Collection<BoxMedia>> update = updateBoxMedia(as[0], as[1]);
            uploadedMedia.addAll(update.getRight());
            count += update.getLeft();
            ongoingUpdateMedia(start, count);
        }
        endUpdateMedia(count, uploadedMedia, start);
    }

    private Pair<Integer, Collection<BoxMedia>> updateBoxMedia(String app, String share) {
        List<BoxMedia> uploadedMedia = new ArrayList<>();
        int count = 0;
        LocalDateTime start = LocalDateTime.now();

        List<BoxMedia> files = boxService.getFiles(app, share, fileInfo -> toBoxMedia(app, share, fileInfo),
                Comparator.comparing(BoxMedia::getDate));
        LOGGER.info("Found {} files in {}", files.size(), Duration.between(start, LocalDateTime.now()));

        for (BoxMedia media : files) {
            try {
                Pair<BoxMedia, Integer> result = processBoxMedia(media);
                if (result.getValue() > 0) {
                    uploadedMedia.add(result.getKey());
                }
                ongoingUpdateMedia(start, share, count++);
            } catch (UploadException | IOException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }

        return Pair.of(count, uploadedMedia);
    }

    private BoxMedia toBoxMedia(String app, String share, BoxFile.Info fileInfo) {
        BoxMedia media = new BoxMedia(app, share, Long.parseLong(fileInfo.getID()));
        Optional.ofNullable(fileInfo.getContentCreatedAt()).map(AbstractOrgBoxService::toZonedDateTime)
                .ifPresent(media::setContentCreationDate);
        media.setDate(toZonedDateTime(fileInfo.getCreatedAt()));
        media.setTitle(fileInfo.getName());
        media.setDescription(fileInfo.getDescription());
        media.setCreator(fileInfo.getCreatedBy().getName());
        media.setThumbnailUrl(
                newURL(boxService.getThumbnailUrl(app, Long.parseLong(fileInfo.getVersion().getVersionID()), share)));
        addMetadata(media, boxService.getSharedLink(app, share, fileInfo), m -> fillMetadata(m, fileInfo));
        return media;
    }

    private static FileMetadata fillMetadata(FileMetadata m, BoxFile.Info fileInfo) {
        m.setExtension(fileInfo.getExtension());
        m.setSha1(fileInfo.getSha1());
        m.setSize(fileInfo.getSize());
        return m;
    }

    private static ZonedDateTime toZonedDateTime(Date date) {
        return date.toInstant().atZone(ZoneOffset.ofHours(-4));
    }

    private Pair<BoxMedia, Integer> processBoxMedia(BoxMedia mediaFromApi) throws IOException, UploadException {
        BoxMedia media = null;
        boolean save = false;
        Optional<BoxMedia> mediaInDb = mediaRepository.findById(mediaFromApi.getId());
        if (mediaInDb.isPresent()) {
            media = mediaInDb.get();
        } else {
            media = mediaFromApi;
            save = true;
        }
        save |= doCommonUpdate(media);
        int uploadCount = 0;
        if (shouldUploadAuto(media, false)) {
            Triple<BoxMedia, Collection<FileMetadata>, Integer> upload = upload(media, true, false);
            uploadCount = upload.getRight();
            media = upload.getLeft();
            save = true;
        }
        if (save) {
            saveMedia(media);
        }
        return Pair.of(media, uploadCount);
    }

    @Override
    protected final BoxMedia refresh(BoxMedia media) throws IOException {
        return media.copyDataFrom(media); // FIXME
    }

    @Override
    public final URL getSourceUrl(BoxMedia media, FileMetadata metadata) {
        return metadata.getAssetUrl();
    }

    @Override
    protected final String getAuthor(BoxMedia media) throws MalformedURLException {
        return boxService.getSharedItem(getSharedLink(media.getId())).getCreatedBy().getName();
    }

    @Override
    protected final Optional<Temporal> getCreationDate(BoxMedia media) {
        return Optional.ofNullable(media.getContentCreationDate());
    }

    @Override
    protected final Optional<Temporal> getUploadDate(BoxMedia media) {
        return Optional.of(media.getDate());
    }

    private static Set<String> shares(Set<String> appShares) {
        return appShares.stream().map(as -> as.split(":")[1]).collect(toSet());
    }

    @Override
    public long countAllMedia() {
        return mediaRepository.count(shares(appShares));
    }

    @Override
    public long countIgnored() {
        return mediaRepository.countByIgnoredTrue(shares(appShares));
    }

    @Override
    public long countMissingMedia() {
        return mediaRepository.countMissingInCommonsByShare(shares(appShares));
    }

    @Override
    public long countMissingImages() {
        return mediaRepository.countMissingImagesInCommons(shares(appShares));
    }

    @Override
    public long countMissingVideos() {
        return mediaRepository.countMissingVideosInCommons(shares(appShares));
    }

    @Override
    public long countPerceptualHashes() {
        return mediaRepository.countByMetadata_PhashNotNull(shares(appShares));
    }

    @Override
    public long countUploadedMedia() {
        return mediaRepository.countUploadedToCommons(shares(appShares));
    }

    @Override
    public Iterable<BoxMedia> listAllMedia() {
        return mediaRepository.findAll(shares(appShares));
    }

    @Override
    public Page<BoxMedia> listAllMedia(Pageable page) {
        return mediaRepository.findAll(shares(appShares), page);
    }

    @Override
    public List<BoxMedia> listMissingMedia() {
        return mediaRepository.findMissingInCommonsByShare(shares(appShares));
    }

    @Override
    public Page<BoxMedia> listMissingMedia(Pageable page) {
        return mediaRepository.findMissingInCommonsByShare(shares(appShares), page);
    }

    @Override
    public Page<BoxMedia> listMissingImages(Pageable page) {
        return mediaRepository.findMissingImagesInCommons(shares(appShares), page);
    }

    @Override
    public Page<BoxMedia> listMissingVideos(Pageable page) {
        return mediaRepository.findMissingVideosInCommons(shares(appShares), page);
    }

    @Override
    public Page<BoxMedia> listHashedMedia(Pageable page) {
        return mediaRepository.findByMetadata_PhashNotNull(shares(appShares), page);
    }

    @Override
    public List<BoxMedia> listUploadedMedia() {
        return mediaRepository.findUploadedToCommons(shares(appShares));
    }

    @Override
    public Page<BoxMedia> listUploadedMedia(Pageable page) {
        return mediaRepository.findUploadedToCommons(shares(appShares), page);
    }

    @Override
    public List<BoxMedia> listDuplicateMedia() {
        return mediaRepository.findDuplicateInCommons(shares(appShares));
    }

    @Override
    public List<BoxMedia> listIgnoredMedia() {
        return mediaRepository.findByIgnoredTrue(shares(appShares));
    }

    @Override
    public Page<BoxMedia> listIgnoredMedia(Pageable page) {
        return mediaRepository.findByIgnoredTrue(shares(appShares), page);
    }
}
