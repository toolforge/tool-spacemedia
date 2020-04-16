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
import javax.persistence.EntityManagerFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.ClientProtocolException;
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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.wikimedia.commons.donvip.spacemedia.data.domain.Media;
import org.wikimedia.commons.donvip.spacemedia.data.domain.MediaRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.Problem;
import org.wikimedia.commons.donvip.spacemedia.data.domain.ProblemRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.Statistics;
import org.wikimedia.commons.donvip.spacemedia.exception.ImageNotFoundException;
import org.wikimedia.commons.donvip.spacemedia.exception.ImageUploadForbiddenException;
import org.wikimedia.commons.donvip.spacemedia.service.CommonsService;
import org.wikimedia.commons.donvip.spacemedia.service.MediaService;
import org.wikimedia.commons.donvip.spacemedia.utils.Csv;
import org.xml.sax.SAXException;

/**
 * Superclass of space agencies services.
 * 
 * @param <T> the media type the repository manages
 * @param <ID> the type of the id of the entity the repository manages
 * @param <D> the media date type
 */
public abstract class AbstractSpaceAgencyService<T extends Media<ID, D>, ID, D extends Temporal>
        implements Comparable<AbstractSpaceAgencyService<T, ID, D>>, SpaceAgency<T, ID, D> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractSpaceAgencyService.class);

    protected final MediaRepository<T, ID, D> repository;

    @Autowired
    protected ProblemRepository problemRepository;
    @Autowired
    protected MediaService mediaService;
    @Autowired
    protected CommonsService commonsService;

    @Autowired
    private Environment env;

    @Autowired
    @Qualifier("domainEntityManagerFactory")
    private EntityManagerFactory entityManagerFactory;

    private Set<String> ignoredCommonTerms;

    public AbstractSpaceAgencyService(MediaRepository<T, ID, D> repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    @PostConstruct
    void init() throws IOException {
        ignoredCommonTerms = Csv.loadSet(getClass().getResource("/ignored.terms.csv"));
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
        return getFullTextQuery(q, entityManagerFactory.createEntityManager()).getResultList();
    }

    @Override
    @SuppressWarnings("unchecked")
    public final Page<T> searchMedia(String q, Pageable page) {
        EntityManager searchEntityManager = entityManagerFactory.createEntityManager();
        searchEntityManager.getTransaction().begin();
        try {
            FullTextQuery fullTextQuery = getFullTextQuery(q, searchEntityManager);
            fullTextQuery.setFirstResult(page.getPageNumber() * page.getPageSize());
            fullTextQuery.setMaxResults(page.getPageSize());
            return new PageImpl<>(fullTextQuery.getResultList(), page, fullTextQuery.getResultSize());
        } finally {
            searchEntityManager.getTransaction().commit();
        }
    }

    @Override
    public final List<TermStats> getTopTerms() throws Exception {
        EntityManager searchEntityManager = entityManagerFactory.createEntityManager();
        SearchFactory searchFactory = Search.getFullTextEntityManager(searchEntityManager).getSearchFactory();
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
        ignoreMediaInOriginalRepository();
        LOGGER.info("{} medias update completed: {} medias in {}", getName(), count,
                Duration.between(LocalDateTime.now(), start));
    }

    @Override
    public Statistics getStatistics() {
        long problems = getProblemsCount();
        return new Statistics(getName(), countAllMedia(), countIgnored(), countMissingMedia(),
                problems > 0 ? problems : null);
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
            LOGGER.warn(pb.toString());
            return problemRepository.save(pb);
        }
    }

    protected T findBySha1OrThrow(String sha1) {
        return repository.findBySha1(sha1).orElseThrow(() -> new ImageNotFoundException(sha1));
    }

    @Override
    public final T upload(String sha1) throws IOException {
        if (!env.getProperty(getClass().getName() + ".upload.enabled", Boolean.class, Boolean.FALSE)) {
            throw new ImageUploadForbiddenException("Upload is not enabled for " + getClass().getSimpleName());
        }
        T media = findBySha1OrThrow(sha1);
        checkUploadPreconditions(media);
        doUpload(getWikiCode(media), media);
        return repository.save(media);
    }

    protected void doUpload(String wikiCode, T media) throws IOException {
        commonsService.upload(wikiCode, media.getTitle(), media.getAssetUrl());
    }

    @Override
    public String getWikiHtmlPreview(String sha1)
            throws ClientProtocolException, IOException, ParserConfigurationException, SAXException {
        T media = findBySha1OrThrow(sha1);
        return commonsService.getWikiHtmlPreview(getWikiCode(media), getPageTile(media),
                media.getAssetUrl().toExternalForm());
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
                    sb.append("{{Taken on|").append(d).append("}}");
                } else {
                    sb.append("{{Taken in|").append(d).append("}}");
                }
            } else {
                getUploadDate(media).ifPresent(d -> sb.append("\n| date = {{Upload date|").append(d).append("}}"));
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
        return Optional.ofNullable(media.getThumbnailUrl()).orElse(media.getAssetUrl());
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
			result.add("Spacemedia files uploaded by Vipbot");
		}
		return result;
    }

    public List<String> findTemplates(T media) {
        // TODO
		return new ArrayList<>();
    }

    protected void ignoreMediaInOriginalRepository() {
        if (getOriginalRepository() != null) {
            for (T m : repository.findMissingInCommons()) {
                if (getOriginalRepository().countBySha1(m.getSha1()) > 0) {
                    m.setIgnored(true);
                    m.setIgnoredReason("Already present in main repository.");
                    repository.save(m);
                }
            }
        }
    }

    protected final String wikiLink(URL url, String text) {
        return "[" + Objects.requireNonNull(url, "url") + " " + Objects.requireNonNull(text, "text") + "]";
    }

    protected void checkUploadPreconditions(T media) throws IOException {
        if (media.isIgnored() == Boolean.TRUE) {
            throw new ImageUploadForbiddenException(media + " is marked as ignored.");
        }
        if (media.getSha1() == null) {
            throw new ImageUploadForbiddenException(media + " SHA-1 has not been computed.");
        }
        // Forbid upload of duplicate medias for a single repo, they may have different descriptions
        if (repository.countBySha1(media.getSha1()) > 1) {
            throw new ImageUploadForbiddenException(media + " is present several times.");
        }
        // Double-check for duplicates before upload!
        if (CollectionUtils.isNotEmpty(media.getCommonsFileNames()) || mediaService.findCommonsFilesWithSha1(media)) {
            throw new ImageUploadForbiddenException(media + " is already on Commons: " + media.getCommonsFileNames());
        }
    }

    protected MediaRepository<?, ?, ?> getOriginalRepository() {
        return null;
    }

    protected abstract Class<T> getMediaClass();

    protected Class<? extends T> getTopTermsMediaClass() {
        return getMediaClass();
    }

	protected final Map<String, String> loadCsvMapping(String filename) throws IOException {
		return Csv.loadMap(getClass().getResource("/mapping/" + filename));
	}

    protected final boolean ignoreFile(T media, String reason) {
        media.setIgnored(Boolean.TRUE);
        media.setIgnoredReason(reason);
        return true;
    }

    @Override
    public int compareTo(AbstractSpaceAgencyService<T, ID, D> o) {
        return getName().compareTo(o.getName());
    }
}
