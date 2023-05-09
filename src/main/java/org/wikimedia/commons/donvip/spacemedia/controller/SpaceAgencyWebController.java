package org.wikimedia.commons.donvip.spacemedia.controller;

import static org.springframework.data.domain.Sort.Direction.DESC;
import static org.wikimedia.commons.donvip.spacemedia.controller.PagingSortingDefaults.SIZE;
import static org.wikimedia.commons.donvip.spacemedia.controller.PagingSortingDefaults.SORT;

import java.time.temporal.Temporal;
import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.wikimedia.commons.donvip.spacemedia.data.domain.Media;
import org.wikimedia.commons.donvip.spacemedia.data.domain.Search;
import org.wikimedia.commons.donvip.spacemedia.service.SearchService;
import org.wikimedia.commons.donvip.spacemedia.service.agencies.AbstractAgencyService;
import org.wikimedia.commons.donvip.spacemedia.service.agencies.Agency;
import org.wikimedia.commons.donvip.spacemedia.utils.Pagination;

/**
 * Superclass of space agencies web controllers. Sub-classes are created dynamically.
 *
 * @param <T> the media type the repository manages
 * @param <ID> the type of the id of the entity the repository manages
 * @param <D> the media date type
 */
public class SpaceAgencyWebController<T extends Media<ID, D>, ID, D extends Temporal> {

    @Autowired
    private List<AbstractAgencyService<? extends Media<?, ?>, ?, ?>> agencies;

    @Autowired
    private SearchService searchService;

    protected final Agency<T, ID, D> service;

    public SpaceAgencyWebController(AbstractAgencyService<T, ID, D> service) {
        this.service = Objects.requireNonNull(service);
    }

    @GetMapping
    public String index(Model model) {
        return template(model, "agency_index");
    }

    @GetMapping("/all")
    public String all(Model model, @PageableDefault(size = SIZE, sort = SORT, direction = DESC) Pageable page) {
        return media(model, "all", service.listAllMedia(page));
    }

    @GetMapping("/missing")
    public String missing(Model model, @PageableDefault(size = SIZE, sort = SORT, direction = DESC) Pageable page) {
        return media(model, "missing", service.listMissingMedia(page));
    }

    @GetMapping("/missing/images")
    public String missingImages(Model model, @PageableDefault(size = SIZE, sort = SORT, direction = DESC) Pageable page) {
        return media(model, "missing/images", service.listMissingImages(page));
    }

    @GetMapping("/missing/videos")
    public String missingVideos(Model model, @PageableDefault(size = SIZE, sort = SORT, direction = DESC) Pageable page) {
        return media(model, "missing/videos", service.listMissingVideos(page));
    }

    @GetMapping("/hashes")
    public String hashes(Model model, @PageableDefault(size = SIZE, sort = SORT, direction = DESC) Pageable page) {
        return media(model, "hashes", service.listHashedMedia(page));
    }

    @GetMapping("/uploaded")
    public String uploaded(Model model, @PageableDefault(size = SIZE, sort = SORT, direction = DESC) Pageable page) {
        return media(model, "uploaded", service.listUploadedMedia(page));
    }

    @GetMapping("/ignored")
    public String ignored(Model model, @PageableDefault(size = SIZE, sort = SORT, direction = DESC) Pageable page) {
        return media(model, "ignored", service.listIgnoredMedia(page));
    }

    @GetMapping("/stats")
    public String stats(Model model) {
        model.addAttribute("stats", service.getStatistics(true));
        return template(model, "agency_stats");
    }

    @GetMapping("/problems")
    public String problems(Model model, @PageableDefault(size = SIZE) Pageable page) {
        return pageIndex(model, "problems", service.getProblems(page), "problems", newSearch());
    }

    @GetMapping("/search")
    public final String search(Model model, @ModelAttribute Search search,
            @PageableDefault(size = SIZE) Pageable page) {
        return media(model, "search", service.searchMedia(search.getQ(), page), search);
    }

    private String template(Model model, String template) {
        return template(model, template, newSearch());
    }

    private String template(Model model, String template, Search search) {
        model.addAttribute("search", search);
        model.addAttribute("agencies", agencies.stream().sorted().toList());
        model.addAttribute("agency", service);
        return template;
    }

    private String media(Model model, String tab, Page<T> medias) {
        return media(model, tab, medias, newSearch());
    }

    private String media(Model model, String tab, Page<T> medias, Search search) {
        return pageIndex(model, tab, medias, "medias", search);
    }

    private String index(Model model, String tab, Iterable<?> items, String itemsName, Search search) {
        model.addAttribute(itemsName, items);
        model.addAttribute("tab", tab);
        return template(model, "agency_index", search);
    }

    private String pageIndex(Model model, String tab, Page<?> items, String itemsName, Search search) {
        Pagination.setPageNumbers(model, items);
        return index(model, tab, items, itemsName, search);
    }

    private Search newSearch() {
        return searchService.isSearchEnabled() ? new Search() : null;
    }
}
