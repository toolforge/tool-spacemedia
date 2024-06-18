package org.wikimedia.commons.donvip.spacemedia.controller;

import static org.springframework.data.domain.Sort.Direction.DESC;
import static org.wikimedia.commons.donvip.spacemedia.controller.PagingSortingDefaults.SIZE;
import static org.wikimedia.commons.donvip.spacemedia.controller.PagingSortingDefaults.SORT;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.wikimedia.commons.donvip.spacemedia.data.domain.Statistics;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.Media;
import org.wikimedia.commons.donvip.spacemedia.exception.ImageNotFoundException;
import org.wikimedia.commons.donvip.spacemedia.exception.TooManyResultsException;
import org.wikimedia.commons.donvip.spacemedia.exception.UploadException;
import org.wikimedia.commons.donvip.spacemedia.service.orgs.AbstractOrgService;
import org.wikimedia.commons.donvip.spacemedia.service.orgs.AsyncOrgUpdaterService;
import org.wikimedia.commons.donvip.spacemedia.service.orgs.Org;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Superclass of orgs REST controllers. Sub-classes are created dynamically.
 *
 * @param <T>  the media type the repository manages
 */
public abstract class OrgRestController<T extends Media> {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrgRestController.class);

    @Lazy
    @Autowired
    private AsyncOrgUpdaterService async;

    protected final Org<T> service;

    protected OrgRestController(AbstractOrgService<T> service) {
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

    @GetMapping("/uploadbydate/{date}")
    public final List<T> uploadByDate(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(name = "repo", required = false) String repo)
            throws UploadException {
        return service.uploadAndSaveByDate(date, repo, x -> true, true);
    }

    @GetMapping("/uploadimagesbydate/{date}")
    public final List<T> uploadImagesByDate(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(name = "repo", required = false) String repo)
            throws UploadException {
        return service.uploadAndSaveByDate(date, repo, Media::isImage, true);
    }

    @GetMapping("/uploadvideosbydate/{date}")
    public final List<T> uploadVideosByDate(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(name = "repo", required = false) String repo)
            throws UploadException {
        return service.uploadAndSaveByDate(date, repo, Media::isVideo, true);
    }

    @GetMapping("/uploaddocumentsbydate/{date}")
    public final List<T> uploadDocumentsByDate(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(name = "repo", required = false) String repo)
            throws UploadException {
        return service.uploadAndSaveByDate(date, repo, Media::isDocument, true);
    }

    @GetMapping("/uploadbymonth/{month}")
    public final List<T> uploadByMonth(@PathVariable YearMonth month,
            @RequestParam(name = "repo", required = false) String repo) throws UploadException {
        return service.uploadAndSaveByMonth(month, repo, x -> true, true);
    }

    @GetMapping("/uploadimagesbymonth/{month}")
    public final List<T> uploadImagesByMonth(@PathVariable YearMonth month,
            @RequestParam(name = "repo", required = false) String repo) throws UploadException {
        return service.uploadAndSaveByMonth(month, repo, Media::isImage, true);
    }

    @GetMapping("/uploadvideosbymonth/{month}")
    public final List<T> uploadVideosByMonth(@PathVariable YearMonth month,
            @RequestParam(name = "repo", required = false) String repo) throws UploadException {
        return service.uploadAndSaveByMonth(month, repo, Media::isVideo, true);
    }

    @GetMapping("/uploaddocumentsbymonth/{month}")
    public final List<T> uploadDocumentsByMonth(@PathVariable YearMonth month,
            @RequestParam(name = "repo", required = false) String repo) throws UploadException {
        return service.uploadAndSaveByMonth(month, repo, Media::isDocument, true);
    }

    @GetMapping("/uploadbyyear/{year}")
    public final List<T> uploadByYear(@PathVariable Year year,
            @RequestParam(name = "repo", required = false) String repo) throws UploadException {
        return service.uploadAndSaveByYear(year, repo, x -> true, true);
    }

    @GetMapping("/uploadimagesbyyear/{year}")
    public final List<T> uploadImagesByYear(@PathVariable Year year,
            @RequestParam(name = "repo", required = false) String repo) throws UploadException {
        return service.uploadAndSaveByYear(year, repo, Media::isImage, true);
    }

    @GetMapping("/uploadvideosbyyear/{year}")
    public final List<T> uploadVideosByYear(@PathVariable Year year,
            @RequestParam(name = "repo", required = false) String repo) throws UploadException {
        return service.uploadAndSaveByYear(year, repo, Media::isVideo, true);
    }

    @GetMapping("/uploaddocumentsbyyear/{year}")
    public final List<T> uploadDocumentsByYear(@PathVariable Year year,
            @RequestParam(name = "repo", required = false) String repo) throws UploadException {
        return service.uploadAndSaveByYear(year, repo, Media::isDocument, true);
    }

    @GetMapping("/uploadbytitle/{title}")
    public final List<T> uploadByTitle(@PathVariable String title,
            @RequestParam(name = "repo", required = false) String repo) throws UploadException {
        return service.uploadAndSaveByTitle(title, repo, x -> true, true);
    }

    @GetMapping("/uploadimagesbytitle/{title}")
    public final List<T> uploadImagesByTitle(@PathVariable String title,
            @RequestParam(name = "repo", required = false) String repo) throws UploadException {
        return service.uploadAndSaveByTitle(title, repo, Media::isImage, true);
    }

    @GetMapping("/uploadvideosbytitle/{title}")
    public final List<T> uploadVideosByTitle(@PathVariable String title,
            @RequestParam(name = "repo", required = false) String repo) throws UploadException {
        return service.uploadAndSaveByTitle(title, repo, Media::isVideo, true);
    }

    @GetMapping("/uploaddocumentsbytitle/{title}")
    public final List<T> uploadDocumentsByTitle(@PathVariable String title,
            @RequestParam(name = "repo", required = false) String repo) throws UploadException {
        return service.uploadAndSaveByTitle(title, repo, Media::isDocument, true);
    }

    @GetMapping("/uploadmedia/**")
    public final T uploadMedia(HttpServletRequest request) throws UploadException, TooManyResultsException {
        return service.uploadAndSaveById(extractId(request, "uploadmedia"), true);
    }

    @GetMapping("/refreshmedia/**")
    public final T refreshMedia(HttpServletRequest request) throws ImageNotFoundException, IOException {
        return service.refreshAndSaveById(extractId(request, "refreshmedia"));
    }

    @GetMapping("/refreshbydate/{date}")
    public final List<T> refreshByDate(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(name = "repo", required = false) String repo) throws ImageNotFoundException, IOException {
        return service.refreshAndSaveByDate(date, repo, x -> true);
    }

    @GetMapping("/refreshimagesbydate/{date}")
    public final List<T> refreshImagesByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(name = "repo", required = false) String repo) throws ImageNotFoundException, IOException {
        return service.refreshAndSaveByDate(date, repo, Media::isImage);
    }

    @GetMapping("/refreshvideosbydate/{date}")
    public final List<T> refreshVideosByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(name = "repo", required = false) String repo) throws ImageNotFoundException, IOException {
        return service.refreshAndSaveByDate(date, repo, Media::isVideo);
    }

    @GetMapping("/refreshdocumentsbydate/{date}")
    public final List<T> refreshDocumentsByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(name = "repo", required = false) String repo) throws ImageNotFoundException, IOException {
        return service.refreshAndSaveByDate(date, repo, Media::isDocument);
    }

    @GetMapping("/refreshbymonth/{month}")
    public final List<T> refreshByMonth(@PathVariable YearMonth month,
            @RequestParam(name = "repo", required = false) String repo) throws ImageNotFoundException, IOException {
        return service.refreshAndSaveByMonth(month, repo, x -> true);
    }

    @GetMapping("/refreshimagesbymonth/{month}")
    public final List<T> refreshImagesByMonth(@PathVariable YearMonth month,
            @RequestParam(name = "repo", required = false) String repo) throws ImageNotFoundException, IOException {
        return service.refreshAndSaveByMonth(month, repo, Media::isImage);
    }

    @GetMapping("/refreshvideosbymonth/{month}")
    public final List<T> refreshVideosByMonth(@PathVariable YearMonth month,
            @RequestParam(name = "repo", required = false) String repo) throws ImageNotFoundException, IOException {
        return service.refreshAndSaveByMonth(month, repo, Media::isVideo);
    }

    @GetMapping("/refreshdocumentsbymonth/{month}")
    public final List<T> refreshDocumentsByMonth(@PathVariable YearMonth month,
            @RequestParam(name = "repo", required = false) String repo) throws ImageNotFoundException, IOException {
        return service.refreshAndSaveByMonth(month, repo, Media::isDocument);
    }

    @GetMapping("/refreshbyyear/{year}")
    public final List<T> refreshByYear(@PathVariable Year year,
            @RequestParam(name = "repo", required = false) String repo) throws ImageNotFoundException, IOException {
        return service.refreshAndSaveByYear(year, repo, x -> true);
    }

    @GetMapping("/refreshimagesbyyear/{year}")
    public final List<T> refreshImagesByYear(@PathVariable Year year,
            @RequestParam(name = "repo", required = false) String repo) throws ImageNotFoundException, IOException {
        return service.refreshAndSaveByYear(year, repo, Media::isImage);
    }

    @GetMapping("/refreshvideosbyyear/{year}")
    public final List<T> refreshVideosByYear(@PathVariable Year year,
            @RequestParam(name = "repo", required = false) String repo) throws ImageNotFoundException, IOException {
        return service.refreshAndSaveByYear(year, repo, Media::isVideo);
    }

    @GetMapping("/refreshdocumentsbyyear/{year}")
    public final List<T> refreshDocumentsByYear(@PathVariable Year year,
            @RequestParam(name = "repo", required = false) String repo) throws ImageNotFoundException, IOException {
        return service.refreshAndSaveByYear(year, repo, Media::isDocument);
    }

    @GetMapping("/refreshmissing")
    public final void refreshMissingMedia() {
        refreshMissingMedia(m -> true);
    }

    @GetMapping("/refreshmissing/images")
    public final void refreshMissingImages() {
        refreshMissingMedia(T::isImage);
    }

    @GetMapping("/refreshmissing/videos")
    public final void refreshMissingVideos() {
        refreshMissingMedia(T::isVideo);
    }

    @GetMapping("/refreshmissing/documents")
    public final void refreshMissingDocuments() {
        refreshMissingMedia(T::isDocument);
    }

    @GetMapping("/syncuploaded")
    public final void syncUploadedMedia() {
        syncUploadedMedia(m -> true);
    }

    @GetMapping("/syncuploaded/images")
    public final void syncUploadedImages() {
        syncUploadedMedia(T::isImage);
    }

    @GetMapping("/syncuploaded/videos")
    public final void syncUploadedVideos() {
        syncUploadedMedia(T::isVideo);
    }

    @GetMapping("/syncuploaded/documents")
    public final void syncUploadedDocuments() {
        syncUploadedMedia(T::isDocument);
    }

    private final void refreshMissingMedia(Predicate<T> mustRefresh) {
        batchProcess(mustRefresh, service::listMissingMedia, media -> {
            try {
                service.refreshAndSave(media);
            } catch (IOException e) {
                LOGGER.error("Failed to refresh {}: {}", media, e.getMessage());
            }
        });
    }

    private final void syncUploadedMedia(Predicate<T> mustSync) {
        batchProcess(mustSync, service::listUploadedMedia, media -> {
            try {
                service.syncAndSave(media);
            } catch (IOException e) {
                LOGGER.error("Failed to sync {}: {}", media, e.getMessage());
            }
        });
    }

    private final void batchProcess(Predicate<T> mustProcess, Function<Pageable, Page<T>> fetcher,
            Consumer<T> processor) {
        Pageable page = PageRequest.of(0, SIZE, DESC, SORT);
        while (true) {
            Page<T> medias = fetcher.apply(page);
            if (medias.isEmpty()) {
                return;
            }
            for (T media : medias) {
                if (mustProcess.test(media)) {
                    try {
                        processor.accept(media);
                    } catch (RuntimeException e) {
                        LOGGER.error("Failed to process {}: {} => {}", media, e.getClass(), e.getMessage());
                    }
                }
            }
            page = page.next();
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
        try {
            return URLDecoder.decode(requestURI.substring(
                    requestURI.indexOf('/', ("/" + service.getId() + "/rest/" + name).length()) + 1), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }
}
