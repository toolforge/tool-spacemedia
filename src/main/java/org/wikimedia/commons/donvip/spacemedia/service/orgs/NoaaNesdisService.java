package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import static java.util.Optional.empty;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.getWithJsoup;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.uriExists;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
import org.wikimedia.commons.donvip.spacemedia.data.domain.noaa.nesdis.NoaaNesdisMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.noaa.nesdis.NoaaNesdisMediaRepository;

@Service
public class NoaaNesdisService extends AbstractOrgHtmlGalleryService<NoaaNesdisMedia> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NoaaNesdisService.class);

    private static final String BASE_URL = "https://www.nesdis.noaa.gov/";

    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH);

    public NoaaNesdisService(NoaaNesdisMediaRepository repository) {
        super(repository, "noaa.nesdis", Set.of("nesdis"));
    }

    @Override
    public String getName() {
        return "NOAAA (NESDIS)";
    }

    @Override
    protected List<String> fetchGalleryUrls(String repoId) {
        List<String> result = new ArrayList<>();
        result.addAll(fetchGalleryUrls(repoId, "real-time-imagery/imagery-collections"));
        result.addAll(fetchGalleryUrls(repoId, "imagery/media-stills-animation"));
        return result;
    }

    private List<String> fetchGalleryUrls(String repoId, String path) {
        String pageUrl = BASE_URL + path;
        try {
            return getGalleryItems(repoId, getWithJsoup(pageUrl, 30_000, 3)).stream()
                    .map(item -> item.getElementsByTag("a").first().attr("href")).toList();
        } catch (IOException | RuntimeException e) {
            LOGGER.error("Error while fetching {}", pageUrl, e);
            return List.of();
        }
    }

    @Override
    protected String getGalleryPageUrl(String galleryUrl, int page) {
        return galleryUrl + "?page=" + (page - 1);
    }

    @Override
    protected Elements getGalleryItems(String repoId, Element html) {
        Element itemList = html.getElementsByClass("item-list").first();
        return itemList == null ? html.getElementsByClass("cards--media") : itemList.getElementsByTag("li");
    }

    @Override
    protected String extractIdFromGalleryItem(Element result) {
        String href = result.getElementsByTag("a").first().attr("href");
        if (href.endsWith("/")) {
            href = href.substring(0, href.length() - 1);
        }
        return href.substring(href.lastIndexOf('/') + 1);
    }

    @Override
    protected String getSourceUrl(CompositeMediaId id) {
        String url = BASE_URL + "news/" + id.getMediaId();
        if (uriExists(url)) {
            return url;
        }
        return null;
    }

    @Override
    protected String getAuthor(NoaaNesdisMedia media, FileMetadata metadata) {
        return Optional.ofNullable(media.getCredits()).orElse("NOAA");
    }

    @Override
    protected void fillMediaWithHtml(String url, Document html, NoaaNesdisMedia media) throws IOException {
        try {
            media.setTitle(html.getElementsByTag("h1").first().text());
            media.setPublicationDate(
                    LocalDate.parse(html.getElementsByClass("news-date").first().text(), dateFormatter));
            html.getElementsByClass("call-to-action blue")
                    .stream().filter(x -> x.text().startsWith("Download"))
                    .forEach(x -> addMetadata(media, BASE_URL + x.attr("href"), null));
            media.setDescription(html.getElementsByClass("field__item").first().text());
        } catch (RuntimeException e) {
            LOGGER.error("Failed to parse HTML for {} => {}", media, html.html());
            throw e;
        }
    }

    @Override
    public Set<String> findLicenceTemplates(NoaaNesdisMedia media, FileMetadata metadata) {
        Set<String> result = super.findLicenceTemplates(media, metadata);
        result.add("PD-USGov-NOAA");
        return result;
    }

    @Override
    protected NoaaNesdisMedia refresh(NoaaNesdisMedia media) throws IOException {
        return media.copyDataFrom(fetchMedia(media.getId(), empty()));
    }

    @Override
    protected Class<NoaaNesdisMedia> getMediaClass() {
        return NoaaNesdisMedia.class;
    }

    @Override
    protected Set<String> getTwitterAccounts(NoaaNesdisMedia uploadedMedia) {
        return Set.of("@NOAASatellites");
    }
}
