package org.wikimedia.commons.donvip.spacemedia.service.flickr;

import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrMedia;

public interface IgnoreCriteria {
    boolean match(FlickrMedia media);
}
