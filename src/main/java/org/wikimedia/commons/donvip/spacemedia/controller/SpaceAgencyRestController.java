package org.wikimedia.commons.donvip.spacemedia.controller;

import static org.springframework.data.domain.Sort.Direction.DESC;
import static org.wikimedia.commons.donvip.spacemedia.controller.PagingSortingDefaults.SIZE;
import static org.wikimedia.commons.donvip.spacemedia.controller.PagingSortingDefaults.SORT;

import java.io.IOException;
import java.time.temporal.Temporal;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.wikimedia.commons.donvip.spacemedia.data.domain.Statistics;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.Media;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.Problem;
import org.wikimedia.commons.donvip.spacemedia.exception.ImageNotFoundException;
import org.wikimedia.commons.donvip.spacemedia.exception.TooManyResultsException;
import org.wikimedia.commons.donvip.spacemedia.exception.UploadException;
import org.wikimedia.commons.donvip.spacemedia.service.agencies.AbstractAgencyService;
import org.wikimedia.commons.donvip.spacemedia.service.agencies.Agency;
import org.wikimedia.commons.donvip.spacemedia.service.agencies.AsyncAgencyUpdaterService;

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

    protected SpaceAgencyRestController(AbstractAgencyService<T, ID, D> service) {
        this.service = Objects.requireNonNull(service);
    }

    @GetMapping("/stats")
    public final Statistics getStats() {
        return service.getStatistics(true);
    }

    @GetMapping("/all")
    public final Page<T> listAll(@PageableDefault(size = SIZE, sort = SORT, direction = DESC) Pageable page) {
        return service.listAllMedia(page);
    }

    @GetMapping("/missing")
    public final Page<T> listMissing(@PageableDefault(size = SIZE, sort = SORT, direction = DESC) Pageable page) {
        return service.listMissingMedia(page);
    }

    @GetMapping("/missing/images")
    public final Page<T> listMissingImages(@PageableDefault(size = SIZE, sort = SORT, direction = DESC) Pageable page) {
        return service.listMissingImages(page);
    }

    @GetMapping("/missing/videos")
    public final Page<T> listMissingVideos(@PageableDefault(size = SIZE, sort = SORT, direction = DESC) Pageable page) {
        return service.listMissingVideos(page);
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
    public final void update() throws IOException, UploadException {
        async.updateMedia(service);
    }

    @GetMapping("/media/**")
    public final T getMedia(HttpServletRequest request) throws ImageNotFoundException {
        return service.getById(extractId(request, "media"));
    }

    @PutMapping("/media/**")
    public final T putMedia(HttpServletRequest request, @RequestBody T media) {
        T result = service.saveMedia(media);
        service.evictCaches();
        return result;
    }

    @DeleteMapping("/media/**")
    public final void deleteMedia(HttpServletRequest request) throws ImageNotFoundException {
        service.deleteById(extractId(request, "media"));
    }

    @GetMapping("/upload/{sha1}")
    public final T upload(@PathVariable String sha1) throws UploadException, TooManyResultsException {
        return service.uploadAndSaveBySha1(sha1, true);
    }

    @GetMapping("/uploadmedia/**")
    public final T uploadMedia(HttpServletRequest request) throws UploadException, TooManyResultsException {
        return service.uploadAndSaveById(extractId(request, "uploadmedia"), true);
    }

    @GetMapping("/refreshmedia/**")
    public final T refreshMedia(HttpServletRequest request) throws ImageNotFoundException, IOException {
        return service.refreshAndSaveById(extractId(request, "refreshmedia"));
    }

    @GetMapping("/refreshmissing")
    public final void refreshMissingMedia() throws IOException {
        refreshMissingMedia(m -> true);
    }

    @GetMapping("/refreshmissing/images")
    public final void refreshMissingImages() throws IOException {
        refreshMissingMedia(T::isImage);
    }

    @GetMapping("/refreshmissing/videos")
    public final void refreshMissingVideos() throws IOException {
        refreshMissingMedia(T::isVideo);
    }

    private final void refreshMissingMedia(Predicate<T> mustRefresh) throws IOException {
        for (T media : service.listMissingMedia()) {
            if (mustRefresh.test(media)) {
                service.refreshAndSave(media);
            }
        }
    }

    @GetMapping("/wiki/{sha1}")
    public final String wikiPreview(@PathVariable String sha1) throws TooManyResultsException {
        return service.getWikiHtmlPreview(sha1);
    }

    @GetMapping("/wikicode/{sha1}")
    public final String wikiCode(@PathVariable String sha1) throws TooManyResultsException {
        return service.getWikiCode(sha1);
    }

    @GetMapping("/evictcaches")
    public final void evictCaches() {
        service.evictCaches();
    }

    /**
     * STScI ids contain slashes so we cannot use {@link RequestParam} for ids.
     *
     * @param request HTTP request
     * @param name    name
     * @return id request param
     */
    String extractId(HttpServletRequest request, String name) {
        String requestURI = request.getRequestURI();
        return requestURI.substring(requestURI.indexOf('/', ("/" + service.getId() + "/rest/" + name).length()) + 1);
    }
}
