package org.wikimedia.commons.donvip.spacemedia.service.agencies;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.temporal.Temporal;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.search.Query;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.query.dsl.SimpleQueryStringMatchingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.wikimedia.commons.donvip.spacemedia.data.domain.Statistics;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrFreeLicense;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.exception.ImageUploadForbiddenException;
import org.wikimedia.commons.donvip.spacemedia.service.FlickrService;

import com.flickr4java.flickr.FlickrException;
import com.flickr4java.flickr.people.User;
import com.github.dozermapper.core.Mapper;

public abstract class AbstractSpaceAgencyFlickrService
        extends AbstractSpaceAgencyService<FlickrMedia, Long, LocalDateTime> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractSpaceAgencyFlickrService.class);


    @Autowired
    protected FlickrMediaRepository flickrRepository;
    @Autowired
    protected FlickrService flickrService;
    @Autowired
    protected Mapper dozerMapper;

    protected final Set<String> flickrAccounts;

    @Value("${flickr.video.download.url}")
    private String flickrVideoDownloadUrl;

    public AbstractSpaceAgencyFlickrService(FlickrMediaRepository repository, Set<String> flickrAccounts) {
        super(repository);
        this.flickrAccounts = Objects.requireNonNull(flickrAccounts);
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
    public Statistics getStatistics() {
        Statistics stats = super.getStatistics();
        if (flickrAccounts.size() > 1) {
            stats.setDetails(flickrAccounts.stream()
                    .map(this::getStatistics)
                    .sorted().collect(Collectors.toList()));
        }
        return stats;
    }

    private Statistics getStatistics(String alias) {
        Set<String> singleton = Collections.singleton(alias);
        return new Statistics(alias, flickrRepository.count(singleton),
                flickrRepository.countByIgnoredTrue(singleton),
                flickrRepository.countMissingInCommons(singleton), null);
    }

    @Override
    public final URL getSourceUrl(FlickrMedia media) throws MalformedURLException {
        return getPhotoUrl(media);
    }

    @Override
    protected Optional<Temporal> getCreationDate(FlickrMedia media) {
        return Optional.ofNullable(media.getDateTaken());
    }

    @Override
    protected Optional<Temporal> getUploadDate(FlickrMedia media) {
        return Optional.of(media.getDate());
    }

    @Override
    protected final String getAuthor(FlickrMedia media) throws MalformedURLException {
        try {
            User user = flickrService.findUser(getUserPhotosUrl(media));
            URL profileUrl = flickrService.findUserProfileUrl(user.getId());
            return wikiLink(profileUrl, user.getUsername());
        } catch (FlickrException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected final String getPageTile(FlickrMedia media) {
        return super.getPageTile(media) + "(" + media.getId() + ")";
    }

    @Override
	public Set<String> findCategories(FlickrMedia media, boolean includeHidden) {
		Set<String> result = super.findCategories(media, includeHidden);
		if (includeHidden) {
			result.remove("Spacemedia files uploaded by Vipbot");
			result.add("Spacemedia Flickr files uploaded by Vipbot");
		}
        return result;
    }

    @Override
    public List<String> findTemplates(FlickrMedia media) {
        List<String> result = super.findTemplates(media);
        result.add(FlickrFreeLicense.of(media.getLicense()).getWikiTemplate());
        result.add("Flickrreview");
        return result;
    }

    @Override
    protected void checkUploadPreconditions(FlickrMedia media) throws IOException {
        super.checkUploadPreconditions(media);
        if (isBadVideoEntry(media)) {
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
                LOGGER.info("Fetching Flickr images from account '{}'...", flickrAccount);
                for (FlickrMedia media : flickrService.findFreePhotos(flickrAccount).stream()
                        .map(p -> dozerMapper.map(p, FlickrMedia.class)).collect(Collectors.toList())) {
                    try {
						processFlickrMedia(media, flickrAccount);
                        count++;
                    } catch (IOException | URISyntaxException e) {
                        problem(getPhotoUrl(media), e);
                    }
                }
			} catch (FlickrException | MalformedURLException | RuntimeException e) {
                LOGGER.error("Error while fetching Flickr images from account " + flickrAccount, e);
            }
        }
        endUpdateMedia(count, start);
    }

    private URL getVideoUrl(FlickrMedia media) throws MalformedURLException {
        return new URL(flickrVideoDownloadUrl.replace("<id>", media.getId().toString()));
    }

    private boolean isBadVideoEntry(FlickrMedia media) throws MalformedURLException {
        if ("video".equals(media.getMedia())) {
            if (!getVideoUrl(media).equals(media.getAssetUrl())) {
                return true;
            }
        }
        return false;
    }

	private FlickrMedia processFlickrMedia(FlickrMedia media, String flickrAccount)
			throws IOException, URISyntaxException {
        boolean save = false;
        Optional<FlickrMedia> mediaInRepo = flickrRepository.findById(media.getId());
        if (mediaInRepo.isPresent()) {
            FlickrMedia mediaInDb = mediaInRepo.get();
            // FIXME migration code, to delete later
            if (media.getThumbnailUrl() != null && mediaInDb.getThumbnailUrl() == null) {
                mediaInDb.setThumbnailUrl(media.getThumbnailUrl());
                save = true;
            }
            media = mediaInDb;
        } else {
            save = true;
        }
        if (isBadVideoEntry(media)) {
            media.setAssetUrl(getVideoUrl(media));
            media.setCommonsFileNames(null);
            media.setSha1(null);
            save = true;
        }
		if (StringUtils.isEmpty(media.getPathAlias())) {
			media.setPathAlias(flickrAccount);
			save = true;
		}
        if (mediaService.updateMedia(media)) {
            save = true;
        }
        if (save) {
            media = flickrRepository.save(media);
        }
        return media;
    }

    @Override
    protected Query getSearchQuery(QueryBuilder queryBuilder, SimpleQueryStringMatchingContext context, String q) {
        return queryBuilder.bool()
                .must(super.getSearchQuery(queryBuilder, context, q))
                .must(queryBuilder.simpleQueryString().onField("pathAlias").matching(String.join(" ", flickrAccounts)).createQuery())
                .createQuery();
    }
}
