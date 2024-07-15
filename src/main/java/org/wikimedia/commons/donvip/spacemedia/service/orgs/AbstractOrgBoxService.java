package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.newURL;

import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
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
import org.springframework.context.annotation.Lazy;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.CompositeMediaId;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.box.BoxMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.box.BoxMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.exception.UploadException;
import org.wikimedia.commons.donvip.spacemedia.service.UrlResolver;
import org.wikimedia.commons.donvip.spacemedia.service.box.BoxService;
import org.wikimedia.commons.donvip.spacemedia.service.wikimedia.GlitchTip;

import com.box.sdk.BoxAPIResponseException;
import com.box.sdk.BoxFile;

/**
 * Service fetching images from box.com
 */
public abstract class AbstractOrgBoxService extends AbstractOrgService<BoxMedia> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractOrgBoxService.class);

    @Autowired
    private BoxMediaRepository mediaRepository;

    @Lazy
    @Autowired
    private BoxService boxService;

    protected AbstractOrgBoxService(BoxMediaRepository repository, String id, Set<String> appShares) {
        super(repository, id, appShares);
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
    protected UrlResolver<BoxMedia> getUrlResolver() {
        return (media, metadata) -> {
            try {
                return boxService.getSharedFile(getSharedLink(media.getId()), media.getId().getMediaId())
                        .getDownloadURL();
            } catch (BoxAPIResponseException e) {
                if (e.getResponseCode() == 404) {
                    LOGGER.warn("Deleting image as no longer found: {}", e.getMessage());
                    deleteMedia(media, e);
                }
                throw e;
            }
        };
    }

    private String getSharedLink(CompositeMediaId id) {
        return boxService.getSharedLink(getApp(id), getShare(id));
    }

    protected static final String getApp(CompositeMediaId id) {
        return id.getRepoId().split("/")[0];
    }

    protected static final String getShare(CompositeMediaId id) {
        return id.getRepoId().split("/")[1];
    }

    @Override
    public void updateMedia(String[] args) {
        LocalDateTime start = startUpdateMedia();
        List<BoxMedia> uploadedMedia = new ArrayList<>();
        int count = 0;
        for (String appShare : getRepoIdsFromArgs(args)) {
            String[] as = appShare.split("/");
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
                Comparator.comparing(BoxMedia::getPublicationDateTime));
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
                GlitchTip.capture(e);
            }
        }

        return Pair.of(count, uploadedMedia);
    }

    private BoxMedia toBoxMedia(String app, String share, BoxFile.Info fileInfo) {
        BoxMedia media = new BoxMedia(app, share, Long.parseLong(fileInfo.getID()));
        Optional.ofNullable(fileInfo.getContentCreatedAt()).map(AbstractOrgBoxService::toZonedDateTime)
                .ifPresent(media::setCreationDateTime);
        media.setPublicationDateTime(toZonedDateTime(fileInfo.getCreatedAt()));
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
        m.setOriginalFileName(fileInfo.getName());
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
    public final URL getSourceUrl(BoxMedia media, FileMetadata metadata, String ext) {
        return metadata.getAssetUrl();
    }

    @Override
    protected final String getAuthor(BoxMedia media, FileMetadata metadata) {
        return boxService.getSharedItem(getSharedLink(media.getId())).getCreatedBy().getName();
    }
}
