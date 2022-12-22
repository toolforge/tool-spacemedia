package org.wikimedia.commons.donvip.spacemedia.service.agencies;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.temporal.Temporal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpClientErrorException.BadRequest;
import org.springframework.web.client.HttpClientErrorException.Forbidden;
import org.springframework.web.client.HttpClientErrorException.NotFound;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriTemplate;
import org.wikimedia.commons.donvip.spacemedia.data.domain.Media;
import org.wikimedia.commons.donvip.spacemedia.data.domain.Metadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.Statistics;
import org.wikimedia.commons.donvip.spacemedia.data.domain.dvids.DvidsAudio;
import org.wikimedia.commons.donvip.spacemedia.data.domain.dvids.DvidsAudioRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.dvids.DvidsCredit;
import org.wikimedia.commons.donvip.spacemedia.data.domain.dvids.DvidsCreditRepository;
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
import org.wikimedia.commons.donvip.spacemedia.exception.ImageNotFoundException;
import org.wikimedia.commons.donvip.spacemedia.exception.TooManyResultsException;
import org.wikimedia.commons.donvip.spacemedia.exception.UploadException;
import org.wikimedia.commons.donvip.spacemedia.service.wikimedia.CommonsService;
import org.wikimedia.commons.donvip.spacemedia.utils.UnitedStates;
import org.wikimedia.commons.donvip.spacemedia.utils.UnitedStates.VirinTemplates;

/**
 * Service fetching images from https://api.dvidshub.net/
 *
 * @param <OT> Original media type
 * @param <OID> Original media identifier type
 * @param <OD> Original media date type
 */
