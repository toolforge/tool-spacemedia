package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toSet;
import static org.wikimedia.commons.donvip.spacemedia.utils.CsvHelper.loadCsvMapping;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.newURL;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.replace;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.toZonedDateTime;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.wikimedia.commons.donvip.spacemedia.data.domain.Statistics;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.CompositeMediaId;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.ImageDimensions;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrLicense;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrMediaType;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrPhotoSet;
import org.wikimedia.commons.donvip.spacemedia.exception.ImageUploadForbiddenException;
import org.wikimedia.commons.donvip.spacemedia.service.ExecutionMode;
import org.wikimedia.commons.donvip.spacemedia.service.flickr.FlickrMediaProcessorService;
import org.wikimedia.commons.donvip.spacemedia.service.flickr.FlickrService;
import org.wikimedia.commons.donvip.spacemedia.utils.UnitedStates;
import org.wikimedia.commons.donvip.spacemedia.utils.UnitedStates.VirinTemplates;

import com.flickr4java.flickr.FlickrException;
import com.flickr4java.flickr.people.User;
import com.flickr4java.flickr.photos.Photo;
import com.flickr4java.flickr.tags.Tag;

public abstract class AbstractOrgFlickrService extends AbstractOrgService<FlickrMedia> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractOrgFlickrService.class);
    private static final Pattern DELETED_PHOTO = Pattern.compile("Photo \"(\\d+)\" not found \\(invalid ID\\)");

    @Autowired
    protected FlickrMediaRepository flickrRepository;
    @Autowired
    protected FlickrService flickrService;
    @Autowired
    protected FlickrMediaProcessorService processor;

    protected final Map<String, Map<String, String>> flickrPhotoSets;
    protected final Map<String, Map<String, String>> flickrTags;

    protected AbstractOrgFlickrService(FlickrMediaRepository repository, String id, Set<String> flickrAccounts) {
        super(repository, id, flickrAccounts);
        this.flickrPhotoSets = new HashMap<>();
        this.flickrTags = new HashMap<>();
    }

    @Override
    @PostConstruct
    void init() throws IOException {
        super.init();
        for (String account : getRepoIds()) {
            ofNullable(loadCsvMapping("flickr/" + account + ".photosets.csv"))
                    .ifPresent(mapping -> flickrPhotoSets.put(account, mapping));
            ofNullable(loadCsvMapping("flickr/" + account + ".tags.csv"))
                    .ifPresent(mapping -> flickrTags.put(account, mapping));
        }
    }

    @Override
    protected final Class<FlickrMedia> getMediaClass() {
        return FlickrMedia.class;
    }

    @Override
    public Statistics getStatistics(boolean details) {
        Statistics stats = super.getStatistics(details);
        if (details && getRepoIds().size() > 1) {
            stats.setDetails(getRepoIds().stream()
                    .map(this::getStatistics)
                    .sorted().toList());
        }
        return stats;
    }

    private Statistics getStatistics(String alias) {
        Set<String> singleton = Collections.singleton(alias);
        return new Statistics(alias, alias,
                flickrRepository.count(singleton),
                flickrRepository.countUploadedToCommons(singleton),
                flickrRepository.countByIgnoredTrue(singleton),
                flickrRepository.countMissingImagesInCommons(singleton),
                flickrRepository.countMissingVideosInCommons(singleton),
                flickrRepository.countByMetadata_PhashNotNull(singleton), null);
    }

    @Override
    public final URL getSourceUrl(FlickrMedia media, FileMetadata metadata) {
        return getPhotoUrl(media);
    }

    @Override
    protected final String getAuthor(FlickrMedia media) throws MalformedURLException {
        URL userPhotosUrl = getUserPhotosUrl(media);
        try {
            User user = flickrService.findUser(userPhotosUrl);
            URL profileUrl = flickrService.findUserProfileUrl(user.getId());
            return wikiLink(profileUrl, user.getUsername());
        } catch (FlickrException e) {
            throw new IllegalArgumentException(
                    String.format("Unable to retrieve Flickr author for %s with url %s", media, userPhotosUrl), e);
        }
    }

    @Override
    protected final String getPageTitle(FlickrMedia media) {
        return super.getPageTitle(media) + "(" + media.getId().getMediaId() + ")";
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
    public Set<String> findCategories(FlickrMedia media, FileMetadata metadata, boolean includeHidden) {
        Set<String> result = super.findCategories(media, metadata, includeHidden);
        if (includeHidden) {
            replace(result, "Spacemedia files uploaded by " + commonsService.getAccount(),
                    "Spacemedia Flickr files uploaded by " + commonsService.getAccount());
        }
        mediaService.useMapping(result, media.getPathAlias(), media.getPhotosets(), flickrPhotoSets,
                FlickrPhotoSet::getTitle);
        mediaService.useMapping(result, media.getPathAlias(), media.getTags(), flickrTags, Function.identity());
        return result;
    }

    @Override
    public Set<String> findLicenceTemplates(FlickrMedia media, FileMetadata metadata) {
        Set<String> result = super.findLicenceTemplates(media, metadata);
        try {
            FlickrLicense license = FlickrLicense.of(media.getLicense());
            result.add(license.getWikiTemplate()
                    + (license.isFree() ? "" : "|1={{tl|" + getNonFreeLicenceTemplate(media) + "}}"));
            if (license.isFree()) {
                result.add("Flickrreview"); // Normal case
            }
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Unknown Flickr licence for media {}: {}", media.getId(), e.getMessage());
            result.add("Flickrreview"); // Strange case
        }
        VirinTemplates t = UnitedStates.getUsVirinTemplates(media.getTitle(),
                getSourceUrl(media, media.getUniqueMetadata()));
        if (t != null) {
            result.add(t.getVirinTemplate());
            if (StringUtils.isNotBlank(t.getPdTemplate())) {
                result.add(t.getPdTemplate());
            }
        }
        String description = media.getDescription();
        if (description != null) {
            if (result.contains("Flickr-public domain mark") && UnitedStates.isClearPublicDomain(description)) {
                result.remove("Flickr-public domain mark");
            }
            if (description.contains("hoto by SpaceX") || description.contains("hoto/SpaceX")) {
                result.add("PD-SpaceX");
            }
            if (description.contains("hoto by NASA") || description.contains("hoto/NASA")) {
                result.add("PD-USGov-NASA");
            }
        }
        return result;
    }

    @Override
    protected void checkUploadPreconditions(FlickrMedia media, boolean checkUnicity, boolean isManual)
            throws MalformedURLException, URISyntaxException {
        super.checkUploadPreconditions(media, checkUnicity, isManual);
        if (processor.isBadVideoEntry(media)) {
            throw new ImageUploadForbiddenException("Bad video download link: " + media);
        }
        if (!"ready".equals(media.getMediaStatus()) && StringUtils.isNotBlank(media.getMediaStatus())) {
            throw new ImageUploadForbiddenException("Media is not ready: " + media);
        }
    }

    private static final URL getUserPhotosUrl(FlickrMedia media) {
        return newURL("https://www.flickr.com/photos/" + media.getPathAlias());
    }

    private static final URL getPhotoUrl(FlickrMedia media) {
        return newURL(getUserPhotosUrl(media).toExternalForm() + '/' + media.getId().getMediaId());
    }

    protected boolean includeAllLicences() {
        return false;
    }

    protected String getNonFreeLicenceTemplate(FlickrMedia media) {
        throw new UnsupportedOperationException(
                "getNonFreeLicenceTemplate needs to be overriden if includeAllLicences is set to true");
    }

    @Override
    public void updateMedia() {
        LocalDateTime start = startUpdateMedia();
        List<FlickrMedia> uploadedMedia = new ArrayList<>();
        LocalDate minUploadDate = getRuntimeData().getDoNotFetchEarlierThan();
        int count = 0;
        for (String flickrAccount : getRepoIds()) {
            try {
                LOGGER.info("Fetching Flickr media from account '{}'...", flickrAccount);
                List<FlickrMedia> freePictures = buildFlickrMediaList(
                        flickrService.searchPhotos(flickrAccount, minUploadDate, includeAllLicences()), flickrAccount);
                LOGGER.info("Found {} free Flickr media for account '{}'", freePictures.size(), flickrAccount);
                Pair<Integer, Collection<FlickrMedia>> result = processFlickrMedia(freePictures, flickrAccount);
                Collection<FlickrMedia> localUploadedImages = result.getRight();
                uploadedMedia.addAll(localUploadedImages);
                count += result.getLeft();
                postSocialMedia(localUploadedImages,
                        localUploadedImages.stream().map(m -> m.getUniqueMetadata()).toList());
                if (minUploadDate == null) {
                    // Only delete pictures not found in complete updates
                    Set<FlickrMedia> noLongerFreePictures = flickrRepository.findNotIn(Set.of(flickrAccount),
                            freePictures.stream().map(m -> m.getId().getMediaId()).collect(toSet()));
                    if (!noLongerFreePictures.isEmpty()) {
                        count += updateNoLongerFreeFlickrMedia(flickrAccount, noLongerFreePictures);
                    }
                }
            } catch (FlickrException | RuntimeException e) {
                LOGGER.error("Error while fetching Flickr media from account {}", flickrAccount, e);
            }
        }
        endUpdateMedia(count, uploadedMedia, start, false /* tweets already posted - one by Flickr account */);
    }

    private int updateNoLongerFreeFlickrMedia(String flickrAccount, Set<FlickrMedia> pictures) {
        int count = 0;
        LOGGER.info("Checking {} Flickr images no longer free for account '{}'...", pictures.size(), flickrAccount);
        for (FlickrMedia picture : pictures) {
            try {
                count += processFlickrMedia(
                        buildFlickrMediaList(List.of(flickrService.findPhoto(picture.getId().getMediaId())),
                                flickrAccount),
                        flickrAccount).getLeft();
            } catch (FlickrException e) {
                if (e.getErrorMessage() != null) {
                    Matcher m = DELETED_PHOTO.matcher(e.getErrorMessage());
                    if (m.matches()) {
                        String id = m.group(1);
                        LOGGER.warn("Flickr image {} has been deleted for account '{}'", id, flickrAccount);
                        processor.deleteFlickrMedia(new CompositeMediaId(flickrAccount, id));
                        if (executionMode == ExecutionMode.LOCAL) {
                            evictRemoteCaches();
                        }
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

    private List<FlickrMedia> buildFlickrMediaList(List<Photo> photos, String flickrAccount) {
        return photos.stream().map(x -> photoToFlickrMedia(x, flickrAccount)).toList();
    }

    private FlickrMedia photoToFlickrMedia(Photo p, String flickrAccount) {
        return repository.findById(new CompositeMediaId(getPathAlias(p, flickrAccount), p.getId())).orElseGet(() -> {
            try {
                return saveMedia(mapPhoto(p, flickrAccount, true));
            } catch (FlickrException e) {
                throw new IllegalArgumentException(e);
            }
        });
    }

    private Pair<Integer, Collection<FlickrMedia>> processFlickrMedia(Iterable<FlickrMedia> medias,
            String flickrAccount) {
        int count = 0;
        LocalDateTime start = LocalDateTime.now();
        Collection<FlickrMedia> uploadedMedia = new ArrayList<>();
        for (FlickrMedia media : medias) {
            try {
                Pair<FlickrMedia, Integer> result = processor.processFlickrMedia(media, flickrAccount,
                        () -> getStringsToRemove(media), this::shouldUploadAuto, this::uploadWrapped,
                        getUrlResolver(), this::saveMedia);
                if (result.getValue() > 0) {
                    uploadedMedia.add(result.getKey());
                }
                ongoingUpdateMedia(start, flickrAccount, count++);
            } catch (IOException | RuntimeException e) {
                problem(getPhotoUrl(media), e);
            }
        }
        return Pair.of(count, uploadedMedia);
    }

    @Override
    protected final FlickrMedia refresh(FlickrMedia media) throws IOException {
        try {
            return media.copyDataFrom(
                    mapPhoto(flickrService.findPhoto(media.getId().getMediaId()), media.getPathAlias(), false));
        } catch (FlickrException e) {
            throw new IOException(e);
        }
    }

    private FlickrMedia mapPhoto(Photo p, String flickrAccount, boolean saveMetadata) throws FlickrException {
        FlickrMedia m = new FlickrMedia();
        ofNullable(p.getGeoData()).ifPresent(geo -> {
            m.setLatitude(geo.getLatitude());
            m.setLongitude(geo.getLongitude());
            m.setAccuracy(geo.getAccuracy());
        });
        m.setPublicationDateTime(toZonedDateTime(p.getDatePosted()));
        m.setCreationDateTime(toZonedDateTime(p.getDateTaken()));
        ofNullable(p.getTakenGranularity()).ifPresent(granu -> m.setDateTakenGranularity(Integer.parseInt(granu)));
        m.setDescription(p.getDescription());
        m.setId(new CompositeMediaId(getPathAlias(p, flickrAccount), p.getId()));
        m.setLicense(Integer.parseInt(p.getLicense()));
        m.setMedia(FlickrMediaType.valueOf(p.getMedia()));
        m.setMediaStatus(p.getMediaStatus());
        m.setOriginalFormat(p.getOriginalFormat());
        FileMetadata md = new FileMetadata(newURL(p.getOriginalUrl()));
        md.setImageDimensions(new ImageDimensions(p.getOriginalWidth(), p.getOriginalHeight()));
        m.addMetadata(saveMetadata ? metadataRepository.save(md) : md);
        m.setTags(p.getTags().stream().map(Tag::getValue).collect(toSet()));
        m.setThumbnailUrl(newURL(p.getThumbnailUrl()));
        m.setTitle(p.getTitle());
        return m;
    }

    private static String getPathAlias(Photo p, String flickrAccount) {
        return StringUtils.isEmpty(p.getPathAlias()) ? flickrAccount : p.getPathAlias();
    }
}
