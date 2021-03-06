package org.wikimedia.commons.donvip.spacemedia.service.agencies;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.temporal.Temporal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.search.Query;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.query.dsl.SimpleQueryStringMatchingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.wikimedia.commons.donvip.spacemedia.commons.api.exceptions.UploadException;
import org.wikimedia.commons.donvip.spacemedia.data.domain.Media;
import org.wikimedia.commons.donvip.spacemedia.data.domain.Statistics;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrPhotoSet;
import org.wikimedia.commons.donvip.spacemedia.exception.ImageUploadForbiddenException;
import org.wikimedia.commons.donvip.spacemedia.repo.flickr.FlickrFreeLicense;
import org.wikimedia.commons.donvip.spacemedia.repo.flickr.FlickrApiService;
import org.wikimedia.commons.donvip.spacemedia.service.FlickrMediaProcessorService;
import org.wikimedia.commons.donvip.spacemedia.utils.UnitedStates;
import org.wikimedia.commons.donvip.spacemedia.utils.UnitedStates.VirinTemplates;

import com.flickr4java.flickr.FlickrException;
import com.flickr4java.flickr.people.User;
import com.flickr4java.flickr.photos.Photo;
import com.github.dozermapper.core.Mapper;

public abstract class AbstractAgencyFlickrService<OT extends Media<OID, OD>, OID, OD extends Temporal>
        extends AbstractAgencyService<FlickrMedia, Long, LocalDateTime, OT, OID, OD> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractAgencyFlickrService.class);
    private static final Pattern DELETED_PHOTO = Pattern.compile("Photo \"([0-9]+)\" not found \\(invalid ID\\)");

    @Autowired
    protected FlickrMediaRepository flickrRepository;
    @Autowired
    protected FlickrApiService flickrService;
    @Autowired
    protected Mapper dozerMapper;
    @Autowired
    protected FlickrMediaProcessorService processor;

    protected final Set<String> flickrAccounts;
    protected final Map<String, Map<String, String>> flickrPhotoSets;

    public AbstractAgencyFlickrService(FlickrMediaRepository repository, String id, Set<String> flickrAccounts) {
        super(repository, id);
        this.flickrAccounts = Objects.requireNonNull(flickrAccounts);
        this.flickrPhotoSets = new HashMap<>();
    }

    @Override
    @PostConstruct
    void init() throws IOException {
        super.init();
        for (String account : flickrAccounts) {
            Map<String, String> mapping = loadCsvMapping("flickr/" + account + ".photosets.csv");
            if (mapping != null) {
                flickrPhotoSets.put(account, mapping);
            }
        }
    }

    @Override
    protected final Class<FlickrMedia> getMediaClass() {
        return FlickrMedia.class;
    }

    @Override
    public final long countAllMedia() {
        return flickrRepository.count(flickrAccounts);
    }

    @Override
    public final long countIgnored() {
        return flickrRepository.countByIgnoredTrue(flickrAccounts);
    }

    @Override
    public final long countMissingMedia() {
        return flickrRepository.countMissingInCommons(flickrAccounts);
    }

    @Override
    public final long countPerceptualHashes() {
        return flickrRepository.countByMetadata_PhashNotNull(flickrAccounts);
    }

    @Override
    public final long countUploadedMedia() {
        return flickrRepository.countUploadedToCommons(flickrAccounts);
    }

    @Override
    public final Iterable<FlickrMedia> listAllMedia() {
        return flickrRepository.findAll(flickrAccounts);
    }

    @Override
    public final Page<FlickrMedia> listAllMedia(Pageable page) {
        return flickrRepository.findAll(flickrAccounts, page);
    }

    @Override
    public final List<FlickrMedia> listIgnoredMedia() {
        return flickrRepository.findByIgnoredTrue(flickrAccounts);
    }

    @Override
    public final Page<FlickrMedia> listIgnoredMedia(Pageable page) {
        return flickrRepository.findByIgnoredTrue(flickrAccounts, page);
    }

    @Override
    public final List<FlickrMedia> listMissingMedia() {
        return flickrRepository.findMissingInCommons(flickrAccounts);
    }

    @Override
    public final Page<FlickrMedia> listMissingMedia(Pageable page) {
        return flickrRepository.findMissingInCommons(flickrAccounts, page);
    }

    @Override
    public final Page<FlickrMedia> listHashedMedia(Pageable page) {
        return flickrRepository.findByMetadata_PhashNotNull(flickrAccounts, page);
    }

    @Override
    public final List<FlickrMedia> listUploadedMedia() {
        return flickrRepository.findUploadedToCommons(flickrAccounts);
    }

    @Override
    public final Page<FlickrMedia> listUploadedMedia(Pageable page) {
        return flickrRepository.findUploadedToCommons(flickrAccounts, page);
    }

    @Override
    public final List<FlickrMedia> listDuplicateMedia() {
        return flickrRepository.findDuplicateInCommons(flickrAccounts);
    }

    @Override
    public Statistics getStatistics(boolean details) {
        Statistics stats = super.getStatistics(details);
        if (details && flickrAccounts.size() > 1) {
            stats.setDetails(flickrAccounts.stream()
                    .map(this::getStatistics)
                    .sorted().collect(Collectors.toList()));
        }
        return stats;
    }

    private Statistics getStatistics(String alias) {
        Set<String> singleton = Collections.singleton(alias);
        return new Statistics(alias, alias,
                flickrRepository.count(singleton),
                flickrRepository.countUploadedToCommons(singleton),
                flickrRepository.countByIgnoredTrue(singleton),
                flickrRepository.countMissingInCommons(singleton),
                flickrRepository.countByMetadata_PhashNotNull(singleton), null);
    }

    @Override
    public final URL getSourceUrl(FlickrMedia media) throws MalformedURLException {
        return getPhotoUrl(media);
    }

    @Override
    protected final Optional<Temporal> getCreationDate(FlickrMedia media) {
        return Optional.ofNullable(media.getDateTaken());
    }

    @Override
    protected final Optional<Temporal> getUploadDate(FlickrMedia media) {
        return Optional.of(media.getDate());
    }

    @Override
    protected final String getAuthor(FlickrMedia media) throws MalformedURLException {
        try {
            User user = flickrService.findUser(getUserPhotosUrl(media));
            URL profileUrl = flickrService.findUserProfileUrl(user.getId());
            return wikiLink(profileUrl, user.getUsername());
        } catch (FlickrException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    protected final String getPageTile(FlickrMedia media) {
        return super.getPageTile(media) + "(" + media.getId() + ")";
    }

    @Override
    protected Optional<String> getOtherFields(FlickrMedia media) {
        StringBuilder sb = new StringBuilder();
        addOtherField(sb, "Flickr set", media.getPhotosets());
        addOtherField(sb, "Flickr tag", media.getTags());
        String s = sb.toString();
        return s.isEmpty() ? Optional.empty() : Optional.of(s);
    }

    @Override
    public Set<String> findCategories(FlickrMedia media, boolean includeHidden) {
        Set<String> result = super.findCategories(media, includeHidden);
        if (includeHidden) {
            result.remove("Spacemedia files uploaded by " + commonsApiService.getAccount());
            result.add("Spacemedia Flickr files uploaded by " + commonsApiService.getAccount());
        }
        if (CollectionUtils.isNotEmpty(media.getPhotosets())) {
            Map<String, String> mapping = flickrPhotoSets.get(media.getPathAlias());
            if (MapUtils.isNotEmpty(mapping)) {
                for (FlickrPhotoSet album : media.getPhotosets()) {
                    String cats = mapping.get(album.getTitle());
                    if (StringUtils.isNotBlank(cats)) {
                        Arrays.stream(cats.split(";")).map(String::trim).forEach(result::add);
                    }
                }
            }
        }
        return result;
    }

    @Override
    public Set<String> findTemplates(FlickrMedia media) {
        Set<String> result = super.findTemplates(media);
        try {
            result.add(FlickrFreeLicense.of(media.getLicense()).getWikiTemplate());
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Non-free Flickr licence for media {}: {}", media.getId(), e.getMessage());
        }
        result.add("Flickrreview");
        try {
            VirinTemplates t = UnitedStates.getUsVirinTemplates(media.getTitle(), getSourceUrl(media));
            if (t != null) {
                result.add(t.getVirinTemplate());
                if (StringUtils.isNotBlank(t.getPdTemplate())) {
                    result.add(t.getPdTemplate());
                }
            }
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
        if (result.contains("Flickr-public domain mark") && UnitedStates.isClearPublicDomain(media.getDescription())) {
            result.remove("Flickr-public domain mark");
        }
        return result;
    }

    @Override
    protected void checkUploadPreconditions(FlickrMedia media, boolean checkUnicity) throws IOException {
        super.checkUploadPreconditions(media, checkUnicity);
        if (processor.isBadVideoEntry(media)) {
            throw new ImageUploadForbiddenException("Bad video download link: " + media);
        }
        if (!"ready".equals(media.getMediaStatus())) {
            throw new ImageUploadForbiddenException("Media is not ready: " + media);
        }
    }

    private static final URL getUserPhotosUrl(FlickrMedia media) throws MalformedURLException {
        return new URL("https://www.flickr.com/photos/" + media.getPathAlias());
    }

    private static final URL getPhotoUrl(FlickrMedia media) throws MalformedURLException {
        return new URL(getUserPhotosUrl(media).toExternalForm() + "/" + media.getId());
    }

    protected void updateFlickrMedia() {
        LocalDateTime start = startUpdateMedia();
        int count = 0;
        for (String flickrAccount : flickrAccounts) {
            try {
                LOGGER.info("Fetching Flickr media from account '{}'...", flickrAccount);
                List<FlickrMedia> freePictures = buildFlickrMediaList(flickrService.findFreePhotos(flickrAccount));
                count += processFlickrMedia(freePictures, flickrAccount);
                Set<FlickrMedia> noLongerFreePictures = flickrRepository.findAll(Set.of(flickrAccount));
                noLongerFreePictures.removeAll(freePictures);
                if (!noLongerFreePictures.isEmpty()) {
                    count += updateNoLongerFreeFlickrMedia(flickrAccount, noLongerFreePictures);
                }
            } catch (FlickrException | MalformedURLException | RuntimeException e) {
                LOGGER.error("Error while fetching Flickr media from account " + flickrAccount, e);
            }
        }
        endUpdateMedia(count, start);
    }

    private int updateNoLongerFreeFlickrMedia(String flickrAccount, Set<FlickrMedia> pictures)
            throws MalformedURLException {
        int count = 0;
        LOGGER.info("Checking {} Flickr images no longer free for account '{}'...", pictures.size(), flickrAccount);
        for (FlickrMedia picture : pictures) {
            try {
                count += processFlickrMedia(
                        buildFlickrMediaList(flickrService.findPhotos(Set.of(picture.getId().toString()))),
                        flickrAccount);
            } catch (FlickrException e) {
                if (e.getErrorMessage() != null) {
                    Matcher m = DELETED_PHOTO.matcher(e.getErrorMessage());
                    if (m.matches()) {
                        String id = m.group(1);
                        LOGGER.warn("Flickr image {} has been deleted for account '{}'", id, flickrAccount);
                        flickrRepository.deleteById(Long.valueOf(id));
                        count++;
                    } else {
                        LOGGER.error("Error while processing non-free Flickr image " + picture.getId()
                                + " from account " + flickrAccount, e);
                    }
                } else {
                    LOGGER.error("Error while processing non-free Flickr image " + picture.getId()
                            + " from account " + flickrAccount, e);
                }
            }
        }
        return count;
    }

    private List<FlickrMedia> buildFlickrMediaList(List<Photo> photos) {
        return photos.stream()
                .map(p -> flickrRepository.findById(Long.parseLong(p.getId()))
                        .orElseGet(() -> dozerMapper.map(p, FlickrMedia.class)))
                .collect(Collectors.toList());
    }

    private int processFlickrMedia(Iterable<FlickrMedia> medias, String flickrAccount) throws MalformedURLException {
        int count = 0;
        for (FlickrMedia media : medias) {
            try {
                processor.processFlickrMedia(media, flickrAccount, getOriginalRepository(),
                        this::customProcessing, this::shouldUploadAuto, this::uploadWrapped);
                count++;
            } catch (IOException e) {
                problem(getPhotoUrl(media), e);
            }
        }
        return count;
    }

    protected final FlickrMedia uploadWrapped(FlickrMedia media) {
        try {
            return upload(media, true);
        } catch (UploadException e) {
            throw new RuntimeException(e);
        }
    }

    protected boolean customProcessing(FlickrMedia media) {
        // TO be overriden for custom processing
        return false;
    }

    @Override
    protected Query getSearchQuery(QueryBuilder queryBuilder, SimpleQueryStringMatchingContext context, String q) {
        return queryBuilder.bool()
                .must(super.getSearchQuery(queryBuilder, context, q))
                .must(queryBuilder.simpleQueryString().onField("pathAlias").matching(String.join(" ", flickrAccounts)).createQuery())
                .createQuery();
    }

    @Override
    protected final Long getMediaId(String id) {
        return Long.parseLong(id);
    }

    @Override
    protected final List<FlickrMedia> findDuplicates() {
        return flickrRepository.findByDuplicatesIsNotEmpty(flickrAccounts);
    }

    @Override
    protected final int doResetIgnored() {
        return flickrRepository.resetIgnored(flickrAccounts);
    }

    @Override
    protected final int doResetPerceptualHashes() {
        return flickrRepository.resetPerceptualHashes(flickrAccounts);
    }

    @Override
    protected final int doResetSha1Hashes() {
        return flickrRepository.resetSha1Hashes(flickrAccounts);
    }
}
