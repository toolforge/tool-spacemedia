package org.wikimedia.commons.donvip.spacemedia.controller;

import static org.springframework.data.domain.Sort.Direction.DESC;
import static org.wikimedia.commons.donvip.spacemedia.controller.PagingSortingDefaults.SIZE;
import static org.wikimedia.commons.donvip.spacemedia.controller.PagingSortingDefaults.SORT;

import java.io.IOException;
import java.time.temporal.Temporal;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.wikimedia.commons.donvip.spacemedia.data.domain.Media;
import org.wikimedia.commons.donvip.spacemedia.service.agencies.AbstractSpaceAgencyService;
import org.wikimedia.commons.donvip.spacemedia.service.agencies.SpaceAgency;

/**
 * Superclass of space agencies web controllers. Sub-classes are created dynamically.
 *
 * @param <T> the media type the repository manages
 * @param <ID> the type of the id of the entity the repository manages
 * @param <D> the media date type
 */
public class SpaceAgencyWebController<T extends Media<ID, D>, ID, D extends Temporal> {

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
        return media(model, "agency_media_all", service.listAllMedia(page));
    }

    @GetMapping("/missing")
    public String missing(Model model, @PageableDefault(size = SIZE, sort = SORT, direction = DESC) Pageable page) {
        return media(model, "agency_media_missing", service.listMissingMedia(page));
    }

    @GetMapping("/uploaded")
    public String uploaded(Model model, @PageableDefault(size = SIZE, sort = SORT, direction = DESC) Pageable page) {
        return media(model, "agency_media_uploaded", service.listUploadedMedia(page));
    }

    @GetMapping("/ignored")
    public String ignored(Model model, @PageableDefault(size = SIZE, sort = SORT, direction = DESC) Pageable page) {
        return media(model, "agency_media_ignored", service.listIgnoredMedia(page));
    }

    @GetMapping("/stats")
    public String stats(Model model) throws IOException {
        model.addAttribute("stats", service.getStatistics());
        return template(model, "agency_stats");
    }

    @GetMapping("/problems")
    public String problems(Model model) throws IOException {
        return template(model, "agency_problems");
    }

    private String template(Model model, String template) {
        model.addAttribute("agency", service);
        return template;
    }

    private String media(Model model, String template, Page<T> medias) {
        model.addAttribute("medias", medias);
        int totalPages = medias.getTotalPages();
        if (totalPages > 0) {
            model.addAttribute("pageNumbers",
                    IntStream.rangeClosed(1, totalPages).boxed().collect(Collectors.toList()));
        }
        return template(model, template);
    }
}
