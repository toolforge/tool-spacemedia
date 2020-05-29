package org.wikimedia.commons.donvip.spacemedia.service.agencies;

import java.io.IOException;
import java.time.temporal.Temporal;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;

import org.apache.commons.collections.CollectionUtils;
import org.wikimedia.commons.donvip.spacemedia.data.domain.FullResMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.FullResMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.Media;
import org.wikimedia.commons.donvip.spacemedia.data.domain.Metadata;
import org.wikimedia.commons.donvip.spacemedia.exception.ImageUploadForbiddenException;
import org.wikimedia.commons.donvip.spacemedia.exception.TooManyResultsException;

public abstract class AbstractFullResAgencyService<T extends FullResMedia<ID, D>, ID, D extends Temporal, OT extends Media<OID, OD>, OID, OD extends Temporal>
        extends AbstractAgencyService<T, ID, D, OT, OID, OD> {

    private FullResMediaRepository<T, ID, D> fullResRepository;

    public AbstractFullResAgencyService(FullResMediaRepository<T, ID, D> repository, String id) {
        super(repository, id);
        this.fullResRepository = repository;
    }

    @Override
    protected void checkUploadPreconditions(T media) throws IOException {
        super.checkUploadPreconditions(media);
        // Forbid upload of duplicate medias for a single repo, they may have different descriptions
        String fullResSha1 = media.getFullResMetadata().getSha1();
        if (fullResSha1 != null && fullResRepository.countByFullResMetadata_Sha1(fullResSha1) > 1) {
            throw new ImageUploadForbiddenException(media + " is present several times.");
        }
    }

    @Override
    public final T uploadAndSave(String sha1) throws IOException, TooManyResultsException {
        T media = findBySha1OrThrow(sha1, false);
        if (media == null) {
            media = findByFullResSha1OrThrow(sha1, true);
        }
        return repository.save(upload(media));
    }

    @Override
    protected final void doUpload(T media) throws IOException {
        super.doUpload(media);
        doUpload(getWikiCode(media, media.getFullResMetadata()), media, media.getFullResMetadata(), media::setFullResCommonsFileNames);
    }

    protected final T findByFullResSha1OrThrow(String sha1, boolean throwIfNotFound) throws TooManyResultsException {
        return findBySomeSha1OrThrow(sha1, fullResRepository::findByFullResMetadata_Sha1, throwIfNotFound);
    }

    private <S> S doForSha1OrFullResSha1(String sha1, BiFunction<T, Metadata, S> function) throws TooManyResultsException {
        T media = findBySha1OrThrow(sha1, false);
        if (media != null) {
            return function.apply(media, media.getMetadata());
        } else {
            media = findByFullResSha1OrThrow(sha1, true);
            return function.apply(media, media.getFullResMetadata());
        }
    }

    @Override
    public final String getWikiCode(String sha1) throws TooManyResultsException {
        return doForSha1OrFullResSha1(sha1, this::getWikiCode);
    }

    @Override
    public final String getWikiHtmlPreview(String sha1) throws TooManyResultsException {
        return doForSha1OrFullResSha1(sha1, this::getWikiHtmlPreview);
    }

    @Override
    protected Optional<String> getOtherVersions(T media, Metadata metadata) {
        if (metadata.equals(media.getMetadata()) && media.getFullResMetadata() != null && media.getFullResMetadata().getAssetUrl() != null) {
            return getOtherVersion(media, media.getFullResMetadata(), media.getFullResCommonsFileNames());
        } else if (metadata.equals(media.getFullResMetadata()) && media.getMetadata() != null && media.getMetadata().getAssetUrl() != null) {
            return getOtherVersion(media, media.getMetadata(), media.getCommonsFileNames());
        }
        return Optional.empty();
    }

    private final Optional<String> getOtherVersion(T media, Metadata metadata, Set<String> commonsFileNames) {
        String url = metadata.getAssetUrl().toExternalForm();
        String ext = url.substring(url.lastIndexOf('.') + 1);
        String filename = CollectionUtils.isEmpty(commonsFileNames)
                ? media.getUploadTitle() + '.' + ext
                : commonsFileNames.iterator().next();
        return Optional.of(gallery(filename, ext.toUpperCase(Locale.ENGLISH) + " version"));
    }
}