@Service
public abstract class AbstractAgencyDvidsService<OT extends Media<OID, OD>, OID, OD extends Temporal>
        extends AbstractAgencyService<DvidsMedia, DvidsMediaTypedId, ZonedDateTime, OT, OID, OD> {

    private static final String OLD_DVIDS_CDN = "https://cdn.dvidshub.net/";

    private static final Pattern US_MEDIA_BY = Pattern
            .compile(".*\\((U\\.S\\. .+ (?:photo|graphic|video) by )[^\\)]+\\)", Pattern.DOTALL);

    private static final int MAX_RESULTS = 1000;

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractAgencyDvidsService.class);

    protected static final Map<String, String> KEYWORDS_CATS = loadCsvMapping(
            AbstractAgencyDvidsService.class, "dvids.keywords.csv");

    private static final Set<Class<? extends DvidsMedia>> MEDIA_WITH_CATEGORIES = Set.of(DvidsVideo.class,
            DvidsNews.class, DvidsAudio.class);

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

    @Autowired
    private DvidsCreditRepository creditRepository;

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

    @Value("${dvids.ignored.categories}")
    private Set<String> ignoredCategories;

    @Value("${videos.enabled}")
    private boolean videosEnabled;

    private final int minYear;

    private final Set<String> units;

    protected AbstractAgencyDvidsService(DvidsMediaRepository<DvidsMedia> repository, String id, Set<String> units, int minYear) {
        super(repository, id);
        this.units = units;
        this.minYear = minYear;
    }

    @Override
    protected Class<DvidsMedia> getMediaClass() {
        return DvidsMedia.class;
    }

    @Override
    protected Class<DvidsImage> getTopTermsMediaClass() {
        return DvidsImage.class; // TODO can't get a direct lucene reader on DvidsMedia
    }

    @Override
    protected final DvidsMediaTypedId getMediaId(String id) {
        return new DvidsMediaTypedId(id);
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
        Set<String> idsKnownToDvidsApi = new HashSet<>();
        int count = 0;
        for (String unit : units) {
            count += updateDvidsMedia(unit, DvidsMediaType.image, idsKnownToDvidsApi);
            if (videosEnabled) {
                count += updateDvidsMedia(unit, DvidsMediaType.video, idsKnownToDvidsApi);
            }
        }
        deleteOldDvidsMedia(idsKnownToDvidsApi);
        endUpdateMedia(count, start);
    }

    private void deleteOldDvidsMedia(Set<String> idsKnownToDvidsApi) {
        // DVIDS API Terms of Service force us to check for deleted content
        // https://api.dvidshub.net/docs/tos
        for (DvidsMedia media : listMissingMedia()) {
            String id = media.getId().toString();
            if (!idsKnownToDvidsApi.contains(id)) {
                try {
                    refreshAndSaveById(id);
                } catch (IOException | ImageNotFoundException e) {
                    LOGGER.error(e.getMessage(), e);
                }
            }
        }
    }

    private int updateDvidsMedia(String unit, DvidsMediaType type, Set<String> idsKnownToDvidsApi) {
        LocalDateTime start = LocalDateTime.now();
        RestTemplate rest = new RestTemplate();
        int count = 0;
        try {
            boolean loop = true;
            int page = 1;
            LOGGER.info("Fetching DVIDS {}s from unit '{}' (page {}/?)...", type, unit, page);
            while (loop) {
                DvidsUpdateResult ur = doUpdateDvidsMedia(rest,
                        searchDvidsMediaIds(rest, false, type, unit, 0, page++), unit);
                idsKnownToDvidsApi.addAll(ur.idsKnownToDvidsApi);
                count += ur.count;
                loop = count < ur.totalResults;
                if (loop) {
                    LOGGER.info("Fetching DVIDS {}s from unit '{}' (page {}/{})...", type, unit, page,
                            ur.numberOfPages());
                }
            }
            LOGGER.info("{} {}s completed: {} {}s in {}", unit, type, count, type,
                    Duration.between(LocalDateTime.now(), start));
        } catch (TooManyResultsException ex) {
            LOGGER.trace("TooManyResults", ex);
            int year = LocalDateTime.now().getYear();
            while (year >= minYear) {
                try {
                    boolean loop = true;
                    int page = 1;
                    start = LocalDateTime.now();
                    count = 0;
                    LOGGER.info("Fetching DVIDS {}s from unit '{}' for year {} (page {}/?)...", type, unit, year, page);
                    while (loop) {
                        DvidsUpdateResult ur = doUpdateDvidsMedia(rest,
                                searchDvidsMediaIds(rest, true, type, unit, year, page++), unit);
                        idsKnownToDvidsApi.addAll(ur.idsKnownToDvidsApi);
                        count += ur.count;
                        loop = count < ur.totalResults;
                        if (loop) {
                            LOGGER.info("Fetching DVIDS {}s from unit '{}' for year {} (page {}/{})...", type, unit,
                                    year, page, ur.numberOfPages());
                        }
                    }
                    LOGGER.info("{} {}s for year {} completed: {} {}s in {}", unit, type, year,
                            count, type, Duration.between(LocalDateTime.now(), start));
                    year--;
                } catch (ApiException | TooManyResultsException exx) {
                    LOGGER.error("Error while fetching DVIDS " + type + "s from unit " + unit, exx);
                }
            }
        } catch (RuntimeException | ApiException e) {
            LOGGER.error("Error while fetching DVIDS " + type + "s from unit " + unit, e);
        }
        return count;
    }

    private DvidsUpdateResult doUpdateDvidsMedia(RestTemplate rest, ApiSearchResponse response, String unit) {
        int count = 0;
        Set<String> idsKnownToDvidsApi = new HashSet<>();
        for (String id : response.getResults().stream().map(ApiSearchResult::getId).distinct().sorted().toList()) {
            try {
                idsKnownToDvidsApi.add(id);
                count += processDvidsMedia(mediaRepository.findById(new DvidsMediaTypedId(id)),
                        () -> getMediaFromApi(rest, id, unit));
            } catch (HttpClientErrorException e) {
                LOGGER.error("API error while processing DVIDS {} from unit {}: {}", id, unit, smartExceptionLog(e));
            } catch (IOException e) {
                LOGGER.error("I/O error while processing DVIDS {} from unit {}: {}", id, unit, smartExceptionLog(e));
            } catch (UploadException e) {
                LOGGER.error("Upload error while processing DVIDS {} from unit {}: {}", id, unit, smartExceptionLog(e));
            } catch (DataAccessException e) {
                LOGGER.error("DAO error while processing DVIDS {} from unit {}: {}", id, unit, smartExceptionLog(e));
            }
        }
        ApiPageInfo pi = response.getPageInfo();
        return new DvidsUpdateResult(pi.getResultsPerPage(), pi.getTotalResults(), count, idsKnownToDvidsApi);
    }

    private static Object smartExceptionLog(Throwable e) {
        return e.getCause() instanceof RuntimeException ? e : e.toString();
    }

    private DvidsMedia getMediaFromApi(RestTemplate rest, String id, String unit) {
        DvidsMedia media = Optional.ofNullable(
                rest.getForObject(assetApiEndpoint.expand(Map.of("api_key", apiKey, "id", id)), ApiAssetResponse.class))
                .orElseThrow(() -> new IllegalArgumentException("No result from DVIDS API for " + id))
                .getResults();
        media.setUnit(unit);
        return media;
    }

    private static class DvidsUpdateResult {
        private final Set<String> idsKnownToDvidsApi;
        private final int count;
        private final int resultsPerPage;
        private final int totalResults;

        public DvidsUpdateResult(int resultsPerPage, int totalResults, int count, Set<String> processedIds) {
            this.count = count;
            this.resultsPerPage = resultsPerPage;
            this.totalResults = totalResults;
            this.idsKnownToDvidsApi = processedIds;
        }

        public int numberOfPages() {
            return (int) Math.ceil((double) totalResults / (double) resultsPerPage);
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
        if (response == null || response.getErrors() != null) {
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
        } else if (pageInfo.getTotalResults() == 0) {
            LOGGER.warn("No {} for {} in year {}", type, unit, year);
        } else if (page == 1) {
            LOGGER.debug("{} {}s to process for {}", pageInfo.getTotalResults(), type, unit);
        }
        return response;
    }

    private int processDvidsMedia(Optional<DvidsMedia> mediaInDb, Supplier<DvidsMedia> apiFetcher)
            throws IOException, UploadException {
        DvidsMedia media = null;
        boolean save = false;
        if (mediaInDb.isPresent()) {
            media = mediaInDb.get();
            save = updateCategoryAndCdnUrls(media, apiFetcher);
        } else {
            media = apiFetcher.get();
            save = true;
        }
        if (mediaService.updateMedia(media, getOriginalRepository(), false).getResult()) {
            save = true;
        }
        if (!Boolean.TRUE.equals(media.isIgnored()) && ignoredCategories.contains(media.getCategory())) {
            ignoreFile(media, "Ignored category: " + media.getCategory());
            save = true;
        }
        if (shouldUploadAuto(media)) {
            media = upload(media, true);
            save = true;
        }
        if (save) {
            if (isNotEmpty(media.getCredit())) {
                creditRepository.saveAll(media.getCredit());
            }
            save(media);
        }
        return 1;
    }

    protected boolean updateCategoryAndCdnUrls(DvidsMedia media, Supplier<DvidsMedia> apiFetcher) {
        // DVIDS changed its CDN around 2021/2022. Example:
        // old: https://cdn.dvidshub.net/media/photos/2104/6622429.jpg
        // new: https://d34w7g4gy10iej.cloudfront.net/photos/2104/6622429.jpg
        if ((MEDIA_WITH_CATEGORIES.contains(media.getClass()) && media.getCategory() == null)
                || media.getMetadata().getAssetUrl().toExternalForm().startsWith(OLD_DVIDS_CDN)
                || media.getThumbnailUrl().toExternalForm().startsWith(OLD_DVIDS_CDN)
                || (media instanceof DvidsVideo videoMedia
                        && videoMedia.getImage().toExternalForm().startsWith(OLD_DVIDS_CDN))) {
            DvidsMedia mediaFromApi = apiFetcher.get();
            media.setCategory(mediaFromApi.getCategory());
            media.getMetadata().setAssetUrl(mediaFromApi.getMetadata().getAssetUrl());
            media.setThumbnailUrl(mediaFromApi.getThumbnailUrl());
            if (media instanceof DvidsVideo videoMedia && mediaFromApi instanceof DvidsVideo videoMediaFromApi) {
                videoMedia.setImage(videoMediaFromApi.getImage());
            }
            return true;
        }
        return false;
    }

    @Override
    public final DvidsMedia refreshAndSave(DvidsMedia media) throws IOException {
        media = refresh(media);
        Exception e = doCommonUpdate(media, true).getException();
        if (e instanceof NotFound) {
            return deleteMedia(media, e);
        } else {
            return saveMedia(media);
        }
    }

    @Override
    protected final DvidsMedia refresh(DvidsMedia media) throws IOException {
        // DVIDS API Terms of Service force us to check for deleted content
        // https://api.dvidshub.net/docs/tos
        try {
            return media.copyDataFrom(getMediaFromApi(new RestTemplate(), media.getId().toString(), media.getUnit()));
        } catch (IllegalArgumentException e) {
            String message = e.getMessage();
            if (message != null && message.startsWith("No result from DVIDS API for ")) {
                return deleteMedia(media, e);
            } else {
                LOGGER.error(message, e);
            }
        } catch (BadRequest e) {
            String message = e.getMessage();
            if (message != null && message.contains(" was not found")) {
                return deleteMedia(media, e);
            } else {
                LOGGER.error(message, e);
            }
        } catch (Forbidden e) {
            String message = e.getMessage();
            if (message != null && message.contains(" was found, but is not published.")) {
                return deleteMedia(media, e);
            } else {
                LOGGER.error(message, e);
            }
        }
        return media;
    }

    @Override
    public final URL getSourceUrl(DvidsMedia media) throws MalformedURLException {
        return mediaUrl.expand(Map.of("type", media.getId().getType(), "id", media.getId().getId())).toURL();
    }

    @Override
    protected final String getSource(DvidsMedia media) throws MalformedURLException {
        URL sourceUrl = getSourceUrl(media);
        VirinTemplates t = UnitedStates.getUsVirinTemplates(media.getVirin(), sourceUrl);
        return t != null ? "{{" + t.getVirinTemplate() + "}}" : sourceUrl.toExternalForm();
    }

    @Override
    protected final String getAuthor(DvidsMedia media) throws MalformedURLException {
        StringBuilder result = new StringBuilder();
        Matcher m = US_MEDIA_BY.matcher(media.getDescription());
        if (m.matches()) {
            result.append(m.group(1));
        } else if (StringUtils.isNotBlank(media.getBranch()) && !"Joint".equals(media.getBranch())) {
            result.append("U.S. ").append(media.getBranch()).append(' ').append(media.getId().getType()).append(" by ");
        }
        result.append(media.getCredit().stream().map(this::dvidsCreditToString).distinct().collect(joining(", ")));
        return result.toString();
    }

    private String dvidsCreditToString(DvidsCredit credit) {
        StringBuilder result = new StringBuilder();
        if (StringUtils.isNotBlank(credit.getRank())) {
            result.append(credit.getRank()).append(' ');
        }
        result.append('[').append(credit.getUrl()).append(' ').append(credit.getName()).append(']');
        return result.toString();
    }

    @Override
    protected final Optional<Temporal> getCreationDate(DvidsMedia media) {
        return Optional.of(media.getDate());
    }

    @Override
    protected final Optional<Temporal> getUploadDate(DvidsMedia media) {
        return Optional.of(media.getDatePublished());
    }

    @Override
    protected final String getWikiFileDesc(DvidsMedia media, Metadata metadata) throws MalformedURLException {
        StringBuilder sb = new StringBuilder("{{milim\n| description = ")
                .append("{{").append(getLanguage(media)).append("|1=")
                .append(CommonsService.formatWikiCode(getDescription(media))).append("}}");
        getWikiDate(media).ifPresent(s -> sb.append("\n| date = ").append(s));
        sb.append("\n| source = ").append(getSource(media))
          .append("\n| author = ").append(getAuthor(media));
        getPermission(media).ifPresent(s -> sb.append("\n| permission = ").append(s));
        Optional.ofNullable(media.getLocation()).ifPresent(l -> sb.append("\n| location = ").append(l));
        sb.append("\n| virin = ").append(media.getVirin());
        Optional.ofNullable(media.getDatePublished()).ifPresent(p -> sb.append("\n| dateposted = ").append(toIso8601(p)));
        Optional.ofNullable(media.getRating()).ifPresent(r -> sb.append("\n| stars = ").append(r.intValue()));
        getOtherVersions(media, metadata).ifPresent(s -> sb.append("\n| other versions = ").append(s));
        getOtherFields(media).ifPresent(s -> sb.append("\n| other fields = ").append(s));
        getOtherFields1(media).ifPresent(s -> sb.append("\n| other fields 1 = ").append(s));
        sb.append("\n}}");
        return sb.toString();
    }

    @Override
    public Set<String> findCategories(DvidsMedia media, Metadata metadata, boolean includeHidden) {
        Set<String> result = super.findCategories(media, metadata, includeHidden);
        if (isNotEmpty(media.getKeywords())) {
            result.addAll(media.getKeywords().stream().map(KEYWORDS_CATS::get).filter(StringUtils::isNotBlank)
                    .flatMap(s -> Arrays.stream(s.split(";"))).collect(toSet()));
        }
        if (includeHidden) {
            result.add("Photographs by Defense Video and Imagery Distribution System");
        }
        return result;
    }

    @Override
    public Set<String> findTemplates(DvidsMedia media) {
        Set<String> result = super.findTemplates(media);
        VirinTemplates t = UnitedStates.getUsVirinTemplates(media.getVirin(), media.getMetadata().getAssetUrl());
        if (t != null && StringUtils.isNotBlank(t.getPdTemplate())) {
            result.add(t.getPdTemplate());
        }
        if (media.getDescription().contains("Space Force photo")) {
            result.add("PD-USGov-Military-Space Force");
            result.remove("PD-USGov-Military-Air Force");
        }
        return result;
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
        return mediaRepository.countMissingInCommonsByUnit(units);
    }

    @Override
    public final long countMissingImages() {
        return mediaRepository.countMissingImagesInCommons(units);
    }

    @Override
    public final long countMissingVideos() {
        return mediaRepository.countMissingVideosInCommons(units);
    }

    @Override
    public final long countPerceptualHashes() {
        return mediaRepository.countByMetadata_PhashNotNull(units);
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
        return mediaRepository.findMissingInCommonsByUnit(units);
    }

    @Override
    public final Page<DvidsMedia> listMissingMedia(Pageable page) {
        return mediaRepository.findMissingInCommonsByUnit(units, page);
    }

    @Override
    public final Page<DvidsMedia> listMissingImages(Pageable page) {
        return mediaRepository.findMissingImagesInCommons(units, page);
    }

    @Override
    public final Page<DvidsMedia> listMissingVideos(Pageable page) {
        return mediaRepository.findMissingVideosInCommons(units, page);
    }

    @Override
    public final Page<DvidsMedia> listHashedMedia(Pageable page) {
        return mediaRepository.findByMetadata_PhashNotNull(units, page);
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
    public Statistics getStatistics(boolean details) {
        Statistics stats = super.getStatistics(details);
        if (details && units.size() > 1) {
            stats.setDetails(units.stream()
                    .map(this::getStatistics)
                    .sorted().toList());
        }
        return stats;
    }

    private Statistics getStatistics(String alias) {
        Set<String> singleton = Collections.singleton(alias);
        return new Statistics(alias, alias,
                mediaRepository.count(singleton),
                mediaRepository.countUploadedToCommons(singleton),
                mediaRepository.countByIgnoredTrue(singleton),
                mediaRepository.countMissingImagesInCommons(singleton),
                mediaRepository.countMissingVideosInCommons(singleton),
                mediaRepository.countByMetadata_PhashNotNull(singleton), null);
    }

    @Override
    protected final List<DvidsMedia> findDuplicates() {
        return mediaRepository.findByDuplicatesIsNotEmpty(units);
    }

    @Override
    protected final int doResetIgnored() {
        return mediaRepository.resetIgnored(units);
    }

    @Override
    protected final int doResetPerceptualHashes() {
        return mediaRepository.resetPerceptualHashes(units);
    }

    @Override
    protected final int doResetSha1Hashes() {
        return mediaRepository.resetSha1Hashes(units);
    }
}
