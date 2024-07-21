package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toSet;
import static org.wikimedia.commons.donvip.spacemedia.utils.CsvHelper.loadCsvMapping;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpClientErrorException.BadRequest;
import org.springframework.web.client.HttpClientErrorException.Forbidden;
import org.springframework.web.client.HttpClientErrorException.NotFound;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriTemplate;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.CompositeMediaId;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.dvids.DvidsImage;
import org.wikimedia.commons.donvip.spacemedia.data.domain.dvids.DvidsLocation;
import org.wikimedia.commons.donvip.spacemedia.data.domain.dvids.DvidsMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.dvids.DvidsMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.dvids.DvidsMediaType;
import org.wikimedia.commons.donvip.spacemedia.data.domain.dvids.api.ApiAssetResponse;
import org.wikimedia.commons.donvip.spacemedia.data.domain.dvids.api.ApiPageInfo;
import org.wikimedia.commons.donvip.spacemedia.data.domain.dvids.api.ApiSearchResponse;
import org.wikimedia.commons.donvip.spacemedia.data.domain.dvids.api.ApiSearchResult;
import org.wikimedia.commons.donvip.spacemedia.exception.ApiException;
import org.wikimedia.commons.donvip.spacemedia.exception.ImageNotFoundException;
import org.wikimedia.commons.donvip.spacemedia.exception.TooManyResultsException;
import org.wikimedia.commons.donvip.spacemedia.service.MediaService.MediaUpdateResult;
import org.wikimedia.commons.donvip.spacemedia.service.dvids.DvidsMediaProcessorService;
import org.wikimedia.commons.donvip.spacemedia.service.wikimedia.GlitchTip;
import org.wikimedia.commons.donvip.spacemedia.utils.UnitedStates;
import org.wikimedia.commons.donvip.spacemedia.utils.UnitedStates.VirinTemplates;
import org.wikimedia.commons.donvip.spacemedia.utils.Utils;

/**
 * Service fetching images from https://api.dvidshub.net/
 */
@Service
public abstract class AbstractOrgDvidsService extends AbstractOrgService<DvidsMedia> {

    private static final Pattern US_MEDIA_BY = Pattern
            .compile(".*\\((U\\.S\\. .+ (?:photo|graphic|video) by )[^\\)]+\\)", Pattern.DOTALL);

