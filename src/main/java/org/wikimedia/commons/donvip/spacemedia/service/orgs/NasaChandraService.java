package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.Locale.ENGLISH;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.wikimedia.commons.donvip.spacemedia.service.wikimedia.WikidataItem.Q125191_PHOTOGRAPH;
import static org.wikimedia.commons.donvip.spacemedia.service.wikimedia.WikidataItem.Q98069877_VIDEO;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.getWithJsoup;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.newURL;

import java.io.IOException;
import java.time.LocalDate;
import java.time.Year;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
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
import org.wikimedia.commons.donvip.spacemedia.service.wikimedia.SdcStatements;

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
        return StringUtils.strip(result.getElementsByTag("a").first().attr("href").replace("/photo/", ""), "/");
    }

    @Override
    protected Optional<ZonedDateTime> extractDateFromGalleryItem(Element result) {
        return Optional
                .of(LocalDate.parse(result.getElementsByClass("page_gray").first().text(), DATE_FORMATTERS.get(0))
                .atStartOfDay(ZoneOffset.UTC));
    }

    @Override
    protected String getSourceUrl(CompositeMediaId id) {
        String url = PHOTO_URL + '/' + id.getMediaId();
        return url.contains("/more/") ? url.replaceAll("/more/\\d+", "/more.html") : url;
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
    protected Document fetchUrl(String url) throws IOException {
        return getWithJsoup(url, 60_000, 5);
    }

    @Override
    protected List<NasaChandraMedia> fillMediaWithHtml(String url, Document html, NasaChandraMedia media)
            throws IOException {
        try {
            media.setTitle(ofNullable(html.getElementById("image_title"))
                    .or(() -> ofNullable(html.getElementsByClass("page_title").first()))
                    .or(() -> ofNullable(html.getElementsByClass("press_title").first()))
                    .orElseGet(() -> html.getElementsByTag("h3").first()).text());
            Element textWrap = html.getElementById("text_wrap");
            Element contentText = html.getElementById("content_text");
            Element content = html.getElementById("content");
            media.setDescription(textWrap != null ? textWrap.getElementsByTag("div").get(2).text()
                    : contentText != null ? contentText.getElementsByTag("p").text()
                            : content != null ? content.getElementsByTag("p").text()
                                    : html.getElementsByClass("body").text());
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
            String moreUrl = (url + "/more.html").replace("//", "/").replace(":/", "://");
            if (html.getElementsByTag("a").stream().map(a -> a.attr("href")).distinct()
                    .anyMatch(x -> "more.html".equals(x) || moreUrl.equals(x))) {
                LOGGER.info("Parsing MORE: {}", moreUrl);
                return processMoreHtml(moreUrl, getMore(media.getIdUsedInOrg(), moreUrl), media);
            } else {
                return List.of(media);
            }
        } catch (RuntimeException e) {
            LOGGER.error("Failed to parse HTML for {} => {}", media, html.html());
            throw e;
        }
    }

    protected Document getMore(String id, String url) throws IOException {
        return fetchUrl(url);
    }

    protected List<NasaChandraMedia> processMoreHtml(String url, Document html, NasaChandraMedia media) {
        List<NasaChandraMedia> result = new ArrayList<>(List.of(media));

        int i = 0;
        Element content = html.getElementById("content");
        for (Element number : content.getElementsByClass("more_number")) {
            i++;
            NasaChandraMedia moreMedia = new NasaChandraMedia();
            String numberText = number.text();
            moreMedia.setId(new CompositeMediaId(media.getId().getRepoId(),
                    media.getIdUsedInOrg() + "/more/" + (numberText.isEmpty() ? i : Integer.parseInt(numberText))));
            moreMedia.setPublicationDate(media.getPublicationDate());
            Element title = nextSiblingWithClass(number, "bold");
            if (title != null) {
                moreMedia.setTitle(title.text());
            }
            Element caption = nextSiblingWithClass(title, "caption");
            if (caption == null && title != null) {
                caption = title.parent().nextElementSibling().getElementsByClass("caption").first();
            }
            if (caption != null) {
                moreMedia.setCredits(caption.text());
            }
            Element imageMulti = nextSiblingWithClass(number, "more_image_multi");
            if (imageMulti == null && caption != null) {
                imageMulti = nextSiblingWithClass(caption, "more_image_multi");
            }
            if (imageMulti != null) {
                moreMedia.setThumbnailUrl(
                        newURL(newChandraUrl(url, imageMulti.getElementsByTag("img").first().attr("href"))));
                ofNullable(nextSiblingWithClass(imageMulti, "more_caption_multi")).map(Element::text)
                        .ifPresent(moreMedia::setDescription);
                for (Element image : imageMulti.getElementsByClass("more_image")) {
                    Element moreImageCaption = image.getElementsByClass("caption").first();
                    if (moreImageCaption != null) {
                        addMetadataFromLinks(url, moreImageCaption.getElementsByTag("a"), moreMedia);
                    }
                }
            } else {
                Element image = nextSiblingWithClass(number, "more_image");
                moreMedia.setThumbnailUrl(
                        newURL(newChandraUrl(url, image.getElementsByTag("img").first().attr("href"))));
                Element moreCaption = nextSiblingWithClass(image, "more_caption");
                String credits = moreCaption.getElementsByClass("caption").first().text();
                moreMedia.setCredits(credits);
                moreMedia.setDescription(moreCaption.text().replace(credits, "").trim());
                addMetadataFromLinks(url, image.getElementsByTag("a"), moreMedia);
            }
            result.add(moreMedia);
        }

        return result;
    }

    private static Element nextSiblingWithClass(Element start, String klass) {
        Element elem = start;
        while (elem != null) {
            elem = elem.nextElementSibling();
            if (elem != null) {
                Element result = elem.getElementsByClass(klass).first();
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    private static void fillAttributesFromTable(String url, Element table2, NasaChandraMedia media) {
        for (Element tr : table2.getElementsByTag("tr")) {
            if (!tr.hasAttr("class") && tr.childrenSize() == 2) {
                String key = tr.child(0).text().replace(":", "").replace("(", "").replace(")", "").trim();
                String text = tr.child(1).text();
                switch (key) {
                case "Credit", "Image Credit", "Video Credit":
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
                case "About the Sound", "About the Sonification":
                    media.setAboutTheSound(text);
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
                addMetadata(media, newChandraUrl(url, href), null);
            }
        }
    }

    static String newChandraUrl(String url, String href) {
        return href.contains("://") ? href
                : href.startsWith("/press/") || href.startsWith("/photo/") || href.startsWith("/graphics/")
                        ? BASE_URL + href
                        : url.replace("more.html", "") + '/' + href;
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
    protected SdcStatements getStatements(NasaChandraMedia media, FileMetadata metadata) {
        SdcStatements result = super.getStatements(media, metadata)
                .instanceOf(metadata.isVideo() ? Q98069877_VIDEO : Q125191_PHOTOGRAPH);
        result.creator("Q382494") // Created by Chandra
                .locationOfCreation("Q218056") // Created in high Earth orbit
                .fabricationMethod("Q725252"); // Satellite imagery
        if (isNotBlank(media.getConstellation())) {
            wikidata.searchConstellation(media.getConstellation()).map(Pair::getValue).ifPresent(result::constellation);
        }
        doFor(media.getInstruments(), i -> wikidata.searchInstrument(i).map(Pair::getKey), result::capturedWith);
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
