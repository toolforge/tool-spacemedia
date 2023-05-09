package org.wikimedia.commons.donvip.spacemedia.service.agencies;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.time.temporal.Temporal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

import org.wikimedia.commons.donvip.spacemedia.data.domain.FullResExtraMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.FullResExtraMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.Metadata;
import org.wikimedia.commons.donvip.spacemedia.exception.ImageUploadForbiddenException;
import org.wikimedia.commons.donvip.spacemedia.exception.TooManyResultsException;
import org.wikimedia.commons.donvip.spacemedia.exception.UploadException;

public abstract class AbstractFullResExtraAgencyService<T extends FullResExtraMedia<ID, D>, ID, D extends Temporal>
        extends AbstractFullResAgencyService<T, ID, D> {

    private FullResExtraMediaRepository<T, ID, D> extraRepository;

    protected AbstractFullResExtraAgencyService(FullResExtraMediaRepository<T, ID, D> repository, String id) {
        super(repository, id);
        this.extraRepository = repository;
    }

    @Override
    protected final boolean shouldUploadAuto(T media, boolean isManual) {
        return super.shouldUploadAuto(media, isManual)
                || shouldUploadAuto(new UploadContext<>(media, media.getExtraMetadata(), isManual));
    }

    @Override
    protected void checkUploadPreconditions(T media, boolean checkUnicity, boolean isManual)
            throws MalformedURLException, URISyntaxException {
        super.checkUploadPreconditions(media, checkUnicity, isManual);
        // Forbid upload of duplicate medias for a single repo, they may have different descriptions
        String extraSha1 = media.getExtraMetadata().getSha1();
        if (checkUnicity && extraSha1 != null && extraRepository.countByExtraMetadata_Sha1(extraSha1) > 1) {
            throw new ImageUploadForbiddenException(media + " is present several times.");
        }
    }

    @Override
    public final T uploadAndSaveBySha1(String sha1, boolean isManual) throws UploadException, TooManyResultsException {
        T media = findBySha1OrThrow(sha1, false);
        if (media == null) {
            media = findByFullResSha1OrThrow(sha1, true);
        }
        if (media == null) {
            media = findByExtraSha1OrThrow(sha1, true);
        }
        return saveMedia(upload(media, true, isManual).getLeft());
    }

    @Override
    protected final int doUpload(T media, boolean checkUnicity, Collection<Metadata> uploaded, boolean isManual)
            throws IOException, UploadException {
        return super.doUpload(media, checkUnicity, uploaded, isManual) + doUpload(media, media.getExtraMetadata(),
                checkUnicity, uploaded, isManual);
    }

    protected final T findByExtraSha1OrThrow(String sha1, boolean throwIfNotFound) throws TooManyResultsException {
        return findBySomeSha1OrThrow(sha1, extraRepository::findByExtraMetadata_Sha1, throwIfNotFound);
    }

    @Override
    protected final <S> S doForAnySha1(String sha1, BiFunction<T, Metadata, S> function) throws TooManyResultsException {
        T media = findBySha1OrThrow(sha1, false);
        if (media != null) {
            return function.apply(media, media.getMetadata());
        } else {
            media = findByFullResSha1OrThrow(sha1, false);
            if (media != null) {
                return function.apply(media, media.getFullResMetadata());
            } else {
                media = findByExtraSha1OrThrow(sha1, true);
                return function.apply(media, media.getExtraMetadata());
            }
        }
    }

    @Override
    protected Optional<String> getOtherVersions(T media, Metadata metadata) {
        Optional<String> variants = super.getOtherVersions(media, metadata);
        if (metadata.equals(media.getMetadata())) {
            return getOtherVersions(media, variants, List.of(media.getFullResMetadata(), media.getExtraMetadata()),
                    List.of(media.getFullResCommonsFileNames(), media.getExtraCommonsFileNames()));
        } else if (metadata.equals(media.getFullResMetadata())) {
            return getOtherVersions(media, variants, List.of(media.getMetadata(), media.getExtraMetadata()),
                    List.of(media.getCommonsFileNames(), media.getExtraCommonsFileNames()));
        } else if (metadata.equals(media.getExtraMetadata())) {
            return getOtherVersions(media, variants, List.of(media.getMetadata(), media.getFullResMetadata()),
                    List.of(media.getCommonsFileNames(), media.getFullResCommonsFileNames()));
        }
        return variants;
    }
}
