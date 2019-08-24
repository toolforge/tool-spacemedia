package org.wikimedia.commons.donvip.spacemedia.controller;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.client.ClientProtocolException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.wikimedia.commons.donvip.spacemedia.data.domain.Media;
import org.wikimedia.commons.donvip.spacemedia.data.domain.Problem;
import org.wikimedia.commons.donvip.spacemedia.data.domain.Statistics;
import org.wikimedia.commons.donvip.spacemedia.service.agencies.AbstractSpaceAgencyService;
import org.xml.sax.SAXException;

/**
 * Superclass of space agencies REST controllers. Sub-classes are created dynamically.
 *
 * @param <T> the media type the repository manages
 * @param <ID> the type of the id of the entity the repository manages
 */
public abstract class SpaceAgencyController<T extends Media, ID> {

    protected final AbstractSpaceAgencyService<T, ID> service;

    public SpaceAgencyController(AbstractSpaceAgencyService<T, ID> service) {
        this.service = Objects.requireNonNull(service);
    }

    @GetMapping("/all")
    public final Page<T> listAll(@PageableDefault(size = 100, sort = "id") Pageable page) throws IOException {
        return service.listAllMedia(page);
    }

    @GetMapping("/update")
    public final void update() throws IOException {
        service.updateMedia();
    }

    @GetMapping("/missing")
    public final Page<T> listMissing(@PageableDefault(size = 100, sort = "id") Pageable page) throws IOException {
        return service.listMissingMedia(page);
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

    @GetMapping("/wiki/{sha1}")
    public final String wikiPreview(@PathVariable String sha1)
            throws ClientProtocolException, IOException, ParserConfigurationException, SAXException {
        return service.getWikiHtmlPreview(sha1);
    }

    @GetMapping("/wikicode/{sha1}")
    public final String wikiCode(@PathVariable String sha1) {
        return service.getWikiCode(sha1);
    }

    @GetMapping("/upload/{sha1}")
    public final T upload(@PathVariable String sha1) throws IOException {
        return service.upload(sha1);
    }
}
