package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toSet;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.getWithJsoup;

import java.io.IOException;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
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
import org.wikimedia.commons.donvip.spacemedia.service.wikimedia.GlitchTip;
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
        String url = COLL_URL + '/' + collectionId;
        result.add(url);
        try {
            result.addAll(getWithJsoup(url, 10_000, 3).getElementsByClass("c-view__wrap").first()
                    .getElementsByClass("views-view-responsive-grid__item-inner").stream()
                    .map(x -> BASE_URL + x.getElementsByTag("a").first().attr("href")).toList());
        } catch (IOException e) {
            LOGGER.error("Failed to retrieve sub collections", e);
            GlitchTip.capture(e);
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
        String href = result.getElementsByTag("a").first().attr("href");
        return href.split("/")[3].split("\\?")[0] + '/' + href.split("=")[1] + '/' + result.attr("id");
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
            items(html, "description").map(Elements::text).ifPresent(media::setTitle);
            items(html, "display-date").ifPresent(date -> {
                String text = StringUtils.strip(date.text().replace(" Circa", "").replace(" ca.", "")
                        .replace(" near ", " ").replace("A.M", "AM").replace("P.M", "PM").replace("? ", " ")
                        .replace(" Z", "Z").split("\\(")[0].trim().split("\\-")[0], ",.");
                Utils.extractDate(text, DATE_FORMATTERS).ifPresentOrElse(media::setPublication,
                        () -> LOGGER.warn("Failed to extract date: {} => {}", url, text));
            });
            if (media.getPublicationYear() == null) {
                LOGGER.warn("No publication date found for {}. Setting arbitrary one", url);
                media.setPublicationYear(Year.now());
            }
            items(html, "subject").map(x -> x.stream().map(Element::text).collect(toSet()))
                    .ifPresent(media::setKeywords);
            addMetadata(media, BASE_URL + html.getElementsByClass("image-download-link").first().attr("href"), null);
            return List.of(media);
        } catch (RuntimeException e) {
            LOGGER.error("Failed to parse HTML for {} => {}", media, html.html());
            GlitchTip.capture(e);
            throw e;
        }
    }

    private static Optional<Elements> items(Document html, String klass) {
        return ofNullable(html.getElementsByClass("c-field--name-field-media-ngdl-" + klass).first())
                .map(x -> x.getElementsByClass("c-field__item"));
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
