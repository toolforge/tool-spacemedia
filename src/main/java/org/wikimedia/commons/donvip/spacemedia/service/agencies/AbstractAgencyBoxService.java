package org.wikimedia.commons.donvip.spacemedia.service.agencies;

import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.newURL;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
public abstract class AbstractAgencyBoxService
        extends AbstractAgencyService<BoxMedia, BoxMediaId, ZonedDateTime> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractAgencyBoxService.class);

    @Autowired
    private BoxMediaRepository mediaRepository;

    @Autowired
    private BoxService boxService;

    private final Set<String> appShares;

    protected AbstractAgencyBoxService(BoxMediaRepository repository, String id, Set<String> appShares) {
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
        return (media, metadata) -> boxService
                .getSharedFile(boxService.getSharedLink(media.getId().getApp(), media.getId().getShare()),
                        media.getId().getId())
                .getDownloadURL();
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

        for (BoxMedia media : boxService.getFiles(app, share, fileInfo -> toBoxMedia(app, share, fileInfo))) {
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
        Optional.ofNullable(fileInfo.getContentCreatedAt()).map(AbstractAgencyBoxService::toZonedDateTime)
                .ifPresent(media::setContentCreationDate);
        media.setDate(toZonedDateTime(fileInfo.getCreatedAt()));
        media.setTitle(fileInfo.getName());
        media.setDescription(fileInfo.getDescription());
        media.setCreator(fileInfo.getCreatedBy().getName());
        media.setThumbnailUrl(
                newURL(boxService.getThumbnailUrl(app, Long.parseLong(fileInfo.getVersion().getVersionID()), share)));
        FileMetadata metadata = addMetadata(media, boxService.getSharedLink(app, share, fileInfo), null);
        metadata.setExtension(fileInfo.getExtension());
        metadata.setSha1(fileInfo.getSha1());
        metadata.setSize(fileInfo.getSize());
        return media;
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
        save |= doCommonUpdate(media, false).getResult();
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
    public final URL getSourceUrl(BoxMedia media) {
        return media.getUniqueMetadata().getAssetUrl();
    }

    @Override
    protected final String getAuthor(BoxMedia media) throws MalformedURLException {
        return boxService.getSharedItem(media.getUniqueMetadata().getAssetUrl()).getCreatedBy().getName();
    }

    @Override
    protected final Optional<Temporal> getCreationDate(BoxMedia media) {
        return Optional.ofNullable(media.getContentCreationDate());
    }

    @Override
    protected final Optional<Temporal> getUploadDate(BoxMedia media) {
        return Optional.of(media.getDate());
    }
}