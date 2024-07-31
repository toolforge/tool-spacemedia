package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.wikimedia.commons.donvip.spacemedia.utils.CsvHelper.loadCsvMapping;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.ToLongFunction;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpClientErrorException.BadRequest;
import org.springframework.web.client.HttpClientErrorException.Forbidden;
import org.springframework.web.client.HttpClientErrorException.NotFound;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriTemplate;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.CompositeMediaId;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.dvids.DvidsLocation;
import org.wikimedia.commons.donvip.spacemedia.data.domain.dvids.DvidsMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.dvids.DvidsMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.dvids.DvidsMediaType;
import org.wikimedia.commons.donvip.spacemedia.data.domain.dvids.api.ApiPageInfo;
import org.wikimedia.commons.donvip.spacemedia.data.domain.dvids.api.ApiSearchResponse;
import org.wikimedia.commons.donvip.spacemedia.exception.ApiException;
import org.wikimedia.commons.donvip.spacemedia.exception.ImageNotFoundException;
import org.wikimedia.commons.donvip.spacemedia.exception.TooManyResultsException;
import org.wikimedia.commons.donvip.spacemedia.service.MediaService.MediaUpdateResult;
import org.wikimedia.commons.donvip.spacemedia.service.dvids.DvidsMediaProcessorService;
import org.wikimedia.commons.donvip.spacemedia.service.dvids.DvidsService;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractOrgDvidsService.class);

    protected static final Map<String, String> KEYWORDS_CATS = loadCsvMapping(
            AbstractOrgDvidsService.class, "dvids.keywords.csv");

    @Lazy
    @Autowired
    private DvidsMediaProcessorService dvidsProcessor;

    @Lazy
    @Autowired
    private DvidsService dvids;

    @Value("${dvids.media.url}")
    private UriTemplate mediaUrl;

    @Value("${dvids.ignored.categories}")
    private Set<String> ignoredCategories;

    private final DvidsMediaRepository<DvidsMedia> dvidsRepository;

    private final Set<String> countries;

    private final int minYear;

    private final boolean blocklist;

    protected AbstractOrgDvidsService(DvidsMediaRepository<DvidsMedia> repository, String id, Set<String> units,
            Set<String> countries, int minYear, boolean blocklist) {
        super(repository, id, units);
        this.dvidsRepository = repository;
        this.countries = countries;
        this.minYear = minYear;
        this.blocklist = blocklist;
    }

    @Override
    protected Class<DvidsMedia> getMediaClass() {
        return DvidsMedia.class;
    }

    @Override
    protected boolean checkBlocklist() {
        return blocklist;
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
            for (int month = year == Year.now().getValue() ? YearMonth.now().getMonthValue() : 12; month > 0; month--) {
                for (String unit : getRepoIdsFromArgs(args)) {
                    for (String country : countries) {
                        Pair<Integer, Collection<DvidsMedia>> update = updateDvidsMedia(unit, country, year,
                                month, DvidsMediaType.image, idsKnownToDvidsApi);
                        uploadedMedia.addAll(update.getRight());
                        count += update.getLeft();
                        ongoingUpdateMedia(start, count);
                        if (videosEnabled) {
                            update = updateDvidsMedia(unit, country, year, month, DvidsMediaType.video,
                                    idsKnownToDvidsApi);
                            uploadedMedia.addAll(update.getRight());
                            count += update.getLeft();
                            ongoingUpdateMedia(start, count);
                        }
                    }
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

    private Pair<Integer, Collection<DvidsMedia>> updateDvidsMedia(String unit, String country, int year, int month,
            DvidsMediaType type, Set<String> idsKnownToDvidsApi) {
        RestTemplate rest = new RestTemplate();
        List<DvidsMedia> uploadedMedia = new ArrayList<>();
        int count = 0;
        try {
            boolean loop = true;
            int page = 1;
            LocalDateTime start = LocalDateTime.now();
            count = 0;
            LOGGER.info("Fetching DVIDS {}s from unit '{}', country '{}' for year {}-{} (page {}/?)...", type, unit,
                    country, year, month, page);
            while (loop) {
                DvidsUpdateResult ur = doUpdateDvidsMedia(rest,
                        dvids.searchDvidsMediaIds(true, type, unit, country, year, month, page++), unit);
                idsKnownToDvidsApi.addAll(ur.idsKnownToDvidsApi);
                uploadedMedia.addAll(ur.uploadedMedia);
                count += ur.count;
                ongoingUpdateMedia(start, unit, count);
                loop = count < ur.totalResults;
                if (loop) {
                    LOGGER.info("Fetching DVIDS {}s from unit '{}', country '{}' for year {}-{} (page {}/{})...", type,
                            unit, country, year, month, page, ur.numberOfPages());
                }
            }
            LOGGER.info("{}/{} {}s for year {}-{} completed: {} {}s in {}", unit, country, type, year, month, count,
                    type, Utils.durationInSec(start));
        } catch (ApiException | TooManyResultsException exx) {
            LOGGER.error("Error while fetching DVIDS " + type + "s from unit " + unit + " / country " + country, exx);
            GlitchTip.capture(exx);
        }
        return Pair.of(count, uploadedMedia);
    }

    private DvidsUpdateResult doUpdateDvidsMedia(RestTemplate rest, ApiSearchResponse response, String unit) {
        int count = 0;
        LocalDateTime start = LocalDateTime.now();
        List<DvidsMedia> uploadedMedia = new ArrayList<>();
        Set<String> idsKnownToDvidsApi = new HashSet<>();
        for (CompositeMediaId id : response.getResults().stream().map(x -> x.toCompositeMediaId(dvids)).distinct()
                .sorted().toList()) {
            try {
                idsKnownToDvidsApi.add(id.getMediaId());
                Pair<DvidsMedia, Integer> result = dvidsProcessor.processDvidsMedia(
                        () -> repository.findById(id), () -> dvids.getMediaFromApi(id),
                        media -> processDvidsMediaUpdate(media, false).result(), this::shouldUploadAuto,
                        this::uploadWrapped);
                if (result.getValue() > 0) {
                    uploadedMedia.add(result.getKey());
                }
                ongoingUpdateMedia(start, unit, count++);
            } catch (HttpClientErrorException e) {
                LOGGER.error("API error while processing DVIDS {}: {}", id, smartExceptionLog(e));
                GlitchTip.capture(e);
            } catch (DataAccessException e) {
                LOGGER.error("DAO error while processing DVIDS {}: {}", id, smartExceptionLog(e));
                GlitchTip.capture(e);
            } catch (RuntimeException e) {
                LOGGER.error("Error while processing DVIDS {}: {}", id, smartExceptionLog(e));
                GlitchTip.capture(e);
            }
        }
        ApiPageInfo pi = response.getPageInfo();
        return new DvidsUpdateResult(pi.resultsPerPage(), pi.totalResults(), count, uploadedMedia, idsKnownToDvidsApi);
    }


    private static record DvidsUpdateResult(
            int resultsPerPage, int totalResults, int count, Collection<DvidsMedia> uploadedMedia,
            Set<String> idsKnownToDvidsApi) {

        public int numberOfPages() {
            return (int) Math.ceil((double) totalResults / (double) resultsPerPage);
        }
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
            return media.copyDataFrom(dvids.getMediaFromApi(media.getId()));
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

    private boolean byCountry() {
        return Set.of("*").equals(getRepoIds());
    }

    private long count(ToLongFunction<Set<String>> a, ToLongFunction<Set<String>> b, Set<String> set) {
        return byCountry() ? a.applyAsLong(set) : b.applyAsLong(set);
    }

    @Override
    public long countAllMedia() {
        return byCountry() ? dvidsRepository.countByCountry(countries) : repository.count(getRepoIds());
    }

    @Override
    public long countAllMedia(String roc) {
        return isBlank(roc) ? countAllMedia() : count(dvidsRepository::countByCountry, repository::count, Set.of(roc));
    }

    @Override
    public long countIgnored() {
        return byCountry() ? dvidsRepository.countByMetadata_IgnoredTrueByCountry(countries)
                : repository.countByMetadata_IgnoredTrue(getRepoIds());
    }

    @Override
    public long countIgnored(String roc) {
        return isBlank(roc) ? countIgnored()
                : count(dvidsRepository::countByMetadata_IgnoredTrueByCountry, repository::countByMetadata_IgnoredTrue,
                        Set.of(roc));
    }

    @Override
    public long countMissingMedia() {
        return byCountry() ? dvidsRepository.countMissingInCommonsByCountry(countries)
                : repository.countMissingInCommons(getRepoIds());
    }

    @Override
    public long countMissingMedia(String roc) {
        return isBlank(roc) ? countMissingMedia()
                : count(dvidsRepository::countMissingInCommonsByCountry, repository::countMissingInCommons,
                        Set.of(roc));
    }

    @Override
    public long countMissingImages() {
        return byCountry() ? dvidsRepository.countMissingImagesInCommonsByCountry(countries)
                : repository.countMissingImagesInCommons(getRepoIds());
    }

    @Override
    public long countMissingImages(String roc) {
        return isBlank(roc) ? countMissingImages()
                : count(dvidsRepository::countMissingImagesInCommonsByCountry, repository::countMissingImagesInCommons,
                        Set.of(roc));
    }

    @Override
    public long countMissingVideos() {
        return byCountry() ? dvidsRepository.countMissingVideosInCommonsByCountry(countries)
                : repository.countMissingVideosInCommons(getRepoIds());
    }

    @Override
    public long countMissingVideos(String roc) {
        return isBlank(roc) ? countMissingVideos()
                : count(dvidsRepository::countMissingVideosInCommonsByCountry, repository::countMissingVideosInCommons,
                        Set.of(roc));
    }

    @Override
    public long countMissingDocuments() {
        return byCountry() ? dvidsRepository.countMissingDocumentsInCommonsByCountry(countries)
                : repository.countMissingDocumentsInCommons(getRepoIds());
    }

    @Override
    public long countMissingDocuments(String roc) {
        return isBlank(roc) ? countMissingDocuments()
                : count(dvidsRepository::countMissingDocumentsInCommonsByCountry,
                        repository::countMissingDocumentsInCommons, Set.of(roc));
    }

    @Override
    public long countPerceptualHashes() {
        return byCountry() ? dvidsRepository.countByMetadata_PhashNotNullByCountry(countries)
                : repository.countByMetadata_PhashNotNull(getRepoIds());
    }

    @Override
    public long countPerceptualHashes(String roc) {
        return isBlank(roc) ? countPerceptualHashes()
                : count(dvidsRepository::countByMetadata_PhashNotNullByCountry,
                        repository::countByMetadata_PhashNotNull, Set.of(roc));
    }

    @Override
    public long countUploadedMedia() {
        return byCountry() ? dvidsRepository.countUploadedToCommonsByCountry(countries)
                : repository.countUploadedToCommons(getRepoIds());
    }

    @Override
    public long countUploadedMedia(String roc) {
        return isBlank(roc) ? countUploadedMedia()
                : count(dvidsRepository::countUploadedToCommonsByCountry, repository::countUploadedToCommons,
                        Set.of(roc));
    }

    @Override
    public Iterable<DvidsMedia> listAllMedia() {
        return repository.findAll(getRepoIds());
    }

    @Override
    public Page<DvidsMedia> listAllMedia(Pageable page) {
        return repository.findAll(getRepoIds(), page);
    }

    @Override
    public Page<DvidsMedia> listAllMedia(String roc, Pageable page) {
        return isBlank(roc) ? listAllMedia(page) : repository.findAll(Set.of(roc), page);
    }

    @Override
    public List<DvidsMedia> listMissingMedia() {
        return repository.findMissingInCommons(getRepoIds());
    }

    @Override
    public Page<DvidsMedia> listMissingMedia(Pageable page) {
        return repository.findMissingInCommons(getRepoIds(), page);
    }

    @Override
    public Page<DvidsMedia> listMissingMedia(String roc, Pageable page) {
        return isBlank(roc) ? listMissingMedia(page) : repository.findMissingInCommons(Set.of(roc), page);
    }

    @Override
    public Page<DvidsMedia> listMissingImages(Pageable page) {
        return repository.findMissingImagesInCommons(getRepoIds(), page);
    }

    @Override
    public Page<DvidsMedia> listMissingImages(String roc, Pageable page) {
        return isBlank(roc) ? listMissingImages(page) : repository.findMissingImagesInCommons(Set.of(roc), page);
    }

    @Override
    public Page<DvidsMedia> listMissingVideos(Pageable page) {
        return repository.findMissingVideosInCommons(getRepoIds(), page);
    }

    @Override
    public Page<DvidsMedia> listMissingVideos(String roc, Pageable page) {
        return isBlank(roc) ? listMissingVideos(page) : repository.findMissingVideosInCommons(Set.of(roc), page);
    }

    @Override
    public Page<DvidsMedia> listMissingDocuments(Pageable page) {
        return repository.findMissingDocumentsInCommons(getRepoIds(), page);
    }

    @Override
    public Page<DvidsMedia> listMissingDocuments(String roc, Pageable page) {
        return isBlank(roc) ? listMissingDocuments(page) : repository.findMissingDocumentsInCommons(Set.of(roc), page);
    }

    @Override
    public List<DvidsMedia> listMissingMediaByDate(LocalDate date, String roc) {
        return repository.findMissingInCommonsByPublicationDate(isBlank(roc) ? getRepoIds() : Set.of(roc), date);
    }

    @Override
    public List<DvidsMedia> listMissingMediaByMonth(YearMonth month, String roc) {
        return repository.findMissingInCommonsByPublicationMonth(isBlank(roc) ? getRepoIds() : Set.of(roc), month);
    }

    @Override
    public List<DvidsMedia> listMissingMediaByYear(Year year, String roc) {
        return repository.findMissingInCommonsByPublicationYear(isBlank(roc) ? getRepoIds() : Set.of(roc), year);
    }

    @Override
    public List<DvidsMedia> listMissingMediaByTitle(String title, String roc) {
        return repository.findMissingInCommonsByTitle(isBlank(roc) ? getRepoIds() : Set.of(roc), title);
    }

    @Override
    public Page<DvidsMedia> listHashedMedia(Pageable page) {
        return repository.findByMetadata_PhashNotNull(getRepoIds(), page);
    }

    @Override
    public Page<DvidsMedia> listHashedMedia(String roc, Pageable page) {
        return isBlank(roc) ? listHashedMedia(page) : repository.findByMetadata_PhashNotNull(Set.of(roc), page);
    }

    @Override
    public List<DvidsMedia> listUploadedMedia() {
        return repository.findUploadedToCommons(getRepoIds());
    }

    @Override
    public Page<DvidsMedia> listUploadedMedia(Pageable page) {
        return repository.findUploadedToCommons(getRepoIds(), page);
    }

    @Override
    public List<DvidsMedia> listUploadedMediaByDate(LocalDate date) {
        return repository.findUploadedToCommonsByPublicationDate(getRepoIds(), date);
    }

    @Override
    public Page<DvidsMedia> listUploadedMedia(String roc, Pageable page) {
        return isBlank(roc) ? listUploadedMedia(page) : repository.findUploadedToCommons(Set.of(roc), page);
    }

    @Override
    public List<DvidsMedia> listDuplicateMedia() {
        return repository.findDuplicateInCommons(getRepoIds());
    }

    @Override
    public List<DvidsMedia> listIgnoredMedia() {
        return repository.findByMetadata_IgnoredTrue(getRepoIds());
    }

    @Override
    public Page<DvidsMedia> listIgnoredMedia(Pageable page) {
        return repository.findByMetadata_IgnoredTrue(getRepoIds(), page);
    }

    @Override
    public Page<DvidsMedia> listIgnoredMedia(String roc, Pageable page) {
        return isBlank(roc) ? listIgnoredMedia(page) : repository.findByMetadata_IgnoredTrue(Set.of(roc), page);
    }
}
