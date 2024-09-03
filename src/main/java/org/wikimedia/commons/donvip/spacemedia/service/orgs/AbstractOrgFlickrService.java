package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import static java.lang.Integer.parseInt;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toSet;
import static org.wikimedia.commons.donvip.spacemedia.utils.CsvHelper.loadCsvMapping;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.newURL;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.toZonedDateTime;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
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
import org.springframework.context.annotation.Lazy;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.CompositeMediaId;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.MediaDimensions;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrLicense;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrMediaType;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrPhotoSet;
import org.wikimedia.commons.donvip.spacemedia.exception.ImageUploadForbiddenException;
import org.wikimedia.commons.donvip.spacemedia.service.ExecutionMode;
import org.wikimedia.commons.donvip.spacemedia.service.flickr.FlickrMediaProcessorService;
import org.wikimedia.commons.donvip.spacemedia.service.flickr.FlickrService;
import org.wikimedia.commons.donvip.spacemedia.service.flickr.IgnoreCriteria;
import org.wikimedia.commons.donvip.spacemedia.service.wikimedia.GlitchTip;
import org.wikimedia.commons.donvip.spacemedia.utils.UnitedStates;
import org.wikimedia.commons.donvip.spacemedia.utils.UnitedStates.VirinTemplates;

import com.flickr4java.flickr.FlickrException;
import com.flickr4java.flickr.people.User;
import com.flickr4java.flickr.photos.Photo;
import com.flickr4java.flickr.tags.Tag;

