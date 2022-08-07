package org.wikimedia.commons.donvip.spacemedia.data.commons;

import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface CommonsImageRepository extends PagingAndSortingRepository<CommonsImage, String> {

    List<CommonsImage> findBySha1OrderByTimestamp(String sha1);

    Page<CommonsImageProjection> findByMinorMimeInAndTimestampGreaterThanEqual(Collection<String> minorMimes,
            String timestamp, Pageable pageable);
}
