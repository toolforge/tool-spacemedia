package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.time.LocalDate;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.CompositeMediaId;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.s3.S3Media;
import org.wikimedia.commons.donvip.spacemedia.data.domain.s3.S3MediaRepository;
import org.wikimedia.commons.donvip.spacemedia.exception.UploadException;
import org.wikimedia.commons.donvip.spacemedia.service.s3.S3Service;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;

/**
 * Service fetching images from AWS S3
 */
public abstract class AbstractOrgS3Service extends DefaultOrgService<S3Media> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractOrgS3Service.class);

    @Autowired
    private S3MediaRepository mediaRepository;

    @Autowired
    private S3Service s3;

    private final Regions region;
    private final Set<String> bucketNames;

    protected AbstractOrgS3Service(S3MediaRepository repository, String id, Regions region, Set<String> bucketNames) {
        super(repository, id);
        this.region = region;
        this.bucketNames = bucketNames;
    }

    @Override
    protected Class<S3Media> getMediaClass() {
        return S3Media.class;
    }

    @Override
    protected Class<S3Media> getTopTermsMediaClass() {
        return S3Media.class;
    }

    protected final S3Object getS3Object(CompositeMediaId mediaId) {
        return getS3Object(mediaId.getRepoId(), mediaId.getMediaId());
    }

    protected final S3Object getS3Object(String bucket, String key) {
        return s3.getObject(region, bucket, key);
    }

    protected final String getUrl(CompositeMediaId id) {
        return "http://" + id.getRepoId() + ".s3.amazonaws.com/" + id.getMediaId();
    }

    @Override
    public void updateMedia() {
        LocalDateTime start = startUpdateMedia();
        List<S3Media> uploadedMedia = new ArrayList<>();
        int count = 0;
        for (String bucket : bucketNames) {
            Pair<Integer, Collection<S3Media>> update = updateS3Media(region, bucket);
            uploadedMedia.addAll(update.getRight());
            count += update.getLeft();
            ongoingUpdateMedia(start, count);
        }
        endUpdateMedia(count, uploadedMedia, start);
    }

    private Pair<Integer, Collection<S3Media>> updateS3Media(Regions region, String bucket) {
        List<S3Media> uploadedMedia = new ArrayList<>();
        int count = 0;
        LocalDateTime start = LocalDateTime.now();

        List<S3Media> files = s3.getFiles(region, bucket, summary -> toS3Media(bucket, summary),
                Comparator.comparing(S3Media::getPublicationDateTime));
        LOGGER.info("Found {} files in {}", files.size(), Duration.between(start, LocalDateTime.now()));

        for (S3Media media : files) {
            try {
                Pair<S3Media, Integer> result = processS3Media(media);
                if (result.getValue() > 0) {
                    uploadedMedia.add(result.getKey());
                }
                ongoingUpdateMedia(start, bucket, count++);
            } catch (UploadException | IOException | RuntimeException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }

        return Pair.of(count, uploadedMedia);
    }

    private S3Media toS3Media(String bucket, S3ObjectSummary summary) {
        S3Media media = new S3Media(bucket, summary.getKey());
        media.setPublicationDateTime(toZonedDateTime(summary.getLastModified()));
        addMetadata(media, getUrl(media.getId()), m -> fillMetadata(m, summary));
        return media;
    }

    protected abstract S3Media enrichS3Media(S3Media media) throws IOException;

    private static ZonedDateTime toZonedDateTime(Date date) {
        return date.toInstant().atZone(ZoneOffset.ofHours(-7));
    }

    private static FileMetadata fillMetadata(FileMetadata m, S3ObjectSummary summary) {
        m.setExtension(summary.getKey().substring(summary.getKey().lastIndexOf('.') + 1));
        m.setSize(summary.getSize());
        return m;
    }

    private Pair<S3Media, Integer> processS3Media(S3Media mediaFromApi) throws IOException, UploadException {
        S3Media media = null;
        boolean save = false;
        Optional<S3Media> mediaInDb = mediaRepository.findById(mediaFromApi.getId());
        if (mediaInDb.isPresent()) {
            media = mediaInDb.get();
        } else {
            media = enrichS3Media(mediaFromApi);
            save = true;
        }
        save |= doCommonUpdate(media);
        int uploadCount = 0;
        if (shouldUploadAuto(media, false)) {
            Triple<S3Media, Collection<FileMetadata>, Integer> upload = upload(media, true, false);
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
    protected final S3Media refresh(S3Media media) throws IOException {
        return media.copyDataFrom(media); // FIXME
    }

    @Override
    public final URL getSourceUrl(S3Media media, FileMetadata metadata) {
        return metadata.getAssetUrl();
    }

    @Override
    public long countAllMedia() {
        return mediaRepository.count(bucketNames);
    }

    @Override
    public long countIgnored() {
        return mediaRepository.countByIgnoredTrue(bucketNames);
    }

    @Override
    public long countMissingMedia() {
        return mediaRepository.countMissingInCommonsByShare(bucketNames);
    }

    @Override
    public long countMissingImages() {
        return mediaRepository.countMissingImagesInCommons(bucketNames);
    }

    @Override
    public long countMissingVideos() {
        return mediaRepository.countMissingVideosInCommons(bucketNames);
    }

    @Override
    public long countPerceptualHashes() {
        return mediaRepository.countByMetadata_PhashNotNull(bucketNames);
    }

    @Override
    public long countUploadedMedia() {
        return mediaRepository.countUploadedToCommons(bucketNames);
    }

    @Override
    public Iterable<S3Media> listAllMedia() {
        return mediaRepository.findAll(bucketNames);
    }

    @Override
    public Page<S3Media> listAllMedia(Pageable page) {
        return mediaRepository.findAll(bucketNames, page);
    }

    @Override
    public List<S3Media> listMissingMedia() {
        return mediaRepository.findMissingInCommons(bucketNames);
    }

    @Override
    public Page<S3Media> listMissingMedia(Pageable page) {
        return mediaRepository.findMissingInCommons(bucketNames, page);
    }

    @Override
    public Page<S3Media> listMissingImages(Pageable page) {
        return mediaRepository.findMissingImagesInCommons(bucketNames, page);
    }

    @Override
    public Page<S3Media> listMissingVideos(Pageable page) {
        return mediaRepository.findMissingVideosInCommons(bucketNames, page);
    }

    @Override
    public List<S3Media> listMissingMediaByDate(LocalDate date) {
        return mediaRepository.findMissingInCommonsByDate(bucketNames, date);
    }

    @Override
    public List<S3Media> listMissingMediaByTitle(String title) {
        return mediaRepository.findMissingInCommonsByTitle(bucketNames, title);
    }

    @Override
    public Page<S3Media> listHashedMedia(Pageable page) {
        return mediaRepository.findByMetadata_PhashNotNull(bucketNames, page);
    }

    @Override
    public List<S3Media> listUploadedMedia() {
        return mediaRepository.findUploadedToCommons(bucketNames);
    }

    @Override
    public Page<S3Media> listUploadedMedia(Pageable page) {
        return mediaRepository.findUploadedToCommons(bucketNames, page);
    }

    @Override
    public List<S3Media> listDuplicateMedia() {
        return mediaRepository.findDuplicateInCommons(bucketNames);
    }

    @Override
    public List<S3Media> listIgnoredMedia() {
        return mediaRepository.findByIgnoredTrue(bucketNames);
    }

    @Override
    public Page<S3Media> listIgnoredMedia(Pageable page) {
        return mediaRepository.findByIgnoredTrue(bucketNames, page);
    }
}
