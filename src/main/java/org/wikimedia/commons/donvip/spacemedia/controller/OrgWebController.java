package org.wikimedia.commons.donvip.spacemedia.controller;

import static org.springframework.data.domain.Sort.Direction.DESC;
import static org.wikimedia.commons.donvip.spacemedia.controller.PagingSortingDefaults.SIZE;
import static org.wikimedia.commons.donvip.spacemedia.controller.PagingSortingDefaults.SORT;

import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.wikimedia.commons.donvip.spacemedia.data.domain.Search;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.Media;
import org.wikimedia.commons.donvip.spacemedia.service.SearchService;
import org.wikimedia.commons.donvip.spacemedia.service.orgs.AbstractOrgService;
import org.wikimedia.commons.donvip.spacemedia.service.orgs.Org;
import org.wikimedia.commons.donvip.spacemedia.utils.Pagination;

/**
 * Superclass of orgs web controllers. Sub-classes are created dynamically.
 *
 * @param <T>  the media type the repository manages
 */
public class OrgWebController<T extends Media> {

    @Autowired
    private List<AbstractOrgService<? extends Media>> orgs;

    @Autowired
    private SearchService searchService;

    protected final Org<T> service;

    public OrgWebController(AbstractOrgService<T> service) {
        this.service = Objects.requireNonNull(service);
    }

    @GetMapping
    public String index(Model model, @RequestParam(name = "repo", required = false) String repo) {
        return template(model, "org_index", repo);
    }

    @GetMapping("/all")
    public String all(Model model, @RequestParam(name = "repo", required = false) String repo,
            @PageableDefault(size = SIZE, sort = SORT, direction = DESC) Pageable page) {
        return media(model, "all", repo, service.listAllMedia(repo, page));
    }

    @GetMapping("/missing")
    public String missing(Model model, @RequestParam(name = "repo", required = false) String repo,
            @PageableDefault(size = SIZE, sort = SORT, direction = DESC) Pageable page) {
        return media(model, "missing", repo, service.listMissingMedia(repo, page));
    }

    @GetMapping("/missing/images")
    public String missingImages(Model model, @RequestParam(name = "repo", required = false) String repo,
            @PageableDefault(size = SIZE, sort = SORT, direction = DESC) Pageable page) {
        return media(model, "missing/images", repo, service.listMissingImages(repo, page));
    }

    @GetMapping("/missing/videos")
    public String missingVideos(Model model, @RequestParam(name = "repo", required = false) String repo,
            @PageableDefault(size = SIZE, sort = SORT, direction = DESC) Pageable page) {
        return media(model, "missing/videos", repo, service.listMissingVideos(repo, page));
    }

    @GetMapping("/missing/documents")
    public String missingDocuments(Model model, @RequestParam(name = "repo", required = false) String repo,
            @PageableDefault(size = SIZE, sort = SORT, direction = DESC) Pageable page) {
        return media(model, "missing/documents", repo, service.listMissingDocuments(repo, page));
    }

    @GetMapping("/hashes")
    public String hashes(Model model, @RequestParam(name = "repo", required = false) String repo,
            @PageableDefault(size = SIZE, sort = SORT, direction = DESC) Pageable page) {
        return media(model, "hashes", repo, service.listHashedMedia(repo, page));
    }

    @GetMapping("/uploaded")
    public String uploaded(Model model, @RequestParam(name = "repo", required = false) String repo,
            @PageableDefault(size = SIZE, sort = SORT, direction = DESC) Pageable page) {
        return media(model, "uploaded", repo, service.listUploadedMedia(repo, page));
    }

    @GetMapping("/ignored")
    public String ignored(Model model, @RequestParam(name = "repo", required = false) String repo,
            @PageableDefault(size = SIZE, sort = SORT, direction = DESC) Pageable page) {
        return media(model, "ignored", repo, service.listIgnoredMedia(repo, page));
    }

    @GetMapping("/stats")
    public String stats(Model model, @RequestParam(name = "repo", required = false) String repo) {
        model.addAttribute("stats", service.getStatistics(true));
        return template(model, "org_stats", repo);
    }

    @GetMapping("/search")
    public final String search(Model model, @ModelAttribute Search search,
            @RequestParam(name = "repo", required = false) String repo,
            @PageableDefault(size = SIZE) Pageable page) {
        return media(model, "search", repo, service.searchMedia(search.getQ(), page), search);
    }

    private String template(Model model, String template, String repo) {
        return template(model, template, repo, newSearch());
    }

    private String template(Model model, String template, String repo, Search search) {
        if (repo != null) {
            model.addAttribute("repo", repo);
        }
        model.addAttribute("search", search);
        model.addAttribute("orgs", orgs.stream().sorted().toList());
        model.addAttribute("org", service);
        return template;
    }

    private String media(Model model, String tab, String repo, Page<T> medias) {
        return media(model, tab, repo, medias, newSearch());
    }

    private String media(Model model, String tab, String repo, Page<T> medias, Search search) {
        return pageIndex(model, tab, repo, medias, "medias", search);
    }

    private String index(Model model, String tab, String repo, Iterable<?> items, String itemsName, Search search) {
        model.addAttribute(itemsName, items);
        model.addAttribute("tab", tab);
        return template(model, "org_index", repo, search);
    }

    private String pageIndex(Model model, String tab, String repo, Page<?> items, String itemsName, Search search) {
        Pagination.setPageNumbers(model, items);
        return index(model, tab, repo, items, itemsName, search);
    }

    private Search newSearch() {
        return searchService.isSearchEnabled() ? new Search() : null;
    }
}
