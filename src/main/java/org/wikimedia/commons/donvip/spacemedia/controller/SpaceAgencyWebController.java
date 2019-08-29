package org.wikimedia.commons.donvip.spacemedia.controller;

import java.io.IOException;
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
 * Superclass of space agencies web controllers. Sub-classes are created
 * dynamically.
 *
 * @param <T> the media type the repository manages
 * @param <ID> the type of the id of the entity the repository manages
 */
public class SpaceAgencyWebController<T extends Media, ID> {

    protected final SpaceAgency<T, ID> service;

    public SpaceAgencyWebController(AbstractSpaceAgencyService<T, ID> service) {
        this.service = Objects.requireNonNull(service);
    }

    @GetMapping
    public String index(Model model) {
        return template(model, "agency_index");
    }

    @GetMapping("/all")
    public String listAll(Model model, @PageableDefault(size = 50, sort = "id") Pageable page) {
        return media(model, "agency_media_all", service.listAllMedia(page));
    }

    @GetMapping("/missing")
    public String listMissing(Model model, @PageableDefault(size = 50, sort = "id") Pageable page) {
        return media(model, "agency_media_missing", service.listMissingMedia(page));
    }

    @GetMapping("/uploaded")
    public String listUploaded(Model model, @PageableDefault(size = 50, sort = "id") Pageable page) {
        return media(model, "agency_media_uploaded", service.listUploadedMedia(page));
    }

    @GetMapping("/ignored")
    public String listIgnored(Model model, @PageableDefault(size = 50, sort = "id") Pageable page) {
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
