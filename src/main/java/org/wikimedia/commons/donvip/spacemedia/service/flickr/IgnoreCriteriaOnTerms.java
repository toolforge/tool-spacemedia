package org.wikimedia.commons.donvip.spacemedia.service.flickr;

import java.util.List;

import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrMedia;

public record IgnoreCriteriaOnTerms(List<String> ignoredTerms, List<String> unless) implements IgnoreCriteria {
    @Override
    public boolean match(FlickrMedia media) {
        return media.getDescription() != null && ignoredTerms.stream().anyMatch(media.getDescription()::contains)
                && !unless.stream().allMatch(media.getDescription()::contains);
    }
}