package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.newURL;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
import org.wikimedia.commons.donvip.spacemedia.data.domain.Statistics;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.CompositeMediaId;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.stsci.StsciMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.stsci.StsciMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.exception.UploadException;
import org.wikimedia.commons.donvip.spacemedia.service.stsci.StsciService;
import org.wikimedia.commons.donvip.spacemedia.service.wikimedia.WikidataService;

/**
 * Service harvesting images from NASA Hubble / Jame Webb websites.
 */
public abstract class AbstractOrgStsciService extends AbstractOrgService<StsciMedia> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractOrgStsciService.class);

    private static final Set<String> SHORT_CREDITS_OK = Set.of("A. Fujii");

    private final String searchEndpoint;
    private final String detailEndpoint;
    private final String mission;

    @Autowired
    private StsciService stsci;

    @Autowired
    private WikidataService wikidata;

    protected AbstractOrgStsciService(StsciMediaRepository repository, String mission, String searchEndpoint,
            String detailEndpoint) {
        super(repository, mission + ".nasa", Set.of(mission));
        this.searchEndpoint = searchEndpoint;
        this.detailEndpoint = detailEndpoint;
        this.mission = mission;
    }

    @Override
    protected boolean isNASA(StsciMedia media) {
        return true;
    }

    @Override
    protected final Class<StsciMedia> getMediaClass() {
        return StsciMedia.class;
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
        List<FileMetadata> uploadedMetadata = new ArrayList<>();
        List<StsciMedia> uploadedMedia = new ArrayList<>();
        while (loop) {
            String[] response = stsci.fetchImagesByScrapping(searchEndpoint.replace("<idx>", Integer.toString(idx++)));
            loop = response != null && response.length > 0;
            if (loop) {
                for (String imageId : response) {
                    try {
                        Triple<StsciMedia, Collection<FileMetadata>, Integer> update = doUpdateMedia(
                                new CompositeMediaId(mission, imageId));
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

    private Triple<StsciMedia, Collection<FileMetadata>, Integer> doUpdateMedia(CompositeMediaId id)
            throws IOException, UploadException, UpdateFinishedException {
        boolean save = false;
        StsciMedia media;
        Optional<StsciMedia> mediaInRepo = repository.findById(id);
        if (mediaInRepo.isPresent()) {
            media = mediaInRepo.get();
            LocalDate doNotFetchEarlierThan = getRuntimeData().getDoNotFetchEarlierThan();
            if (doNotFetchEarlierThan != null && media.getPublicationDate().isBefore(doNotFetchEarlierThan)) {
                throw new UpdateFinishedException(media.getPublicationDate().toString());
            }
            String credits = media.getCredits();
            // Fix corrupted files with short credits. Can be removed in the future
            if (isShortCredits(credits) && !SHORT_CREDITS_OK.contains(credits)) {
                media = refresh(media);
                save = !isShortCredits(media.getCredits());
            }
        } else {
            media = getMediaFromWebsite(id.getMediaId());
            save = true;
        }
        if (doCommonUpdate(media)) {
            save = true;
        }
        int uploadCount = 0;
        List<FileMetadata> uploadedMetadata = new ArrayList<>();
        if (shouldUploadAuto(media, false)) {
            Triple<StsciMedia, Collection<FileMetadata>, Integer> upload = upload(save ? saveMedia(media) : media, true,
                    false);
            uploadCount += upload.getRight();
            uploadedMetadata.addAll(upload.getMiddle());
            media = saveMedia(upload.getLeft());
            save = false;
        }
        return Triple.of(saveMediaOrCheckRemote(save, media), uploadedMetadata, uploadCount);
    }

    private static boolean isShortCredits(String credits) {
        return StringUtils.length(credits) < 10;
    }

    private StsciMedia getMediaFromWebsite(String id) throws IOException {
        return stsci.getImageDetailsByScrapping(id, getImageDetailsLink(id));
    }

    @Override
    protected StsciMedia refresh(StsciMedia media) throws IOException {
        return media.copyDataFrom(getMediaFromWebsite(media.getId().getMediaId()));
    }

    @Override
    protected Map<String, Pair<Object, Map<String, Object>>> getStatements(StsciMedia media, FileMetadata metadata) {
        Map<String, Pair<Object, Map<String, Object>>> result = super.getStatements(media, metadata);
        if (StringUtils.isNotBlank(media.getObjectName())) {
            wikidata.searchAstronomicalObject(media.getObjectName()).map(Pair::getKey)
                    .ifPresent(qid -> result.put("P180", Pair.of(qid, null))); // Depicts the object
        }
        return result;
    }

    @Override
    public Set<String> findCategories(StsciMedia media, FileMetadata metadata, boolean includeHidden) {
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
    public final Set<String> findLicenceTemplates(StsciMedia media, FileMetadata metadata) {
        Set<String> result = super.findLicenceTemplates(media, metadata);
        result.add(switch(media.getMission()) {
        case "hubble" -> "PD-Hubble";
        case "webb" -> "PD-Webb";
        default -> throw new IllegalStateException("Unsupported mission: " + media.getMission());
        });
        return result;
    }

    @Override
    public final URL getSourceUrl(StsciMedia media, FileMetadata metadata) {
        return newURL(getImageDetailsLink(media.getId().getMediaId()));
    }

    @Override
    protected final String getAuthor(StsciMedia media) throws MalformedURLException {
        return media.getCredits();
    }

    @Override
    public Statistics getStatistics(boolean details) {
        Statistics stats = super.getStatistics(details);
        if (details) {
            stats.setDetails(List.of(new Statistics(mission, mission,
                    repository.count(getRepoIds()),
                    repository.countUploadedToCommons(getRepoIds()),
                    repository.countByIgnoredTrue(getRepoIds()),
                    repository.countMissingImagesInCommons(getRepoIds()),
                    repository.countMissingVideosInCommons(getRepoIds()),
                    repository.countByMetadata_PhashNotNull(getRepoIds()), null)));
        }
        return stats;
    }
}
