package org.wikimedia.commons.donvip.spacemedia.service.agencies;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
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
import org.wikimedia.commons.donvip.spacemedia.data.domain.Duplicate;
import org.wikimedia.commons.donvip.spacemedia.data.domain.DuplicateMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.Media;
import org.wikimedia.commons.donvip.spacemedia.data.domain.MediaRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.Metadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.Problem;
import org.wikimedia.commons.donvip.spacemedia.data.domain.ProblemRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.RuntimeData;
import org.wikimedia.commons.donvip.spacemedia.data.domain.RuntimeDataRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.Statistics;
import org.wikimedia.commons.donvip.spacemedia.data.domain.UploadMode;
import org.wikimedia.commons.donvip.spacemedia.exception.ImageNotFoundException;
import org.wikimedia.commons.donvip.spacemedia.exception.ImageUploadForbiddenException;
import org.wikimedia.commons.donvip.spacemedia.exception.TooManyResultsException;
import org.wikimedia.commons.donvip.spacemedia.exception.UploadException;
import org.wikimedia.commons.donvip.spacemedia.service.ExecutionMode;
import org.wikimedia.commons.donvip.spacemedia.service.GoogleTranslateService;
import org.wikimedia.commons.donvip.spacemedia.service.MediaService;
import org.wikimedia.commons.donvip.spacemedia.service.MediaService.MediaUpdateResult;
import org.wikimedia.commons.donvip.spacemedia.service.RemoteService;
import org.wikimedia.commons.donvip.spacemedia.service.SearchService;
import org.wikimedia.commons.donvip.spacemedia.service.TransactionService;
import org.wikimedia.commons.donvip.spacemedia.service.twitter.TwitterService;
import org.wikimedia.commons.donvip.spacemedia.service.wikimedia.CommonsService;
import org.wikimedia.commons.donvip.spacemedia.utils.CsvHelper;

/**
 * Superclass of space agencies services.
 *
 * @param <T>   the media type the repository manages
 * @param <ID>  the type of the id of the entity the repository manages
 * @param <D>   the media date type
 * @param <OT>  the media type the original repository manages
 * @param <OID> the type of the id of the entity the original repository manages
 * @param <OD>  the original media date type
 */
