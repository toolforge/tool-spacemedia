package org.wikimedia.commons.donvip.spacemedia.service.agencies;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wikimedia.commons.donvip.spacemedia.data.local.Media;
import org.wikimedia.commons.donvip.spacemedia.data.local.MediaRepository;
import org.wikimedia.commons.donvip.spacemedia.data.local.Problem;
import org.wikimedia.commons.donvip.spacemedia.data.local.ProblemRepository;
import org.wikimedia.commons.donvip.spacemedia.data.local.Statistics;

public abstract class SpaceAgencyService<T extends Media, ID> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpaceAgencyService.class);

    protected final MediaRepository<T, ID> repository;
    protected final ProblemRepository problemrepository;

    public SpaceAgencyService(MediaRepository<T, ID> repository, ProblemRepository problemrepository) {
        this.repository = Objects.requireNonNull(repository);
        this.problemrepository = Objects.requireNonNull(problemrepository);
    }

    public long countAllMedia() {
        return repository.count();
    }

    public long countMissingMedia() {
        return repository.countMissingInCommons();
    }

    public Iterable<T> listAllMedia() {
        return repository.findAll();
    }

    public List<T> listMissingMedia() {
        return repository.findMissingInCommons();
    }

    public List<T> listDuplicateMedia() {
        return repository.findDuplicateInCommons();
    }

    public abstract String getName();

    public abstract List<T> updateMedia() throws IOException;

    public Statistics getStatistics() {
        return new Statistics(getName(), countAllMedia(), countMissingMedia(), getProblemsCount());
    }

    public List<Problem> getProblems() {
        return problemrepository.findByAgency(getName());
    }

    public long getProblemsCount() {
        return problemrepository.countByAgency(getName());
    }

    protected Problem problem(URL problematicUrl, String errorMessage) {
        Optional<Problem> problem = problemrepository.findByAgencyAndProblematicUrl(getName(), problematicUrl);
        if (problem.isPresent()) {
            return problem.get();
        } else {
            Problem pb = new Problem();
            pb.setAgency(getName());
            pb.setErrorMessage(errorMessage);
            pb.setProblematicUrl(problematicUrl);
            LOGGER.warn(pb.toString());
            return problemrepository.save(pb);
        }
    }
}
