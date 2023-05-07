package org.wikimedia.commons.donvip.spacemedia.service.agencies;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.jsoup.HttpStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.wikimedia.commons.donvip.spacemedia.data.domain.Metadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.Statistics;
import org.wikimedia.commons.donvip.spacemedia.data.domain.stsci.StsciMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.stsci.StsciMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.exception.UploadException;
import org.wikimedia.commons.donvip.spacemedia.service.AbstractSocialMediaService;
import org.wikimedia.commons.donvip.spacemedia.service.stsci.StsciService;
import org.wikimedia.commons.donvip.spacemedia.service.wikimedia.WikidataService;

/**
 * Service harvesting images from NASA Hubble / Jame Webb websites.
 */
public abstract class AbstractStsciService
        extends
        AbstractFullResAgencyService<StsciMedia, String, ZonedDateTime, StsciMedia, String, ZonedDateTime> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractStsciService.class);

    private final StsciMediaRepository stsciRepository;
    private final String searchEndpoint;
    private final String detailEndpoint;
    private final String mission;

    @Autowired
    private StsciService stsci;

    @Autowired
    private WikidataService wikidata;

    protected AbstractStsciService(StsciMediaRepository repository, String mission, String searchEndpoint,
            String detailEndpoint) {
        super(repository, mission + ".nasa");
        this.stsciRepository = repository;
        this.searchEndpoint = searchEndpoint;
        this.detailEndpoint = detailEndpoint;
        this.mission = mission;
    }

    @Override
    protected final Class<StsciMedia> getMediaClass() {
        return StsciMedia.class;
    }

    @Override
    protected final String getMediaId(String id) {
        return id;
    }

    private String getImageDetailsLink(String imageId) {
        return detailEndpoint.replace("<id>", imageId);
    }

    @Override
    protected boolean checkBlocklist() {
        return false;
    }

    @Override
    public void updateMedia() throws IOException {
        LocalDateTime start = startUpdateMedia();
        int count = 0;
        boolean loop = true;
        int idx = 1;
        List<Metadata> uploadedMetadata = new ArrayList<>();
        List<StsciMedia> uploadedMedia = new ArrayList<>();
        while (loop) {
            String[] response = stsci.fetchImagesByScrapping(searchEndpoint.replace("<idx>", Integer.toString(idx++)));
            loop = response != null && response.length > 0;
            if (loop) {
                for (String imageId : response) {
                    try {
                        Triple<StsciMedia, Collection<Metadata>, Integer> update = doUpdateMedia(imageId);
                        if (update.getRight() > 0) {
                            uploadedMetadata.addAll(update.getMiddle());
                            uploadedMedia.add(update.getLeft());
                        }
                        ongoingUpdateMedia(start, count++);
                    } catch (UpdateFinishedException e) {
                        // End of search when an old image found
                        LOGGER.info("End of search: {}", e.getMessage());
                        loop = false;
                    } catch (HttpStatusException e) {
                        LOGGER.error("Error while requesting {}: {}", e.getUrl(), e.getMessage());
                        problem(e.getUrl(), e);
                    } catch (IOException | UploadException | RuntimeException e) {
                        LOGGER.error("Error while fetching image " + imageId, e);
                        problem(getImageDetailsLink(imageId), e);
                    }
                }
            }
        }
        endUpdateMedia(count, uploadedMedia, uploadedMetadata, start);
    }

    private Triple<StsciMedia, Collection<Metadata>, Integer> doUpdateMedia(String id)
            throws IOException, UploadException, UpdateFinishedException {
        boolean save = false;
        StsciMedia media;
        Optional<StsciMedia> mediaInRepo = repository.findById(id);
        if (mediaInRepo.isPresent()) {
            media = mediaInRepo.get();
            LocalDate doNotFetchEarlierThan = getRuntimeData().getDoNotFetchEarlierThan();
            if (doNotFetchEarlierThan != null
                    && media.getDate().isBefore(doNotFetchEarlierThan.atStartOfDay(ZoneId.systemDefault()))) {
                throw new UpdateFinishedException(media.getDate().toString());
            }
        } else {
            media = getMediaFromWebsite(id);
            save = true;
        }
        if (doCommonUpdate(media)) {
            save = true;
        }
        int uploadCount = 0;
        List<Metadata> uploadedMetadata = new ArrayList<>();
        if (shouldUploadAuto(media, false)) {
            Triple<StsciMedia, Collection<Metadata>, Integer> upload = upload(save ? saveMedia(media) : media, true,
                    false);
            uploadCount += upload.getRight();
            uploadedMetadata.addAll(upload.getMiddle());
            media = saveMedia(upload.getLeft());
            save = false;
        }
        return Triple.of(saveMediaOrCheckRemote(save, media), uploadedMetadata, uploadCount);
    }

    private StsciMedia getMediaFromWebsite(String id) throws IOException {
        return stsci.getImageDetailsByScrapping(id, getImageDetailsLink(id));
    }

    @Override
    protected StsciMedia refresh(StsciMedia media) throws IOException {
        return media.copyDataFrom(getMediaFromWebsite(media.getId()));
    }

    @Override
    protected Map<String, Pair<Object, Map<String, Object>>> getStatements(StsciMedia media, Metadata metadata)
            throws MalformedURLException {
        Map<String, Pair<Object, Map<String, Object>>> result = super.getStatements(media, metadata);
        if (StringUtils.isNotBlank(media.getObjectName())) {
            wikidata.searchAstronomicalObject(media.getObjectName()).map(Pair::getKey)
                    .ifPresent(qid -> result.put("P180", Pair.of(qid, null))); // Depicts the object
        }
        return result;
    }

    @Override
    public Set<String> findCategories(StsciMedia media, Metadata metadata, boolean includeHidden) {
        Set<String> result = super.findCategories(media, metadata, includeHidden);
        if (StringUtils.isNotBlank(media.getObjectName())) {
            wikidata.searchAstronomicalObject(media.getObjectName()).map(Pair::getValue)
                    .ifPresentOrElse(result::add,
                    () -> {
                        if (StringUtils.isNotBlank(media.getConstellation())) {
                            wikidata.searchConstellation(media.getConstellation())
                                    .map(Pair::getValue).ifPresent(result::add);
                        }
                    });
        }
        return result;
    }

    @Override
    public final Set<String> findLicenceTemplates(StsciMedia media) {
        Set<String> result = super.findLicenceTemplates(media);
        switch (media.getMission()) {
        case "hubble":
            result.add("PD-Hubble");
            break;
        case "webb":
            result.add("PD-Webb");
            break;
        default:
            throw new IllegalStateException("Unsupported mission: " + media.getMission());
        }
        return result;
    }

    @Override
    public final URL getSourceUrl(StsciMedia media) throws MalformedURLException {
        return new URL(getImageDetailsLink(media.getId()));
    }

    @Override
    protected final String getAuthor(StsciMedia media) throws MalformedURLException {
        return media.getCredits();
    }

    @Override
    protected final Optional<Temporal> getCreationDate(StsciMedia media) {
        return Optional.ofNullable(media.getExposureDate());
    }

    @Override
    protected final Optional<Temporal> getUploadDate(StsciMedia media) {
        return Optional.of(media.getDate());
    }

    @Override
    public final long countAllMedia() {
        return stsciRepository.countByMission(mission);
    }

    @Override
    public final long countIgnored() {
        return stsciRepository.countByIgnoredTrueAndMission(mission);
    }

    @Override
    public final long countMissingMedia() {
        return stsciRepository.countMissingInCommons(mission);
    }

    @Override
    public final long countMissingImages() {
        return stsciRepository.countMissingImagesInCommons(mission);
    }

    @Override
    public final long countMissingVideos() {
        return stsciRepository.countMissingVideosInCommons(mission);
    }

    @Override
    public final long countPerceptualHashes() {
        return stsciRepository.countByMetadata_PhashNotNullAndMission(mission);
    }

    @Override
    public final long countUploadedMedia() {
        return stsciRepository.countUploadedToCommons(mission);
    }

    @Override
    public final Iterable<StsciMedia> listAllMedia() {
        return stsciRepository.findAllByMission(mission);
    }

    @Override
    public final Page<StsciMedia> listAllMedia(Pageable page) {
        return stsciRepository.findAllByMission(mission, page);
    }

    @Override
    public final List<StsciMedia> listIgnoredMedia() {
        return stsciRepository.findByIgnoredTrueAndMission(mission);
    }

    @Override
    public final Page<StsciMedia> listIgnoredMedia(Pageable page) {
        return stsciRepository.findByIgnoredTrueAndMission(mission, page);
    }

    @Override
    public final List<StsciMedia> listMissingMedia() {
        return stsciRepository.findMissingInCommons(mission);
    }

    @Override
    public final Page<StsciMedia> listMissingMedia(Pageable page) {
        return stsciRepository.findMissingInCommons(mission, page);
    }

    @Override
    public final Page<StsciMedia> listMissingImages(Pageable page) {
        return stsciRepository.findMissingImagesInCommons(mission, page);
    }

    @Override
    public final Page<StsciMedia> listMissingVideos(Pageable page) {
        return stsciRepository.findMissingVideosInCommons(mission, page);
    }

    @Override
    public final Page<StsciMedia> listHashedMedia(Pageable page) {
        return stsciRepository.findByMetadata_PhashNotNullAndMission(mission, page);
    }

    @Override
    public final List<StsciMedia> listUploadedMedia() {
        return stsciRepository.findUploadedToCommons(mission);
    }

    @Override
    public final Page<StsciMedia> listUploadedMedia(Pageable page) {
        return stsciRepository.findUploadedToCommons(mission, page);
    }

    @Override
    public final List<StsciMedia> listDuplicateMedia() {
        return stsciRepository.findDuplicateInCommons(mission);
    }

    @Override
    public Statistics getStatistics(boolean details) {
        Statistics stats = super.getStatistics(details);
        if (details) {
            stats.setDetails(List.of(new Statistics(mission, mission,
                    stsciRepository.countByMission(mission),
                    stsciRepository.countUploadedToCommons(mission),
                    stsciRepository.countByIgnoredTrueAndMission(mission),
                    stsciRepository.countMissingImagesInCommons(mission),
                    stsciRepository.countMissingVideosInCommons(mission),
                    stsciRepository.countByMetadata_PhashNotNullAndMission(mission), null)));
        }
        return stats;
    }

    @Override
    protected Set<String> getEmojis(StsciMedia uploadedMedia) {
        return AbstractSocialMediaService.getEmojis(uploadedMedia.getKeywords());
    }
}
