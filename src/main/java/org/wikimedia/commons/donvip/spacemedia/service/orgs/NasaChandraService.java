package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.Locale.ENGLISH;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toSet;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.getWithJsoup;

import java.io.IOException;
import java.time.LocalDate;
import java.time.Year;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
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

    private static final String BASE_URL = "https://chandra.si.edu";
    private static final String PHOTO_URL = BASE_URL + "/photo";

    private static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
            ofPattern("dd MMM yy", ENGLISH), ofPattern("MMMM dd, yyyy", ENGLISH), ofPattern("MMMM d, yyyy", ENGLISH),
            ofPattern("MMM dd, yyyy", ENGLISH), ofPattern("MMMM dd , yyyy", ENGLISH));

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
                .mapToObj(x -> String.format("%s/chronological%02d.html", PHOTO_URL, x)).toList();
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
        return Optional
                .of(LocalDate.parse(result.getElementsByClass("page_gray").first().text(), DATE_FORMATTERS.get(0))
                .atStartOfDay(ZoneOffset.UTC));
    }

    @Override
    protected String getSourceUrl(CompositeMediaId id) {
        return PHOTO_URL + '/' + id.getMediaId();
    }

    @Override
    protected boolean loop(String repoId, Elements results) {
        return false;
    }

    @Override
    protected String getAuthor(NasaChandraMedia media, FileMetadata metadata) {
        return ofNullable(media.getCredits()).orElse("NASA/SAO");
    }

    @Override
    protected void fillMediaWithHtml(String url, Document html, NasaChandraMedia media) throws IOException {
        try {
            if (media.getTitle() == null) {
                media.setTitle(ofNullable(html.getElementById("image_title"))
                        .or(() -> ofNullable(html.getElementsByClass("page_title").first()))
                        .or(() -> ofNullable(html.getElementsByClass("press_title").first()))
                        .orElseGet(() -> html.getElementsByTag("h3").first()).text());
            }
            Element textWrap = html.getElementById("text_wrap");
            Element contentText = html.getElementById("content_text");
            Element content = html.getElementById("content");
            if (media.getDescription() == null) {
                media.setDescription(textWrap != null ? textWrap.getElementsByTag("div").get(2).text()
                        : contentText != null ? contentText.getElementsByTag("p").text()
                                : content != null ? content.getElementsByTag("p").text()
                                        : html.getElementsByClass("body").text());
            }
            Element table = ofNullable(html.getElementsByClass("ff_text").first())
                    .orElseGet(() -> contentText != null ? contentText.getElementsByTag("table").first() : null);
            Elements captions = (content != null ? content : html).getElementsByClass("caption");
            Element caption = captions.first();
            if (table != null) {
                Element table2 = table.child(0).getElementsByTag("table").first();
                if (table2 == null) {
                    addMetadataFromLinks(url, table.getElementsByTag("a"), media);
                    table = contentText.getElementsByTag("table").get(1);
                    table2 = table.child(0).getElementsByTag("table").first();
                }
                fillAttributesFromTable(url, table2, media);
            } else {
                if (caption != null) {
                    String text = caption.text();
                    int idx = text.indexOf("Credit:");
                    if (idx == -1) {
                        idx = text.indexOf("Image:");
                    }
                    if (idx == -1) {
                        idx = text.indexOf("Illustration:");
                    }
                    if (idx > -1) {
                        media.setCredits(text.substring(idx));
                    }
                }
                Element releaseDate = (content != null ? content.getElementsByTag("strong")
                        : html.getElementsByClass("boldgray"))
                        .first();
                if (releaseDate != null && media.getPublicationDate() == null) {
                    String text = releaseDate.text();
                    if (content != null && !text.matches(".*\\d.*")) {
                        releaseDate = content.getElementsByTag("strong").first().parent();
                        text = releaseDate.text().replace(text, "").trim();
                    }
                    media.setPublicationDate(parseReleaseDate(text.replace("For Release:", "").trim()));
                }
            }
            Element right = ofNullable(html.getElementById("photo_top_right"))
                    .or(() -> ofNullable(html.getElementById("images_right")))
                    .orElse(caption);
            if (right != null) {
                Elements menu = right.getElementsByClass("side_menu");
                addMetadataFromLinks(url,
                        (menu.isEmpty() ? ofNullable(right.getElementsByClass("leftside_podcast").first()).orElse(right)
                                : menu.get(1)).getElementsByTag("a"),
                        media);
            }
            if (captions != null && !media.hasMetadata()) {
                captions.forEach(c -> addMetadataFromLinks(url, c.parent().getElementsByTag("a"), media));
            }
            if (!media.hasMetadata() && !url.endsWith("/more.html")) {
                String moreUrl = url + "/more.html";
                if (html.getElementsByTag("a").stream().map(a -> a.attr("href")).distinct()
                        .anyMatch(x -> "more.html".equals(x) || moreUrl.equals(x))) {
                    LOGGER.info("Parsing MORE: {}", moreUrl);
                    fillMediaWithHtml(moreUrl, getMore(media.getIdUsedInOrg(), moreUrl), media);
                }
            }
        } catch (RuntimeException e) {
            LOGGER.error("Failed to parse HTML for {} => {}", media, html.html());
            throw e;
        }
    }

    protected Document getMore(String id, String url) throws IOException {
        return getWithJsoup(url, 10_000, 5);
    }

    private static void fillAttributesFromTable(String url, Element table2, NasaChandraMedia media) {
        for (Element tr : table2.getElementsByTag("tr")) {
            if (!tr.hasAttr("class") && tr.childrenSize() == 2) {
                String key = tr.child(0).text().replace(":", "").replace("(", "").replace(")", "").trim();
                String text = tr.child(1).text();
                switch (key) {
                case "Credit":
                    media.setCredits(text);
                    break;
                case "Release Date":
                    media.setPublicationDate(parseReleaseDate(text));
                    break;
                case "Scale":
                    media.setScale(text);
                    break;
                case "Category":
                    media.setCategory(text);
                    break;
                case "Coordinates J2000":
                    media.setCoordinates(text);
                    break;
                case "Constellation":
                    media.setConstellation(text);
                    break;
                case "Distance Estimate":
                    media.setDistance(text);
                    break;
                case "Observation Date", "Observation Dates":
                    media.setObservationDate(text);
                    break;
                case "Observation Time":
                    media.setObservationTime(text);
                    break;
                case "Obs. ID", "Obs. IDs":
                    media.setObservationIds(text);
                    break;
                case "Instrument":
                    media.setInstruments(Arrays.stream(text.split(",")).map(String::trim).collect(toSet()));
                    break;
                case "References":
                    media.setReferences(text);
                    break;
                case "Color Code":
                    media.setColorCode(text);
                    break;
                case "Also Known As":
                    media.setAlsoKnownAs(text);
                    break;
                default:
                    throw new IllegalArgumentException(url + " => " + key);
                }
            }
        }
    }

    private void addMetadataFromLinks(String url, Elements links, NasaChandraMedia media) {
        for (Element a : links) {
            String href = a.attr("href");
            if (!href.isEmpty() && !href.contains(".htm") && !href.contains(".xml") && !href.contains(".aspx")
                    && !href.contains(".ps") && !href.contains(".glb") && !href.contains(".obj")
                    && !href.contains("/xml/") && !href.contains("/openFITS/") && !href.contains("/blog/")
                    && !href.contains("/podcasts/")) {
                addMetadata(media, href.contains("://") ? href
                        : href.startsWith("/press/") || href.startsWith("/photo/") || href.startsWith("/graphics/")
                                ? BASE_URL + href
                                : url.replace("more.html", "") + href,
                        null);
            }
        }
    }

    private static LocalDate parseReleaseDate(String text) {
        for (DateTimeFormatter dtf : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(text, dtf);
            } catch (DateTimeParseException e) {
                LOGGER.trace(e.getMessage(), e);
            }
        }
        throw new IllegalArgumentException(text);
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
