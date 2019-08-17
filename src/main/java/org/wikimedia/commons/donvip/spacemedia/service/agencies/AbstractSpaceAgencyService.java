package org.wikimedia.commons.donvip.spacemedia.service.agencies;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.collections.CollectionUtils;
import org.apache.http.client.ClientProtocolException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.wikimedia.commons.donvip.spacemedia.data.domain.Media;
import org.wikimedia.commons.donvip.spacemedia.data.domain.MediaRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.Problem;
import org.wikimedia.commons.donvip.spacemedia.data.domain.ProblemRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.Statistics;
import org.wikimedia.commons.donvip.spacemedia.exception.ImageNotFoundException;
import org.wikimedia.commons.donvip.spacemedia.exception.ImageUploadForbiddenException;
import org.wikimedia.commons.donvip.spacemedia.service.CommonsService;
import org.wikimedia.commons.donvip.spacemedia.service.MediaService;
import org.wikimedia.commons.donvip.spacemedia.utils.Utils;
import org.xml.sax.SAXException;

public abstract class AbstractSpaceAgencyService<T extends Media, ID> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractSpaceAgencyService.class);

    protected final MediaRepository<T, ID> repository;

    @Autowired
    protected ProblemRepository problemrepository;
    @Autowired
    protected MediaService mediaService;
    @Autowired
    protected CommonsService commonsService;

    @Value("#{${categories}}")
    private Map<String, String> categories;

    public AbstractSpaceAgencyService(MediaRepository<T, ID> repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    public long countAllMedia() {
        return repository.count();
    }

    public long countMissingMedia() {
        return repository.countMissingInCommons();
    }

    public Iterable<T> listAllMedia() {
        return repository.findAll();
    }

    public List<T> listMissingMedia() {
        return repository.findMissingInCommons();
    }

    public List<T> listDuplicateMedia() {
        return repository.findDuplicateInCommons();
    }

    public List<T> listIgnoredMedia() {
        return repository.findByIgnoredTrue();
    }

    /**
     * Returns the space agency name, used in statistics and logs.
     * 
     * @return the space agency name
     */
    public abstract String getName();

    public abstract List<T> updateMedia() throws IOException;

    public Statistics getStatistics() {
        long problems = getProblemsCount();
        return new Statistics(getName(), countAllMedia(), countMissingMedia(), problems > 0 ? problems : null);
    }

    public final List<Problem> getProblems() {
        return problemrepository.findByAgency(getName());
    }

    public final long getProblemsCount() {
        return problemrepository.countByAgency(getName());
    }

    protected final Problem problem(URL problematicUrl, Throwable t) {
        return problem(problematicUrl, t.getMessage());
    }

    protected final Problem problem(URL problematicUrl, String errorMessage) {
        Optional<Problem> problem = problemrepository.findByAgencyAndProblematicUrl(getName(), problematicUrl);
        if (problem.isPresent()) {
            return problem.get();
        } else {
            Problem pb = new Problem();
            pb.setAgency(getName());
            pb.setErrorMessage(errorMessage);
            pb.setProblematicUrl(problematicUrl);
            LOGGER.warn(pb.toString());
            return problemrepository.save(pb);
        }
    }

    private T findBySha1OrThrow(String sha1) {
        return repository.findBySha1(sha1).orElseThrow(() -> new ImageNotFoundException(sha1));
    }

    public final T upload(String sha1) throws MalformedURLException {
        T media = findBySha1OrThrow(sha1);
        checkUploadPreconditions(media);
        String wikiCode = getWikiCode(media);
        // TODO
        return repository.save(media);
    }

    public String getWikiHtmlPreview(String sha1)
            throws ClientProtocolException, IOException, ParserConfigurationException, SAXException {
        T media = findBySha1OrThrow(sha1);
        return commonsService.getWikiHtmlPreview(getWikiCode(media), getPageTile(media),
                media.getAssetUrl().toExternalForm());
    }

    protected String getPageTile(T media) {
        return media.getTitle();
    }

    public final String getWikiCode(String sha1) {
        return getWikiCode(findBySha1OrThrow(sha1));
    }

    public final String getWikiCode(T media) {
        try {
            StringBuilder sb = new StringBuilder("== {{int:filedesc}} ==\n{{Information\n| description = ")
                    .append(CommonsService.formatWikiCode(getDescription(media)));
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
                getUploadDate(media).ifPresent(d -> sb.append("{{Upload date|").append(d).append("}}"));
            }
            sb.append("\n| source = ").append(getSource(media)).append("\n| author = ").append(getAuthor(media));
            getPermission(media).ifPresent(s -> sb.append("\n| permission = ").append(s));
            getOtherVersions(media).ifPresent(s -> sb.append("\n| other versions = ").append(s));
            getOtherFields(media).ifPresent(s -> sb.append("\n| other fields = ").append(s));
            getOtherFields1(media).ifPresent(s -> sb.append("\n| other fields 1 = ").append(s));
            sb.append("\n}}\n=={{int:license-header}}==\n");
            findTemplates(media).forEach(t -> sb.append("{{").append(t).append("}}\n"));
            findCategories(media).forEach(t -> sb.append("[[Category:").append(t).append("]]\n"));
            return sb.toString();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    protected abstract String getDescription(T media) throws MalformedURLException;

    protected abstract String getSource(T media) throws MalformedURLException;

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

    protected Optional<String> getOtherVersions(T media) {
        return Optional.empty();
    }

    protected Optional<String> getOtherFields(T media) {
        return Optional.empty();
    }

    protected Optional<String> getOtherFields1(T media) {
        return Optional.empty();
    }

    protected Set<String> findCategories(T media) {
        Set<String> result = new HashSet<String>();
        for (Entry<String, String> e : categories.entrySet()) {
            if (Utils.isTextFound(media.getTitle(), e.getKey())
                    || Utils.isTextFound(media.getDescription(), e.getKey())) {
                result.add(e.getValue());
            }
        }
        return commonsService.cleanupCategories(result);
    }

    protected List<String> findTemplates(T media) {
        // TODO
        return new ArrayList<String>();
    }

    protected final String wikiLink(URL url, String text) {
        return "[" + Objects.requireNonNull(url, "url") + " " + Objects.requireNonNull(text, "text") + "]";
    }

    protected void checkUploadPreconditions(T media) throws MalformedURLException {
        if (media.isIgnored() == Boolean.TRUE) {
            throw new ImageUploadForbiddenException(media + " is marked as ignored.");
        }
        // Double-check for duplicates before upload!
        if (CollectionUtils.isNotEmpty(media.getCommonsFileNames()) || mediaService.findCommonsFilesWithSha1(media)) {
            throw new ImageUploadForbiddenException(media + " is already on Commons: " + media.getCommonsFileNames());
        }
    }
}
