package org.wikimedia.commons.donvip.spacemedia.data.domain;

import java.net.URL;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface ProblemRepository extends PagingAndSortingRepository<Problem, Integer> {

    List<Problem> findByAgency(String agencyId);

    Page<Problem> findByAgency(String agencyId, Pageable page);

    long countByAgency(String agencyId);

    @Modifying
    int deleteByAgency(String agencyId);

    Optional<Problem> findByAgencyAndProblematicUrl(String agencyId, URL problematicUrl);
}
