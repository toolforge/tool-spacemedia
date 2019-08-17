package org.wikimedia.commons.donvip.spacemedia.data.domain;

import java.net.URL;
import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

public interface ProblemRepository extends CrudRepository<Problem, Integer> {

    List<Problem> findByAgency(String name);

    long countByAgency(String name);

    Optional<Problem> findByAgencyAndProblematicUrl(String agency, URL problematicUrl);
}
