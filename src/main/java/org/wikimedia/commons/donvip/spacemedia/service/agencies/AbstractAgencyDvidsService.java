package org.wikimedia.commons.donvip.spacemedia.service.agencies;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.temporal.Temporal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriTemplate;
import org.wikimedia.commons.donvip.spacemedia.data.domain.Media;
import org.wikimedia.commons.donvip.spacemedia.data.domain.Statistics;
import org.wikimedia.commons.donvip.spacemedia.data.domain.dvids.DvidsAudio;
import org.wikimedia.commons.donvip.spacemedia.data.domain.dvids.DvidsAudioRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.dvids.DvidsGraphic;
import org.wikimedia.commons.donvip.spacemedia.data.domain.dvids.DvidsGraphicRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.dvids.DvidsImage;
import org.wikimedia.commons.donvip.spacemedia.data.domain.dvids.DvidsImageRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.dvids.DvidsMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.dvids.DvidsMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.dvids.DvidsMediaType;
import org.wikimedia.commons.donvip.spacemedia.data.domain.dvids.DvidsMediaTypedId;
import org.wikimedia.commons.donvip.spacemedia.data.domain.dvids.DvidsNews;
import org.wikimedia.commons.donvip.spacemedia.data.domain.dvids.DvidsNewsRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.dvids.DvidsPublication;
import org.wikimedia.commons.donvip.spacemedia.data.domain.dvids.DvidsPublicationRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.dvids.DvidsVideo;
import org.wikimedia.commons.donvip.spacemedia.data.domain.dvids.DvidsVideoRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.dvids.DvidsWebcast;
import org.wikimedia.commons.donvip.spacemedia.data.domain.dvids.DvidsWebcastRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.dvids.api.ApiAssetResponse;
import org.wikimedia.commons.donvip.spacemedia.data.domain.dvids.api.ApiPageInfo;
import org.wikimedia.commons.donvip.spacemedia.data.domain.dvids.api.ApiSearchResponse;
import org.wikimedia.commons.donvip.spacemedia.data.domain.dvids.api.ApiSearchResult;
import org.wikimedia.commons.donvip.spacemedia.exception.ApiException;
import org.wikimedia.commons.donvip.spacemedia.exception.TooManyResultsException;

/**
 * Service fetching images from https://api.dvidshub.net/
 */
