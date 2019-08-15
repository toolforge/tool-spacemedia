package org.wikimedia.commons.donvip.spacemedia.controller;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.wikimedia.commons.donvip.spacemedia.data.local.Media;
import org.wikimedia.commons.donvip.spacemedia.data.local.Problem;
import org.wikimedia.commons.donvip.spacemedia.data.local.Statistics;
import org.wikimedia.commons.donvip.spacemedia.service.agencies.SpaceAgencyService;

public abstract class SpaceAgencyController<T extends Media, ID> {

    protected final SpaceAgencyService<T, ID> service;

    public SpaceAgencyController(SpaceAgencyService<T, ID> service) {
        this.service = Objects.requireNonNull(service);
    }

    @GetMapping("/all")
    public final Iterable<T> listAll() throws IOException {
        return service.listAllMedia();
    }

    @GetMapping("/update")
    public final List<T> update() throws IOException {
        return service.updateMedia();
    }

    @GetMapping("/missing")
    public final List<T> listMissing() throws IOException {
        return service.listMissingMedia();
    }

    @GetMapping("/duplicates")
    public final List<T> listDuplicate() throws IOException {
        return service.listDuplicateMedia();
    }

    @GetMapping("/ignored")
    public List<T> listIgnored() {
        return service.listIgnoredMedia();
    }

    @GetMapping("/stats")
    public final Statistics stats() throws IOException {
        return service.getStatistics();
    }

    @GetMapping("/problems")
    public final List<Problem> problems() throws IOException {
        return service.getProblems();
    }

    @GetMapping("/upload/{sha1}")
    public T upload(@PathVariable String sha1) {
        return service.upload(sha1);
    }
}
