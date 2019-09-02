package org.wikimedia.commons.donvip.spacemedia.controller;

import static org.springframework.data.domain.Sort.Direction.DESC;
import static org.wikimedia.commons.donvip.spacemedia.controller.PagingSortingDefaults.SIZE;
import static org.wikimedia.commons.donvip.spacemedia.controller.PagingSortingDefaults.SORT;

import java.io.IOException;
import java.time.temporal.Temporal;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.wikimedia.commons.donvip.spacemedia.data.domain.Media;
import org.wikimedia.commons.donvip.spacemedia.data.domain.Search;
import org.wikimedia.commons.donvip.spacemedia.service.agencies.AbstractSpaceAgencyService;
import org.wikimedia.commons.donvip.spacemedia.service.agencies.SpaceAgency;
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
    private List<AbstractSpaceAgencyService<? extends Media<?, ?>, ?, ?>> agencies;

    protected final SpaceAgency<T, ID, D> service;

    public SpaceAgencyWebController(AbstractSpaceAgencyService<T, ID, D> service) {
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

    @GetMapping("/uploaded")
    public String uploaded(Model model, @PageableDefault(size = SIZE, sort = SORT, direction = DESC) Pageable page) {
        return media(model, "uploaded", service.listUploadedMedia(page));
    }

    @GetMapping("/ignored")
    public String ignored(Model model, @PageableDefault(size = SIZE, sort = SORT, direction = DESC) Pageable page) {
        return media(model, "ignored", service.listIgnoredMedia(page));
    }

    @GetMapping("/stats")
    public String stats(Model model) throws IOException {
        model.addAttribute("stats", service.getStatistics());
        return template(model, "agency_stats");
    }

    @GetMapping("/problems")
    public String problems(Model model, @PageableDefault(size = SIZE) Pageable page) throws IOException {
        return index(model, "problems", service.getProblems(page), "problems", new Search());
    }

    @GetMapping("/search")
    public final String search(Model model, @ModelAttribute Search search,
            @PageableDefault(size = SIZE) Pageable page) {
        return media(model, "search", service.searchMedia(search.getQ(), page), search);
    }

    private String template(Model model, String template) {
        return template(model, template, new Search());
    }

    private String template(Model model, String template, Search search) {
        model.addAttribute("search", search);
        model.addAttribute("agencies", agencies.stream().sorted().collect(Collectors.toList()));
        model.addAttribute("agency", service);
        return template;
    }

    private String media(Model model, String tab, Page<T> medias) {
        return media(model, tab, medias, new Search());
    }

    private String media(Model model, String tab, Page<T> medias, Search search) {
        return index(model, tab, medias, "medias", search);
    }

    private String index(Model model, String tab, Page<?> items, String itemsName, Search search) {
        model.addAttribute(itemsName, items);
        model.addAttribute("tab", tab);
        Pagination.setPageNumbers(model, items);
        return template(model, "agency_index", search);
    }
}