public abstract class AbstractAgencyService<T extends Media<ID, D>, ID, D extends Temporal, OT extends Media<OID, OD>, OID, OD extends Temporal>
        implements Comparable<AbstractAgencyService<T, ID, D, OT, OID, OD>>, Agency<T, ID, D> {

    protected static final String EN = "en";

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractAgencyService.class);

    private static final Pattern PATTERN_SHORT = Pattern
            .compile("(?:https?://)?(?:bit.ly/[0-9a-zA-Z]{7}|youtu.be/[0-9a-zA-Z]{11}|flic.kr/p/[0-9a-zA-Z]{6})");

    protected final MediaRepository<T, ID, D> repository;

    private final String id;

    @Autowired
    protected TransactionService transactionService;

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
    private TwitterService twitterService;

    @Autowired
    private Environment env;

    @Autowired
    @PersistenceContext(unitName = "domain")
    private EntityManager entityManager;

    @SuppressWarnings("unused")
    private Set<String> ignoredCommonTerms;

    @Value("${courtesy.ok}")
    private Set<String> courtesyOk;

    @Value("${execution.mode}")
    private ExecutionMode executionMode;

    @Value("${upload.auto.min.year}")
    private int minYearUploadAuto;

    private UploadMode uploadMode;

    protected AbstractAgencyService(MediaRepository<T, ID, D> repository, String id) {
        this.repository = Objects.requireNonNull(repository);
        this.id = Objects.requireNonNull(id);
    }

    @PostConstruct
    void init() throws IOException {
        ignoredCommonTerms = CsvHelper.loadSet(getClass().getResource("/search.ignored.terms.csv"));
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
    public final List<T> searchMedia(String q) {
        searchService.checkSearchEnabled();
        throw new UnsupportedOperationException();
    }

    @Override
    public final Page<T> searchMedia(String q, Pageable page) {
        searchService.checkSearchEnabled();
        throw new UnsupportedOperationException();
    }

    /**
     * Returns an unique identifier used for REST controllers and database entries.
     *
     * @return an unique identifier specified by implementations
     */
    @Override
    public final String getId() {
        return id;
    }

    protected final LocalDateTime startUpdateMedia() {
        Thread.currentThread().setName("media-update-" + getId());
        LOGGER.info("Starting {} medias update...", getName());
        RuntimeData runtimeData = getRuntimeData();
        runtimeData.setLastUpdateStart(LocalDateTime.now());
        return runtimeDataRepository.save(runtimeData).getLastUpdateStart();
    }

    protected final void endUpdateMedia(int count, Collection<T> uploadedMedia, Collection<Metadata> uploadedMetadata,
            LocalDateTime start) {
        endUpdateMedia(count, uploadedMedia, uploadedMetadata, start, true);
    }

    protected final void endUpdateMedia(int count, Collection<T> uploadedMedia, Collection<Metadata> uploadedMetadata,
            LocalDateTime start, boolean postTweet) {
        RuntimeData runtimeData = getRuntimeData();
        LocalDateTime end = LocalDateTime.now();
        runtimeData.setLastUpdateEnd(end);
        runtimeData.setLastUpdateDuration(Duration.between(start, end));
        LOGGER.info("{} medias update completed: {} medias in {}", getName(), count,
                runtimeDataRepository.save(runtimeData).getLastUpdateDuration());
        if (postTweet) {
            postTweet(uploadedMedia, uploadedMetadata);
        }
    }

    protected final void postTweet(Collection<? extends T> uploadedMedia, Collection<Metadata> uploadedMetadata) {
        if (!uploadedMedia.isEmpty()) {
            LOGGER.info("Uploaded media: {} ({})", uploadedMedia.size(),
                    uploadedMedia.stream().map(Media::getId).toList());
            try {
                twitterService.tweet(uploadedMedia, uploadedMetadata, getTwitterAccounts(uploadedMedia));
            } catch (IOException e) {
                LOGGER.error("Failed to post tweet", e);
            }
        }
    }

    protected final Set<String> getTwitterAccounts(Collection<? extends T> uploadedMedia) {
        return uploadedMedia.stream().flatMap(media -> getTwitterAccounts(media).stream()).collect(toSet());
    }

    protected abstract Set<String> getTwitterAccounts(T uploadedMedia);

    @Override
    public Statistics getStatistics(boolean details) {
        long problems = getProblemsCount();
        return new Statistics(getName(), getId(), countAllMedia(), countUploadedMedia(), countIgnored(),
                countMissingImages(), countMissingVideos(), countPerceptualHashes(), problems > 0 ? problems : null);
    }

    @Override
    public final List<Problem> getProblems() {
        return problemRepository.findByAgency(getId());
    }

    @Override
    public final Page<Problem> getProblems(Pageable page) {
        return problemRepository.findByAgency(getId(), page);
    }

    @Override
    public final long getProblemsCount() {
        return problemRepository.countByAgency(getId());
    }

    protected final Problem problem(URL problematicUrl, Throwable t) {
        return problem(problematicUrl, t.toString());
    }

    protected final Problem problem(String problematicUrl, Throwable t) throws MalformedURLException {
        return problem(problematicUrl, t.toString());
    }

    protected final Problem problem(String problematicUrl, String errorMessage) throws MalformedURLException {
        return problem(new URL(problematicUrl), errorMessage);
    }

    protected final Problem problem(URL problematicUrl, String errorMessage) {
        Optional<Problem> problem = problemRepository.findByAgencyAndProblematicUrl(getId(), problematicUrl);
        if (problem.isPresent()) {
            return problem.get();
        } else {
            Problem pb = new Problem();
            pb.setAgency(getId());
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

    protected final T findBySha1OrThrow(String sha1, boolean throwIfNotFound) throws TooManyResultsException {
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
    public final T refreshAndSaveById(String id) throws ImageNotFoundException, IOException {
        return refreshAndSave(getById(id));
    }

    @Override
    public T refreshAndSave(T media) throws IOException {
        media = refresh(media);
        doCommonUpdate(media, true);
        return saveMedia(media);
    }

    protected abstract T refresh(T media) throws IOException;

    @Override
    public T saveMedia(T media) {
        T result = repository.save(media);
        checkRemoteMedia(result);
        return result;
    }

    protected final void checkRemoteMedia(T media) {
        if (executionMode == ExecutionMode.REMOTE
                && remoteService.getMedia(getId(), media.getId().toString(), media.getClass()) == null) {
            remoteService.saveMedia(getId(), media);
        } else if (executionMode == ExecutionMode.LOCAL) {
            try {
                remoteService.evictCaches(getId());
            } catch (RestClientException e) {
                LOGGER.warn("{}", e.getMessage(), e);
            }
        }
    }

    protected final T saveMediaOrCheckRemote(boolean save, T media) {
        if (save) {
            return saveMedia(media);
        } else {
            checkRemoteMedia(media);
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
    public final T uploadAndSaveById(String id, boolean isManual) throws UploadException, TooManyResultsException {
        return saveMedia(upload(getById(id), false, isManual).getLeft());
    }

    @Override
    public T uploadAndSaveBySha1(String sha1, boolean isManual) throws UploadException, TooManyResultsException {
        return saveMedia(upload(findBySha1OrThrow(sha1, true), true, isManual).getLeft());
    }

    @Override
    public final Triple<T, Collection<Metadata>, Integer> upload(T media, boolean checkUnicity, boolean isManual)
            throws UploadException {
        if (!isUploadEnabled()) {
            throw new ImageUploadForbiddenException("Upload is not enabled for " + getClass().getSimpleName());
        }
        try {
            checkUploadPreconditions(media, checkUnicity);
            List<Metadata> uploaded = new ArrayList<>();
            return Triple.of(media, uploaded, doUpload(media, checkUnicity, uploaded, isManual));
        } catch (IOException | RuntimeException e) {
            throw new UploadException(e);
        }
    }

    protected int doUpload(T media, boolean checkUnicity, Collection<Metadata> uploaded, boolean isManual)
            throws IOException, UploadException {
        return doUpload(media, media.getMetadata(), media::getCommonsFileNames, media::setCommonsFileNames,
                checkUnicity, uploaded, isManual);
    }

    protected final int doUpload(T media, Metadata metadata, Supplier<Set<String>> getter,
            Consumer<Set<String>> setter, boolean checkUnicity, Collection<Metadata> uploaded, boolean isManual)
            throws IOException, UploadException {
        if (metadata != null && metadata.getAssetUrl() != null
                && shouldUpload(new UploadContext<>(media, metadata, getter.get(), isManual))) {
            checkUploadPreconditions(media, metadata, getter.get(), checkUnicity);
            setter.accept(new HashSet<>(Set.of(
                    commonsService.upload(getWikiCode(media, metadata), media.getUploadTitle(),
                            metadata.getFileExtension(), metadata.getAssetUrl(), metadata.getSha1()))));
            uploaded.add(metadata);
            return 1;
        } else {
            LOGGER.info(
                    "Upload not done for {} / {}. Upload mode: {}. Ignored: {}. Commons filenames: {}. Permitted file type: {}",
                    media.getId(), metadata, getUploadMode(), media.isIgnored(), getter.get(),
                    isPermittedFileType(metadata));
            return 0;
        }
    }

    @Override
    public String getWikiHtmlPreview(String sha1) throws TooManyResultsException {
        T media = findBySha1OrThrow(sha1, true);
        Metadata metadata = media.getMetadata();
        return getWikiHtmlPreview(media, metadata);
    }

    protected final String getWikiHtmlPreview(T media, Metadata metadata) {
        try {
            return commonsService.getWikiHtmlPreview(getWikiCode(media, metadata), getPageTile(media),
                    metadata.getAssetUrl().toExternalForm());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected String getPageTile(T media) {
        return media.getTitle();
    }

    @Override
    public String getWikiCode(String sha1) throws TooManyResultsException {
        T media = findBySha1OrThrow(sha1, true);
        return getWikiCode(media, media.getMetadata());
    }

    @Override
    public final String getWikiCode(T media, Metadata metadata) {
        try {
            StringBuilder sb = new StringBuilder("== {{int:filedesc}} ==\n")
                    .append(getWikiFileDesc(media, metadata))
                    .append("\n=={{int:license-header}}==\n");
            findTemplates(media).forEach(t -> sb.append("{{").append(t).append("}}\n"));
            commonsService.cleanupCategories(findCategories(media, metadata, true), media.getDate())
                    .forEach(t -> sb.append("[[Category:").append(t).append("]]\n"));
            return sb.toString();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    protected String getWikiFileDesc(T media, Metadata metadata) throws IOException {
        String language = getLanguage(media);
        String description = getDescription(media);
        StringBuilder sb = new StringBuilder("{{Information\n| description = ");
        appendWikiDescriptionInLanguage(sb, language, description);
        if (!"en".equals(language)) {
            appendWikiDescriptionInLanguage(sb, "en", translateService.translate(description, language, "en"));
        }
        getWikiDate(media).ifPresent(s -> sb.append("\n| date = ").append(s));
        sb.append("\n| source = ").append(getSource(media))
          .append("\n| author = ").append(CommonsService.formatWikiCode(getAuthor(media)));
        getPermission(media).ifPresent(s -> sb.append("\n| permission = ").append(s));
        getOtherVersions(media, metadata).ifPresent(s -> sb.append("\n| other versions = <gallery>\n").append(s).append("\n</gallery>"));
        getOtherFields(media).ifPresent(s -> sb.append("\n| other fields = ").append(s));
        getOtherFields1(media).ifPresent(s -> sb.append("\n| other fields 1 = ").append(s));
        sb.append("\n}}");
        return sb.toString();
    }

    protected final void appendWikiDescriptionInLanguage(StringBuilder sb, String language, String description) {
        sb.append("{{").append(language).append("|1=").append(CommonsService.formatWikiCode(description)).append("}}");
    }

    protected final Optional<String> getWikiDate(T media) {
        return getCreationWikiDate(media).or(() -> getUploadWikiDate(media));
    }

    protected final Optional<String> getCreationWikiDate(T media) {
        return getCreationDate(media).map(d -> String.format(
                "{{Taken %s|%s}}", d instanceof LocalDate || d instanceof LocalDateTime || d instanceof ZonedDateTime
                        || d instanceof Instant ? "on" : "in",
                toIso8601(d)));
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
            return PATTERN_SHORT.matcher(description).replaceAll(match -> {
                String group = match.group();
                try {
                    String url = group.startsWith("http") ? group : "https://" + group;
                    try (CloseableHttpClient httpclient = HttpClientBuilder.create().disableAutomaticRetries()
                            .disableRedirectHandling().build();
                            CloseableHttpResponse response = httpclient
                                    .execute(new HttpGet(url.replace("http://", "https://")))) {
                        Header location = response.getFirstHeader("Location");
                        if (location != null) {
                            return location.getValue().replace("&feature=youtu.be", "");
                        }
                    }
                } catch (IOException e) {
                    LOGGER.error(group + " -> " + e.getMessage(), e);
                }
                return group;
            });
        }
    }

    protected String getSource(T media) throws MalformedURLException {
        return wikiLink(getSourceUrl(media), media.getTitle());
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

    protected Optional<String> getOtherVersions(T media, Metadata metadata) {
        Set<Duplicate> variants = media.getVariants();
        return isEmpty(variants)
                ? Optional.empty()
                : Optional.of(variants.stream().sorted(Comparator.comparing(Duplicate::getOriginalId))
                    .map(v -> getOriginalRepository().findById(getOriginalId(v.getOriginalId())))
                    .filter(Optional::isPresent).map(Optional::get)
                    .map(o -> o.getFirstCommonsFileNameOrUploadTitle(o.getCommonsFileNames(), o.getMetadata().getFileExtension()))
                        .collect(joining("\n")));
    }

    protected Optional<String> getOtherFields(T media) {
        return Optional.empty();
    }

    protected Optional<String> getOtherFields1(T media) {
        return Optional.empty();
    }

    /**
     * Returns the list of Wikimedia Commons categories to apply to the given media.
     *
     * @param media the media for which category names are wanted
     * @param metadata metadata of media being uploaded
     * @param includeHidden {@code true} if hidden categories are wanted
     * @return the list of Wikimedia Commons categories to apply to {@code media}
     */
    public Set<String> findCategories(T media, Metadata metadata, boolean includeHidden) {
        Set<String> result = new HashSet<>();
        if (includeHidden) {
            result.add("Spacemedia files uploaded by " + commonsService.getAccount());
        }
        return result;
    }

    /**
     * Returns the list of Wikimedia Commons templates to apply to the given media.
     *
     * @param media the media for which template names are wanted
     * @return the list of Wikimedia Commons templates to apply to {@code media}
     */
    public Set<String> findTemplates(T media) {
        return new LinkedHashSet<>();
    }

    protected final String wikiLink(URL url, String text) {
        return "[" + Objects.requireNonNull(url, "url") + " " + Objects.requireNonNull(text, "text") + "]";
    }

    protected void checkUploadPreconditions(T media, boolean checkUnicity) throws MalformedURLException {
        if (Boolean.TRUE.equals(media.isIgnored())) {
            throw new ImageUploadForbiddenException(media + " is marked as ignored.");
        }
    }

    protected void checkUploadPreconditions(T media, Metadata metadata, Set<String> commonsFileNames, boolean checkUnicity) throws IOException {
        String sha1 = metadata.getSha1();
        if (sha1 == null) {
            throw new ImageUploadForbiddenException(media + " SHA-1 has not been computed.");
        }
        // Forbid upload of duplicate medias for a single repo, they may have different descriptions
        if (checkUnicity && repository.countByMetadata_Sha1(sha1) > 1) {
            throw new ImageUploadForbiddenException(media + " is present several times.");
        }
        // Double-check for duplicates before upload!
        if (isNotEmpty(commonsFileNames)) {
            throw new ImageUploadForbiddenException(media + " is already on Commons: " + media.getCommonsFileNames());
        }
        if (mediaService.findCommonsFiles(media)) {
            media = saveMedia(media);
            throw new ImageUploadForbiddenException(media + " is already on Commons: " + media.getCommonsFileNames());
        }
        if (findTemplates(media).isEmpty()) {
            throw new ImageUploadForbiddenException(media + " has no template, so may be not free");
        }
    }

    protected MediaRepository<OT, OID, OD> getOriginalRepository() {
        return null;
    }

    /**
     * Returns the original media identifier for the given string representation.
     *
     * @param id the string representation of an original media identifier
     * @return the original media identifier for the given string representation
     */
    protected OID getOriginalId(String id) {
        return null;
    }

    public final List<DuplicateMedia<OID, OD, OT>> getOriginalMedia(T media) {
        Set<Duplicate> dupes = media.getDuplicates();
        return isEmpty(dupes) ? Collections.emptyList()
                : dupes.stream().sorted().map(this::mapDuplicateMedia).filter(Objects::nonNull).toList();
    }

    private DuplicateMedia<OID, OD, OT> mapDuplicateMedia(Duplicate duplicate) {
        Optional<OT> optional = getOriginalRepository().findById(getOriginalId(duplicate.getOriginalId()));
        return optional.isPresent() ? new DuplicateMedia<>(duplicate, optional.get()) : null;
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

    protected final boolean ignoreFile(T media, String reason) {
        media.setIgnored(Boolean.TRUE);
        media.setIgnoredReason(reason);
        return true;
    }

    protected final MediaUpdateResult doCommonUpdate(T media, boolean forceUpdate) throws IOException {
        MediaUpdateResult ur = mediaService.updateMedia(media, getOriginalRepository(), forceUpdate,
                checkBlocklist(), null);
        boolean result = false;
        if (media.getDescription() != null) {
            String description = media.getDescription().toLowerCase(Locale.ENGLISH);
            if (description.contains("courtesy")
                    && (findTemplates(media).isEmpty() || courtesyOk.stream().noneMatch(description::contains))) {
                result = ignoreFile(media, "Probably non-free image (courtesy)");
            }
        }
        if (StringUtils.length(media.getTitle()) + StringUtils.length(media.getDescription()) <= 2) {
            // To ignore https://www.dvidshub.net/image/6592675 (title and desc are '.')
            result = ignoreFile(media, "Very short or missing title and description");
        }
        return new MediaUpdateResult(result, ur.getException());
    }

    protected boolean checkBlocklist() {
        return true;
    }

    protected final boolean doCommonUpdate(T media) throws IOException {
        return doCommonUpdate(media, false).getResult();
    }

    @Override
    public int compareTo(AbstractAgencyService<T, ID, D, OT, OID, OD> o) {
        return getName().compareTo(o.getName());
    }

    protected List<T> findDuplicates() {
        return repository.findByDuplicatesIsNotEmpty();
    }

    protected long doResetDuplicates() {
        return StreamSupport.stream(repository.saveAll(
                findDuplicates().stream().map(m -> {
                    m.clearDuplicates();
                    m.setIgnoredReason(null);
                    m.setIgnored(null);
                    return m;
                }).toList()).spliterator(), false).count();
    }

    protected int doResetPerceptualHashes() {
        return repository.resetPerceptualHashes();
    }

    protected int doResetSha1Hashes() {
        return repository.resetSha1Hashes();
    }

    protected int doResetIgnored() {
        return repository.resetIgnored();
    }

    protected int doResetProblems() {
        return problemRepository.deleteByAgency(getId());
    }

    public final long resetDuplicates() {
        long result = doResetDuplicates();
        LOGGER.info("Reset {} duplicates for agency {}", result, getName());
        return result;
    }

    public final int resetIgnored() {
        int result = doResetIgnored();
        LOGGER.info("Reset {} ignored media for agency {}", result, getName());
        return result;
    }

    public final int resetPerceptualHashes() {
        int result = doResetPerceptualHashes();
        LOGGER.info("Reset {} perceptual hashes for agency {}", result, getName());
        return result;
    }

    public final int resetSha1Hashes() {
        int result = doResetSha1Hashes();
        LOGGER.info("Reset {} SHA-1 hashes for agency {}", result, getName());
        return result;
    }

    public final int resetProblems() {
        int result = doResetProblems();
        LOGGER.info("Reset {} problems for agency {}", result, getName());
        return result;
    }

    protected final RuntimeData getRuntimeData() {
        return runtimeDataRepository.findById(getId()).orElseGet(() -> new RuntimeData(getId()));
    }

    protected final UploadMode getUploadMode() {
        return uploadMode;
    }

    protected boolean isPermittedFileType(Metadata metadata) {
        return metadata.getAssetUrl() != null
                && commonsService.isPermittedFileType(metadata.getAssetUrl().toExternalForm());
    }

    protected final boolean shouldUpload(UploadContext<T> ctx) {
        return (getUploadMode() == UploadMode.AUTO
                || (getUploadMode() == UploadMode.AUTO_FROM_DATE
                        && (ctx.isManual() || ctx.getMedia().getYear().getValue() >= minYearUploadAuto))
                || getUploadMode() == UploadMode.MANUAL)
                && !Boolean.TRUE.equals(ctx.getMedia().isIgnored()) && isEmpty(ctx.getCommonsFilenames())
                && isPermittedFileType(ctx.getMetadata());
    }

    protected final boolean shouldUploadAuto(UploadContext<T> ctx) {
        return (getUploadMode() == UploadMode.AUTO
                || (getUploadMode() == UploadMode.AUTO_FROM_DATE
                        && (ctx.isManual() || ctx.getMedia().getYear().getValue() >= minYearUploadAuto)))
                && !Boolean.TRUE.equals(ctx.getMedia().isIgnored()) && isEmpty(ctx.getCommonsFilenames())
                && isEmpty(ctx.getMedia().getDuplicates()) && isPermittedFileType(ctx.getMetadata());
    }

    protected boolean shouldUploadAuto(T media, boolean isManual) {
        return shouldUploadAuto(new UploadContext<>(media, media.getMetadata(), media.getCommonsFileNames(), isManual));
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
}
