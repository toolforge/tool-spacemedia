package org.wikimedia.commons.donvip.spacemedia.repo.flickr;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.core.AbstractHarvesterService;
import org.wikimedia.commons.donvip.spacemedia.data.jpa.entity.Depot;
import org.wikimedia.commons.donvip.spacemedia.data.jpa.entity.FilePublication;
import org.wikimedia.commons.donvip.spacemedia.data.jpa.entity.MediaPublication;
import org.wikimedia.commons.donvip.spacemedia.data.jpa.entity.Organization;
import org.wikimedia.commons.donvip.spacemedia.data.jpa.entity.PublicationKey;

import com.flickr4java.flickr.FlickrException;
import com.flickr4java.flickr.photos.Photo;

@Service
public class FlickrHarvesterService extends AbstractHarvesterService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlickrHarvesterService.class);

    private static final Pattern DELETED_PHOTO = Pattern.compile("Photo \"([0-9]+)\" not found \\(invalid ID\\)");

    private static final String FLICKR_CONTEXT = "Flickr";

    @Autowired
    private FlickrApiService flickrService;

    @Value("${flickr.depot.id.prefix}")
    private String depotIdPrefix;

    @Value("${flickr.depot.name.prefix}")
    private String depotNamePrefix;

    @Value("${flickr.org.id}")
    private String orgId;

    @Value("${flickr.lang}")
    private String lang;

    @Value("${flickr.accounts}")
    private Set<String> flickrAccounts;

    @Value("${flickr.credit.regex}")
    private String creditRegex;

    private Pattern creditPattern;

    @PostConstruct
    public void init() {
        if (creditRegex != null) {
            creditPattern = Pattern.compile(creditRegex, Pattern.MULTILINE);
        }
    }

    @Override
    public void harvestMedia() throws IOException {
        LocalDateTime start = startUpdateMedia(depotIdPrefix);
        int count = 0;
        Organization org = organizationRepository.findById(orgId).orElseThrow();
        Set<Organization> operators = Set.of(org, organizationRepository.findById("Flickr").orElseThrow());
        for (String flickrAccount : flickrAccounts) {
            try {
                Depot depot = depotRepository.findOrCreate(depotIdPrefix + '-' + flickrAccount,
                        depotNamePrefix + ' ' + flickrAccount,
                        new URL("https://www.flickr.com/photos/" + flickrAccount), operators);
                LOGGER.info("Fetching Flickr media from account '{}'...", flickrAccount);
                List<MediaPublication> freePictures = buildFlickrMediaList(depot, org,
                        flickrService.findFreePhotos(flickrAccount));
                count += processFlickrMedia(freePictures, flickrAccount);
                Set<MediaPublication> noLongerFreePictures = mediaPublicationRepository
                        .findByDepotIdIn(Set.of(flickrAccount));
                noLongerFreePictures.removeAll(freePictures);
                if (!noLongerFreePictures.isEmpty()) {
                    count += updateNoLongerFreeFlickrMedia(depot, org, flickrAccount, noLongerFreePictures);
                }
            } catch (FlickrException | MalformedURLException | RuntimeException e) {
                LOGGER.error("Error while fetching Flickr media from account " + flickrAccount, e);
            }
        }
        endUpdateMedia(depotIdPrefix, count, start);
    }

    private int updateNoLongerFreeFlickrMedia(Depot depot, Organization org, String flickrAccount,
            Set<MediaPublication> pictures) throws MalformedURLException {
        int count = 0;
        LOGGER.info("Checking {} Flickr images no longer free for account '{}'...", pictures.size(), flickrAccount);
        for (MediaPublication picture : pictures) {
            try {
                count += processFlickrMedia(
                        buildFlickrMediaList(depot, org, flickrService.findPhotos(Set.of(picture.getId().toString()))),
                        flickrAccount);
            } catch (FlickrException e) {
                if (e.getErrorMessage() != null) {
                    Matcher m = DELETED_PHOTO.matcher(e.getErrorMessage());
                    if (m.matches()) {
                        String id = m.group(1);
                        LOGGER.warn("Flickr image {} has been deleted for account '{}'", id, flickrAccount);
                        mediaPublicationRepository.deleteById(new PublicationKey(depot.getId(), id));
                        count++;
                    } else {
                        LOGGER.error("Error while processing non-free Flickr image " + picture.getId()
                                + " from account " + flickrAccount, e);
                    }
                } else {
                    LOGGER.error("Error while processing non-free Flickr image " + picture.getId() + " from account "
                            + flickrAccount, e);
                }
            }
        }
        return count;
    }

    private List<MediaPublication> buildFlickrMediaList(Depot depot, Organization org, List<Photo> photos) {
        return photos.stream()
                .map(p -> mediaPublicationRepository.findById(new PublicationKey(depot.getId(), p.getId()))
                        .orElseGet(() -> toMediaPublication(depot, org, p)))
                .collect(Collectors.toList());
    }

    private MediaPublication toMediaPublication(Depot depot, Organization org, Photo photo) {
        LOGGER.info(photo.getUrl());
        MediaPublication media = new MediaPublication(depot, photo.getId(), newURL(photo.getUrl()));
        media.addAuthor(org);
        media.setPublicationDateTime(photo.getDatePosted().toInstant().atZone(ZoneOffset.UTC));
        media.setDescription(photo.getDescription());
        media.setTitle(photo.getTitle());
        media.setLang(lang);
        media.setThumbnailUrl(newURL(photo.getThumbnailUrl()));
        media.setLicence(FlickrFreeLicense.of(Integer.parseInt(photo.getLicense())).getLicence());
        if (creditPattern != null && photo.getDescription() != null) {
            Matcher m = creditPattern.matcher(photo.getDescription());
            if (m.find()) {
                media.setCredit(m.group(1));
            }
        }
        try {
            FilePublication filePublication = new FilePublication(depot, photo.getId(), newURL(photo.getOriginalUrl()));
            filePublication.addAuthor(org);
            filePublication.setCredit(media.getCredit());
            filePublication.setLicence(media.getLicence());
            filePublication.setPublicationDateTime(media.getPublicationDateTime());
            filePublication.setThumbnailUrl(media.getThumbnailUrl());
            filePublication.setCaptureDateTime(photo.getDateTaken().toInstant().atZone(ZoneOffset.UTC));
            media.addFilePublication(filePublicationRepository.save(filePublication));
        } catch (FlickrException e) {
            LOGGER.error("Error while retrieving original URL", e);
        }
        // TODO missing stuff from photo
        return mediaPublicationRepository.save(media);
    }

    private int processFlickrMedia(Iterable<MediaPublication> medias, String flickrAccount)
            throws MalformedURLException {
        int count = 0;
        for (MediaPublication media : medias) {
            try {
                /*
                 * processor.processFlickrMedia(media, flickrAccount, getOriginalRepository(),
                 * this::customProcessing, this::shouldUploadAuto, this::uploadWrapped);
                 */
                count++;
                throw new IOException(); // FIXME
            } catch (IOException e) {
                problem(media.getUrl(), e);
            }
        }
        return count;
    }
}
