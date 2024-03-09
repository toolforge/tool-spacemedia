package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import static java.util.Arrays.copyOfRange;
import static java.util.Arrays.stream;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.strip;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.durationInSec;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.newHttpGet;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.newURL;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.time.temporal.Temporal;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jsoup.HttpStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.client.HttpClientErrorException.NotFound;
import org.springframework.web.client.RestClientException;
import org.wikidata.wdtk.wikibaseapi.apierrors.MediaWikiApiErrorException;
import org.wikimedia.commons.donvip.spacemedia.data.domain.Statistics;
import org.wikimedia.commons.donvip.spacemedia.data.domain.UploadMode;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.CompositeMediaId;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadataRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.ImageDimensions;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.Media;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.MediaRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.RuntimeData;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.RuntimeDataRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.WithKeywords;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.WithLatLon;
import org.wikimedia.commons.donvip.spacemedia.exception.IgnoreException;
import org.wikimedia.commons.donvip.spacemedia.exception.ImageDecodingException;
import org.wikimedia.commons.donvip.spacemedia.exception.ImageNotFoundException;
import org.wikimedia.commons.donvip.spacemedia.exception.ImageUploadForbiddenException;
import org.wikimedia.commons.donvip.spacemedia.exception.TooManyResultsException;
import org.wikimedia.commons.donvip.spacemedia.exception.UploadException;
import org.wikimedia.commons.donvip.spacemedia.service.AbstractSocialMediaService;
import org.wikimedia.commons.donvip.spacemedia.service.CategorizationService;
import org.wikimedia.commons.donvip.spacemedia.service.ExecutionMode;
import org.wikimedia.commons.donvip.spacemedia.service.GeometryService;
import org.wikimedia.commons.donvip.spacemedia.service.GoogleTranslateService;
import org.wikimedia.commons.donvip.spacemedia.service.MediaService;
import org.wikimedia.commons.donvip.spacemedia.service.MediaService.MediaUpdateResult;
import org.wikimedia.commons.donvip.spacemedia.service.RemoteService;
import org.wikimedia.commons.donvip.spacemedia.service.SearchService;
import org.wikimedia.commons.donvip.spacemedia.service.UrlResolver;
import org.wikimedia.commons.donvip.spacemedia.service.mastodon.MastodonService;
import org.wikimedia.commons.donvip.spacemedia.service.osm.NominatimService;
import org.wikimedia.commons.donvip.spacemedia.service.osm.NominatimService.Address;
import org.wikimedia.commons.donvip.spacemedia.service.twitter.TwitterService;
import org.wikimedia.commons.donvip.spacemedia.service.wikimedia.CommonsService;
import org.wikimedia.commons.donvip.spacemedia.service.wikimedia.SdcStatements;
import org.wikimedia.commons.donvip.spacemedia.service.wikimedia.WikidataItem;
import org.wikimedia.commons.donvip.spacemedia.service.wikimedia.WikidataService;
import org.wikimedia.commons.donvip.spacemedia.utils.Emojis;
import org.wikimedia.commons.donvip.spacemedia.utils.ImageUtils;
import org.wikimedia.commons.donvip.spacemedia.utils.UnitedStates;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Superclass of orgs services.
 *
 * @param <T>  the media type the repository manages
 */
