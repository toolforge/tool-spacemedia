package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toSet;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.newURL;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.replace;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.toLocalDateTime;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.wikimedia.commons.donvip.spacemedia.data.domain.Statistics;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.ImageDimensions;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrFreeLicense;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrMediaType;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrPhotoSet;
import org.wikimedia.commons.donvip.spacemedia.exception.ImageUploadForbiddenException;
import org.wikimedia.commons.donvip.spacemedia.exception.UploadException;
import org.wikimedia.commons.donvip.spacemedia.service.flickr.FlickrMediaProcessorService;
import org.wikimedia.commons.donvip.spacemedia.service.flickr.FlickrService;
import org.wikimedia.commons.donvip.spacemedia.utils.UnitedStates;
import org.wikimedia.commons.donvip.spacemedia.utils.UnitedStates.VirinTemplates;

import com.flickr4java.flickr.FlickrException;
import com.flickr4java.flickr.people.User;
import com.flickr4java.flickr.photos.Photo;
import com.flickr4java.flickr.tags.Tag;

public abstract class AbstractOrgFlickrService extends AbstractOrgService<FlickrMedia, Long, LocalDateTime> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractOrgFlickrService.class);
    private static final Pattern DELETED_PHOTO = Pattern.compile("Photo \"(\\d+)\" not found \\(invalid ID\\)");

    @Autowired
    protected FlickrMediaRepository flickrRepository;
    @Autowired
    protected FlickrService flickrService;
    @Autowired
    protected FlickrMediaProcessorService processor;

    protected final Set<String> flickrAccounts;
    protected final Map<String, Map<String, String>> flickrPhotoSets;
    protected final Map<String, Map<String, String>> flickrTags;

    protected AbstractOrgFlickrService(FlickrMediaRepository repository, String id, Set<String> flickrAccounts) {
        super(repository, id);
        this.flickrAccounts = Objects.requireNonNull(flickrAccounts);
        this.flickrPhotoSets = new HashMap<>();
        this.flickrTags = new HashMap<>();
    }

    @Override
    @PostConstruct
    void init() throws IOException {
        super.init();
        for (String account : flickrAccounts) {
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
    public final long countMissingImages() {
        return flickrRepository.countMissingImagesInCommons(flickrAccounts);
    }

    @Override
    public final long countMissingVideos() {
        return flickrRepository.countMissingVideosInCommons(flickrAccounts);
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
    public final Page<FlickrMedia> listMissingImages(Pageable page) {
        return flickrRepository.findMissingImagesInCommons(flickrAccounts, page);
    }

    @Override
    public final Page<FlickrMedia> listMissingVideos(Pageable page) {
        return flickrRepository.findMissingVideosInCommons(flickrAccounts, page);
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
    protected final Optional<Temporal> getCreationDate(FlickrMedia media) {
        return ofNullable(media.getDateTaken());
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
    public Set<String> findLicenceTemplates(FlickrMedia media) {
        Set<String> result = super.findLicenceTemplates(media);
        try {
            result.add(FlickrFreeLicense.of(media.getLicense()).getWikiTemplate());
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Non-free Flickr licence for media {}: {}", media.getId(), e.getMessage());
        }
        result.add("Flickrreview");
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
            ofNullable(EsaService.getCopernicusTemplate(description)).ifPresent(result::add);
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
        return newURL(getUserPhotosUrl(media).toExternalForm() + "/" + media.getId());
    }

    @Override
    public void updateMedia() {
        LocalDateTime start = startUpdateMedia();
        List<FlickrMedia> uploadedMedia = new ArrayList<>();
        LocalDate minUploadDate = getRuntimeData().getDoNotFetchEarlierThan();
        int count = 0;
        for (String flickrAccount : flickrAccounts) {
            try {
                LOGGER.info("Fetching Flickr media from account '{}'...", flickrAccount);
                List<FlickrMedia> freePictures = buildFlickrMediaList(
                        flickrService.findFreePhotos(flickrAccount, minUploadDate));
                LOGGER.info("Found {} free Flickr media for account '{}'", freePictures.size(), flickrAccount);
                Pair<Integer, Collection<FlickrMedia>> result = processFlickrMedia(freePictures, flickrAccount);
                Collection<FlickrMedia> localUploadedImages = result.getRight();
                uploadedMedia.addAll(localUploadedImages);
                count += result.getLeft();
                postSocialMedia(localUploadedImages,
                        localUploadedImages.stream().flatMap(m -> m.getMetadata().stream()).toList());
                if (minUploadDate == null) {
                    // Only delete pictures not found in complete updates
                    Set<FlickrMedia> noLongerFreePictures = flickrRepository.findNotIn(Set.of(flickrAccount),
                            freePictures.stream().map(FlickrMedia::getId).collect(toSet()));
                    if (!noLongerFreePictures.isEmpty()) {
                        count += updateNoLongerFreeFlickrMedia(flickrAccount, noLongerFreePictures);
                    }
                }
            } catch (FlickrException | MalformedURLException | RuntimeException e) {
                LOGGER.error("Error while fetching Flickr media from account {}", flickrAccount, e);
            }
        }
        endUpdateMedia(count, uploadedMedia, start, false /* tweets already posted - one by Flickr account */);
    }

    private int updateNoLongerFreeFlickrMedia(String flickrAccount, Set<FlickrMedia> pictures)
            throws MalformedURLException {
        int count = 0;
        LOGGER.info("Checking {} Flickr images no longer free for account '{}'...", pictures.size(), flickrAccount);
        for (FlickrMedia picture : pictures) {
            try {
                count += processFlickrMedia(
                        buildFlickrMediaList(List.of(flickrService.findPhoto(picture.getId().toString()))),
                        flickrAccount).getLeft();
            } catch (FlickrException e) {
                if (e.getErrorMessage() != null) {
                    Matcher m = DELETED_PHOTO.matcher(e.getErrorMessage());
                    if (m.matches()) {
                        String id = m.group(1);
                        LOGGER.warn("Flickr image {} has been deleted for account '{}'", id, flickrAccount);
                        processor.deleteFlickrMedia(id);
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
        return photos.stream().map(this::photoToFlickrMedia).toList();
    }

    private FlickrMedia photoToFlickrMedia(Photo p) {
        return flickrRepository.findById(Long.parseLong(p.getId())).orElseGet(() -> {
            try {
                FlickrMedia media = mapPhoto(p);
                metadataRepository.save(media.getUniqueMetadata());
                return flickrRepository.save(media);
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
                        getUrlResolver());
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

    protected final Triple<FlickrMedia, Collection<FileMetadata>, Integer> uploadWrapped(FlickrMedia media) {
        try {
            return upload(media, true, false);
        } catch (UploadException e) {
            LOGGER.error("Failed to upload {}", media, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    protected final FlickrMedia refresh(FlickrMedia media) throws IOException {
        try {
            return media.copyDataFrom(mapPhoto(flickrService.findPhoto(media.getId().toString())));
        } catch (FlickrException e) {
            throw new IOException(e);
        }
    }

    private static FlickrMedia mapPhoto(Photo p) throws FlickrException {
        FlickrMedia m = new FlickrMedia();
        ofNullable(p.getGeoData()).ifPresent(geo -> {
            m.setLatitude(geo.getLatitude());
            m.setLongitude(geo.getLongitude());
            m.setAccuracy(geo.getAccuracy());
        });
        m.setDate(toLocalDateTime(p.getDatePosted()));
        m.setDateTaken(toLocalDateTime(p.getDateTaken()));
        ofNullable(p.getTakenGranularity()).ifPresent(granu -> m.setDateTakenGranularity(Integer.parseInt(granu)));
        m.setDescription(p.getDescription());
        m.setId(Long.valueOf(p.getId()));
        m.setLicense(Integer.parseInt(p.getLicense()));
        m.setMedia(FlickrMediaType.valueOf(p.getMedia()));
        m.setMediaStatus(p.getMediaStatus());
        m.setOriginalFormat(p.getOriginalFormat());
        FileMetadata md = m.getUniqueMetadata();
        md.setAssetUrl(newURL(p.getOriginalUrl()));
        md.setImageDimensions(new ImageDimensions(p.getOriginalWidth(), p.getOriginalHeight()));
        m.setPathAlias(p.getPathAlias());
        m.setTags(p.getTags().stream().map(Tag::getValue).collect(toSet()));
        m.setThumbnailUrl(newURL(p.getThumbnailUrl()));
        m.setTitle(p.getTitle());
        return m;
    }

    @Override
    protected final Long getMediaId(String id) {
        return Long.parseLong(id);
    }

    @Override
    protected final int doResetIgnored() {
        return flickrRepository.resetIgnored(flickrAccounts);
    }
}
