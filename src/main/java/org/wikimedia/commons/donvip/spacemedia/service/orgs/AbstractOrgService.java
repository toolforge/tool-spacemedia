package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.durationInSec;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.newHttpGet;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.newURL;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.time.temporal.Temporal;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.client.RestClientException;
import org.wikidata.wdtk.wikibaseapi.apierrors.MediaWikiApiErrorException;
import org.wikimedia.commons.donvip.spacemedia.data.domain.Statistics;
import org.wikimedia.commons.donvip.spacemedia.data.domain.UploadMode;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadataRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.Media;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.MediaRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.Problem;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.ProblemRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.RuntimeData;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.RuntimeDataRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.WithKeywords;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.WithLatLon;
import org.wikimedia.commons.donvip.spacemedia.exception.ImageNotFoundException;
import org.wikimedia.commons.donvip.spacemedia.exception.ImageUploadForbiddenException;
import org.wikimedia.commons.donvip.spacemedia.exception.TooManyResultsException;
import org.wikimedia.commons.donvip.spacemedia.exception.UploadException;
import org.wikimedia.commons.donvip.spacemedia.service.AbstractSocialMediaService;
import org.wikimedia.commons.donvip.spacemedia.service.ExecutionMode;
import org.wikimedia.commons.donvip.spacemedia.service.GoogleTranslateService;
import org.wikimedia.commons.donvip.spacemedia.service.MediaService;
import org.wikimedia.commons.donvip.spacemedia.service.MediaService.MediaUpdateResult;
import org.wikimedia.commons.donvip.spacemedia.service.RemoteService;
import org.wikimedia.commons.donvip.spacemedia.service.SearchService;
import org.wikimedia.commons.donvip.spacemedia.service.TransactionService;
import org.wikimedia.commons.donvip.spacemedia.service.UrlResolver;
import org.wikimedia.commons.donvip.spacemedia.service.mastodon.MastodonService;
import org.wikimedia.commons.donvip.spacemedia.service.twitter.TwitterService;
import org.wikimedia.commons.donvip.spacemedia.service.wikimedia.CommonsService;
import org.wikimedia.commons.donvip.spacemedia.utils.CsvHelper;
import org.wikimedia.commons.donvip.spacemedia.utils.Emojis;

/**
 * Superclass of orgs services.
 *
 * @param <T>  the media type the repository manages
 * @param <ID> the type of the id of the entity the repository manages
 * @param <D>  the media date type
 */