public abstract class AbstractOrgService<T extends Media>
        implements Comparable<AbstractOrgService<T>>, Org<T> {

    private static final int LOTS_OF_MP = 150_000_000;

    protected static final String EN = "en";
    protected static final String ES = "es";

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractOrgService.class);

    private static final Pattern PATTERN_SHORT = Pattern
            .compile(
                    "(?:https?://)?(?:bit.ly/[0-9a-zA-Z]{6,7}|youtu.be/[\\w\\-]{11}|flic.kr/[ps]/[0-9a-zA-Z]{6,10}|fb.me/e/[0-9a-zA-Z]{9}|ow.ly/[0-9a-zA-Z]{5}|tinyurl.com/[0-9a-zA-Z]{7,8}|goo.gl/[0-9a-zA-Z]{5,6})");

    private static final Pattern PATTERN_TWITTER_SEARCH = Pattern
            .compile("<a href=\"https://twitter.com/search?[^\"]+\">([^<]*)</a>");

    private static final Pattern PATTERN_UNINTERESTING_TITLE = Pattern.compile("Picture \\d+");

    private static final Set<String> PD_US = Set.of("PD-US", "PD-NASA", "PD-Hubble", "PD-Webb");

    private static final Map<String, String> LICENCES = Map.ofEntries(e("YouTube CC-BY", "Q14947546"),
            e("Cc-by-2.0", "Q19125117"), e("Cc-by-4.0", "Q20007257"), e("Cc-by-sa-2.0", "Q19068220"),
            e("Cc-zero", "Q6938433"),
            e("DLR-License", "Q62619894"), e("ESA|", "Q26259495"), e("ESO", "Q20007257"), e("IAU", "Q20007257"),
            e("KOGL", "Q12584618"), e("NOIRLab", "Q20007257"), e("ESA-Hubble", "Q20007257"),
            e("ESA-Webb", "Q20007257"));

    private static final List<String> COURTESY_SPELLINGS = List.of("courtesy", "courtsey", "contributed photo");

    protected final MediaRepository<T> repository;

    private final String id;

    private final Set<String> repoIds;

    @Autowired
    private FileMetadataRepository metadataRepository;
    @Autowired
    protected RuntimeDataRepository runtimeDataRepository;
    @Autowired
    protected ObjectMapper jackson;
    @Lazy
    @Autowired
    protected MediaService mediaService;
    @Lazy
    @Autowired
    protected CategorizationService categorizationService;
    @Lazy
    @Autowired
    protected CommonsService commonsService;
    @Lazy
    @Autowired
    protected WikidataService wikidata;
    @Lazy
    @Autowired
    private SearchService searchService;
    @Lazy
    @Autowired
    private RemoteService remoteService;
    @Lazy
    @Autowired
    private GoogleTranslateService translateService;
    @Lazy
    @Autowired
    private List<AbstractSocialMediaService<?, ?>> socialMediaServices;
    @Lazy
    @Autowired
    protected GeometryService geometry;
    @Lazy
    @Autowired
    protected NominatimService nominatim;

    @Autowired
    private Environment env;

    @Autowired
    @PersistenceContext(unitName = "domain")
    private EntityManager entityManager;

    @Value("${courtesy.ok}")
    private Set<String> courtesyOk;

    @Value("${execution.mode}")
    protected ExecutionMode executionMode;

    @Value("${upload.auto.min.year}")
    private int minYearUploadAuto;

    @Value("${audios.enabled}")
    protected boolean audiosEnabled;

    @Value("${videos.enabled}")
    protected boolean videosEnabled;

    private UploadMode uploadMode;

    protected AbstractOrgService(MediaRepository<T> repository, String id, Set<String> repoIds) {
        this.repository = requireNonNull(repository);
        this.id = requireNonNull(id);
        this.repoIds = new TreeSet<>(repoIds);
    }

    @PostConstruct
    void init() throws IOException {
        uploadMode = UploadMode.valueOf(
                env.getProperty(id + ".upload", String.class, UploadMode.DISABLED.name())
                    .toUpperCase(Locale.ENGLISH));
        LOGGER.info("{} upload mode: {}", id, uploadMode);
    }

    /**
     * Checks that given Commons categories exist and are not redirected. Otherwise, log a warning.
     *
     * @param categories Commons categories to check
     */
    protected void checkCommonsCategories(Map<String, String> categories) {
        Set<String> problematicCategories = commonsService.findNonUpToDateCategories(categories.values());
        if (!problematicCategories.isEmpty()) {
            LOGGER.warn("problematicCategories : {}", problematicCategories);
        }
    }

    @Override
    public void evictCaches() {
        LOGGER.info("Evicting caches of {}...", getId());
        repository.evictCaches();
    }

    @Override
    public long countAllMedia() {
        return repository.count(getRepoIds());
    }

    @Override
    public long countAllMedia(String repo) {
        return isBlank(repo) ? countAllMedia() : repository.count(Set.of(repo));
    }

    @Override
    public long countIgnored() {
        return repository.countByMetadata_IgnoredTrue(getRepoIds());
    }

    @Override
    public long countIgnored(String repo) {
        return isBlank(repo) ? countIgnored() : repository.countByMetadata_IgnoredTrue(Set.of(repo));
    }

    @Override
    public long countMissingMedia() {
        return repository.countMissingInCommons(getRepoIds());
    }

    @Override
    public long countMissingMedia(String repo) {
        return isBlank(repo) ? countMissingMedia() : repository.countMissingInCommons(Set.of(repo));
    }

    @Override
    public long countMissingImages() {
        return repository.countMissingImagesInCommons(getRepoIds());
    }

    @Override
    public long countMissingImages(String repo) {
        return isBlank(repo) ? countMissingImages() : repository.countMissingImagesInCommons(Set.of(repo));
    }

    @Override
    public long countMissingVideos() {
        return repository.countMissingVideosInCommons(getRepoIds());
    }

    @Override
    public long countMissingVideos(String repo) {
        return isBlank(repo) ? countMissingVideos() : repository.countMissingVideosInCommons(Set.of(repo));
    }

    @Override
    public long countMissingDocuments() {
        return repository.countMissingDocumentsInCommons(getRepoIds());
    }

    @Override
    public long countMissingDocuments(String repo) {
        return isBlank(repo) ? countMissingDocuments() : repository.countMissingDocumentsInCommons(Set.of(repo));
    }

    @Override
    public long countPerceptualHashes() {
        return repository.countByMetadata_PhashNotNull(getRepoIds());
    }

    @Override
    public long countPerceptualHashes(String repo) {
        return isBlank(repo) ? countPerceptualHashes() : repository.countByMetadata_PhashNotNull(Set.of(repo));
    }

    @Override
    public long countUploadedMedia() {
        return repository.countUploadedToCommons(getRepoIds());
    }

    @Override
    public long countUploadedMedia(String repo) {
        return isBlank(repo) ? countUploadedMedia() : repository.countUploadedToCommons(Set.of(repo));
    }

    @Override
    public Iterable<T> listAllMedia() {
        return repository.findAll(getRepoIds());
    }

    @Override
    public Page<T> listAllMedia(Pageable page) {
        return repository.findAll(getRepoIds(), page);
    }

    @Override
    public Page<T> listAllMedia(String repo, Pageable page) {
        return isBlank(repo) ? listAllMedia(page) : repository.findAll(Set.of(repo), page);
    }

    @Override
    public List<T> listMissingMedia() {
        return repository.findMissingInCommons(getRepoIds());
    }

    @Override
    public Page<T> listMissingMedia(Pageable page) {
        return repository.findMissingInCommons(getRepoIds(), page);
    }

    @Override
    public Page<T> listMissingMedia(String repo, Pageable page) {
        return isBlank(repo) ? listMissingMedia(page) : repository.findMissingInCommons(Set.of(repo), page);
    }

    @Override
    public Page<T> listMissingImages(Pageable page) {
        return repository.findMissingImagesInCommons(getRepoIds(), page);
    }

    @Override
    public Page<T> listMissingImages(String repo, Pageable page) {
        return isBlank(repo) ? listMissingImages(page) : repository.findMissingImagesInCommons(Set.of(repo), page);
    }

    @Override
    public Page<T> listMissingVideos(Pageable page) {
        return repository.findMissingVideosInCommons(getRepoIds(), page);
    }

    @Override
    public Page<T> listMissingVideos(String repo, Pageable page) {
        return isBlank(repo) ? listMissingVideos(page) : repository.findMissingVideosInCommons(Set.of(repo), page);
    }

    @Override
    public Page<T> listMissingDocuments(Pageable page) {
        return repository.findMissingDocumentsInCommons(getRepoIds(), page);
    }

    @Override
    public Page<T> listMissingDocuments(String repo, Pageable page) {
        return isBlank(repo) ? listMissingDocuments(page)
                : repository.findMissingDocumentsInCommons(Set.of(repo), page);
    }

    @Override
    public List<T> listMissingMediaByDate(LocalDate date, String repo) {
        return repository.findMissingInCommonsByPublicationDate(isBlank(repo) ? getRepoIds() : Set.of(repo), date);
    }

    @Override
    public List<T> listMissingMediaByMonth(YearMonth month, String repo) {
        return repository.findMissingInCommonsByPublicationMonth(isBlank(repo) ? getRepoIds() : Set.of(repo), month);
    }

    @Override
    public List<T> listMissingMediaByYear(Year year, String repo) {
        return repository.findMissingInCommonsByPublicationYear(isBlank(repo) ? getRepoIds() : Set.of(repo), year);
    }

    @Override
    public List<T> listMissingMediaByTitle(String title, String repo) {
        return repository.findMissingInCommonsByTitle(isBlank(repo) ? getRepoIds() : Set.of(repo), title);
    }

    @Override
    public Page<T> listHashedMedia(Pageable page) {
        return repository.findByMetadata_PhashNotNull(getRepoIds(), page);
    }

    @Override
    public Page<T> listHashedMedia(String repo, Pageable page) {
        return isBlank(repo) ? listHashedMedia(page) : repository.findByMetadata_PhashNotNull(Set.of(repo), page);
    }

    @Override
    public List<T> listUploadedMedia() {
        return repository.findUploadedToCommons(getRepoIds());
    }

    @Override
    public Page<T> listUploadedMedia(Pageable page) {
        return repository.findUploadedToCommons(getRepoIds(), page);
    }

    @Override
    public Page<T> listUploadedMedia(String repo, Pageable page) {
        return isBlank(repo) ? listUploadedMedia(page) : repository.findUploadedToCommons(Set.of(repo), page);
    }

    @Override
    public List<T> listDuplicateMedia() {
        return repository.findDuplicateInCommons(getRepoIds());
    }

    @Override
    public List<T> listIgnoredMedia() {
        return repository.findByMetadata_IgnoredTrue(getRepoIds());
    }

    @Override
    public Page<T> listIgnoredMedia(Pageable page) {
        return repository.findByMetadata_IgnoredTrue(getRepoIds(), page);
    }

    @Override
    public Page<T> listIgnoredMedia(String repo, Pageable page) {
        return isBlank(repo) ? listIgnoredMedia(page) : repository.findByMetadata_IgnoredTrue(Set.of(repo), page);
    }

    @Override
    public List<T> searchMedia(String q) {
        searchService.checkSearchEnabled();
        throw new UnsupportedOperationException();
    }

    @Override
    public Page<T> searchMedia(String q, Pageable page) {
        searchService.checkSearchEnabled();
        throw new UnsupportedOperationException();
    }

    /**
     * Returns an unique identifier used for REST controllers and database entries.
     *
     * @return an unique identifier specified by implementations
     */
    @Override
    public String getId() {
        return id;
    }

    public Set<String> getRepoIds() {
        return repoIds;
    }

    protected Set<String> getRepoIdsFromArgs(String[] args) {
        if (args != null && args.length >= 1 && !isBlank(args[args.length - 1])) {
            Set<String> ids = stream(args[args.length - 1].split(",")).filter(repoIds::contains).collect(toSet());
            if (!ids.isEmpty()) {
                return ids;
            }
        }
        return repoIds;
    }

    protected static LocalDate getLocalDateFromArgs(String[] args) {
        if (args != null && args.length >= 1 && !isBlank(args[args.length - 1])) {
            try {
                return LocalDate.parse(args[args.length - 1]);
            } catch (DateTimeParseException e) {
                LOGGER.trace("Can't parse date", e);
            }
        }
        return LocalDate.now();
    }

    public String getUiRepoId(String repoId) {
        return repoId;
    }

    public boolean isMultiRepo() {
        return repoIds.size() > 1;
    }

    protected final LocalDateTime startUpdateMedia() {
        Thread.currentThread().setName("media-update-" + getId());
        LOGGER.info("Starting {} medias update...", getName());
        RuntimeData runtimeData = getRuntimeData();
        runtimeData.setLastUpdateStart(LocalDateTime.now());
        return runtimeDataRepository.save(runtimeData).getLastUpdateStart();
    }

    protected final void ongoingUpdateMedia(LocalDateTime start, int count) {
        ongoingUpdateMedia(start, getId(), count);
    }

    protected final void ongoingUpdateMedia(LocalDateTime start, String who, int count) {
        if (LOGGER.isInfoEnabled() && count > 0 && count % 1000 == 0) {
            Duration durationInSec = durationInSec(start);
            LOGGER.info("Processed {} {} media in {} ({} media/s) - ({} ms/media)", count, who, durationInSec,
                    String.format("%.2f", (double) count / durationInSec.getSeconds()),
                    String.format("%d", durationInSec.getSeconds() * 1000 / count));
        }
    }

    protected final List<FileMetadata> allMetadata(Collection<T> uploadedMedia) {
        return uploadedMedia.stream().flatMap(Media::getMetadataStream).toList();
    }

    protected final void endUpdateMedia(int count, Collection<T> uploadedMedia, LocalDateTime start) {
        endUpdateMedia(count, uploadedMedia, start, true);
    }

    protected final void endUpdateMedia(int count, Collection<T> uploadedMedia, LocalDateTime start,
            boolean postTweet) {
        endUpdateMedia(count, uploadedMedia, allMetadata(uploadedMedia), start, postTweet);
    }

    protected final void endUpdateMedia(int count, Collection<T> uploadedMedia, Collection<FileMetadata> uploadedMetadata,
            LocalDateTime start) {
        endUpdateMedia(count, uploadedMedia, uploadedMetadata, start, true);
    }

    protected final void endUpdateMedia(int count, Collection<T> uploadedMedia,
            Collection<FileMetadata> uploadedMetadata, LocalDateTime start, boolean postTweet) {
        endUpdateMedia(count, uploadedMedia, uploadedMetadata, start, LocalDate.now().minusDays(7), postTweet);
    }

    protected final void endUpdateMedia(int count, Collection<T> uploadedMedia, Collection<FileMetadata> uploadedMetadata,
            LocalDateTime start, LocalDate newDoNotFetchEarlierThan) {
        endUpdateMedia(count, uploadedMedia, uploadedMetadata, start, newDoNotFetchEarlierThan, true);
    }

    protected final void endUpdateMedia(int count, Collection<T> uploadedMedia, Collection<FileMetadata> uploadedMetadata,
            LocalDateTime start, LocalDate newDoNotFetchEarlierThan, boolean postTweet) {
        RuntimeData runtimeData = getRuntimeData();
        LocalDateTime end = LocalDateTime.now();
        runtimeData.setLastUpdateEnd(end);
        Duration lastUpdateDuration = Duration.between(start, end);
        runtimeData.setLastUpdateDuration(lastUpdateDuration);
        runtimeData.setLastUpdateDurationMin(lastUpdateDuration.toMinutes());
        if (uploadedMetadata.isEmpty()) {
            runtimeData.setLastUpdateDurationWithoutUploadsMin(lastUpdateDuration.toMinutes());
        } else {
            runtimeData.setLastUpdateDurationWithUploadsMin(lastUpdateDuration.toMinutes());
        }
        runtimeData.setDoNotFetchEarlierThan(newDoNotFetchEarlierThan);
        LOGGER.info("{} medias update completed: {} medias in {}", getName(), count,
                runtimeDataRepository.save(runtimeData).getLastUpdateDuration());
        if (postTweet) {
            postSocialMedia(uploadedMedia, uploadedMetadata);
        }
    }

    protected Collection<String> getStringsToRemove(T media) {
        // To be overriden if special strings have to be removed from description
        return List.of();
    }

    protected final void postSocialMedia(Collection<? extends T> uploadedMedia, Collection<FileMetadata> uploadedMetadata) {
        if (!uploadedMedia.isEmpty()) {
            LOGGER.info("Uploaded media: {} ({})", uploadedMedia.size(),
                    uploadedMedia.stream().map(Media::getIdUsedInOrg).toList());
            socialMediaServices.forEach(socialMedia -> {
                try {
                    Set<String> accounts = getSocialMediaAccounts(socialMedia, uploadedMedia);
                    if (accounts.isEmpty()) {
                        accounts = Set.of(getName());
                    }
                    socialMedia.postStatus(uploadedMedia, uploadedMetadata, getEmojis(uploadedMedia), accounts);
                } catch (IOException e) {
                    LOGGER.error("Failed to post status", e);
                }
            });
        }
    }

    protected final Set<String> getEmojis(Collection<? extends T> uploadedMedia) {
        return uploadedMedia.stream().flatMap(media -> getEmojis(media).stream()).collect(toSet());
    }

    protected final Set<String> getSocialMediaAccounts(AbstractSocialMediaService<?, ?> socialMedia,
            Collection<? extends T> uploadedMedia) {
        if (socialMedia instanceof MastodonService) {
            return uploadedMedia.stream().flatMap(media -> getMastodonAccounts(media).stream()).collect(toSet());
        } else if (socialMedia instanceof TwitterService) {
            return uploadedMedia.stream().flatMap(media -> getTwitterAccounts(media).stream()).collect(toSet());
        }
        return Set.of();
    }

    protected Set<String> getEmojis(T uploadedMedia) {
        Set<String> result = new HashSet<>();
        String description = uploadedMedia.getDescription();
        if (description != null) {
            if (List.of("astronaut", "cosmonaut", "spationaut").stream().anyMatch(description::contains)) {
                result.add(Emojis.ASTRONAUT);
            }
            if (List.of("lift off", "lifts off").stream().anyMatch(description::contains)) {
                result.add(Emojis.ROCKET);
            }
        }
        if (uploadedMedia instanceof WithKeywords kw) {
            result.addAll(AbstractSocialMediaService.getEmojis(kw.getKeywords()));
        }
        return result;
    }

    protected Set<String> getMastodonAccounts(T uploadedMedia) {
        return Set.of();
    }

    protected Set<String> getTwitterAccounts(T uploadedMedia) {
        return Set.of();
    }

    @Override
    public Statistics getStatistics(boolean details) {
        Statistics stats = new Statistics(getName(), getId(), countAllMedia(), countUploadedMedia(), countIgnored(),
                countMissingImages(), countMissingVideos(), countMissingDocuments(), countPerceptualHashes(),
                getRuntimeData().getLastUpdateEnd());
        if (details && getRepoIds().size() > 1) {
            stats.setDetails(getRepoIds().stream().map(this::getStatistics).sorted().toList());
        }
        return stats;
    }

    private Statistics getStatistics(String alias) {
        Set<String> singleton = Collections.singleton(alias);
        return new Statistics(alias, alias, repository.count(singleton), repository.countUploadedToCommons(singleton),
                repository.countByMetadata_IgnoredTrue(singleton), repository.countMissingImagesInCommons(singleton),
                repository.countMissingVideosInCommons(singleton), repository.countMissingDocumentsInCommons(singleton),
                repository.countByMetadata_PhashNotNull(singleton),
                null);
    }

    protected final void problem(URL problematicUrl, Throwable t) {
        problem(problematicUrl, t.toString());
    }

    protected final void problem(String problematicUrl, Throwable t) {
        problem(problematicUrl, t.toString());
    }

    protected final void problem(String problematicUrl, String errorMessage) {
        problem(newURL(problematicUrl), errorMessage);
    }

    protected final void problem(URL problematicUrl, String errorMessage) {
        LOGGER.warn("{} => {}", problematicUrl, errorMessage);
    }

    protected final T findBySomeSha1OrThrow(String sha1, Function<String, List<T>> finder, boolean throwIfNotFound)
            throws TooManyResultsException {
        List<T> result = finder.apply(sha1);
        if (isEmpty(result)) {
            if (throwIfNotFound) {
                throw new ImageNotFoundException(sha1);
            } else {
                return null;
            }
        }
        if (result.size() > 1) {
            throw new TooManyResultsException("Several images found for " + sha1);
        }
        return result.get(0);
    }

    protected T findBySha1OrThrow(String sha1, boolean throwIfNotFound) throws TooManyResultsException {
        return findBySomeSha1OrThrow(sha1, repository::findByMetadata_Sha1, throwIfNotFound);
    }

    @Override
    public T getById(String id) throws ImageNotFoundException {
        LOGGER.info("Looking for media by id: {}", id);
        return repository.findById(new CompositeMediaId(id)).orElseThrow(() -> new ImageNotFoundException(id));
    }

    @Override
    public void deleteById(String id) throws ImageNotFoundException {
        repository.deleteById(new CompositeMediaId(id));
    }

    @Override
    public T refreshAndSaveById(String id) throws ImageNotFoundException, IOException {
        return refreshAndSave(getById(id));
    }

    @Override
    public T refreshAndSave(T media) throws IOException {
        T refreshedMedia = null;
        try {
            refreshedMedia = refresh(media);
        } catch (NotFound e) {
            LOGGER.warn("Refresh of {} failed: {}", media, e.getMessage());
        } catch (HttpStatusException e) {
            if (e.getStatusCode() != 404) {
                throw e;
            }
            LOGGER.warn("Refresh of {} failed: {}", media, e.getMessage());
        }
        if (refreshedMedia != null) {
            doCommonUpdate(refreshedMedia, true);
            return saveMedia(refreshedMedia);
        } else {
            deleteMedia(media, "refresh did not find media anymore");
            return null;
        }
    }

    protected abstract T refresh(T media) throws IOException;

    @Override
    public T saveMedia(T media) {
        LOGGER.info("Saving {}", media);
        T result = repository.save(media);
        checkRemoteMedia(result);
        return result;
    }

    protected final void checkRemoteMedia(T media) {
        if (executionMode == ExecutionMode.REMOTE
                && remoteService.getMedia(getId(), media.getId().toString(), media.getClass()) == null) {
            remoteService.saveMedia(getId(), media);
        } else if (executionMode == ExecutionMode.LOCAL) {
            evictRemoteCaches();
        }
    }

    protected final void evictRemoteCaches() {
        try {
            remoteService.evictCaches(getId());
        } catch (RestClientException e) {
            LOGGER.warn("Remote instance returned: {}", e.getMessage());
        }
    }

    protected final T saveMediaOrCheckRemote(boolean save, T media) {
        if (save) {
            T savedMedia = saveMedia(media);
            checkRemoteMedia(savedMedia);
            return savedMedia;
        } else {
            return media;
        }
    }

    protected final T deleteMedia(T media, Exception e) {
        return deleteMedia(media, e.getMessage());
    }

    protected final T deleteMedia(T media, String message) {
        LOGGER.warn("Deleting {} ({})", media, message);
        repository.delete(media);
        return media;
    }

    public final boolean isUploadEnabled() {
        return uploadMode != UploadMode.DISABLED;
    }

    @Override
    public T uploadAndSaveById(String id, boolean isManual) throws UploadException, TooManyResultsException {
        return saveMedia(upload(getById(id), false, isManual).getLeft());
    }

    @Override
    public T uploadAndSaveBySha1(String sha1, boolean isManual) throws UploadException, TooManyResultsException {
        return saveMedia(upload(findBySha1OrThrow(sha1, true), true, isManual).getLeft());
    }

    @Override
    public List<T> uploadAndSaveByDate(LocalDate date, String repo, Predicate<Media> predicate, boolean isManual)
            throws UploadException {
        return uploadAndSaveMedias(listMissingMediaByDate(date, repo).stream().filter(predicate), isManual);
    }

    @Override
    public List<T> uploadAndSaveByMonth(YearMonth month, String repo, Predicate<Media> predicate, boolean isManual)
            throws UploadException {
        return uploadAndSaveMedias(listMissingMediaByMonth(month, repo).stream().filter(predicate), isManual);
    }

    @Override
    public List<T> uploadAndSaveByYear(Year year, String repo, Predicate<Media> predicate, boolean isManual)
            throws UploadException {
        return uploadAndSaveMedias(listMissingMediaByYear(year, repo).stream().filter(predicate), isManual);
    }

    @Override
    public List<T> uploadAndSaveByTitle(String title, String repo, Predicate<Media> predicate, boolean isManual)
            throws UploadException {
        return uploadAndSaveMedias(listMissingMediaByTitle(title, repo).stream().filter(predicate), isManual);
    }

    private List<T> uploadAndSaveMedias(Stream<T> medias, boolean isManual) {
        return medias.map(media -> {
            try {
                return saveMedia(upload(media, false, isManual).getLeft());
            } catch (UploadException e) {
                LOGGER.error("Failed to upload {}", media, e);
                return null;
            }
        }).filter(Objects::nonNull).toList();
    }

    protected final Triple<T, Collection<FileMetadata>, Integer> uploadWrapped(T media) {
        try {
            return upload(media, true, false);
        } catch (UploadException e) {
            LOGGER.error("Failed to upload {}", media, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public Triple<T, Collection<FileMetadata>, Integer> upload(T media, boolean checkUnicity, boolean isManual)
            throws UploadException {
        if (!isUploadEnabled()) {
            throw new ImageUploadForbiddenException("Upload is not enabled for " + getClass().getSimpleName());
        }
        try {
            checkUploadPreconditions(media, checkUnicity, isManual);
            List<FileMetadata> uploaded = new ArrayList<>();
            return Triple.of(media, uploaded, doUpload(media, checkUnicity, uploaded, isManual));
        } catch (RuntimeException | URISyntaxException e) {
            throw new UploadException(e);
        }
    }

    protected int doUpload(T media, boolean checkUnicity, Collection<FileMetadata> uploaded, boolean isManual) {
        int count = 0;
        for (FileMetadata metadata : media.getMetadataStream().filter(FileMetadata::shouldUpload).toList()) {
            try {
                count += doUpload(media, metadata, checkUnicity, uploaded, isManual);
            } catch (ImageUploadForbiddenException | UploadException | IOException e) {
                LOGGER.warn("File {} not uploaded: {}", metadata, e.getMessage());
            } catch (IgnoreException e) {
                mediaService.ignoreAndSaveMetadata(metadata, e.getMessage(), e);
            }
        }
        return count;
    }

    protected final int doUpload(T media, FileMetadata metadata, boolean checkUnicity, Collection<FileMetadata> uploaded,
            boolean isManual) throws IOException, UploadException {
        boolean mediaLooksOk = metadata != null && metadata.getAssetUrl() != null;
        if (mediaLooksOk && new UploadContext<>(media, metadata, getUploadMode(),
                minYearUploadAuto, this::isPermittedFileType, isManual).shouldUpload()) {
            checkUploadPreconditions(media, metadata, checkUnicity);
            List<String> smallerFiles = mediaService.findSmallerCommonsFilesWithIdAndPhash(media, metadata);
            URL downloadUrl = getUrlResolver().resolveDownloadUrl(media, metadata);
            if (!smallerFiles.isEmpty()) {
                LOGGER.info(
                        "Found existing smaller files with same id and perceptual hash on Commons, replacing them: {}",
                        smallerFiles);
                for (String smallerFile : smallerFiles) {
                    commonsService.uploadExistingFile(smallerFile, downloadUrl, metadata.getSha1());
                }
                metadata.setCommonsFileNames(new HashSet<>(smallerFiles));
            } else {
                Pair<String, Map<String, String>> codeAndLegends = getWikiCode(media, metadata);
                String uploadedFilename = commonsService.uploadNewFile(codeAndLegends.getLeft(),
                        media.getUploadTitle(metadata), metadata.getFileExtension(), downloadUrl, metadata.getSha1());
                metadata.setCommonsFileNames(new HashSet<>(Set.of(uploadedFilename)));
                editStructuredDataContent(uploadedFilename, codeAndLegends.getRight(), media, metadata);
            }
            uploaded.add(metadataRepository.save(metadata));
            evictRemoteCaches();
            return 1;
        } else {
            if (mediaLooksOk) {
                LOGGER.info("Upload not done for {} / {}. Upload mode: {}. Ignored: {}. Permitted file type: {}",
                        media.getId(), metadata, getUploadMode(), media.isIgnored(), isPermittedFileType(metadata));
            }
            return 0;
        }
    }

    protected void editStructuredDataContent(String uploadedFilename, Map<String, String> legends, T media,
            FileMetadata metadata) {
        SdcStatements statements = getStatements(media, metadata);
        try {
            commonsService.editStructuredDataContent(uploadedFilename, legends, statements);
        } catch (MediaWikiApiErrorException | IOException | RuntimeException e) {
            LOGGER.error("Unable to add SDC data: {}", statements, e);
        }
    }

    protected SdcStatements getStatements(T media, FileMetadata metadata) {
        SdcStatements result = new SdcStatements();
        // Source: file available on the internet
        result.put("P7482", Pair.of("Q74228490",
                new TreeMap<>(Map.of("P973", getSourceUrl(media, metadata), "P2699", metadata.getAssetUrl()))));
        // Licences
        Set<String> licences = findLicenceTemplates(media, metadata);
        if (PD_US.stream().anyMatch(pd -> licences.stream().anyMatch(l -> l.startsWith(pd)))) {
            result.put("P6216", Pair.of("Q19652", new TreeMap<>(Map.of("P459", "Q60671452", "P1001", "Q30"))));
        } else {
            result.put("P6216", Pair.of("Q50423863", null));
            LICENCES.entrySet().stream().filter(e -> licences.stream().anyMatch(l -> l.startsWith(e.getKey())))
                    .map(Entry::getValue).distinct().forEach(l -> result.put("P275", Pair.of(l, null)));
        }
        // MIME type
        ofNullable(metadata.getMime()).ifPresent(mime -> result.put("P1163", Pair.of(mime, null)));
        // Hashes
        ofNullable(metadata.getSha1()).ifPresent(h -> result.put("P4092", Pair.of(h, Map.of("P459", "Q13414952"))));
        ofNullable(metadata.getPhash()).ifPresent(h -> result.put("P9310", Pair.of(h, Map.of("P459", "Q118189277"))));
        // Dates
        Temporal creationDate = getCreationDate(media).orElse(null);
        Temporal publicationDate = getUploadDate(media).orElse(null);
        if (creationDate != null) {
            result.put("P571", Pair.of(creationDate, null));
            if (publicationDate != null) {
                result.put("P577", Pair.of(publicationDate, null));
            }
        } else if (publicationDate != null) {
            result.put("P571", Pair.of(publicationDate, null));
        }
        // File size
        if (metadata.hasSize()) {
            result.put("P3575", Pair.of(Pair.of(metadata.getSize(), "Q8799"), null));
        }
        // Video
        if (metadata.isVideo()) {
            result.instanceOf(WikidataItem.Q98069877_VIDEO);
        }
        // Dimensions
        if (metadata.getImageDimensions() != null) {
            ofNullable(metadata.getImageDimensions().getWidth())
                    .ifPresent(w -> result.put("P2049", Pair.of(Pair.of(w, "Q355198"), null)));
            ofNullable(metadata.getImageDimensions().getHeight())
                    .ifPresent(h -> result.put("P2048", Pair.of(Pair.of(h, "Q355198"), null)));
        }
        // Location
        if (media instanceof WithLatLon ll && (ll.getLatitude() != 0d || ll.getLongitude() != 0d)) {
            result.put("P9149", Pair
                    .of(Triple.of(ll.getLatitude(), ll.getLongitude(), ll.getPrecision()), null));
        }
        // Categories
        categorizationService.findCategoriesStatements(result, findCategories(media, metadata, false));
        return result;
    }

    protected final void wikidataStatementMapping(String value, Map<String, Map<String, String>> csvMapping,
            String property, SdcStatements result) {
        ofNullable(value).map(csvMapping::get).map(m -> m.get("Wikidata")).filter(Objects::nonNull)
                .ifPresent(m -> result.put(property, Pair.of(m, null)));
    }

    @Override
    public String getWikiHtmlPreview(String sha1) throws TooManyResultsException {
        return getWikiHtmlPreview(findBySha1OrThrow(sha1, true), metadataRepository.findBySha1(sha1));
    }

    protected final String getWikiHtmlPreview(T media, FileMetadata metadata) {
        try {
            return commonsService.getWikiHtmlPreview(getWikiCode(media, metadata).getKey(), getPageTitle(media),
                    metadata.getAssetUrl().toExternalForm());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    protected String getPageTitle(T media) {
        return media.getTitle();
    }

    @Override
    public String getWikiCode(String sha1) throws TooManyResultsException {
        return getWikiCode(findBySha1OrThrow(sha1, true), metadataRepository.findBySha1(sha1)).getKey();
    }

    @Override
    public Pair<String, Map<String, String>> getWikiCode(T media, FileMetadata metadata) {
        try {
            Pair<String, Map<String, String>> desc = getWikiFileDesc(media, metadata);
            StringBuilder sb = new StringBuilder("== {{int:filedesc}} ==\n")
                    .append(desc.getKey())
                    .append("\n=={{int:license-header}}==\n");
            findLicenceTemplates(media, metadata).forEach(t -> sb.append("{{").append(t).append("}}\n"));
            commonsService
                    .cleanupCategories(findCategories(media, metadata, true), media.getBestTemporal(), needsReview())
                    .forEach(t -> sb.append("[[Category:").append(t).append("]]\n"));
            return Pair.of(sb.toString(), getLegends(media, desc.getValue()));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    protected Map<String, String> getLegends(T media, Map<String, String> descriptions) {
        return descriptions;
    }

    protected Map<String, String> replaceLegendByCorrectSentence(String lang, String text,
            Map<String, String> descriptions) {
        // For services that usually don't describe the picture in the first sentence.
        String enDesc = descriptions.get(lang);
        if (enDesc != null) {
            int idxText = enDesc.indexOf(text);
            if (idxText > -1) {
                int idxStart = enDesc.lastIndexOf('.', idxText) + 1;
                int idxEnd = enDesc.indexOf('.', idxText);
                descriptions.put(lang,
                        (idxEnd > -1 ? enDesc.substring(idxStart, idxEnd + 1) : enDesc.substring(idxStart)).trim());
            }
        }
        return descriptions;
    }

    protected Pair<String, Map<String, String>> getWikiFileDesc(T media, FileMetadata metadata) throws IOException {
        String language = getLanguage(media);
        String description = getDescription(media, metadata);
        StringBuilder sb = new StringBuilder();
        findBeforeInformationTemplates(media, metadata).forEach(t -> sb.append("{{").append(t).append("}}\n"));
        sb.append("{{Information\n| description = ");
        Map<String, String> descriptions = new HashMap<>(Map.of(language, description));
        appendWikiDescriptionInLanguage(sb, language, description);
        if (!"en".equals(language)) {
            String englishDescription = translateService.translate(description, language, "en");
            appendWikiDescriptionInLanguage(sb, "en", englishDescription);
            descriptions.put("en", englishDescription);
        }
        getWikiDate(media).ifPresent(s -> sb.append("\n| date = ").append(s));
        sb.append("\n| source = ").append(getSource(media, metadata))
                .append("\n| author = ").append(CommonsService.formatWikiCode(getAuthor(media, metadata)));
        getPermission(media).ifPresent(s -> sb.append("\n| permission = ").append(s));
        appendWikiOtherVersions(sb, media, metadata, "other versions");
        getOtherFields(media).ifPresent(s -> sb.append("\n| other fields = ").append(s));
        getOtherFields1(media).ifPresent(s -> sb.append("\n| other fields 1 = ").append(s));
        sb.append("\n}}");
        if (media instanceof WithLatLon ll && (ll.getLatitude() != 0d || ll.getLongitude() != 0d)) {
            sb.append("{{Object location |1=" + ll.getLatitude() + " |2=" + ll.getLongitude() + "}}\n");
        }
        findAfterInformationTemplates(media, metadata).forEach(t -> sb.append("{{").append(t).append("}}\n"));
        return Pair.of(sb.toString(), descriptions);
    }

    protected final void appendWikiOtherVersions(StringBuilder sb, T media, FileMetadata metadata, String key) {
        getOtherVersions(media, metadata)
                .ifPresent(s -> sb.append("\n| " + key + " = <gallery>\n").append(s).append("\n</gallery>"));
    }

    protected final void appendWikiDescriptionInLanguage(StringBuilder sb, String language, String description) {
        sb.append("{{").append(language).append("|1=").append(CommonsService.formatWikiCode(description)).append("}}");
    }

    protected final Optional<String> getWikiDate(T media) {
        return getCreationWikiDate(media).or(() -> getUploadWikiDate(media));
    }

    protected final Optional<String> getCreationWikiDate(T media) {
        return getCreationDate(media).map(d -> String.format(
                "{{Taken %s|%s|location=%s}}",
                d instanceof LocalDate || d instanceof LocalDateTime || d instanceof ZonedDateTime
                        || d instanceof Instant ? "on" : "in",
                toIso8601(d), getTakenLocation(media)));
    }

    protected String getTakenLocation(T media) {
        return "";
    }

    protected final Optional<String> getUploadWikiDate(T media) {
        return getUploadDate(media).map(d -> String.format("{{Upload date|%s}}", toIso8601(d)));
    }

    protected final String toIso8601(Temporal t) {
        Temporal d = t;
        if (d instanceof Instant instant) {
            d = instant.atZone(ZoneOffset.UTC);
        }
        if ((d instanceof LocalDateTime || d instanceof ZonedDateTime)
                && d.get(ChronoField.SECOND_OF_MINUTE) == 0 && d.get(ChronoField.MINUTE_OF_HOUR) == 0) {
            d = LocalDate.of(d.get(ChronoField.YEAR), d.get(ChronoField.MONTH_OF_YEAR), d.get(ChronoField.DAY_OF_MONTH));
        }
        if (d instanceof ZonedDateTime zoned) {
            return zoned.withNano(0).toInstant().toString();
        }
        return d.toString();
    }

    /**
     * Returns the ISO 639 (alpha-2) language code for the title/description of the given media. English by default
     *
     * @param media media for which irs title/description language is wanted
     * @return the ISO 639 (alpha-2) language code for the title/description of {@code media}. English by default.
     */
    protected String getLanguage(T media) {
        return media instanceof WithKeywords kw && kw.getKeywords().contains("en Español") ? ES : EN;
    }

    protected String getDescription(T media, FileMetadata fileMetadata) {
        String description = media.getDescription(fileMetadata);
        if (StringUtils.isBlank(description)) {
            return media.getTitle();
        } else {
            // Resolve url shortener/redirect blocked in spam disallow list
            String result = PATTERN_SHORT.matcher(description).replaceAll(match -> {
                String group = match.group();
                try {
                    String url = group.startsWith("http") ? group : "https://" + group;
                    try (CloseableHttpClient httpclient = HttpClientBuilder.create().disableAutomaticRetries()
                            .disableRedirectHandling().build();
                            CloseableHttpResponse response = httpclient
                                    .execute(newHttpGet(url.replace("http://", "https://")))) {
                        Header location = response.getFirstHeader("Location");
                        if (location != null) {
                            return location.getValue().replace("&feature=youtu.be", "");
                        }
                    }
                } catch (IOException e) {
                    LOGGER.error(group + " -> " + e.getMessage(), e);
                }
                return group;
            }).replace("http://", "https://");
            // Remove twitter hashtags search links, bloked in spam disallow list
            result = PATTERN_TWITTER_SEARCH.matcher(result).replaceAll(match -> match.group(1));
            for (FileMetadata metadata : media.getMetadata()) {
                result = result.replaceAll(
                        "<a href=\"" + metadata.getAssetUrl() + "\"><img src=\"[^\"]+\" alt=\"[^\"]*\"></a>",
                        "[[File:" + CommonsService.normalizeFilename(media.getUploadTitle(metadata)) + '.'
                                + metadata.getFileExtension() + "|120px]]");
            }
            return result;
        }
    }

    protected String getSource(T media, FileMetadata metadata) {
        return wikiLink(getSourceUrl(media, metadata), media.getTitle().replace('|', '-'));
    }

    protected String getAuthor(T media, FileMetadata metadata) {
        return media.getCredits();
    }

    protected final Optional<Temporal> getCreationDate(T media) {
        return Optional.<Temporal>ofNullable(media.getCreationDateTime())
                .or(() -> ofNullable(media.getCreationDate()));
    }

    protected final Optional<Temporal> getUploadDate(T media) {
        return Optional.<Temporal>ofNullable(media.getPublicationDateTime())
                .or(() -> ofNullable(media.getPublicationDate()));
    }

    protected Optional<String> getPermission(T media) {
        return Optional.empty();
    }

    protected Optional<String> getOtherVersions(T media, FileMetadata metadata) {
        StringBuilder sb = new StringBuilder();
        media.getMetadataStream().filter(m -> m != metadata && m.getAssetUrl() != null && m.isIgnored() != Boolean.TRUE)
                .map(m -> media.getFirstCommonsFileNameOrUploadTitle(m) + '|'
                        + ofNullable(m.getFileExtension()).orElse("TBD").toUpperCase(Locale.ENGLISH) + " version\n")
                .distinct().forEach(sb::append);
        String result = sb.toString();
        return result.isEmpty() ? Optional.empty() : Optional.of(result.trim());
    }

    protected Optional<String> getOtherFields(T media) {
        if (media instanceof WithKeywords kw) {
            StringBuilder sb = new StringBuilder();
            addOtherField(sb, "Keyword", kw.getKeywords());
            String s = sb.toString();
            return s.isEmpty() ? Optional.empty() : Optional.of(s);
        }
        return Optional.empty();
    }

    protected Optional<String> getOtherFields1(T media) {
        return Optional.empty();
    }

    protected boolean isSatellitePicture(T media, FileMetadata metadata) {
        return false;
    }

    /**
     * Returns the list of Wikimedia Commons categories to apply to the given media,
     * without hidden ones.
     *
     * @param media    the media for which category names are wanted
     * @param metadata metadata of media being uploaded
     * @return the list of Wikimedia Commons categories to apply to {@code media}
     */
    public final Set<String> findCategories(T media, FileMetadata metadata) {
        return findCategories(media, metadata, false);
    }

    /**
     * Returns the list of Wikimedia Commons categories to apply to the given media.
     *
     * @param media         the media for which category names are wanted
     * @param metadata      metadata of media being uploaded
     * @param includeHidden {@code true} if hidden categories are wanted
     * @return the list of Wikimedia Commons categories to apply to {@code media}
     */
    public Set<String> findCategories(T media, FileMetadata metadata, boolean includeHidden) {
        Set<String> result = new TreeSet<>();
        if (media.containsInTitleOrDescriptionOrKeywords("360 Panorama")) {
            result.add("360° panoramas");
        } else if (media.containsInTitleOrDescriptionOrKeywords("map")) {
            if (media.containsInTitleOrDescriptionOrKeywords("Land Surface Temperature")) {
                result.add("Land surface temperature maps");
            } else if (media.containsInTitleOrDescriptionOrKeywords("Sea Surface Temperature")) {
                result.add("Sea surface temperature maps");
            } else if (media.containsInTitleOrDescriptionOrKeywords("Tropospheric Nitrogen Dioxide")) {
                result.add("Nitrogen dioxide maps");
            }
        }
        findCategoriesFromTitleAndYear(media.getTitle(), media.getYear().getValue()).forEach(title -> {
            if (isSatellitePicture(media, metadata)) {
                categorizationService
                        .findCategoryForEarthObservationTargetOrSubject(x -> "Satellite pictures of " + x, title)
                        .ifPresentOrElse(result::add, () -> result.add(title));
            } else {
                result.add(title);
            }
        });
        if (isSatellitePicture(media, metadata)) {
            result.add(media.getYear() + " satellite pictures");
            if (media.containsInTitleOrDescriptionOrKeywords("false color")) {
                result.add("False-color satellite images");
            }
        }
        if (media instanceof WithLatLon ll) {
            result.addAll(findGeolocalizedCategories(ll));
        }
        if (metadata.isVideo() && isNASA(media) && addNASAVideoCategory()) {
            result.add("NASA videos in " + media.getYear().getValue());
        }
        if (includeHidden) {
            UnitedStates.getUsMilitaryCategory(media).ifPresent(result::add);
            result.add("Spacemedia files uploaded by " + commonsService.getAccount());
            if ("gif".equals(metadata.getFileExtension())) {
                try {
                    int numImages = ImageUtils.readImage(metadata.getAssetUri(), true, true).numImages();
                    LOGGER.info("GIF file with {} image(s): {}", numImages, metadata.getAssetUri());
                    if (numImages > 1) {
                        long megaPixels = numImages * metadata.getImageDimensions().getPixelsNumber();
                        result.add("Animated GIF files"
                                + (megaPixels > 100_000_000 ? " exceeding the 100 MP limit"
                                        : megaPixels > 50_000_000 ? " between 50 MP and 100 MP" : ""));
                        if (isNASA(media)) {
                            result.add("Animations from NASA");
                        }
                    }
                } catch (IOException | ImageDecodingException e) {
                    LOGGER.error("Failed to read GIF file: {}", e.getMessage());
                }
            }
        }
        return result;
    }

    protected boolean categorizeGeolocalizedByName() {
        return false;
    }

    protected Set<String> findGeolocalizedCategories(WithLatLon media) {
        Set<String> result = new HashSet<>();
        if (categorizeGeolocalizedByName()) {
            boolean preciseCatAdded = false;
            String name = getName();
            try {
                Address address = nominatim.reverse(media.getLatitude(), media.getLongitude(), 5).address();
                if (address != null) {
                    if (isNotBlank(address.state())
                            && addCategoryIfExists(result, "Images of " + address.state() + " by " + name)) {
                        preciseCatAdded = true;
                    } else if (isNotBlank(address.country())) {
                        result.add("Images of " + address.country() + " by " + name);
                        preciseCatAdded = true;
                    }
                }
            } catch (IOException e) {
                LOGGER.error("Nominatim error: {}", e.getMessage());
            }
            String continent = geometry.getContinent(media.getLatitude(), media.getLongitude());
            if (!preciseCatAdded) {
                result.add("Images" + (continent != null ? " of " + continent : "") + " by " + name);
            }
        }
        return result;
    }

    boolean addCategoryIfExists(Set<String> result, String cat) {
        if (commonsService.existsCategoryPage(cat)) {
            result.add(cat);
            return true;
        }
        return false;
    }

    protected static Optional<String> findCategoryFromMapping(String value, String type,
            Map<String, Map<String, String>> mappings) {
        if (value != null) {
            Map<String, String> map = mappings.get(value.replace('\n', ' ').trim());
            if (map != null) {
                String cat = map.get("Commons categories");
                if (isBlank(cat)) {
                    LOGGER.warn("No category found for {} {}", type, value);
                } else {
                    return Optional.of(cat);
                }
            }
        }
        return Optional.empty();
    }

    protected static record Affixes(Iterable<String> values, boolean areOptional) {
    }

    protected Set<String> findCategoriesFromTitleAndYear(String title, int year) {
        return findCategoriesFromTitleAndAffixes(title, new Affixes(List.of(year + " ", year + " in "), true),
                new Affixes(List.of(" (" + year + ")", " in " + year), true));
    }

    protected Set<String> findCategoriesFromTitleAndAffixes(String title, Affixes prefixes, Affixes suffixes) {
        Set<String> result = new TreeSet<>();
        if (title != null) {
            String[] words = title.strip().split(" ");
            if (words.length >= 2) {
                // Try first words
                findCategoriesFromWords(words.length, n -> copyOfRange(words, 0, words.length - n), prefixes, suffixes)
                        .ifPresent(result::add);
                // Try last words
                findCategoriesFromWords(words.length, n -> copyOfRange(words, n, words.length), prefixes, suffixes)
                        .ifPresent(result::add);
            }
        }
        return result;
    }

    private Optional<String> findCategoriesFromWords(int len, IntFunction<String[]> words, Affixes prefixes,
            Affixes suffixes) {
        for (int n = 0; n <= len - 2; n++) {
            String firstWords = strip(String.join(" ", words.apply(n)), ",-.!?'\"");
            if (prefixes != null && suffixes != null) {
                for (String prefix : prefixes.values()) {
                    for (String suffix : suffixes.values()) {
                        String firstWordsWithPrefixAndSuffix = prefix + firstWords + suffix;
                        if (commonsService.existsCategoryPage(firstWordsWithPrefixAndSuffix)) {
                            return Optional.of(firstWordsWithPrefixAndSuffix);
                        }
                    }
                }
            }
            if (prefixes != null && (suffixes == null || suffixes.areOptional())) {
                for (String prefix : prefixes.values()) {
                    String firstWordsWithPrefix = prefix + firstWords;
                    if (commonsService.existsCategoryPage(firstWordsWithPrefix)) {
                        return Optional.of(firstWordsWithPrefix);
                    }
                }
            }
            if (suffixes != null && (prefixes == null || prefixes.areOptional())) {
                for (String suffix : suffixes.values()) {
                    String firstWordsWithSuffix = firstWords + suffix;
                    if (commonsService.existsCategoryPage(firstWordsWithSuffix)) {
                        return Optional.of(firstWordsWithSuffix);
                    }
                }
            }
            if ((prefixes == null || prefixes.areOptional()) && (suffixes == null || suffixes.areOptional())) {
                if (commonsService.existsCategoryPage(firstWords)) {
                    return Optional.of(firstWords);
                }
                String firstWordsWithoutComma = firstWords.replace(",", "");
                if (commonsService.existsCategoryPage(firstWordsWithoutComma)) {
                    return Optional.of(firstWordsWithoutComma);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Returns the list of information templates to apply to the given media, before
     * the main one.
     *
     * @param media    the media for which information template names are wanted
     * @param metadata the file metadata
     * @return the list of information templates to apply to {@code media}
     */
    public Set<String> findBeforeInformationTemplates(T media, FileMetadata metadata) {
        Set<String> result = new LinkedHashSet<>();
        Long size = metadata.getSize();
        ImageDimensions dims = metadata.getImageDimensions();
        if (size != null && size >= LOTS_OF_MP || (dims != null && dims.getPixelsNumber() >= LOTS_OF_MP)) {
            result.add("LargeImage");
        }
        return result;
    }

    /**
     * Returns the list of information templates to apply to the given media, after
     * the main one.
     *
     * @param media    the media for which information template names are wanted
     * @param metadata the file metadata
     * @return the list of information templates to apply to {@code media}
     */
    public Set<String> findAfterInformationTemplates(T media, FileMetadata metadata) {
        return new LinkedHashSet<>();
    }

    /**
     * Returns the list of licence templates to apply to the given media.
     *
     * @param media    the media for which licence template names are wanted
     * @param metadata the file metadata for which licence template names are wanted
     * @return the list of licence templates to apply to {@code media}
     */
    public Set<String> findLicenceTemplates(T media, FileMetadata metadata) {
        Set<String> result = new LinkedHashSet<>();
        String description = media.getDescription(metadata);
        if (description != null) {
            ofNullable(CategorizationService.getCopernicusTemplate(description)).ifPresent(result::add);
        }
        return result;
    }

    /**
     * Determines if the given media has been produced by NASA.
     *
     * @param media media
     * @return {@code true} if media has been produced by NASA
     */
    protected boolean isNASA(T media) {
        return getClass().getSimpleName().toLowerCase(Locale.ENGLISH).contains("nasa");
    }

    protected boolean addNASAVideoCategory() {
        return true;
    }

    protected final String wikiLink(URL url, String text) {
        return "[" + requireNonNull(url, "url").toExternalForm().replace(" ", "%20") + ' '
                + requireNonNull(text, "text") + ']';
    }

    protected void checkUploadPreconditions(T media, boolean checkUnicity, boolean isManual)
            throws URISyntaxException {
        if (UploadContext.isForbiddenUpload(media, isManual)) {
            throw new ImageUploadForbiddenException(media + " is marked as ignored (manual: " + isManual + ")");
        }
    }

    protected void checkUploadPreconditions(T media, FileMetadata metadata, boolean checkUnicity) throws IOException {
        if (!metadata.hasSha1()) {
            throw new ImageUploadForbiddenException(media + " SHA-1 has not been computed.");
        }
        // Forbid upload of duplicate medias for a single repo, they may have different descriptions
        if (checkUnicity && repository.countByMetadata_Sha1(metadata.getSha1()) > 1) {
            throw new ImageUploadForbiddenException(media + " is present several times.");
        }
        // Double-check for duplicates before upload!
        if (isNotEmpty(metadata.getCommonsFileNames())) {
            throw new ImageUploadForbiddenException(media + " is already on Commons: " + metadata.getCommonsFileNames());
        }
        if (mediaService.findCommonsFiles(List.of(metadata), media.getSearchTermsInCommons(),
                includeByPerceptualHash())) {
            metadata = metadataRepository.save(metadata);
            throw new ImageUploadForbiddenException(
                    media + " is already on Commons: " + metadata.getCommonsFileNames());
        }
        if (findLicenceTemplates(media, metadata).isEmpty()) {
            mediaService.ignoreAndSaveMetadata(metadata, "no template found, so may be not free");
            throw new ImageUploadForbiddenException(media + " has no template, so may be not free");
        }
    }

    protected abstract Class<T> getMediaClass();

    protected Class<? extends T> getTopTermsMediaClass() {
        return getMediaClass();
    }

    protected final MediaUpdateResult doCommonUpdate(T media, boolean forceUpdate) throws IOException {
        MediaUpdateResult ur = mediaService.updateMedia(media, getStringsToRemove(media),
                forceUpdate, getUrlResolver(), checkBlocklist(), includeByPerceptualHash(), ignoreExifMetadata(), null);
        boolean result = ur.getResult();
        if (!media.isIgnored() && media.hasMetadata()) {
            LOGGER.trace("Start common update checks for {}", media);
            for (FileMetadata fm : media.getMetadata()) {
                if (Boolean.TRUE != fm.isIgnored()) {
                    result |= doCheckNonFree(media, fm);
                    result |= doCheckShortOrMissingTitleAndDescription(media, fm);
                    result |= doCheckImageTooBigOrTooSmall(fm);
                    result |= doCheckUninterestingTitle(media, fm);
                }
            }
            LOGGER.trace("Ended common update checks for {}", media);
        }
        return new MediaUpdateResult(result, ur.getException());
    }

    private boolean doCheckNonFree(T media, FileMetadata fm) {
        String description = ofNullable(media.getDescription(fm)).orElse("").toLowerCase(Locale.ENGLISH);
        if (description.contains("by-nc") || description.contains("by-nd")) {
            LOGGER.debug("Non-free licence test has been trigerred for {}", fm);
            return mediaService.ignoreAndSaveMetadata(fm, "Non-free licence");
        } else if (isCourtesy(description) && (findLicenceTemplates(media, fm).isEmpty()
                || courtesyOk.stream().noneMatch(description::contains))) {
            LOGGER.debug("Courtesy test has been trigerred for {}", fm);
            return mediaService.ignoreAndSaveMetadata(fm, "Probably non-free file (courtesy)");
        } else if (description.contains("citizen scien") || description.contains("citizenscien")) {
            LOGGER.debug("Citizen science test has been trigerred for {}", fm);
            return mediaService.ignoreAndSaveMetadata(fm, "Probably non-free file (citizen science)");
        }
        return false;
    }

    private boolean doCheckShortOrMissingTitleAndDescription(T media, FileMetadata fm) {
        if (StringUtils.length(media.getTitle()) + StringUtils.length(media.getDescription(fm)) <= 2) {
            // To ignore https://www.dvidshub.net/image/6592675 (title and desc are '.')
            LOGGER.debug("Short title or description test has been trigerred for {}", fm);
            return mediaService.ignoreAndSaveMetadata(fm, "Very short or missing title and description");
        }
        return false;
    }

    private boolean doCheckImageTooBigOrTooSmall(FileMetadata fm) {
        if (fm.isImage()) {
            if (fm.hasValidDimensions() && fm.getImageDimensions().getPixelsNumber() < 20_000) {
                LOGGER.debug("Too small image test has been trigerred for {}", fm);
                return mediaService.ignoreAndSaveMetadata(fm, "Too small image");
            } else if (fm.getSize() != null && fm.getSize() > Integer.MAX_VALUE) {
                // See https://commons.wikimedia.org/wiki/Commons:Maximum_file_size
                LOGGER.debug("Too big image test has been trigerred for {}", fm);
                return mediaService.ignoreAndSaveMetadata(fm, "Too big image");
            }
        }
        return false;
    }

    private boolean doCheckUninterestingTitle(T media, FileMetadata fm) {
        if (isBlank(media.getDescription(fm)) && media.getTitle() != null
                && PATTERN_UNINTERESTING_TITLE.matcher(media.getTitle()).matches()) {
            LOGGER.debug("No description and uninteresting title test has been trigerred for {}", fm);
            return mediaService.ignoreAndSaveMetadata(fm, "Media without description and with uninteresting title");
        }
        return false;
    }

    protected static boolean isCourtesy(String description) {
        return COURTESY_SPELLINGS.stream().anyMatch(description::contains);
    }

    protected boolean checkBlocklist() {
        return true;
    }

    protected boolean includeByPerceptualHash() {
        return true;
    }

    protected boolean ignoreExifMetadata() {
        return false;
    }

    protected boolean needsReview() {
        return true;
    }

    protected final boolean doCommonUpdate(T media) throws IOException {
        return doCommonUpdate(media, false).getResult();
    }

    @Override
    public int compareTo(AbstractOrgService<T> o) {
        return getName().compareTo(o.getName());
    }

    protected final RuntimeData getRuntimeData() {
        return runtimeDataRepository.findById(getId()).orElseGet(() -> new RuntimeData(getId()));
    }

    protected final UploadMode getUploadMode() {
        return uploadMode;
    }

    protected boolean isPermittedFileType(FileMetadata metadata) {
        return commonsService.isPermittedFileExt(metadata.getExtension()) || "mp4".equals(metadata.getExtension())
                || (metadata.getAssetUrl() != null && (commonsService.isPermittedFileUrl(metadata.getAssetUrl())
                        || metadata.getAssetUrl().getFile().endsWith(".mp4")
                        || "www.youtube.com".equals(metadata.getAssetUrl().getHost())));
    }

    protected boolean shouldUploadAuto(T media, boolean isManual) {
        return media.getMetadataStream().anyMatch(
                metadata -> new UploadContext<>(media, metadata, getUploadMode(), minYearUploadAuto,
                        this::isPermittedFileType, isManual).shouldUploadAuto());
    }

    protected UrlResolver<T> getUrlResolver() {
        return (media, metadata) -> metadata.getAssetUrl();
    }

    protected FileMetadata addMetadata(T media, String assetUrl, Consumer<FileMetadata> consumer) {
        return addMetadata(media, newURL(assetUrl), consumer);
    }

    protected FileMetadata addMetadata(T media, URL assetUrl, Consumer<FileMetadata> consumer) {
        return addMetadata(media, assetUrl, consumer, false);
    }

    protected FileMetadata addMetadata(T media, URL assetUrl, Consumer<FileMetadata> consumer, boolean forceConsumer) {
        try {
            FileMetadata fm = metadataRepository.findByAssetUrl(assetUrl).map(x -> {
                if (forceConsumer && consumer != null) {
                    consumer.accept(x);
                    x = metadataRepository.save(x);
                }
                return x;
            }).orElseGet(() -> metadataRepository.save(newFileMetadata(assetUrl, consumer)));
            media.addMetadata(fm);
            return fm;
        } catch (RuntimeException e) {
            LOGGER.error("Error while adding metadata {} for media {}", assetUrl, media);
            throw e;
        }
    }

    protected static FileMetadata newFileMetadata(URL assetUrl, Consumer<FileMetadata> consumer) {
        FileMetadata metadata = new FileMetadata(assetUrl);
        if (consumer != null) {
            consumer.accept(metadata);
        }
        return metadata;
    }

    protected static void addOtherField(StringBuilder sb, String name, Collection<?> values, Map<String, String> catMapping) {
        if (isNotEmpty(values)) {
            addOtherField(sb, name + (values.size() > 1 ? "s" : ""),
                    values.stream().filter(Objects::nonNull).map(Object::toString).filter(StringUtils::isNotBlank).map(s -> {
                        if (catMapping != null) {
                            String cat = catMapping.get(s);
                            if (StringUtils.isNotBlank(cat)) {
                                return stream(cat.split(";"))
                                        .map(c -> "[[:Category:" + c + '|' + s + "]]")
                                        .collect(joining("; "));
                            }
                        }
                        return s;
                    }).collect(joining("; ")));
        }
    }

    protected static void addOtherField(StringBuilder sb, String name, Collection<?> values) {
        addOtherField(sb, name, values, null);
    }

    protected static void addOtherField(StringBuilder sb, String name, String value) {
        addOtherField(sb, name, value, null);
    }

    protected static void addOtherField(StringBuilder sb, String name, String value, Map<String, String> catMapping) {
        if (StringUtils.isNotBlank(value)) {
            String s = value;
            if (catMapping != null) {
                String cat = catMapping.get(value);
                if (StringUtils.isNotBlank(cat)) {
                    s = "[[:Category:" + cat + '|' + value + "]]";
                }
            }
            sb.append("{{information field|name=").append(name).append("|value=").append(s).append("}}");
        }
    }

    protected static void doFor(Iterable<String> set, Function<String, Optional<String>> mapper,
            Consumer<String> consumer) {
        if (set != null) {
            for (String s : set) {
                mapper.apply(s).ifPresent(consumer::accept);
            }
        }
    }

    protected static SimpleEntry<String, String> e(String k, String v) {
        return new SimpleEntry<>(k, v);
    }

    protected static Object smartExceptionLog(Throwable e) {
        return e.getCause() instanceof RuntimeException ? e : e.toString();
    }

    protected Pair<String, Map<String, String>> milim(T media, FileMetadata metadata, String virin,
            Optional<String> location, Optional<Float> rating) {
        String lang = getLanguage(media);
        String desc = getDescription(media, metadata);
        StringBuilder sb = new StringBuilder("{{milim\n| description = ").append("{{").append(lang).append("|1=")
                .append(CommonsService.formatWikiCode(desc)).append("}}");
        getWikiDate(media).ifPresent(s -> sb.append("\n| date = ").append(s));
        sb.append("\n| source = ").append(getSource(media, metadata)).append("\n| author = ")
                .append(getAuthor(media, metadata));
        getPermission(media).ifPresent(s -> sb.append("\n| permission = ").append(s));
        location.ifPresent(l -> sb.append("\n| location = ").append(l));
        sb.append("\n| virin = ").append(virin).append("\n| dateposted = ").append(toIso8601(
                Optional.<Temporal>ofNullable(media.getPublicationDateTime()).orElse(media.getPublicationDate())));
        rating.ifPresent(r -> sb.append("\n| stars = ").append(r.intValue()));
        getOtherVersions(media, metadata).ifPresent(s -> sb.append("\n| other versions = ").append(s));
        getOtherFields(media).ifPresent(s -> sb.append("\n| other fields = ").append(s));
        getOtherFields1(media).ifPresent(s -> sb.append("\n| other fields 1 = ").append(s));
        sb.append("\n}}");
        return Pair.of(sb.toString(), Map.of(lang, desc));
    }

    protected static final class UpdateFinishedException extends Exception {
        private static final long serialVersionUID = 1L;

        public UpdateFinishedException(String message) {
            super(message);
        }
    }
}
