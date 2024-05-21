package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import static java.util.Optional.empty;

import java.io.IOException;
import java.time.LocalDate;
import java.time.Year;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.CompositeMediaId;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.chandra.NasaChandraMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.chandra.NasaChandraMediaRepository;

@Service
public class NasaChandraService extends AbstractOrgHtmlGalleryService<NasaChandraMedia> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NasaChandraService.class);

    private static final String BASE_URL = "https://chandra.si.edu/photo";

    private static final DateTimeFormatter DATE_FORMATTER_1 = DateTimeFormatter.ofPattern("dd MMM yy", Locale.ENGLISH);

    private static final DateTimeFormatter DATE_FORMATTER_2 = DateTimeFormatter.ofPattern("MMMM dd, yyyy",
            Locale.ENGLISH);

    private static final DateTimeFormatter DATE_FORMATTER_3 = DateTimeFormatter.ofPattern("MMMM d, yyyy",
            Locale.ENGLISH);

    public NasaChandraService(NasaChandraMediaRepository repository) {
        super(repository, "nasa.chandra", Set.of("chandra"));
    }

    @Override
    public String getName() {
        return "Chandra X-ray Observatory";
    }

    @Override
    protected List<String> fetchGalleryUrls(String repoId) {
        int from = 1999;
        int to = Year.now().getValue();
        return IntStream.rangeClosed(from, to).map(x -> ((to - x + from - 2000) % 100 + 100) % 100)
                .mapToObj(x -> String.format("%s/chronological%02d.html", BASE_URL, x)).toList();
    }

    @Override
    protected String getGalleryPageUrl(String galleryUrl, int page) {
        return galleryUrl;
    }

    @Override
    protected Elements getGalleryItems(String repoId, Element html) {
        return html.getElementsByClass("page_box2");
    }

    @Override
    protected String extractIdFromGalleryItem(Element result) {
        return result.getElementsByTag("a").first().attr("href").replace("/photo/", "");
    }

    @Override
    protected Optional<ZonedDateTime> extractDateFromGalleryItem(Element result) {
        return Optional.of(LocalDate.parse(result.getElementsByClass("page_gray").first().text(), DATE_FORMATTER_1)
                .atStartOfDay(ZoneOffset.UTC));
    }

    @Override
    protected String getSourceUrl(CompositeMediaId id) {
        return BASE_URL + '/' + id.getMediaId();
    }

    @Override
    protected boolean loop(String repoId, Elements results) {
        return false;
    }

    @Override
    protected String getAuthor(NasaChandraMedia media, FileMetadata metadata) {
        return Optional.ofNullable(media.getCredits()).orElse("NASA/SAO");
    }

    @Override
    protected void fillMediaWithHtml(String url, Document html, NasaChandraMedia media) throws IOException {
        try {
            media.setTitle(Optional.ofNullable(html.getElementById("image_title"))
                    .or(() -> Optional.ofNullable(html.getElementsByClass("page_title").first()))
                    .orElseGet(() -> html.getElementsByClass("press_title").first()).text());
            Element textWrap = html.getElementById("text_wrap");
            Element contentText = html.getElementById("content_text");
            Element content = html.getElementById("content");
            media.setDescription(textWrap != null ? textWrap.getElementsByTag("div").get(2).text()
                    : contentText != null ? contentText.getElementsByTag("p").text()
                            : content.getElementsByTag("div").get(2).text());
            Element table = Optional.ofNullable(html.getElementsByClass("ff_text").first())
                    .orElseGet(() -> contentText != null ? contentText.getElementsByTag("table").first() : null);
            Element caption = html.getElementsByClass("caption").first();
            if (table != null) {
                for (Element tr : table.child(0).getElementsByTag("table").first().getElementsByTag("tr")) {
                    if (!tr.hasAttr("class")) {
                        String text = tr.child(1).text();
                        switch (tr.child(0).text().trim()) {
                        case "Credit":
                            media.setCredits(text);
                            break;
                        case "Release Date":
                            media.setPublicationDate(parseReleaseDate(text));
                            break;
                        }
                    }
                }
            } else {
                if (caption != null) {
                    String text = caption.text();
                    media.setCredits(text.substring(text.indexOf("Credit:")));
                }
                if (content != null) {
                    media.setPublicationDate(parseReleaseDate(
                            content.getElementsByTag("strong").first().text().replace("For Release:", "").trim()));
                }
            }
            Element right = Optional.ofNullable(html.getElementById("photo_top_right"))
                    .or(() -> Optional.ofNullable(html.getElementById("images_right")))
                    .orElse(caption);
            Elements menu = right.getElementsByClass("side_menu");
            for (Element a : (menu.isEmpty()
                    ? Optional.ofNullable(right.getElementsByClass("leftside_podcast").first()).orElse(right)
                    : menu.get(1)).getElementsByTag("a")) {
                String href = a.attr("href");
                if (!href.contains(".htm") && !href.contains(".xml") && !href.contains(".aspx")) {
                    addMetadata(media, href.contains("://") ? href : url + href, null);
                }
            }
        } catch (RuntimeException e) {
            LOGGER.error("Failed to parse HTML for {} => {}", media, html.html());
            throw e;
        }
    }

    private static LocalDate parseReleaseDate(String text) {
        try {
            return LocalDate.parse(text, DATE_FORMATTER_2);
        } catch (DateTimeParseException e) {
            return LocalDate.parse(text, DATE_FORMATTER_3);
        }
    }

    @Override
    protected boolean checkBlocklist() {
        return false;
    }

    @Override
    public Set<String> findLicenceTemplates(NasaChandraMedia media, FileMetadata metadata) {
        Set<String> result = super.findLicenceTemplates(media, metadata);
        result.add("PD-USGov-NASA");
        return result;
    }

    @Override
    protected NasaChandraMedia refresh(NasaChandraMedia media) throws IOException {
        return media.copyDataFrom(fetchMedia(media.getId(), empty()));
    }

    @Override
    protected Class<NasaChandraMedia> getMediaClass() {
        return NasaChandraMedia.class;
    }

    @Override
    protected Set<String> getTwitterAccounts(NasaChandraMedia uploadedMedia) {
        return Set.of("@chandraxray");
    }
}