public abstract class AbstractOrgService<T extends Media<ID, D>, ID, D extends Temporal>
        implements Comparable<AbstractOrgService<T, ID, D>>, Org<T, ID, D> {

    protected static final String EN = "en";

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractOrgService.class);

    private static final Pattern PATTERN_SHORT = Pattern
            .compile(
                    "(?:https?://)?(?:bit.ly/[0-9a-zA-Z]{7}|youtu.be/[\\w\\-]{11}|flic.kr/p/[0-9a-zA-Z]{6}|fb.me/e/[0-9a-zA-Z]{9})");

    private static final Pattern PATTERN_TWITTER_SEARCH = Pattern
            .compile("<a href=\"https://twitter.com/search?[^\"]+\">([^<]*)</a>");

    private static final Set<String> PD_US = Set.of("PD-US", "PD-NASA", "PD-Hubble", "PD-Webb");

    static final Pattern COPERNICUS_CREDIT = Pattern.compile(
            ".*Copernicus[ -](?:Sentinel[ -])?dat(?:a|en)(?:/ESA)? [\\(\\[](2\\d{3}(?:[-–/]\\d{2,4})?)[\\)\\]].*",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private static final Map<String, String> LICENCES = Map.ofEntries(e("YouTube CC-BY", "Q14947546"),
            e("Cc-by-2.0", "Q19125117"), e("Cc-by-sa-2.0", "Q19068220"), e("Cc-zero", "Q6938433"),
            e("DLR-License", "Q62619894"), e("ESA|", "Q26259495"), e("ESO", "Q20007257"), e("IAU", "Q20007257"),
            e("KOGL", "Q12584618"), e("NOIRLab", "Q20007257"), e("ESA-Hubble", "Q20007257"),
            e("ESA-Webb", "Q20007257"));

    protected final MediaRepository<T, ID, D> repository;

    private final String id;

    @Autowired
    protected TransactionService transactionService;

    @Autowired
    protected FileMetadataRepository metadataRepository;
    @Autowired
    protected RuntimeDataRepository runtimeDataRepository;
    @Autowired
    protected ProblemRepository problemRepository;
    @Autowired
    protected MediaService mediaService;
    @Autowired
    protected CommonsService commonsService;
    @Autowired
    private SearchService searchService;
    @Autowired
    private RemoteService remoteService;
    @Autowired
    private GoogleTranslateService translateService;
    @Autowired
    private List<AbstractSocialMediaService<?, ?>> socialMediaServices;

    @Autowired
    private Environment env;

    @Autowired
    @PersistenceContext(unitName = "domain")
    private EntityManager entityManager;

    @SuppressWarnings("unused")
    private Set<String> ignoredCommonTerms;

    private Set<String> satellitePicturesCategories;

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

    private Map<String, String> categoriesStatements;

    private UploadMode uploadMode;

    protected AbstractOrgService(MediaRepository<T, ID, D> repository, String id) {
        this.repository = Objects.requireNonNull(repository);
        this.id = Objects.requireNonNull(id);
    }

    @PostConstruct
    void init() throws IOException {
        ignoredCommonTerms = CsvHelper.loadSet(getClass().getResource("/search.ignored.terms.csv"));
        satellitePicturesCategories = CsvHelper.loadSet(getClass().getResource("/satellite.pictures.categories.txt"));
        categoriesStatements = loadCsvMapping("categories.statements.csv");
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
        return repository.count();
    }

    @Override
    public long countIgnored() {
        return repository.countByIgnoredTrue();
    }

    @Override
    public long countMissingMedia() {
        return repository.countMissingInCommons();
    }

    @Override
    public long countMissingImages() {
        return repository.countMissingImagesInCommons();
    }

    @Override
    public long countMissingVideos() {
        return repository.countMissingVideosInCommons();
    }

    @Override
    public long countPerceptualHashes() {
        return repository.countByMetadata_PhashNotNull();
    }

    @Override
    public long countUploadedMedia() {
        return repository.countUploadedToCommons();
    }

    @Override
    public Iterable<T> listAllMedia() {
        return repository.findAll();
    }

    @Override
    public Page<T> listAllMedia(Pageable page) {
        return repository.findAll(page);
    }

    @Override
    public List<T> listMissingMedia() {
        return repository.findMissingInCommons();
    }

    @Override
    public Page<T> listMissingMedia(Pageable page) {
        return repository.findMissingInCommons(page);
    }

    @Override
    public Page<T> listMissingImages(Pageable page) {
        return repository.findMissingImagesInCommons(page);
    }

    @Override
    public Page<T> listMissingVideos(Pageable page) {
        return repository.findMissingVideosInCommons(page);
    }

    @Override
    public Page<T> listHashedMedia(Pageable page) {
        return repository.findByMetadata_PhashNotNull(page);
    }

    @Override
    public List<T> listUploadedMedia() {
        return repository.findUploadedToCommons();
    }

    @Override
    public Page<T> listUploadedMedia(Pageable page) {
        return repository.findUploadedToCommons(page);
    }

    @Override
    public List<T> listDuplicateMedia() {
        return repository.findDuplicateInCommons();
    }

    @Override
    public List<T> listIgnoredMedia() {
        return repository.findByIgnoredTrue();
    }

    @Override
    public Page<T> listIgnoredMedia(Pageable page) {
        return repository.findByIgnoredTrue(page);
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

    protected final void endUpdateMedia(int count, Collection<T> uploadedMedia, LocalDateTime start) {
        endUpdateMedia(count, uploadedMedia, start, true);
    }

    protected final void endUpdateMedia(int count, Collection<T> uploadedMedia, LocalDateTime start,
            boolean postTweet) {
        endUpdateMedia(count, uploadedMedia, uploadedMedia.stream().flatMap(m -> m.getMetadata().stream()).toList(),
                start, postTweet);
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
        long problems = getProblemsCount();
        return new Statistics(getName(), getId(), countAllMedia(), countUploadedMedia(), countIgnored(),
                countMissingImages(), countMissingVideos(), countPerceptualHashes(), problems > 0 ? problems : null);
    }

    @Override
    public List<Problem> getProblems() {
        return problemRepository.findByOrg(getId());
    }

    @Override
    public Page<Problem> getProblems(Pageable page) {
        return problemRepository.findByOrg(getId(), page);
    }

    @Override
    public long getProblemsCount() {
        return problemRepository.countByOrg(getId());
    }

    protected final Problem problem(URL problematicUrl, Throwable t) {
        return problem(problematicUrl, t.toString());
    }

    protected final Problem problem(String problematicUrl, Throwable t) {
        return problem(problematicUrl, t.toString());
    }

    protected final Problem problem(String problematicUrl, String errorMessage) {
        return problem(newURL(problematicUrl), errorMessage);
    }

    protected final Problem problem(URL problematicUrl, String errorMessage) {
        Optional<Problem> problem = problemRepository.findByOrgAndProblematicUrl(getId(), problematicUrl);
        if (problem.isPresent()) {
            return problem.get();
        } else {
            Problem pb = new Problem();
            pb.setOrg(getId());
            pb.setErrorMessage(errorMessage);
            pb.setProblematicUrl(problematicUrl);
            pb.setDate(LocalDateTime.now());
            LOGGER.warn("{}", pb);
            return problemRepository.save(pb);
        }
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
        return repository.findById(getMediaId(id)).orElseThrow(() -> new ImageNotFoundException(id));
    }

    @Override
    public void deleteById(String id) throws ImageNotFoundException {
        repository.deleteById(getMediaId(id));
    }

    @Override
    public T refreshAndSaveById(String id) throws ImageNotFoundException, IOException {
        return refreshAndSave(getById(id));
    }

    @Override
    public T refreshAndSave(T media) throws IOException {
        T refreshedMedia = refresh(media);
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
        } catch (IOException | RuntimeException | URISyntaxException e) {
            throw new UploadException(e);
        }
    }

    protected int doUpload(T media, boolean checkUnicity, Collection<FileMetadata> uploaded, boolean isManual)
            throws IOException, UploadException {
        int count = 0;
        for (FileMetadata metadata : media.getMetadata()) {
            try {
                count += doUpload(media, metadata, checkUnicity, uploaded, isManual);
            } catch (ImageUploadForbiddenException e) {
                LOGGER.warn("File {} not uploaded: {}", metadata, e.getMessage());
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
        Map<String, Pair<Object, Map<String, Object>>> statements = getStatements(media, metadata);
        try {
            commonsService.editStructuredDataContent(uploadedFilename, legends, statements);
        } catch (MediaWikiApiErrorException | IOException | RuntimeException e) {
            LOGGER.error("Unable to add SDC data: {}", statements, e);
        }
    }

    protected Map<String, Pair<Object, Map<String, Object>>> getStatements(T media, FileMetadata metadata) {
        Map<String, Pair<Object, Map<String, Object>>> result = new TreeMap<>();
        // Source: file available on the internet
        result.put("P7482", Pair.of("Q74228490", new TreeMap<>(Map.of("P973",
                getSourceUrl(media, metadata).toExternalForm(),
                "P2699", metadata.getAssetUrl().toExternalForm()))));
        // Licences
        Set<String> licences = findLicenceTemplates(media);
        boolean usPublicDomain = PD_US.stream().anyMatch(pd -> licences.stream().anyMatch(l -> l.startsWith(pd)));
        result.put("P6216", Pair.of(usPublicDomain ? "Q19652" : "Q50423863",
                usPublicDomain ? new TreeMap<>(Map.of("P459", "Q60671452", "P1001", "Q30")) : null));
        if (!usPublicDomain) {
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
        if (metadata.getSize() != null) {
            result.put("P3575", Pair.of(Pair.of(metadata.getSize(), "Q8799"), null));
        }
        // Video
        if (metadata.isVideo()) {
            result.put("P31", Pair.of("Q98069877", null));
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
        Set<String> cats = findCategories(media, metadata, false);
        for (Entry<String, String> e : categoriesStatements.entrySet()) {
            if (Arrays.stream(e.getKey().split(";")).anyMatch(cats::contains)) {
                for (String statement : e.getValue().split(";")) {
                    String[] kv = statement.split("=");
                    result.put(kv[0], Pair.of(kv[1], null));
                }
            }
        }
        return result;
    }

    protected final void wikidataStatementMapping(String value, Map<String, Map<String, String>> csvMapping,
            String property, Map<String, Pair<Object, Map<String, Object>>> result) {
        ofNullable(value).map(csvMapping::get).map(m -> m.get("Wikidata")).filter(Objects::nonNull)
                .ifPresent(m -> result.put(property, Pair.of(m, null)));
    }

    @Override
    public String getWikiHtmlPreview(String sha1) throws TooManyResultsException {
        return getWikiHtmlPreview(findBySha1OrThrow(sha1, true), metadataRepository.findBySha1(sha1));
    }

    protected final String getWikiHtmlPreview(T media, FileMetadata metadata) {
        try {
            return commonsService.getWikiHtmlPreview(getWikiCode(media, metadata).getKey(), getPageTile(media),
                    metadata.getAssetUrl().toExternalForm());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    protected String getPageTile(T media) {
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
            findLicenceTemplates(media).forEach(t -> sb.append("{{").append(t).append("}}\n"));
            commonsService.cleanupCategories(findCategories(media, metadata, true), media.getDate())
                    .forEach(t -> sb.append("[[Category:").append(t).append("]]\n"));
            return Pair.of(sb.toString(), getLegends(media, desc.getValue()));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    protected Map<String, String> getLegends(T media, Map<String, String> descriptions) {
        return descriptions;
    }

    protected Pair<String, Map<String, String>> getWikiFileDesc(T media, FileMetadata metadata) throws IOException {
        String language = getLanguage(media);
        String description = getDescription(media);
        StringBuilder sb = new StringBuilder("{{Information\n| description = ");
        Map<String, String> descriptions = new HashMap<>(Map.of(language, description));
        appendWikiDescriptionInLanguage(sb, language, description);
        if (!"en".equals(language)) {
            String englishDescription = translateService.translate(description, language, "en");
            appendWikiDescriptionInLanguage(sb, "en", englishDescription);
            descriptions.put("en", englishDescription);
        }
        getWikiDate(media).ifPresent(s -> sb.append("\n| date = ").append(s));
        sb.append("\n| source = ").append(getSource(media, metadata))
          .append("\n| author = ").append(CommonsService.formatWikiCode(getAuthor(media)));
        getPermission(media).ifPresent(s -> sb.append("\n| permission = ").append(s));
        appendWikiOtherVersions(sb, media, metadata, "other versions");
        getOtherFields(media).ifPresent(s -> sb.append("\n| other fields = ").append(s));
        getOtherFields1(media).ifPresent(s -> sb.append("\n| other fields 1 = ").append(s));
        sb.append("\n}}");
        if (media instanceof WithLatLon ll && (ll.getLatitude() != 0d || ll.getLongitude() != 0d)) {
            sb.append("{{Object location |1=" + ll.getLatitude() + " |2=" + ll.getLongitude() + "}}\n");
        }
        findInformationTemplates(media).forEach(t -> sb.append("{{").append(t).append("}}\n"));
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
            return zoned.toInstant().toString();
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
        return EN;
    }

    protected String getDescription(T media) {
        String description = media.getDescription();
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
        return wikiLink(getSourceUrl(media, metadata), media.getTitle());
    }

    protected abstract String getAuthor(T media) throws MalformedURLException;

    protected Optional<Temporal> getCreationDate(T media) {
        return Optional.empty();
    }

    protected Optional<Temporal> getUploadDate(T media) {
        return Optional.empty();
    }

    protected Optional<String> getPermission(T media) {
        return Optional.empty();
    }

    protected Optional<String> getOtherVersions(T media, FileMetadata metadata) {
        StringBuilder sb = new StringBuilder();
        media.getMetadata().stream().distinct().filter(m -> m != metadata && m.getAssetUrl() != null)
                .map(m -> media.getFirstCommonsFileNameOrUploadTitle(m) + '|'
                        + m.getFileExtension().toUpperCase(Locale.ENGLISH) + " version\n")
                .distinct().forEach(sb::append);
        String result = sb.toString();
        return result.isEmpty() ? Optional.empty() : Optional.of(result.trim());
    }

    protected Optional<String> getOtherFields(T media) {
        return Optional.empty();
    }

    protected Optional<String> getOtherFields1(T media) {
        return Optional.empty();
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
        Set<String> result = new HashSet<>();
        if (media.containsInTitleOrDescription("360 Panorama")) {
            result.add("360° panoramas");
        } else if (media.containsInTitleOrDescription("Land Surface Temperature")) {
            result.add("Land surface temperature maps");
        } else if (media.containsInTitleOrDescription("Sea Surface Temperature")) {
            result.add("Sea surface temperature maps");
        } else if (media.containsInTitleOrDescription("Tropospheric Nitrogen Dioxide")) {
            result.add("Nitrogen dioxide maps");
        }
        findCategoryFromTitle(media.getTitle()).ifPresent(result::add);
        if (includeHidden) {
            result.add("Spacemedia files uploaded by " + commonsService.getAccount());
        }
        return result;
    }

    protected Optional<String> findCategoryFromTitle(String title) {
        if (title != null) {
            String[] words = title.strip().split(" ");
            if (words.length >= 2) {
                for (int n = words.length; n >= 2; n--) {
                    String firstWords = String.join(" ", Arrays.copyOfRange(words, 0, n));
                    if (commonsService.existsCategoryPage(firstWords)) {
                        return Optional.of(firstWords);
                    }
                    String firstWordsWithoutComma = firstWords.replace(",", "");
                    if (commonsService.existsCategoryPage(firstWordsWithoutComma)) {
                        return Optional.of(firstWordsWithoutComma);
                    }
                }
            }
        }
        return Optional.empty();
    }

    protected Set<String> findCategoriesForEarthObservationImage(Media<?, ?> image, UnaryOperator<String> categorizer,
            String defaultCat) {
        Set<String> result = new TreeSet<>();
        for (String targetOrSubject : satellitePicturesCategories) {
            if (image.containsInTitleOrDescription(targetOrSubject)) {
                String cat = categorizer.apply(targetOrSubject);
                if (commonsService.existsCategoryPage(cat)) {
                    result.add(cat);
                } else {
                    String theCat = categorizer.apply("the " + targetOrSubject);
                    if (commonsService.existsCategoryPage(theCat)) {
                        result.add(theCat);
                    } else {
                        String cats = categorizer.apply(targetOrSubject + "s");
                        if (commonsService.existsCategoryPage(cats)) {
                            result.add(cats);
                        }
                    }
                }
            }
        }
        if (result.isEmpty()) {
            result.add(defaultCat);
        }
        result.add(image.getYear() + " satellite pictures");
        return result;
    }

    /**
     * Returns the list of licence templates to apply to the given media.
     *
     * @param media the media for which licence template names are wanted
     * @return the list of licence templates to apply to {@code media}
     */
    public Set<String> findInformationTemplates(T media) {
        return new LinkedHashSet<>();
    }

    /**
     * Returns the list of licence templates to apply to the given media.
     *
     * @param media the media for which licence template names are wanted
     * @return the list of licence templates to apply to {@code media}
     */
    public Set<String> findLicenceTemplates(T media) {
        Set<String> result = new LinkedHashSet<>();
        String description = media.getDescription();
        if (description != null) {
            ofNullable(getCopernicusTemplate(description)).ifPresent(result::add);
        }
        return result;
    }

    protected static String getCopernicusTemplate(String text) {
        Matcher m = COPERNICUS_CREDIT.matcher(text);
        return m.matches() ? "Attribution-Copernicus |year=" + m.group(1) : null;
    }

    protected final String wikiLink(URL url, String text) {
        return "[" + Objects.requireNonNull(url, "url") + ' ' + Objects.requireNonNull(text, "text") + ']';
    }

    protected void checkUploadPreconditions(T media, boolean checkUnicity, boolean isManual)
            throws MalformedURLException, URISyntaxException {
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
        for (String idUsedInCommons : media.getIdUsedInCommons()) {
            if (mediaService.findCommonsFiles(List.of(metadata), idUsedInCommons, includeByPerceptualHash())) {
                metadata = metadataRepository.save(metadata);
                throw new ImageUploadForbiddenException(
                        media + " is already on Commons: " + metadata.getCommonsFileNames());
            }
        }
        if (findLicenceTemplates(media).isEmpty()) {
            throw new ImageUploadForbiddenException(media + " has no template, so may be not free");
        }
    }

    protected abstract Class<T> getMediaClass();

    /**
     * Returns the media identifier for the given string representation.
     *
     * @param id the string representation of a media identifier
     * @return the media identifier for the given string representation
     */
    protected abstract ID getMediaId(String id);

    protected Class<? extends T> getTopTermsMediaClass() {
        return getMediaClass();
    }

    protected final Map<String, String> loadCsvMapping(String filename) {
        return loadCsvMapping(getClass(), filename);
    }

    protected static final Map<String, String> loadCsvMapping(Class<?> klass, String filename) {
        try {
            return CsvHelper.loadMap(klass.getResource("/mapping/" + filename));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    protected final Map<String, Map<String, String>> loadCsvMapMapping(String filename) throws IOException {
        return CsvHelper.loadMapMap(getClass().getResource("/mapping/" + filename));
    }

    protected final boolean ignoreFile(T media, String reason) {
        return MediaService.ignoreMedia(media, reason);
    }

    protected final MediaUpdateResult doCommonUpdate(T media, boolean forceUpdate) throws IOException {
        MediaUpdateResult ur = mediaService.updateMedia(media, getStringsToRemove(media),
                forceUpdate, getUrlResolver(), checkBlocklist(), includeByPerceptualHash(), null);
        boolean result = ur.getResult();
        if (media.isIgnored() != Boolean.TRUE) {
            if (media.getDescription() != null) {
                String description = media.getDescription().toLowerCase(Locale.ENGLISH);
                if (description.contains("courtesy")
                        && (findLicenceTemplates(media).isEmpty() || courtesyOk.stream().noneMatch(description::contains))) {
                    result = ignoreFile(media, "Probably non-free image (courtesy)");
                    LOGGER.debug("Courtesy test has been trigerred for {}", media);
                }
            }
            if (StringUtils.length(media.getTitle()) + StringUtils.length(media.getDescription()) <= 2) {
                // To ignore https://www.dvidshub.net/image/6592675 (title and desc are '.')
                result = ignoreFile(media, "Very short or missing title and description");
                LOGGER.debug("Short title or description test has been trigerred for {}", media);
            }
        }
        return new MediaUpdateResult(result, ur.getException());
    }

    protected boolean checkBlocklist() {
        return true;
    }

    protected boolean includeByPerceptualHash() {
        return true;
    }

    protected final boolean doCommonUpdate(T media) throws IOException {
        return doCommonUpdate(media, false).getResult();
    }

    @Override
    public int compareTo(AbstractOrgService<T, ID, D> o) {
        return getName().compareTo(o.getName());
    }

    protected int doResetIgnored() {
        return repository.resetIgnored();
    }

    protected int doResetProblems() {
        return problemRepository.deleteByOrg(getId());
    }

    public final int resetIgnored() {
        int result = doResetIgnored();
        LOGGER.info("Reset {} ignored media for org {}", result, getName());
        return result;
    }

    public final int resetProblems() {
        int result = doResetProblems();
        LOGGER.info("Reset {} problems for org {}", result, getName());
        return result;
    }

    protected final RuntimeData getRuntimeData() {
        return runtimeDataRepository.findById(getId()).orElseGet(() -> new RuntimeData(getId()));
    }

    protected final UploadMode getUploadMode() {
        return uploadMode;
    }

    protected boolean isPermittedFileType(FileMetadata metadata) {
        return commonsService.isPermittedFileExt(metadata.getExtension())
                || (metadata.getAssetUrl() != null && commonsService.isPermittedFileUrl(metadata.getAssetUrl()));
    }

    protected boolean shouldUploadAuto(T media, boolean isManual) {
        return media.getMetadata().stream().anyMatch(
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
        FileMetadata fm = metadataRepository.findByAssetUrl(assetUrl)
                .orElseGet(() -> metadataRepository.save(newFileMetadata(assetUrl, consumer)));
        media.addMetadata(fm);
        return fm;
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
                                return Arrays.stream(cat.split(";"))
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

    protected static final class UpdateFinishedException extends Exception {
        private static final long serialVersionUID = 1L;

        public UpdateFinishedException(String message) {
            super(message);
        }
    }
}