    private static final int MAX_RESULTS = 1000;

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractOrgDvidsService.class);

    protected static final Map<String, String> KEYWORDS_CATS = loadCsvMapping(
            AbstractOrgDvidsService.class, "dvids.keywords.csv");

    @Lazy
    @Autowired
    private DvidsMediaProcessorService dvidsProcessor;

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

    private final int minYear;

    protected AbstractOrgDvidsService(DvidsMediaRepository<DvidsMedia> repository, String id, Set<String> units, int minYear) {
        super(repository, id, units);
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
    public final void checkCommonsCategories() {
        checkCommonsCategories(KEYWORDS_CATS);
    }

    @Override
    public void updateMedia(String[] args) {
        LocalDateTime start = startUpdateMedia();
        Set<String> idsKnownToDvidsApi = new HashSet<>();
        List<DvidsMedia> uploadedMedia = new ArrayList<>();
        int count = 0;
        LocalDate doNotFetchEarlierThan = getRuntimeData().getDoNotFetchEarlierThan();
        for (int year = LocalDateTime.now().getYear(); year >= minYear
                && (doNotFetchEarlierThan == null || year >= doNotFetchEarlierThan.getYear()); year--) {
            for (String unit : getRepoIdsFromArgs(args)) {
                Pair<Integer, Collection<DvidsMedia>> update = updateDvidsMedia(unit, year, DvidsMediaType.image,
                        idsKnownToDvidsApi);
                uploadedMedia.addAll(update.getRight());
                count += update.getLeft();
                ongoingUpdateMedia(start, count);
                if (videosEnabled) {
                    update = updateDvidsMedia(unit, year, DvidsMediaType.video, idsKnownToDvidsApi);
                    uploadedMedia.addAll(update.getRight());
                    count += update.getLeft();
                    ongoingUpdateMedia(start, count);
                }
            }
        }
        if (doNotFetchEarlierThan == null) {
            // Only delete pictures not found in complete updates
            deleteOldDvidsMedia(idsKnownToDvidsApi);
        }
        endUpdateMedia(count, uploadedMedia, allMetadata(uploadedMedia), start, LocalDate.now().minusYears(1), true);
    }

    private void deleteOldDvidsMedia(Set<String> idsKnownToDvidsApi) {
        // DVIDS API Terms of Service force us to check for deleted content
        // https://api.dvidshub.net/docs/tos
        for (DvidsMedia media : listMissingMedia()) {
            String id = media.getId().getMediaId();
            if ((videosEnabled || !media.isVideo()) && !idsKnownToDvidsApi.contains(id)) {
                try {
                    refreshAndSaveById(id);
                } catch (IOException | ImageNotFoundException e) {
                    LOGGER.error(e.getMessage(), e);
                    GlitchTip.capture(e);
                }
            }
        }
    }

    private Pair<Integer, Collection<DvidsMedia>> updateDvidsMedia(String unit, int year, DvidsMediaType type,
            Set<String> idsKnownToDvidsApi) {
        RestTemplate rest = new RestTemplate();
        List<DvidsMedia> uploadedMedia = new ArrayList<>();
        int count = 0;
        try {
            boolean loop = true;
            int page = 1;
            LocalDateTime start = LocalDateTime.now();
            count = 0;
            LOGGER.info("Fetching DVIDS {}s from unit '{}' for year {} (page {}/?)...", type, unit, year, page);
            while (loop) {
                DvidsUpdateResult ur = doUpdateDvidsMedia(rest,
                        searchDvidsMediaIds(rest, true, type, unit, year, page++), unit);
                idsKnownToDvidsApi.addAll(ur.idsKnownToDvidsApi);
                uploadedMedia.addAll(ur.uploadedMedia);
                count += ur.count;
                ongoingUpdateMedia(start, unit, count);
                loop = count < ur.totalResults;
                if (loop) {
                    LOGGER.info("Fetching DVIDS {}s from unit '{}' for year {} (page {}/{})...", type, unit, year, page,
                            ur.numberOfPages());
                }
            }
            LOGGER.info("{} {}s for year {} completed: {} {}s in {}", unit, type, year, count, type,
                    Utils.durationInSec(start));
        } catch (ApiException | TooManyResultsException exx) {
            LOGGER.error("Error while fetching DVIDS " + type + "s from unit " + unit, exx);
            GlitchTip.capture(exx);
        }
        return Pair.of(count, uploadedMedia);
    }

    private DvidsUpdateResult doUpdateDvidsMedia(RestTemplate rest, ApiSearchResponse response, String unit) {
        int count = 0;
        LocalDateTime start = LocalDateTime.now();
        List<DvidsMedia> uploadedMedia = new ArrayList<>();
        Set<String> idsKnownToDvidsApi = new HashSet<>();
        for (String id : response.getResults().stream().map(ApiSearchResult::getId).distinct().sorted().toList()) {
            try {
                idsKnownToDvidsApi.add(id);
                Pair<DvidsMedia, Integer> result = dvidsProcessor.processDvidsMedia(
                        () -> repository.findById(new CompositeMediaId(unit, id)),
                        () -> getMediaFromApi(rest, id, unit),
                        media -> processDvidsMediaUpdate(media, false).result(), this::shouldUploadAuto,
                        this::uploadWrapped);
                if (result.getValue() > 0) {
                    uploadedMedia.add(result.getKey());
                }
                ongoingUpdateMedia(start, unit, count++);
            } catch (HttpClientErrorException e) {
                LOGGER.error("API error while processing DVIDS {} from unit {}: {}", id, unit, smartExceptionLog(e));
                GlitchTip.capture(e);
            } catch (DataAccessException e) {
                LOGGER.error("DAO error while processing DVIDS {} from unit {}: {}", id, unit, smartExceptionLog(e));
                GlitchTip.capture(e);
            } catch (RuntimeException e) {
                LOGGER.error("Error while processing DVIDS {} from unit {}: {}", id, unit, smartExceptionLog(e));
                GlitchTip.capture(e);
            }
        }
        ApiPageInfo pi = response.getPageInfo();
        return new DvidsUpdateResult(pi.getResultsPerPage(), pi.getTotalResults(), count, uploadedMedia,
                idsKnownToDvidsApi);
    }

    private DvidsMedia getMediaFromApi(RestTemplate rest, String id, String unit) {
        DvidsMedia media = ofNullable(
                rest.getForObject(assetApiEndpoint.expand(Map.of("api_key", apiKey, "id", id)), ApiAssetResponse.class))
                .orElseThrow(() -> new IllegalArgumentException("No result from DVIDS API for " + id))
                .getResults();
        media.setId(new CompositeMediaId(unit, id));
        return media;
    }

    private static record DvidsUpdateResult(
            int resultsPerPage, int totalResults, int count, Collection<DvidsMedia> uploadedMedia,
            Set<String> idsKnownToDvidsApi) {

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

    private MediaUpdateResult<DvidsMedia> processDvidsMediaUpdate(DvidsMedia media, boolean forceUpdate) {
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            MediaUpdateResult<DvidsMedia> commonUpdate = doCommonUpdate(media, httpClient, null, forceUpdate);
            boolean save = commonUpdate.result();
            if (!media.isIgnored()) {
                if (ignoredCategories.contains(media.getCategory())) {
                    save = mediaService.ignoreMedia(media, "Ignored category: " + media.getCategory());
                } else if (findLicenceTemplates(media, media.getUniqueMetadata()).isEmpty()) {
                    // DVIDS media with VIRIN "O". we can assume it implies a courtesy photo
                    // https://www.dvidshub.net/image/3322521/45th-sw-supports-successful-atlas-v-oa-7-launch
                    save = mediaService.ignoreMedia(media, "No template found (VIRIN O): " + media.getVirin());
                }
            }
            return new MediaUpdateResult<>(media, save, commonUpdate.exception());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public final DvidsMedia refreshAndSave(DvidsMedia media) throws IOException {
        media = refresh(media);
        Exception e = processDvidsMediaUpdate(media, true).exception();
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
            return media.copyDataFrom(
                    getMediaFromApi(new RestTemplate(), media.getId().getMediaId(), media.getId().getRepoId()));
        } catch (IllegalArgumentException e) {
            String message = e.getMessage();
            if (message != null && message.startsWith("No result from DVIDS API for ")) {
                return deleteMedia(media, e);
            } else {
                LOGGER.error(message, e);
                GlitchTip.capture(e);
            }
        } catch (BadRequest e) {
            String message = e.getMessage();
            if (message != null && message.contains(" was not found")) {
                return deleteMedia(media, e);
            } else {
                LOGGER.error(message, e);
                GlitchTip.capture(e);
            }
        } catch (Forbidden e) {
            String message = e.getMessage();
            if (message != null && message.contains(" was found, but is not published.")) {
                return deleteMedia(media, e);
            } else {
                LOGGER.error(message, e);
                GlitchTip.capture(e);
            }
        }
        return media;
    }

    @Override
    public final URL getSourceUrl(DvidsMedia media, FileMetadata metadata, String ext) {
        try {
            String[] typedId = media.getId().getMediaId().split(":");
            return mediaUrl.expand(Map.of("type", typedId[0], "id", typedId[1])).toURL();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    protected final String getSource(DvidsMedia media, FileMetadata metadata) {
        URL sourceUrl = getSourceUrl(media, metadata, metadata.getExtension());
        VirinTemplates t = UnitedStates.getUsVirinTemplates(media.getVirin(), sourceUrl);
        return t != null ? "{{" + t.virinTemplate() + "}}" : sourceUrl.toExternalForm();
    }

    @Override
    protected final String getAuthor(DvidsMedia media, FileMetadata metadata) {
        StringBuilder result = new StringBuilder();
        Matcher m = US_MEDIA_BY.matcher(media.getDescription());
        if (m.matches()) {
            result.append(m.group(1));
        } else if (StringUtils.isNotBlank(media.getBranch()) && !"Joint".equals(media.getBranch())) {
            result.append("U.S. ").append(media.getBranch()).append(' ').append(media.getId().getRepoId())
                    .append(" by ");
        }
        return result.append(media.getCredits()).toString();
    }

    @Override
    protected final Pair<String, Map<String, String>> getWikiFileDesc(DvidsMedia media, FileMetadata metadata) {
        return milim(media, metadata, media.getVirin(), ofNullable(media.getLocation()).map(DvidsLocation::toString),
                ofNullable(media.getRating()));
    }

    @Override
    public Set<String> findCategories(DvidsMedia media, FileMetadata metadata, boolean includeHidden) {
        Set<String> result = super.findCategories(media, metadata, includeHidden);
        result.addAll(media.getKeywordStream().map(KEYWORDS_CATS::get).filter(StringUtils::isNotBlank)
                .flatMap(s -> Arrays.stream(s.split(";"))).collect(toSet()));
        if (metadata.isVideo()) {
            VirinTemplates t = UnitedStates.getUsVirinTemplates(media.getVirin(),
                    media.getUniqueMetadata().getAssetUrl());
            if (t != null && StringUtils.isNotBlank(t.videoCategory())) {
                result.add(t.videoCategory());
            }
        }
        if (includeHidden) {
            result.add("Photographs by Defense Video and Imagery Distribution System");
        }
        return result;
    }

    @Override
    public Set<String> findLicenceTemplates(DvidsMedia media, FileMetadata metadata) {
        Set<String> result = super.findLicenceTemplates(media, metadata);
        VirinTemplates t = UnitedStates.getUsVirinTemplates(media.getVirin(), media.getUniqueMetadata().getAssetUrl());
        if (t != null && StringUtils.isNotBlank(t.pdTemplate())) {
            result.add(t.pdTemplate());
        }
        if (media.getDescription() != null) {
            if (media.getDescription().contains("Space Force photo")) {
                result.add("PD-USGov-Military-Space Force");
                result.remove("PD-USGov-Military-Air Force");
            }
            if (media.getDescription().contains("hoto by SpaceX") || media.getDescription().contains("hoto/SpaceX")) {
                result.add("PD-SpaceX");
            }
            if (media.getDescription().contains("hoto by NASA") || media.getDescription().contains("hoto/NASA")) {
                result.add("PD-USGov-NASA");
            }
        }
        return result;
    }

    @Override
    protected Set<String> getEmojis(DvidsMedia uploadedMedia) {
        Set<String> result = super.getEmojis(uploadedMedia);
        result.add(UnitedStates.getUsMilitaryEmoji(uploadedMedia));
        return result;
    }

    @Override
    protected Set<String> getTwitterAccounts(DvidsMedia uploadedMedia) {
        Set<String> result = super.getTwitterAccounts(uploadedMedia);
        result.add(UnitedStates.getUsMilitaryTwitterAccount(uploadedMedia));
        return result;
    }
}
