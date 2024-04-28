package org.wikimedia.commons.donvip.spacemedia.service.flickr;

import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.newURL;

import java.net.URL;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrLicense;

import com.flickr4java.flickr.Flickr;
import com.flickr4java.flickr.FlickrException;
import com.flickr4java.flickr.REST;
import com.flickr4java.flickr.people.User;
import com.flickr4java.flickr.photos.Photo;
import com.flickr4java.flickr.photos.PhotoList;
import com.flickr4java.flickr.photos.PhotoSet;
import com.flickr4java.flickr.photos.SearchParameters;

@Lazy
@Service
public class FlickrService {

    private static final int MAX_PER_PAGE = 500;

    private static final Set<String> EXTRAS = new HashSet<>(Arrays.asList(
            "description", "license", "date_upload", "date_taken",
            "original_format", "last_update", "geo", "tags",
            "media", "path_alias", "url_m", "url_o"));

    private final Flickr flickr;

    @Autowired
    public FlickrService(
            @Value("${flickr.api.key}") String flickrApiKey,
            @Value("${flickr.secret}") String flickrSecret) {
        flickr = new Flickr(flickrApiKey, flickrSecret, new REST());
    }

    @Cacheable("usersByUrl")
    public User findUser(URL url) throws FlickrException {
        return flickr.getUrlsInterface().lookupUserByURL(url.toExternalForm());
    }

    @Cacheable("userNamesByNsid")
    public String findUserName(String nsid) throws FlickrException {
        return flickr.getPeopleInterface().getInfo(nsid).getUsername();
    }

    @Cacheable("userProfilesByNsid")
    public URL findUserProfileUrl(String nsid) throws FlickrException {
        return newURL(flickr.getUrlsInterface().getUserProfile(nsid));
    }

    public Photo findPhoto(String photoId) throws FlickrException {
        return flickr.getPhotosInterface().getPhoto(photoId);
    }

    public List<Photo> findPhotos(Set<String> photoIds) throws FlickrException {
        List<Photo> result = new ArrayList<>();
        for (String photoId : photoIds) {
            result.add(findPhoto(photoId));
        }
        return result;
    }

    public List<Photo> searchFreePhotos(String userId, LocalDate minUploadDate) throws FlickrException {
        return searchPhotos(userId, minUploadDate, false);
    }

    public List<Photo> searchPhotos(String userId, LocalDate minUploadDate, boolean includeAll) throws FlickrException {
        List<Photo> result = new ArrayList<>();
        SearchParameters params = new SearchParameters();
        params.setUserId(Objects.requireNonNull(userId));
        params.setExtras(EXTRAS);
        if (minUploadDate != null) {
            params.setMinUploadDate(Date.from(minUploadDate.atStartOfDay(ZoneId.systemDefault()).toInstant()));
        }
        // Multi-license search does not work
        // https://www.flickr.com/groups/51035612836@N01/discuss/72157665503298714/72157667263924940
        for (int license : Arrays.stream(FlickrLicense.values()).filter(l -> includeAll || l.isFree())
                .map(FlickrLicense::getCode).toList()) {
            PhotoList<Photo> photos;
            params.setLicense(Integer.toString(license));
            int page = 1;
            do {
                photos = flickr.getPhotosInterface().search(params, MAX_PER_PAGE, page++);
                result.addAll(photos);
            } while (photos.getPage() < photos.getPages());
        }
        return result;
    }

    public List<PhotoSet> findPhotoSets(String photoId) throws FlickrException {
        return flickr.getPhotosInterface().getAllContexts(photoId).getPhotoSetList();
    }
}
