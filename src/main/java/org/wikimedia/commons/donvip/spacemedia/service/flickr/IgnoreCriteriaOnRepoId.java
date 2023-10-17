package org.wikimedia.commons.donvip.spacemedia.service.flickr;

import java.util.List;

import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrMedia;

public record IgnoreCriteriaOnRepoId(List<String> repoIds, List<String> unless) implements IgnoreCriteria {
    @Override
    public boolean match(FlickrMedia media) {
        return repoIds.contains(media.getId().getRepoId())
                && (media.getDescription() == null || !unless.stream().allMatch(media.getDescription()::contains));
    }
}