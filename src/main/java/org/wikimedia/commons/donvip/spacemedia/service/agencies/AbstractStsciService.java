package org.wikimedia.commons.donvip.spacemedia.service.agencies;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.temporal.Temporal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.jsoup.HttpStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.wikimedia.commons.donvip.spacemedia.data.domain.Statistics;
import org.wikimedia.commons.donvip.spacemedia.data.domain.stsci.StsciMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.stsci.StsciMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.service.stsci.StsciService;

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
        while (loop) {
            String[] response = stsci.fetchImagesByScrapping(searchEndpoint.replace("<idx>", Integer.toString(idx++)));
            loop = response != null && response.length > 0;
            if (loop) {
                for (String imageId : response) {
                    try {
                        count += doUpdateMedia(imageId);
                    } catch (HttpStatusException e) {
                        LOGGER.error("Error while requesting {}: {}", e.getUrl(), e.getMessage());
                        problem(e.getUrl(), e);
                    } catch (IOException | RuntimeException e) {
                        LOGGER.error("Error while fetching image " + imageId, e);
                        problem(getImageDetailsLink(imageId), e);
                    }
                }
            }
        }
        endUpdateMedia(count, start);
    }

    private int doUpdateMedia(String id) throws IOException {
        boolean save = false;
        StsciMedia media;
        Optional<StsciMedia> mediaInRepo = repository.findById(id);
        if (mediaInRepo.isPresent()) {
            media = mediaInRepo.get();
        } else {
            media = getMediaFromWebsite(id);
            save = true;
        }
        if (doCommonUpdate(media)) {
            save = true;
        }
        saveMediaOrCheckRemote(save, media);
        return save ? 1 : 0;
    }

    private StsciMedia getMediaFromWebsite(String id) throws IOException {
        return stsci.getImageDetailsByScrapping(id, getImageDetailsLink(id));
    }

    @Override
    protected StsciMedia refresh(StsciMedia media) throws IOException {
        return media.copyDataFrom(getMediaFromWebsite(media.getId()));
    }

    @Override
    public final Set<String> findTemplates(StsciMedia media) {
        Set<String> result = super.findTemplates(media);
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
}
