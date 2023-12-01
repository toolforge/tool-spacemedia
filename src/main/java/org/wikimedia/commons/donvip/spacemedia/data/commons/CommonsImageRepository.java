package org.wikimedia.commons.donvip.spacemedia.data.commons;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CommonsImageRepository extends JpaRepository<CommonsImage, String> {

    List<CommonsImageProjection> findBySha1OrderByTimestamp(String sha1);

    List<CommonsImageProjection> findBySha1InOrderByTimestamp(Collection<String> sha1);

    @Query("select max(timestamp) from CommonsImage where sha1 in ?1")
    String findMaxTimestampBySha1In(Collection<String> sha1);

    Page<CommonsImageProjection> findByMinorMimeInAndTimestampBetween(Collection<String> minorMimes,
            String startTimestamp, String endTimestamp, Pageable pageable);

    Optional<CommonsImageProjection> findByName(String id);
}
