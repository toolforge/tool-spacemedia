package org.wikimedia.commons.donvip.spacemedia.data.domain.base;

import java.net.URL;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

public interface ProblemRepository extends JpaRepository<Problem, Integer> {

    List<Problem> findByOrg(String orgId);

    Page<Problem> findByOrg(String orgId, Pageable page);

    long countByOrg(String orgId);

    @Modifying
    int deleteByOrg(String orgId);

    Optional<Problem> findByOrgAndProblematicUrl(String orgId, URL problematicUrl);
}
