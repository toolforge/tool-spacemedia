package org.wikimedia.commons.donvip.spacemedia.service.agencies;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.time.temporal.Temporal;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;

import org.wikimedia.commons.donvip.spacemedia.data.domain.FullResMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.FullResMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.Media;
import org.wikimedia.commons.donvip.spacemedia.data.domain.Metadata;
import org.wikimedia.commons.donvip.spacemedia.exception.ImageUploadForbiddenException;
import org.wikimedia.commons.donvip.spacemedia.exception.TooManyResultsException;
import org.wikimedia.commons.donvip.spacemedia.exception.UploadException;

public abstract class AbstractFullResAgencyService<T extends FullResMedia<ID, D>, ID, D extends Temporal, OT extends Media<OID, OD>, OID, OD extends Temporal>
        extends AbstractAgencyService<T, ID, D, OT, OID, OD> {

    private FullResMediaRepository<T, ID, D> fullResRepository;

    protected AbstractFullResAgencyService(FullResMediaRepository<T, ID, D> repository, String id) {
        super(repository, id);
        this.fullResRepository = repository;
    }

    @Override
    protected boolean shouldUploadAuto(T media, boolean isManual) {
        return super.shouldUploadAuto(media, isManual)
                || shouldUploadAuto(new UploadContext<>(media, media.getFullResMetadata(), isManual));
    }

    @Override
    protected void checkUploadPreconditions(T media, boolean checkUnicity, boolean isManual)
            throws MalformedURLException, URISyntaxException {
        super.checkUploadPreconditions(media, checkUnicity, isManual);
        // Forbid upload of duplicate medias for a single repo, they may have different descriptions
        String fullResSha1 = media.getFullResMetadata().getSha1();
        if (checkUnicity && fullResSha1 != null && fullResRepository.countByFullResMetadata_Sha1(fullResSha1) > 1) {
            throw new ImageUploadForbiddenException(media + " is present several times.");
        }
    }

    @Override
    public T uploadAndSaveBySha1(String sha1, boolean isManual) throws UploadException, TooManyResultsException {
        T media = findBySha1OrThrow(sha1, false);
        if (media == null) {
            media = findByFullResSha1OrThrow(sha1, true);
        }
        return saveMedia(upload(media, true, isManual).getLeft());
    }

    @Override
    protected int doUpload(T media, boolean checkUnicity, Collection<Metadata> uploaded, boolean isManual)
            throws IOException, UploadException {
        return super.doUpload(media, checkUnicity, uploaded, isManual) + doUpload(media, media.getFullResMetadata(),
                checkUnicity, uploaded, isManual);
    }

    protected final T findByFullResSha1OrThrow(String sha1, boolean throwIfNotFound) throws TooManyResultsException {
        return findBySomeSha1OrThrow(sha1, fullResRepository::findByFullResMetadata_Sha1, throwIfNotFound);
    }

    protected <S> S doForAnySha1(String sha1, BiFunction<T, Metadata, S> function) throws TooManyResultsException {
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
        return doForAnySha1(sha1, this::getWikiCode).getKey();
    }

    @Override
    public final String getWikiHtmlPreview(String sha1) throws TooManyResultsException {
        return doForAnySha1(sha1, this::getWikiHtmlPreview);
    }

    @Override
    protected Optional<String> getOtherVersions(T media, Metadata metadata) {
        Optional<String> variants = super.getOtherVersions(media, metadata);
        if (metadata.equals(media.getMetadata())) {
            return getOtherVersion(media, variants, media.getFullResMetadata(), media.getFullResCommonsFileNames());
        } else if (metadata.equals(media.getFullResMetadata())) {
            return getOtherVersion(media, variants, media.getMetadata(), media.getCommonsFileNames());
        }
        return variants;
    }

    private Optional<String> getOtherVersion(T media, Optional<String> variants, Metadata metadata, Set<String> commonsFileNames) {
        return getOtherVersions(media, variants, List.of(metadata), List.of(commonsFileNames));
    }

    protected final Optional<String> getOtherVersions(T media, Optional<String> variants, List<Metadata> metadatas,
            List<Set<String>> commonsFileNames) {
        if (metadatas.size() != commonsFileNames.size()) {
            throw new IllegalStateException("Size mismatch");
        }
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < metadatas.size(); i++) {
            Metadata metadata = metadatas.get(i);
            if (metadata != null && metadata.getAssetUrl() != null) {
                String ext = metadata.getFileExtension();
                String filename = media.getFirstCommonsFileNameOrUploadTitle(commonsFileNames.get(i), ext);
                result.append(filename).append('|').append(ext.toUpperCase(Locale.ENGLISH)).append(" version");
            }
        }
        if (variants.isPresent()) {
            result.append('\n').append(variants);
        }
        return Optional.of(result.toString());
    }

    protected static final class UpdateFinishedException extends Exception {
        private static final long serialVersionUID = 1L;

        public UpdateFinishedException(String message) {
            super(message);
        }
    }
}
