package org.wikimedia.commons.donvip.spacemedia.service.agencies;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
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
import java.util.stream.StreamSupport;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.misc.HighFreqTerms;
import org.apache.lucene.misc.HighFreqTerms.DocFreqComparator;
import org.apache.lucene.misc.TermStats;
import org.apache.lucene.search.Query;
import org.hibernate.search.SearchFactory;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.FullTextQuery;
import org.hibernate.search.jpa.Search;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.query.dsl.SimpleQueryStringMatchingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
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
import org.wikimedia.commons.donvip.spacemedia.service.CommonsService;
import org.wikimedia.commons.donvip.spacemedia.service.MediaService;
import org.wikimedia.commons.donvip.spacemedia.service.SearchService;
import org.wikimedia.commons.donvip.spacemedia.service.TransactionService;
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
    private Environment env;

    @Autowired
    @PersistenceContext(unitName = "domain")
    private EntityManager entityManager;

    private Set<String> ignoredCommonTerms;

    private UploadMode uploadMode;

    protected AbstractAgencyService(MediaRepository<T, ID, D> repository, String id) {
        this.repository = Objects.requireNonNull(repository);
        this.id = Objects.requireNonNull(id);
    }

    @PostConstruct
    void init() throws IOException {
        ignoredCommonTerms = CsvHelper.loadSet(getClass().getResource("/ignored.terms.csv"));
        uploadMode = UploadMode.valueOf(
                env.getProperty(id + ".upload", String.class, UploadMode.DISABLED.name())
                    .toUpperCase(Locale.ENGLISH));
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

    /**
     * Builds the Lucene search query to provide to Hibernate Search.
     *
     * @param queryBuilder the query builder
     * @param context a simple query search context, initialized to search on
     *            "title" and "description"
     * @param q the search string
     * @return the lucene query obtained from the simple query search context and
     *         potentially more field
     */
    protected Query getSearchQuery(QueryBuilder queryBuilder, SimpleQueryStringMatchingContext context, String q) {
        return context.withAndAsDefaultOperator().matching(q).createQuery();
    }

    /**
     * Builds the Hibernate Search query.
     *
     * @param q the search string
     * @param searchEntityManager the entity manager
     * @return the Hibernate Search query
     */
    private FullTextQuery getFullTextQuery(String q, EntityManager searchEntityManager) {
        FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(searchEntityManager);
        QueryBuilder queryBuilder = fullTextEntityManager.getSearchFactory().buildQueryBuilder()
                .forEntity(getMediaClass()).get();
        return fullTextEntityManager.createFullTextQuery(getSearchQuery(queryBuilder,
                queryBuilder.simpleQueryString().onField("title").boostedTo(5f).andField("description").boostedTo(2f),
                q),
                getMediaClass());
    }

    @Override
    @SuppressWarnings("unchecked")
    public final List<T> searchMedia(String q) {
        searchService.checkSearchEnabled();
        return transactionService.doInTransaction(() -> getFullTextQuery(q, entityManager).getResultList());
    }

    @Override
    @SuppressWarnings("unchecked")
    public final Page<T> searchMedia(String q, Pageable page) {
        searchService.checkSearchEnabled();
        return transactionService.doInTransaction(() -> {
            FullTextQuery fullTextQuery = getFullTextQuery(q, entityManager);
            fullTextQuery.setFirstResult(page.getPageNumber() * page.getPageSize());
            fullTextQuery.setMaxResults(page.getPageSize());
            return new PageImpl<>(fullTextQuery.getResultList(), page, fullTextQuery.getResultSize());
        });
    }

    @Override
    public final List<TermStats> getTopTerms() throws Exception {
        searchService.checkSearchEnabled();
        return transactionService.doInTransaction(() -> {
            SearchFactory searchFactory = Search.getFullTextEntityManager(entityManager).getSearchFactory();
            IndexReader indexReader = searchFactory.getIndexReaderAccessor().open(getTopTermsMediaClass());
            try {
                return Arrays
                        .stream(HighFreqTerms.getHighFreqTerms(indexReader, 1000, "description", new DocFreqComparator()))
                        .filter(ts -> {
                            String s = ts.termtext.utf8ToString();
                            return s.length() > 1 && !ignoredCommonTerms.contains(s) && !s.matches("\\d+");
                        })
                        .collect(toList()).subList(0, 500);
            } finally {
                searchFactory.getIndexReaderAccessor().close(indexReader);
            }
        });
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
        LOGGER.info("Starting {} medias update...", getName());
        RuntimeData runtimeData = getRuntimeData();
        runtimeData.setLastUpdateStart(LocalDateTime.now());
        return runtimeDataRepository.save(runtimeData).getLastUpdateStart();
    }

    protected final void endUpdateMedia(int count, LocalDateTime start) {
        RuntimeData runtimeData = getRuntimeData();
        LocalDateTime end = LocalDateTime.now();
        runtimeData.setLastUpdateEnd(end);
        runtimeData.setLastUpdateDuration(Duration.between(start, end));
        LOGGER.info("{} medias update completed: {} medias in {}", getName(), count,
                runtimeDataRepository.save(runtimeData).getLastUpdateDuration());
    }

    @Override
    public Statistics getStatistics(boolean details) {
        long problems = getProblemsCount();
        return new Statistics(getName(), getId(), countAllMedia(), countUploadedMedia(), countIgnored(), countMissingMedia(),
                countPerceptualHashes(), problems > 0 ? problems : null);
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
    public final T refreshAndSaveById(String id) throws ImageNotFoundException, IOException {
        T media = refresh(repository.findById(getMediaId(id)).orElseThrow(() -> new ImageNotFoundException(id)));
        doCommonUpdate(media, true);
        return repository.save(media);
    }

    protected abstract T refresh(T media) throws IOException;

    public final boolean isUploadEnabled() {
        return uploadMode == UploadMode.MANUAL || uploadMode == UploadMode.AUTO;
    }

    @Override
    public final T uploadAndSaveById(String id) throws UploadException, TooManyResultsException {
        return repository.save(upload(repository.findById(getMediaId(id)).orElseThrow(() -> new ImageNotFoundException(id)), false));
    }

    @Override
    public T uploadAndSaveBySha1(String sha1) throws UploadException, TooManyResultsException {
        return repository.save(upload(findBySha1OrThrow(sha1, true), true));
    }

    @Override
    public final T upload(T media, boolean checkUnicity) throws UploadException {
        if (!isUploadEnabled()) {
            throw new ImageUploadForbiddenException("Upload is not enabled for " + getClass().getSimpleName());
        }
        try {
            checkUploadPreconditions(media, checkUnicity);
            doUpload(media, checkUnicity);
        } catch (IOException | RuntimeException e) {
            throw new UploadException(e);
        }
        return media;
    }

    protected void doUpload(T media, boolean checkUnicity) throws IOException, UploadException {
        doUpload(media, media.getMetadata(), media::getCommonsFileNames, media::setCommonsFileNames, checkUnicity);
    }

    protected final void doUpload(T media, Metadata metadata, Supplier<Set<String>> getter, Consumer<Set<String>> setter, boolean checkUnicity)
            throws IOException, UploadException {
        if (metadata != null && metadata.getAssetUrl() != null && shouldUpload(media, getter.get())) {
            checkUploadPreconditions(media, metadata, getter.get(), checkUnicity);
            setter.accept(new HashSet<>(Set.of(
                    commonsService.upload(getWikiCode(media, metadata), media.getUploadTitle(), metadata.getAssetUrl(), metadata.getSha1()))));
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
            commonsService.cleanupCategories(findCategories(media, true))
                    .forEach(t -> sb.append("[[Category:").append(t).append("]]\n"));
            return sb.toString();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    protected String getWikiFileDesc(T media, Metadata metadata) throws MalformedURLException {
        StringBuilder sb = new StringBuilder("{{Information\n| description = ")
                .append("{{").append(getLanguage(media)).append("|1=")
                .append(CommonsService.formatWikiCode(getDescription(media))).append("}}");
        getWikiDate(media).ifPresent(s -> sb.append("\n| date = ").append(s));
        sb.append("\n| source = ").append(getSource(media))
          .append("\n| author = ").append(getAuthor(media));
        getPermission(media).ifPresent(s -> sb.append("\n| permission = ").append(s));
        getOtherVersions(media, metadata).ifPresent(s -> sb.append("\n| other versions = <gallery>\n").append(s).append("\n</gallery>"));
        getOtherFields(media).ifPresent(s -> sb.append("\n| other fields = ").append(s));
        getOtherFields1(media).ifPresent(s -> sb.append("\n| other fields 1 = ").append(s));
        sb.append("\n}}");
        return sb.toString();
    }

    protected final Optional<String> getWikiDate(T media) {
        Optional<Temporal> creationDate = getCreationDate(media);
        if (creationDate.isPresent()) {
            Temporal d = creationDate.get();
            return Optional.of(String.format("{{Taken %s|%s}}",
                    d instanceof LocalDate || d instanceof LocalDateTime || d instanceof ZonedDateTime || d instanceof Instant ? "on" : "in",
                            toIso8601(d)));
        } else {
            return getUploadDate(media).map(d -> String.format("{{Upload date|%s}}", toIso8601(d)));
        }
    }

    protected final String toIso8601(Temporal t) {
        Temporal d = t;
        if (d instanceof Instant) {
            d = ((Instant) d).atZone(ZoneOffset.UTC);
        }
        if ((d instanceof LocalDateTime || d instanceof ZonedDateTime)
                && d.get(ChronoField.SECOND_OF_MINUTE) == 0 && d.get(ChronoField.MINUTE_OF_HOUR) == 0) {
            d = LocalDate.of(d.get(ChronoField.YEAR), d.get(ChronoField.MONTH_OF_YEAR), d.get(ChronoField.DAY_OF_MONTH));
        }
        if (d instanceof ZonedDateTime) {
            return ((ZonedDateTime) d).toInstant().toString();
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
        return StringUtils.isBlank(description) ? media.getTitle() : description;
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
     * @param includeHidden {@code true} if hidden categories are wanted
     * @return the list of Wikimedia Commons categories to apply to {@code media}
     */
    public Set<String> findCategories(T media, boolean includeHidden) {
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

    protected void checkUploadPreconditions(T media, boolean checkUnicity) throws IOException {
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
        if (mediaService.findCommonsFilesWithSha1(media) || mediaService.findCommonsFilesWithPhash(media)) {
            media = repository.save(media);
            throw new ImageUploadForbiddenException(media + " is already on Commons: " + media.getCommonsFileNames());
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
                : dupes.stream().sorted().map(this::mapDuplicateMedia).filter(Objects::nonNull).collect(toList());
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

    protected final boolean doCommonUpdate(T media, boolean forceUpdate) throws IOException {
        return mediaService.updateMedia(media, getOriginalRepository(), forceUpdate);
    }

    protected final boolean doCommonUpdate(T media) throws IOException {
        return doCommonUpdate(media, false);
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
                }).collect(toList())).spliterator(), false).count();
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

    protected final boolean isPermittedFileType(Metadata metadata) {
        return metadata.getAssetUrl() == null
                || commonsService.isPermittedFileType(metadata.getAssetUrl().toExternalForm());
    }

    protected boolean isPermittedFileType(T media) {
        return isPermittedFileType(media.getMetadata());
    }

    protected final boolean shouldUpload(T media, Set<String> commonsFilenames) {
        return (getUploadMode() == UploadMode.AUTO || getUploadMode() == UploadMode.MANUAL)
                && !Boolean.TRUE.equals(media.isIgnored()) && isEmpty(commonsFilenames) && isPermittedFileType(media);
    }

    protected final boolean shouldUploadAuto(T media, Set<String> commonsFilenames) {
        return getUploadMode() == UploadMode.AUTO
                && !Boolean.TRUE.equals(media.isIgnored()) && isEmpty(commonsFilenames)
                && isEmpty(media.getDuplicates()) && isPermittedFileType(media);
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
