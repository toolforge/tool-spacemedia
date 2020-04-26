package org.wikimedia.commons.donvip.spacemedia.controller;

import static org.springframework.data.domain.Sort.Direction.DESC;
import static org.wikimedia.commons.donvip.spacemedia.controller.PagingSortingDefaults.SIZE;
import static org.wikimedia.commons.donvip.spacemedia.controller.PagingSortingDefaults.SORT;

import java.io.IOException;
import java.time.temporal.Temporal;
import java.util.List;
import java.util.Objects;

import javax.xml.parsers.ParserConfigurationException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.wikimedia.commons.donvip.spacemedia.data.domain.Media;
import org.wikimedia.commons.donvip.spacemedia.data.domain.Problem;
import org.wikimedia.commons.donvip.spacemedia.data.domain.Statistics;
import org.wikimedia.commons.donvip.spacemedia.service.agencies.AbstractAgencyService;
import org.wikimedia.commons.donvip.spacemedia.service.agencies.Agency;
import org.wikimedia.commons.donvip.spacemedia.service.agencies.AsyncAgencyUpdaterService;
import org.xml.sax.SAXException;

/**
 * Superclass of space agencies REST controllers. Sub-classes are created dynamically.
 *
 * @param <T> the media type the repository manages
 * @param <ID> the type of the id of the entity the repository manages
 * @param <D> the media date type
 */
public abstract class SpaceAgencyRestController<T extends Media<ID, D>, ID, D extends Temporal> {

    @Autowired
    private AsyncAgencyUpdaterService async;

    protected final Agency<T, ID, D> service;

    public SpaceAgencyRestController(AbstractAgencyService<T, ID, D, ?, ?, ?> service) {
        this.service = Objects.requireNonNull(service);
    }

    @GetMapping("/stats")
    public final Statistics getStats() {
        return service.getStatistics();
    }

    @GetMapping("/all")
    public final Page<T> listAll(@PageableDefault(size = SIZE, sort = SORT, direction = DESC) Pageable page) {
        return service.listAllMedia(page);
    }

    @GetMapping("/missing")
    public final Page<T> listMissing(@PageableDefault(size = SIZE, sort = SORT, direction = DESC) Pageable page) {
        return service.listMissingMedia(page);
    }

    @GetMapping("/uploaded")
    public final Page<T> listUploaded(@PageableDefault(size = SIZE, sort = SORT, direction = DESC) Pageable page) {
        return service.listUploadedMedia(page);
    }

    @GetMapping("/ignored")
    public final Page<T> listIgnored(@PageableDefault(size = SIZE, sort = SORT, direction = DESC) Pageable page) {
        return service.listIgnoredMedia(page);
    }

    @GetMapping("/duplicates")
    public final List<T> listDuplicate() {
        return service.listDuplicateMedia();
    }

    @GetMapping("/problems")
    public final List<Problem> listProblems() {
        return service.getProblems();
    }

    @GetMapping("/search")
    public final Page<T> search(@RequestParam("q") String q, @PageableDefault(size = SIZE) Pageable page) {
        return service.searchMedia(q, page);
    }

    @GetMapping("/update")
    public final void update() throws IOException {
        async.updateMedia(service);
    }

    @GetMapping("/upload/{sha1}")
    public final T upload(@PathVariable String sha1) throws IOException {
        return service.upload(sha1);
    }

    @GetMapping("/wiki/{sha1}")
    public final String wikiPreview(@PathVariable String sha1) throws IOException, ParserConfigurationException, SAXException {
        return service.getWikiHtmlPreview(sha1);
    }

    @GetMapping("/wikicode/{sha1}")
    public final String wikiCode(@PathVariable String sha1) {
        return service.getWikiCode(sha1);
    }
}
