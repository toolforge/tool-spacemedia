package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import static java.util.Optional.empty;
import static java.util.stream.Collectors.toSet;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.getWithJsoup;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.CompositeMediaId;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.noaa.library.NoaaLibraryMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.noaa.library.NoaaLibraryMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.utils.Utils;

@Service
public class NoaaLibraryService extends AbstractOrgHtmlGalleryService<NoaaLibraryMedia> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NoaaLibraryService.class);

    private static final String BASE_URL = "https://www.noaa.gov";
    private static final String COLL_URL = BASE_URL + "/digital-library/collections";

    public NoaaLibraryService(NoaaLibraryMediaRepository repository) {
        super(repository, "noaa.library", Set.of("noaa.library"));
    }

    @Override
    public String getName() {
        return "NOAA (Digital Library)";
    }

    @Override
    protected List<String> fetchGalleryUrls(String repoId) {
        List<String> result = new ArrayList<>();
        result.addAll(fetchGalleryUrls(repoId, "2268"));
        return result;
    }

    private List<String> fetchGalleryUrls(String repoId, String collectionId) {
        List<String> result = new ArrayList<>();
        result.add(COLL_URL + '/' + collectionId);
        try {
            result.addAll(getWithJsoup(COLL_URL + collectionId, 10_000, 3)
                    .getElementsByClass("views-view-responsive-grid__item-inner").stream()
                    .map(x -> COLL_URL + x.getElementsByTag("a").first().attr("href")).toList());
        } catch (IOException e) {
            LOGGER.error("Failed to retrieve sub collections", e);
        }
        return result;
    }

    @Override
    protected String getGalleryPageUrl(String galleryUrl, int page) {
        return galleryUrl + "?page=" + (page - 1);
    }

    @Override
    protected Elements getGalleryItems(String repoId, String url, Element html) {
        return html.getElementsByClass("ngdl-photo");
    }

    @Override
    protected String extractIdFromGalleryItem(String url, Element result) {
        return url.split("/")[5].split("\\?")[0] + '/' + url.split("=")[1] + '/' + result.attr("id");
    }

    @Override
    protected String getSourceUrl(CompositeMediaId id) {
        String[] tab = id.getMediaId().split("/");
        return COLL_URL + '/' + tab[0] + "/item?page=" + tab[1];
    }

    @Override
    protected String getAuthor(NoaaLibraryMedia media, FileMetadata metadata) {
        return Optional.ofNullable(media.getCredits()).orElse("NOAA");
    }

    @Override
    protected List<NoaaLibraryMedia> fillMediaWithHtml(String url, Document html, Element galleryItem,
            NoaaLibraryMedia media) throws IOException {
        try {
            media.setTitle(items(html, "description").text());
            Utils.extractDate(items(html, "display-date").text().replace(" Circa", ""), DATE_FORMATTERS)
                    .ifPresent(media::setPublication);
            if (media.getPublicationYear() == null) {
                LOGGER.warn("Failed to extract publication date: {}", url);
            }
            media.setKeywords(items(html, "subject").stream().map(Element::text).collect(toSet()));
            addMetadata(media, BASE_URL + html.getElementsByClass("image-download-link").first().attr("href"), null);
            return List.of(media);
        } catch (RuntimeException e) {
            LOGGER.error("Failed to parse HTML for {} => {}", media, html.html());
            throw e;
        }
    }

    private static Elements items(Document html, String klass) {
        return html.getElementsByClass("c-field--name-field-media-ngdl-" + klass).first()
                .getElementsByClass("c-field__item");
    }

    @Override
    protected boolean checkBlocklist() {
        return false;
    }

    @Override
    protected boolean isSatellitePicture(NoaaLibraryMedia media, FileMetadata metadata) {
        return media.getKeywords().contains("Imagery");
    }

    @Override
    public Set<String> findLicenceTemplates(NoaaLibraryMedia media, FileMetadata metadata) {
        Set<String> result = super.findLicenceTemplates(media, metadata);
        result.add("PD-USGov-NOAA");
        return result;
    }

    @Override
    protected NoaaLibraryMedia refresh(NoaaLibraryMedia media) throws IOException {
        return media.copyDataFrom(fetchMedia(media.getId(), empty()));
    }

    @Override
    protected Class<NoaaLibraryMedia> getMediaClass() {
        return NoaaLibraryMedia.class;
    }

    @Override
    protected Set<String> getTwitterAccounts(NoaaLibraryMedia uploadedMedia) {
        return Set.of("@NOAASatellites", "@NOAALibrary");
    }
}