public abstract class AbstractOrgFlickrService extends AbstractOrgService<FlickrMedia> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractOrgFlickrService.class);
    private static final Pattern DELETED_PHOTO = Pattern.compile("Photo \"(\\d+)\" not found \\(invalid ID\\)");
    private static final Pattern ISO_DATE = Pattern.compile(".*(\\d{4}-(?:0[1-9]|1[0-2])-\\d{2}).*", Pattern.DOTALL);

    @Autowired
    protected FlickrMediaRepository flickrRepository;
    @Lazy
    @Autowired
    protected FlickrService flickrService;
    @Lazy
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
    public String getUiRepoId(String repoId) {
        return repoId.replaceAll("@N0[1-9]", "");
    }

    @Override
    public final URL getSourceUrl(FlickrMedia media, FileMetadata metadata, String ext) {
        return getPhotoUrl(media);
    }

    @Override
    protected final String getAuthor(FlickrMedia media, FileMetadata metadata) {
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
    protected String hiddenUploadCategory(String repoId) {
        return "Flickr files uploaded by " + commonsService.getAccount();
    }

    @Override
    public Set<String> findCategories(FlickrMedia media, FileMetadata metadata, boolean includeHidden) {
        Set<String> result = super.findCategories(media, metadata, includeHidden);
        mediaService.useMapping(result, media.getPathAlias(), media.getPhotosets(), flickrPhotoSets,
                FlickrPhotoSet::getTitle);
        mediaService.useMapping(result, media.getPathAlias(), media.getTags(), flickrTags, Function.identity());
        if (metadata.isVideo()) {
            VirinTemplates t = UnitedStates.getUsVirinTemplates(media.getTitle(),
                    getSourceUrl(media, metadata, metadata.getExtension()));
            if (t != null && StringUtils.isNotBlank(t.videoCategory())) {
                result.add(t.videoCategory());
            }
        }
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
        final String publicDomainMark = FlickrLicense.Public_Domain_Mark.getWikiTemplate();
        VirinTemplates t = UnitedStates.getUsVirinTemplates(media.getTitle(),
                getSourceUrl(media, metadata, metadata.getExtension()));
        if (t != null) {
            result.add(t.virinTemplate());
            if (StringUtils.isNotBlank(t.pdTemplate())) {
                result.remove(publicDomainMark);
                result.add(t.pdTemplate());
            }
        }
        final String description = media.getDescription();
        if (description != null) {
            if (result.contains(publicDomainMark) && UnitedStates.isClearPublicDomain(description)) {
                result.remove(publicDomainMark);
            }
            final LocalDate pubDate = media.getPublicationDate();
            if (pubDate != null && pubDate.isAfter(LocalDate.of(2015, 2, 1))
                    && pubDate.isBefore(LocalDate.of(2019, 8, 31))
                    && (description.contains("hoto by SpaceX") || description.contains("hoto/SpaceX"))) {
                result.add("PD-SpaceX");
            }
            if (description.contains("hoto by NASA") || description.contains("hoto/NASA")
                    || description.contains("redit: NASA")) {
                result.add("PD-USGov-NASA");
            }
        }
        if (result.contains(publicDomainMark) && result.stream().anyMatch(x -> x.startsWith("PD-"))) {
            result.remove(publicDomainMark);
        }
        return result;
    }

    @Override
    protected void checkUploadPreconditions(FlickrMedia media, boolean checkUnicity, boolean isManual) {
        super.checkUploadPreconditions(media, checkUnicity, isManual);
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
    public void updateMedia(String[] args) {
        LocalDateTime start = startUpdateMedia();
        List<FlickrMedia> uploadedMedia = new ArrayList<>();
        LocalDate minUploadDate = getRuntimeData().getDoNotFetchEarlierThan();
        int count = 0;
        for (String flickrAccount : getRepoIdsFromArgs(args)) {
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
                        localUploadedImages.stream().flatMap(FlickrMedia::getMetadataStream).toList());
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
                GlitchTip.capture(e);
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
                        GlitchTip.capture(e);
                    }
                } else {
                    LOGGER.error("Error while processing non-free Flickr image " + picture.getId()
                            + " from account " + flickrAccount, e);
                    GlitchTip.capture(e);
                }
            }
        }
        return count;
    }

    private List<FlickrMedia> buildFlickrMediaList(List<Photo> photos, String flickrAccount) {
        return photos.stream().map(x -> photoToFlickrMedia(x, flickrAccount)).toList();
    }

    private FlickrMedia photoToFlickrMedia(Photo p, String flickrAccount) {
        return repository.findById(new CompositeMediaId(getPathAlias(p, flickrAccount), p.getId()))
                .orElseGet(() -> saveMedia(mapPhoto(p, flickrAccount)));
    }

    private Pair<Integer, Collection<FlickrMedia>> processFlickrMedia(Iterable<FlickrMedia> medias,
            String flickrAccount) {
        int count = 0;
        LocalDateTime start = LocalDateTime.now();
        Collection<FlickrMedia> uploadedMedia = new ArrayList<>();
        for (FlickrMedia media : medias) {
            try {
                Pair<FlickrMedia, Integer> result = processor.processFlickrMedia(media, flickrAccount,
                        () -> getPatternsToRemove(media), () -> getStringsToRemove(media), this::shouldUploadAuto,
                        this::uploadWrapped, getUrlResolver(), this::getSimilarUploadedMediaByDate,
                        checkAllowlist(), checkBlocklist(), this::saveMedia, getIgnoreCriteria());
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

    protected List<IgnoreCriteria> getIgnoreCriteria() {
        return new ArrayList<>();
    }

    @Override
    protected final FlickrMedia refresh(FlickrMedia media) throws IOException {
        try {
            media.copyDataFrom(
                    mapPhoto(flickrService.findPhoto(media.getId().getMediaId()), media.getPathAlias()));
            if (processor.checkIgnoredCriteria(media, getIgnoreCriteria())) {
                for (FileMetadata metadata : media.getMetadata()) {
                    mediaService.saveMetadata(metadata);
                }
            }
            return media;
        } catch (FlickrException e) {
            if (e.getMessage().endsWith("not found (invalid ID)")) {
                LOGGER.warn(e.getMessage());
                return null;
            }
            throw new IOException(e);
        }
    }

    private FlickrMedia mapPhoto(Photo p, String flickrAccount) {
        FlickrMedia m = new FlickrMedia();
        ofNullable(p.getGeoData()).ifPresent(geo -> {
            m.setLatitude(geo.getLatitude());
            m.setLongitude(geo.getLongitude());
            m.setAccuracy(geo.getAccuracy());
        });
        m.setPublicationDateTime(toZonedDateTime(p.getDatePosted()));
        Matcher matcher = ISO_DATE.matcher(p.getTitle());
        if (matcher.matches()) {
            m.setCreationDate(LocalDate.parse(matcher.group(1), DateTimeFormatter.ISO_LOCAL_DATE));
        } else {
            matcher = ISO_DATE.matcher(ofNullable(p.getDescription()).orElse(""));
            if (matcher.matches()) {
                m.setCreationDate(LocalDate.parse(matcher.group(1), DateTimeFormatter.ISO_LOCAL_DATE));
            } else {
                m.setCreationDateTime(toZonedDateTime(p.getDateTaken()));
            }
        }
        ofNullable(p.getTakenGranularity()).ifPresent(granu -> m.setDateTakenGranularity(parseInt(granu)));
        m.setDescription(p.getDescription());
        m.setId(new CompositeMediaId(getPathAlias(p, flickrAccount), p.getId()));
        m.setLicense(parseInt(p.getLicense()));
        m.setMedia(FlickrMediaType.valueOf(p.getMedia()));
        m.setMediaStatus(p.getMediaStatus());
        m.setOriginalFormat(p.getOriginalFormat());
        MediaDimensions dimensions = new MediaDimensions(p.getOriginalWidth(), p.getOriginalHeight());
        try {
            addMetadata(m, p.getOriginalUrl(), md -> {
                md.setMediaDimensions(dimensions);
                md.setExtension(p.getOriginalFormat());
            });
        } catch (FlickrException e) {
            LOGGER.error("Flickr error : {}", e.getMessage(), e);
            GlitchTip.capture(e);
        }
        if ("jpg".equals(p.getOriginalFormat()) && "video".equals(p.getMedia())) {
            addMetadata(m, processor.getVideoUrl(p.getId()), md -> {
                md.setMediaDimensions(dimensions);
                md.setExtension("mp4");
            });
        }
        m.setTags(p.getTags().stream().map(Tag::getValue).collect(toSet()));
        m.setThumbnailUrl(newURL(p.getThumbnailUrl()));
        m.setTitle(p.getTitle());
        return m;
    }

    private static String getPathAlias(Photo p, String flickrAccount) {
        return StringUtils.isEmpty(p.getPathAlias()) ? flickrAccount : p.getPathAlias();
    }
}
