package org.wikimedia.commons.donvip.spacemedia.repo.flickr;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import com.flickr4java.flickr.Flickr;
import com.flickr4java.flickr.FlickrException;
import com.flickr4java.flickr.REST;
import com.flickr4java.flickr.Response;
import com.flickr4java.flickr.Transport;
import com.flickr4java.flickr.people.User;
import com.flickr4java.flickr.photos.Photo;
import com.flickr4java.flickr.photos.PhotoList;
import com.flickr4java.flickr.photos.PhotoSet;
import com.flickr4java.flickr.photos.SearchParameters;
import com.flickr4java.flickr.urls.UrlsInterface;

@Service
public class FlickrApiService {

    private static final int MAX_PER_PAGE = 500;

    private static final Set<String> EXTRAS = new HashSet<>(Arrays.asList(
            "description", "license", "date_upload", "date_taken",
            "original_format", "last_update", "geo", "tags", 
            "media", "path_alias", "url_m", "url_o"));

    private final Flickr flickr;

    @Autowired
    public FlickrApiService(
            @Value("${flickr.api.key}") String flickrApiKey, 
            @Value("${flickr.secret}") String flickrSecret) {
        flickr = new Flickr(flickrApiKey, flickrSecret, new REST());
    }

    @Cacheable("usersByUrl")
    public User findUser(URL url) throws FlickrException {
        return new UrlsInterfacePatched(flickr).lookupUserPatched(url.toExternalForm());
    }

    @Cacheable("userNamesByNsid")
    public String findUserName(String nsid) throws FlickrException {
        return flickr.getPeopleInterface().getInfo(nsid).getUsername();
    }

    @Cacheable("userProfilesByNsid")
    public URL findUserProfileUrl(String nsid) throws FlickrException, MalformedURLException {
        return new URL(flickr.getUrlsInterface().getUserProfile(nsid));
    }

    public List<Photo> findPhotos(Set<String> photoIds) throws FlickrException {
        List<Photo> result = new ArrayList<>();
        for (String photoId : photoIds) {
            result.add(flickr.getPhotosInterface().getPhoto(photoId));
        }
        return result;
    }

    public List<Photo> findFreePhotos(String userId) throws FlickrException {
        List<Photo> result = new ArrayList<>();
        SearchParameters params = new SearchParameters();
        params.setUserId(Objects.requireNonNull(userId));
        params.setExtras(EXTRAS);
        // Multi-license search does not work
        // https://www.flickr.com/groups/51035612836@N01/discuss/72157665503298714/72157667263924940
        for (int license : Arrays.stream(FlickrFreeLicense.values()).map(FlickrFreeLicense::getCode).collect(Collectors.toList())) {
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

    // TODO: create a pull request at https://github.com/boncey/Flickr4Java
    static class UrlsInterfacePatched extends UrlsInterface {

        private final String apiKey;
        private final String sharedSecret;
        private final Transport transport;

        public UrlsInterfacePatched(Flickr flickr) {
            super(flickr.getApiKey(), flickr.getSharedSecret(), flickr.getTransport());
            this.apiKey = flickr.getApiKey();
            this.sharedSecret = flickr.getSharedSecret();
            this.transport = flickr.getTransport();
        }

        /**
         * Lookup the userid and username for the specified User URL.
         * 
         * @param url The user profile URL
         * @return The userid and username
         * @throws FlickrException
         */
        public User lookupUserPatched(String url) throws FlickrException {
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("method", METHOD_LOOKUP_USER);
            parameters.put("url", url);

            Response response = transport.get(transport.getPath(), parameters, apiKey, sharedSecret);
            if (response.isError()) {
                throw new FlickrException(response.getErrorCode(), response.getErrorMessage());
            }

            Element payload = response.getPayload();
            Element groupnameElement = (Element) payload.getElementsByTagName("username").item(0);
            User user = new User();
            user.setId(payload.getAttribute("id"));
            user.setUsername(((Text) groupnameElement.getFirstChild()).getData());
            return user;
        }
    }
}
