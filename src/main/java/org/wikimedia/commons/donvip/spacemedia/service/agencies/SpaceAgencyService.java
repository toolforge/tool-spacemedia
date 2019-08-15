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
import org.wikimedia.commons.donvip.spacemedia.exception.ImageNotFoundException;
import org.wikimedia.commons.donvip.spacemedia.exception.ImageUploadForbiddenException;

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

    public List<T> listIgnoredMedia() {
        return repository.findByIgnoredTrue();
    }

    public abstract String getName();

    public abstract List<T> updateMedia() throws IOException;

    public Statistics getStatistics() {
        long problems = getProblemsCount();
        return new Statistics(getName(), countAllMedia(), countMissingMedia(), problems > 0 ? problems : null);
    }

    public List<Problem> getProblems() {
        return problemrepository.findByAgency(getName());
    }

    public long getProblemsCount() {
        return problemrepository.countByAgency(getName());
    }

    protected Problem problem(URL problematicUrl, Throwable t) {
        return problem(problematicUrl, t.getMessage());
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

    public T upload(String sha1) {
        T media = repository.findBySha1(sha1).orElseThrow(() -> new ImageNotFoundException(sha1));
        checkUploadPreconditions(media);
        // TODO
        return media;
    }

    protected void checkUploadPreconditions(T media) {
        if (media.isIgnored() == Boolean.TRUE) {
            throw new ImageUploadForbiddenException(media.getSha1());
        }
    }
}