@Service
public abstract class AbstractAgencyDvidsService<OT extends Media<OID, OD>, OID, OD extends Temporal>
        extends AbstractAgencyService<DvidsMedia, DvidsMediaTypedId, ZonedDateTime, OT, OID, OD> {

    private static final int MAX_RESULTS = 1000;

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractAgencyDvidsService.class);

    @Autowired
    private DvidsAudioRepository audioRepository;

    @Autowired
    private DvidsImageRepository imageRepository;

    @Autowired
    private DvidsGraphicRepository graphicRepository;

    @Autowired
    private DvidsVideoRepository videoRepository;

    @Autowired
    private DvidsNewsRepository newsRepository;

    @Autowired
    private DvidsPublicationRepository publiRepository;

    @Autowired
    private DvidsWebcastRepository webcastRepository;

    @Autowired
    private DvidsMediaRepository<DvidsMedia> mediaRepository;

    @Value("${dvids.api.key}")
    private String apiKey;

    @Value("${dvids.api.search.url}")
    private UriTemplate searchApiEndpoint;

    @Value("${dvids.api.search.year.url}")
    private UriTemplate searchYearApiEndpoint;

    @Value("${dvids.api.asset.url}")
    private UriTemplate assetApiEndpoint;

    @Value("${dvids.media.url}")
    private UriTemplate mediaUrl;

    @Value("${dvids.min.year}")
    private int minYear;

    private final Set<String> units;

    public AbstractAgencyDvidsService(DvidsMediaRepository<DvidsMedia> repository, Set<String> units) {
        super(repository);
        this.units = units;
    }

    @Override
    protected Class<DvidsMedia> getMediaClass() {
        return DvidsMedia.class;
    }

    @Override
    protected Class<DvidsImage> getTopTermsMediaClass() {
        return DvidsImage.class; // TODO can't get a direct lucene reader on DvidsMedia
    }

    private DvidsMedia save(DvidsMedia media) {
        switch (media.getMediaType()) {
        case image: return imageRepository.save((DvidsImage) media);
        case graphic: return graphicRepository.save((DvidsGraphic) media);
        case video: return videoRepository.save((DvidsVideo) media);
        case audio: return audioRepository.save((DvidsAudio) media);
        case news: return newsRepository.save((DvidsNews) media);
        case publication_issue: return publiRepository.save((DvidsPublication) media);
        case webcast: return webcastRepository.save((DvidsWebcast) media);
        }
        throw new IllegalArgumentException(media.toString());
    }

    protected void updateDvidsMedia() {
        LocalDateTime start = startUpdateMedia();
        RestTemplate rest = new RestTemplate();
        int count = 0;
        for (String unit : units) {
            for (DvidsMediaType type : new DvidsMediaType[] {
                    DvidsMediaType.image, DvidsMediaType.video }) {
                LOGGER.info("Fetching DVIDS {}s from unit '{}'...", type, unit);
                try {
                    boolean loop = true;
                    int page = 1;
                    while (loop) {
                        UpdateResult ur = doUpdateDvidsMedia(rest,
                                searchDvidsMediaIds(rest, false, type, unit, 0, page++), unit);
                        count += ur.count;
                        loop = ur.loop;
                    }
                } catch (TooManyResultsException ex) {
                    LOGGER.trace("TooManyResults", ex);
                    int year = LocalDateTime.now().getYear();
                    while (year >= minYear) {
                        try {
                            boolean loop = true;
                            int page = 1;
                            while (loop) {
                                UpdateResult ur = doUpdateDvidsMedia(rest,
                                        searchDvidsMediaIds(rest, true, type, unit, year--, page++), unit);
                                count += ur.count;
                                loop = ur.loop;
                            }
                        } catch (ApiException | TooManyResultsException exx) {
                            LOGGER.error("Error while fetching DVIDS " + type + "s from unit " + unit, exx);
                        }
                    }
                } catch (RuntimeException | ApiException e) {
                    LOGGER.error("Error while fetching DVIDS " + type + "s from unit " + unit, e);
                }
            }
        }
        endUpdateMedia(count, start);
    }

    private UpdateResult doUpdateDvidsMedia(RestTemplate rest, ApiSearchResponse response, String unit) {
        int count = 0;
        List<String> results = response.getResults().stream().map(ApiSearchResult::getId).collect(Collectors.toList());
        for (String id : results) {
            try {
                count += processDvidsMedia(rest.getForObject(
                        assetApiEndpoint.expand(Map.of("api_key", apiKey, "id", id)),
                        ApiAssetResponse.class).getResults(), unit);
            } catch (IOException | URISyntaxException e) {
                LOGGER.error("Error while processing DVIDS " + id + " from unit " + unit, e);
            }
        }
        ApiPageInfo pi = response.getPageInfo();
        return new UpdateResult(pi.getResultsPerPage() > 0 && results.size() == pi.getResultsPerPage(), count);
    }

    private static class UpdateResult {
        private boolean loop;
        private int count;

        public UpdateResult(boolean loop, int count) {
            this.loop = loop;
            this.count = count;
        }
    }

    private ApiSearchResponse searchDvidsMediaIds(RestTemplate rest, boolean allowCappedResults, DvidsMediaType type,
            String unit, int year, int page)
            throws ApiException, TooManyResultsException {
        Map<String, Object> variables = Map.of("api_key", apiKey, "type", type, "unit", unit, "page", page);
        ApiSearchResponse response;
        if (year <= 0) {
            response = rest.getForObject(searchApiEndpoint.expand(variables), ApiSearchResponse.class);
        } else {
            variables = new HashMap<>(variables);
            variables.put("from_date", year + "-01-01T00:00:00Z");
            variables.put("to_date", year + "-12-31T23:59:59Z");
            response = rest.getForObject(searchYearApiEndpoint.expand(variables), ApiSearchResponse.class);
        }
        if (response.getErrors() != null) {
            throw new ApiException(
                    String.format("API error while fetching DVIDS %ss from unit '%s': %s", type, unit, response));
        }
        ApiPageInfo pageInfo = response.getPageInfo();
        if (pageInfo.getTotalResults() == MAX_RESULTS) {
            String msg = String.format("Incomplete search! More criteria must be defined for %ss of '%s' (%d)!",
                    type, unit, year);
            if (allowCappedResults) {
                LOGGER.warn(msg);
            } else {
                throw new TooManyResultsException(msg);
            }
        }
        return response;
    }

    private int processDvidsMedia(DvidsMedia media, String unit) throws IOException, URISyntaxException {
        boolean save = false;
        Optional<DvidsMedia> optMediaInRepo = mediaRepository.findById(media.getId());
        if (optMediaInRepo.isPresent()) {
            media = optMediaInRepo.get();
        } else {
            media.setUnit(unit);
            save = true;
        }
        if (mediaService.updateMedia(media, getOriginalRepository())) {
            save = true;
        }
        if (save) {
            save(media);
        }
        return 1;
    }

    @Override
    public final URL getSourceUrl(DvidsMedia media) throws MalformedURLException {
        return mediaUrl.expand(Map.of("type", media.getId().getType(), "id", media.getId().getId())).toURL();
    }

    @Override
    protected final String getAuthor(DvidsMedia media) throws MalformedURLException {
        return media.getCredit().toString(); // FIXME
    }

    @Override
    public final long countAllMedia() {
        return mediaRepository.count(units);
    }

    @Override
    public final long countIgnored() {
        return mediaRepository.countByIgnoredTrue(units);
    }

    @Override
    public final long countMissingMedia() {
        return mediaRepository.countMissingInCommons(units);
    }

    @Override
    public final long countUploadedMedia() {
        return mediaRepository.countUploadedToCommons(units);
    }

    @Override
    public final Iterable<DvidsMedia> listAllMedia() {
        return mediaRepository.findAll(units);
    }

    @Override
    public final Page<DvidsMedia> listAllMedia(Pageable page) {
        return mediaRepository.findAll(units, page);
    }

    @Override
    public final List<DvidsMedia> listIgnoredMedia() {
        return mediaRepository.findByIgnoredTrue(units);
    }

    @Override
    public final Page<DvidsMedia> listIgnoredMedia(Pageable page) {
        return mediaRepository.findByIgnoredTrue(units, page);
    }

    @Override
    public final List<DvidsMedia> listMissingMedia() {
        return mediaRepository.findMissingInCommons(units);
    }

    @Override
    public final Page<DvidsMedia> listMissingMedia(Pageable page) {
        return mediaRepository.findMissingInCommons(units, page);
    }

    @Override
    public final List<DvidsMedia> listUploadedMedia() {
        return mediaRepository.findUploadedToCommons(units);
    }

    @Override
    public final Page<DvidsMedia> listUploadedMedia(Pageable page) {
        return mediaRepository.findUploadedToCommons(units, page);
    }

    @Override
    public final List<DvidsMedia> listDuplicateMedia() {
        return mediaRepository.findDuplicateInCommons(units);
    }

    @Override
    public Statistics getStatistics() {
        Statistics stats = super.getStatistics();
        if (units.size() > 1) {
            stats.setDetails(units.stream()
                    .map(this::getStatistics)
                    .sorted().collect(Collectors.toList()));
        }
        return stats;
    }

    private Statistics getStatistics(String alias) {
        Set<String> singleton = Collections.singleton(alias);
        return new Statistics(alias, mediaRepository.count(singleton),
                mediaRepository.countByIgnoredTrue(singleton),
                mediaRepository.countMissingInCommons(singleton), null);
    }
}
