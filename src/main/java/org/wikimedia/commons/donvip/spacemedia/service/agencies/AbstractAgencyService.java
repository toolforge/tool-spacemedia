package org.wikimedia.commons.donvip.spacemedia.service.agencies;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.collections.CollectionUtils;
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
import org.wikimedia.commons.donvip.spacemedia.data.domain.Media;
import org.wikimedia.commons.donvip.spacemedia.data.domain.MediaRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.Problem;
import org.wikimedia.commons.donvip.spacemedia.data.domain.ProblemRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.Statistics;
import org.wikimedia.commons.donvip.spacemedia.exception.ImageNotFoundException;
import org.wikimedia.commons.donvip.spacemedia.exception.ImageUploadForbiddenException;
import org.wikimedia.commons.donvip.spacemedia.service.CommonsService;
import org.wikimedia.commons.donvip.spacemedia.service.MediaService;
import org.wikimedia.commons.donvip.spacemedia.service.SearchService;
import org.wikimedia.commons.donvip.spacemedia.service.TransactionService;
import org.wikimedia.commons.donvip.spacemedia.utils.CsvHelper;
import org.xml.sax.SAXException;

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

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractAgencyService.class);

    protected final MediaRepository<T, ID, D> repository;

    @Autowired
    protected TransactionService transactionService;

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

    private boolean uploadEnabled;

    public AbstractAgencyService(MediaRepository<T, ID, D> repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    @PostConstruct
    void init() throws IOException {
        ignoredCommonTerms = CsvHelper.loadSet(getClass().getResource("/ignored.terms.csv"));
        uploadEnabled = env.getProperty(
                getClass().getSimpleName().replace("Service", "").toLowerCase(Locale.ENGLISH)
                        .replace("flickr", ".flickr").replace("dvids", ".dvids")
                        .replace("youtube", ".youtube")
                        + ".upload.enabled",
                Boolean.class, Boolean.FALSE);
    }

    /**
	 * Checks that given Commons categories exist and are not redirected. Otherwise,
	 * log a warning.
	 * 
	 * @param categories Commons categories to check
	 */
    protected void checkCommonsCategories(Map<String, String> categories) {
        Set<String> problematicCategories = categories.values().parallelStream()
				.flatMap(s -> Arrays.stream(s.split(";")))
				.filter(c -> !c.isEmpty() && !commonsService.isUpToDateCategory(c))
                .collect(Collectors.toSet());
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
                        .collect(Collectors.toList()).subList(0, 500);
            } finally {
                searchFactory.getIndexReaderAccessor().close(indexReader);
            }
        });
    }

    /**
     * Returns the space agency name, used in statistics and logs.
     * 
     * @return the space agency name
     */
    @Override
    public abstract String getName();

    /**
     * Returns an unique identifier used for REST controllers.
     * 
     * @return an unique identifier based on class name
     */
    @Override
    public String getId() {
        return getClass().getSimpleName().replace("Service", "").toLowerCase(Locale.ENGLISH);
    }

    @Override
    public abstract void updateMedia() throws IOException;

    protected final LocalDateTime startUpdateMedia() {
        LOGGER.info("Starting {} medias update...", getName());
        return LocalDateTime.now();
    }

    protected final void endUpdateMedia(int count, LocalDateTime start) {
        LOGGER.info("{} medias update completed: {} medias in {}", getName(), count,
                Duration.between(LocalDateTime.now(), start));
    }

    @Override
    public Statistics getStatistics() {
        long problems = getProblemsCount();
        return new Statistics(getName(), countAllMedia(), countIgnored(), countMissingMedia(),
                countPerceptualHashes(), problems > 0 ? problems : null);
    }

    @Override
    public final List<Problem> getProblems() {
        return problemRepository.findByAgency(getName());
    }

    @Override
    public final Page<Problem> getProblems(Pageable page) {
        return problemRepository.findByAgency(getName(), page);
    }

    @Override
    public final long getProblemsCount() {
        return problemRepository.countByAgency(getName());
    }

    protected final Problem problem(URL problematicUrl, Throwable t) {
        return problem(problematicUrl, t.getMessage());
    }

	protected final Problem problem(String problematicUrl, Throwable t) throws MalformedURLException {
		return problem(problematicUrl, t.getMessage());
	}

	protected final Problem problem(String problematicUrl, String errorMessage) throws MalformedURLException {
		return problem(new URL(problematicUrl), errorMessage);
	}

    protected final Problem problem(URL problematicUrl, String errorMessage) {
        Optional<Problem> problem = problemRepository.findByAgencyAndProblematicUrl(getName(), problematicUrl);
        if (problem.isPresent()) {
            return problem.get();
        } else {
            Problem pb = new Problem();
            pb.setAgency(getName());
            pb.setErrorMessage(errorMessage);
            pb.setProblematicUrl(problematicUrl);
            LOGGER.warn("{}", pb);
            return problemRepository.save(pb);
        }
    }

    protected T findBySha1OrThrow(String sha1) {
        List<T> result = repository.findByMetadata_Sha1(sha1);
        if (CollectionUtils.isEmpty(result)) {
            throw new ImageNotFoundException(sha1);
        }
        if (result.size() > 1) {
            throw new RuntimeException("Several images found for " + sha1);
        }
        return result.get(0);
    }

    public final boolean isUploadEnabled() {
        return uploadEnabled;
    }

    @Override
    public final T upload(String sha1) throws IOException {
        if (!isUploadEnabled()) {
            throw new ImageUploadForbiddenException("Upload is not enabled for " + getClass().getSimpleName());
        }
        T media = findBySha1OrThrow(sha1);
        checkUploadPreconditions(media);
        doUpload(getWikiCode(media), media);
        return repository.save(media);
    }

    protected void doUpload(String wikiCode, T media) throws IOException {
        media.setCommonsFileNames(
                Set.of(commonsService.upload(wikiCode, media.getUploadTitle(), media.getMetadata().getAssetUrl(),
                        media.getMetadata().getSha1())));
    }

    @Override
    public String getWikiHtmlPreview(String sha1) throws IOException, ParserConfigurationException, SAXException {
        T media = findBySha1OrThrow(sha1);
        return commonsService.getWikiHtmlPreview(getWikiCode(media), getPageTile(media),
                media.getMetadata().getAssetUrl().toExternalForm());
    }

    protected String getPageTile(T media) {
        return media.getTitle();
    }

    @Override
    public final String getWikiCode(String sha1) {
        return getWikiCode(findBySha1OrThrow(sha1));
    }

    @Override
    public final String getWikiCode(T media) {
        try {
            StringBuilder sb = new StringBuilder("== {{int:filedesc}} ==\n{{Information\n| description = ")
                    .append("{{").append(getLanguage(media)).append(
                            "|1=")
                    .append(CommonsService.formatWikiCode(getDescription(media))).append("}}");
            Optional<Temporal> creationDate = getCreationDate(media);
            if (creationDate.isPresent()) {
                Temporal d = creationDate.get();
                sb.append("\n| date = ");
                if (d instanceof LocalDateTime || d instanceof ZonedDateTime || d instanceof Instant) {
                    sb.append("{{Taken on|").append(toIso8601(d)).append("}}");
                } else {
                    sb.append("{{Taken in|").append(toIso8601(d)).append("}}");
                }
            } else {
                getUploadDate(media)
                        .ifPresent(d -> sb.append("\n| date = {{Upload date|").append(toIso8601(d)).append("}}"));
            }
            sb.append("\n| source = ").append(getSource(media)).append("\n| author = ").append(getAuthor(media));
            getPermission(media).ifPresent(s -> sb.append("\n| permission = ").append(s));
            getOtherVersions(media).ifPresent(s -> sb.append("\n| other versions = ").append(s));
            getOtherFields(media).ifPresent(s -> sb.append("\n| other fields = ").append(s));
            getOtherFields1(media).ifPresent(s -> sb.append("\n| other fields 1 = ").append(s));
            sb.append("\n}}\n=={{int:license-header}}==\n");
            findTemplates(media).forEach(t -> sb.append("{{").append(t).append("}}\n"));
			commonsService.cleanupCategories(findCategories(media, true))
					.forEach(t -> sb.append("[[Category:").append(t).append("]]\n"));
            return sb.toString();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    private String toIso8601(Temporal d) {
        if (d instanceof ZonedDateTime) {
            return toIso8601(((ZonedDateTime) d).toInstant());
        }
        return d.toString();
    }

    protected String getLanguage(T media) {
        return "en";
    }

    protected String getDescription(T media) {
        String description = media.getDescription();
        return StringUtils.isBlank(description) ? media.getTitle() : description;
    }

    @Override
    public abstract URL getSourceUrl(T media) throws MalformedURLException;

    protected String getSource(T media) throws MalformedURLException {
        return wikiLink(getSourceUrl(media), media.getTitle());
    }

    protected abstract String getAuthor(T media) throws MalformedURLException;

    @Override
    public final URL getThumbnailUrl(T media) {
        return Optional.ofNullable(media.getThumbnailUrl()).orElse(media.getMetadata().getAssetUrl());
    }

    protected Optional<Temporal> getCreationDate(T media) {
        return Optional.empty();
    }

    protected Optional<Temporal> getUploadDate(T media) {
        return Optional.empty();
    }

    protected Optional<String> getPermission(T media) {
        return Optional.empty();
    }

    protected Optional<String> getOtherVersions(T media) {
        return Optional.empty();
    }

    protected Optional<String> getOtherFields(T media) {
        return Optional.empty();
    }

    protected Optional<String> getOtherFields1(T media) {
        return Optional.empty();
    }

	public Set<String> findCategories(T media, boolean includeHidden) {
		Set<String> result = new HashSet<>();
		if (includeHidden) {
            result.add("Spacemedia files uploaded by " + commonsService.getAccount());
		}
		return result;
    }

    public List<String> findTemplates(T media) {
		return new ArrayList<>();
    }

    protected final String wikiLink(URL url, String text) {
        return "[" + Objects.requireNonNull(url, "url") + " " + Objects.requireNonNull(text, "text") + "]";
    }

    protected void checkUploadPreconditions(T media) throws IOException {
        if (Boolean.TRUE.equals(media.isIgnored())) {
            throw new ImageUploadForbiddenException(media + " is marked as ignored.");
        }
        String sha1 = media.getMetadata().getSha1();
        if (sha1 == null) {
            throw new ImageUploadForbiddenException(media + " SHA-1 has not been computed.");
        }
        // Forbid upload of duplicate medias for a single repo, they may have different descriptions
        if (repository.countByMetadata_Sha1(sha1) > 1) {
            throw new ImageUploadForbiddenException(media + " is present several times.");
        }
        // Double-check for duplicates before upload!
        if (CollectionUtils.isNotEmpty(media.getCommonsFileNames()) || mediaService.findCommonsFilesWithSha1(media)) {
            throw new ImageUploadForbiddenException(media + " is already on Commons: " + media.getCommonsFileNames());
        }
    }

    protected MediaRepository<OT, OID, OD> getOriginalRepository() {
        return null;
    }

    protected OID getOriginalId(String id) {
        return null;
    }

    public final List<OT> getOriginalMedia(T media) {
        Set<Duplicate> dupes = media.getDuplicates();
        return CollectionUtils.isEmpty(dupes) ? Collections.emptyList()
                : dupes.stream().map(d -> getOriginalRepository().findById(getOriginalId(d.getOriginalId())))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    protected abstract Class<T> getMediaClass();

    protected Class<? extends T> getTopTermsMediaClass() {
        return getMediaClass();
    }

	protected final Map<String, String> loadCsvMapping(String filename) throws IOException {
		return CsvHelper.loadMap(getClass().getResource("/mapping/" + filename));
	}

    protected final boolean ignoreFile(T media, String reason) {
        media.setIgnored(Boolean.TRUE);
        media.setIgnoredReason(reason);
        return true;
    }

    protected final boolean doCommonUpdate(T media) throws IOException {
        return mediaService.updateMedia(media, getOriginalRepository());
    }

    @Override
    public int compareTo(AbstractAgencyService<T, ID, D, OT, OID, OD> o) {
        return getName().compareTo(o.getName());
    }

    protected int doResetPerceptualHashes() {
        return repository.resetPerceptualHashes();
    }

    public final int resetPerceptualHashes() {
        int result = doResetPerceptualHashes();
        LOGGER.info("Reset {} perceptual hashes for agency {}", result, getName());
        return result;
    }
}
