package org.wikimedia.commons.donvip.spacemedia.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.flickr4java.flickr.Flickr;
import com.flickr4java.flickr.FlickrException;
import com.flickr4java.flickr.REST;
import com.flickr4java.flickr.photos.Photo;
import com.flickr4java.flickr.photos.PhotoList;
import com.flickr4java.flickr.photos.SearchParameters;

@Service
public class FlickrService {

    private static final int MAX_PER_PAGE = 500;

    private static final List<Integer> FREE_LICENSES = Arrays.asList(
        4, // "Attribution License",             // https://creativecommons.org/licenses/by/2.0/
        5, // "Attribution-ShareAlike License",  // https://creativecommons.org/licenses/by-sa/2.0/
        7, // "No known copyright restrictions", // https://www.flickr.com/commons/usage/
        8, // "United States Government Work",   // https://www.usa.gov/copyright.shtml
        9, // "Public Domain Dedication (CC0)",  // https://creativecommons.org/publicdomain/zero/1.0/
        10 // "Public Domain Mark"               // https://creativecommons.org/publicdomain/mark/1.0/
        );

    private static final Set<String> EXTRAS = new HashSet<>(Arrays.asList(
            "description", "license", "date_upload", "date_taken", "owner_name",
            "original_format", "last_update", "geo", "tags", 
            "machine_tags", "o_dims", "media", "path_alias", "url_o"));

    private final Flickr flickr;

    @Autowired
    public FlickrService(
            @Value("${flickr.api.key}") String flickrApiKey, 
            @Value("${flickr.secret}") String flickrSecret) {
        flickr = new Flickr(flickrApiKey, flickrSecret, new REST());
    }

    public List<Photo> findFreePhotos(String userId) throws FlickrException {
        List<Photo> result = new ArrayList<Photo>();
        SearchParameters params = new SearchParameters();
        params.setUserId(Objects.requireNonNull(userId));
        params.setExtras(EXTRAS);
        // Multi-license search does not work
        // https://www.flickr.com/groups/51035612836@N01/discuss/72157665503298714/72157667263924940
        for (int license : FREE_LICENSES) {
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
}
