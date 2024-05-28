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
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
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
import org.wikimedia.commons.donvip.spacemedia.utils.Utils;

@Service
public class NasaChandraService extends AbstractOrgHtmlGalleryService<NasaChandraMedia> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NasaChandraService.class);

    private static final String BASE_URL = "https://chandra.si.edu";
    private static final String PHOTO_URL = BASE_URL + "/photo";

    private static final Map<String, List<String>> GALLERY_URLS = new TreeMap<>();

    private static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
            ofPattern("dd MMM yy", ENGLISH), ofPattern("MMMM dd, yyyy", ENGLISH), ofPattern("MMMM d, yyyy", ENGLISH),
            ofPattern("MMM dd, yyyy", ENGLISH), ofPattern("MMMM dd , yyyy", ENGLISH), ofPattern("MMMM yyyy", ENGLISH),
            ofPattern("yyyy", ENGLISH));

    public NasaChandraService(NasaChandraMediaRepository repository) {
        super(repository, "nasa.chandra", Set.of("chandra"));
    }

    @Override
    public String getName() {
        return "Chandra X-ray Observatory";
    }

    @Override
    protected List<String> fetchGalleryUrls(String repoId) {
        List<String> result = new ArrayList<>();
        int from = 1999;
        int to = Year.now().getValue();
        result.addAll(IntStream.rangeClosed(from, to).map(x -> ((to - x + from - 2000) % 100 + 100) % 100)
                .mapToObj(x -> String.format("%s/chronological%02d.html", PHOTO_URL, x)).toList());

        try {
            Set<String> resources = new TreeSet<>();
            String url = BASE_URL + "/resources/chandraMission.html";
            for (Element box : fetchUrl(url).getElementsByClass("discover_Multi_box")) {
                for (Element text : box.getElementsByClass("about_text")) {
                    String galleryUrl = newChandraUrl(url, text.getElementsByTag("a").first().attr("href"));
                    if (galleryUrl.startsWith(BASE_URL + "/resources/")) {
                        resources.add(galleryUrl);
                    }
                }
            }
            resources.addAll(List.of(BASE_URL + "/resources/handouts/lithos/index.html",
                    BASE_URL + "/resources/illustrations/infographics.html",
                    BASE_URL + "/resources/illustrations/3d_files.html"));
            result.addAll(resources);
        } catch (IOException e) {
            LOGGER.error("Failed to parse mission page: {}", e.getMessage());
        }
        GALLERY_URLS.put(repoId, result);
        return result;
    }

    @Override
    protected String getGalleryPageUrl(String galleryUrl, int page) {
        return galleryUrl;
    }

    @Override
    protected Elements getGalleryItems(String repoId, String url, Element html) {
        return html.getElementsByClass(url.contains("/chronological") ? "page_box2" : "resources_box_wrapper");
    }

    @Override
    protected String extractIdFromGalleryItem(String url, Element result) {
        String text = "";
        if (url.contains("/chronological")) {
            text = result.getElementsByTag("a").first().attr("href").replace("/photo/", "");
        }
        if (text.isEmpty()) {
            Element boldgray = result.getElementsByClass("boldgray").first();
            Element link = boldgray.getElementsByTag("a").first();
            if (link != null) {
                text = hrefWithoutLeadingResourcs(link.attr("href"));
            } else {
                text = textFromBoldgray(boldgray);
                if (!text.isEmpty()) {
                    text = hrefWithoutLeadingResourcs(url).replace(".html", "/") + text.replace(' ', '_');
                }
            }
        }
        return StringUtils.strip(text, "/");
    }

    private static String textFromBoldgray(Element boldgray) {
        String text = boldgray.text();
        int idx = text.indexOf(". ");
        if (idx > 0) {
            text = text.substring(idx + 2);
        }
        return text;
    }

    private static String hrefWithoutLeadingResourcs(String href) {
        String result = href.replace(BASE_URL, "");
        if (result.startsWith("/resources")) {
            result = result.replace("/resources", "");
        }
        return result;
    }

    @Override
    protected Optional<Temporal> extractDateFromGalleryItem(Element result) {
        String text = "";
        Element pageGray = result.getElementsByClass("page_gray").first();
        if (pageGray != null) {
            text = pageGray.text();
        } else {
            text = result.getElementsByClass("resources_text").first().getElementsByClass("caption").text();
            if (text.contains("(")) {
                text = text.substring(text.indexOf('(') + 1, text.indexOf(')'));
            }
        }
        return Utils.extractDate(text, DATE_FORMATTERS);
    }

    @Override
    protected String getSourceUrl(CompositeMediaId id) {
        char c = id.getMediaId().charAt(0);
        if (c == '1' || c == '2') {
            String url = PHOTO_URL + '/' + id.getMediaId();
            return url.contains("/more/") ? url.replaceAll("/more/\\d+", "/more.html") : url;
        } else {
            String url = BASE_URL + '/' + id.getMediaId();
            if (Utils.uriExists(url)) {
                return url;
            }
            url = BASE_URL + "/resources/" + id.getMediaId();
            if (Utils.uriExists(url)) {
                return url;
            }
            url = url.replace(url.substring(url.lastIndexOf('/')), ".html");
            if (Utils.uriExists(url)) {
                return url;
            }
            url = url.replace(".html", "/");
            if (Utils.uriExists(url)) {
                return url;
            }
            return null;
        }
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
    protected List<NasaChandraMedia> fillMediaWithHtml(String url, Element galleryItem, NasaChandraMedia media)
            throws IOException {
        if ((url.contains(".htm") || url.lastIndexOf('.') < url.lastIndexOf('/'))
                && !GALLERY_URLS.get(media.getId().getRepoId()).contains(url)) {
            return fillMediaWithHtml(url, fetchUrl(url), galleryItem, media);
        } else {
            Element resourcesText = galleryItem.getElementsByClass("resources_text").first();
            if (resourcesText != null) {
                Element link = resourcesText.getElementsByTag("a").first();
                if (link != null) {
                    media.setTitle(link.text());
                } else {
                    media.setTitle(textFromBoldgray(resourcesText.getElementsByClass("boldgray").first()));
                }
                media.setDescription(resourcesText.ownText());
            }
            addMetadataFromLinks(url, galleryItem.getElementsByTag("a"), media);
            if (media.getPublicationDate() == null) {
                media.deduceApproximatePublicationDate().ifPresent(media::setPublicationDate);
            }
            if (media.getPublicationDate() == null) {
                for (Element div : galleryItem.ownerDocument().getElementById("footer").getElementsByTag("div")) {
                    String text = div.text();
                    if (text.startsWith("Revised:")) {
                        Utils.extractDate(text.replace("Revised:", "").trim(), DATE_FORMATTERS).ifPresent(date -> {
                            media.setPublication(date);
                            LOGGER.warn("Set an arbitrary publication date to {}", media);
                        });
                        break;
                    }
                }
            }
            return List.of(media);
        }
    }

    @Override
    protected List<NasaChandraMedia> fillMediaWithHtml(String url, Document html, Element galleryItem,
            NasaChandraMedia media) throws IOException {
        try {
            Element pageTitle = html.getElementsByClass("page_title").first();
            media.setTitle(ofNullable(html.getElementById("image_title"))
                    .or(() -> ofNullable(pageTitle))
                    .or(() -> ofNullable(html.getElementsByClass("press_title").first()))
                    .orElseGet(() -> html.getElementsByTag("h3").first()).text());
            if (pageTitle != null && "Handouts & Activities".equals(media.getTitle())) {
                media.setTitle(pageTitle.nextElementSibling().nextElementSibling().nextElementSibling().text());
            }
            Element textWrap = html.getElementById("text_wrap");
            Element contentText = html.getElementById("content_text");
            Element content = html.getElementById("content");
            Element photoRightBox = html.getElementById("photo_right_box");
            media.setDescription(textWrap != null ? textWrap.getElementsByTag("div").get(2).text()
                    : contentText != null ? contentText.getElementsByTag("p").text()
                            : content != null ? content.getElementsByTag("p").text()
                                    : html.getElementsByClass("body").text());
            if (content != null && media.getDescription().isBlank()) {
                media.setDescription(content.text().replace("Handouts & Activities", "").trim());
            }
            Element table = ofNullable(html.getElementsByClass("ff_text").first())
                    .orElseGet(() -> contentText != null ? contentText.getElementsByTag("table").first() : null);
            Elements captions = (content != null ? content : html).getElementsByClass("caption");
            Element caption = captions.first();
            if (table != null) {
                Element table2 = table.child(0).getElementsByTag("table").first();
                if (table2 == null) {
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
                    ofNullable(parseReleaseDate(text.replace("For Release:", "").trim()))
                            .ifPresent(media::setPublicationDate);
                }
            }
            if (media.getPublicationDate() == null) {
                media.deduceApproximatePublicationDate().ifPresent(media::setPublicationDate);
            }
            addMetadataFromLinks(url, ofNullable(photoRightBox).or(() -> ofNullable(content))
                    .orElseGet(() -> html.getElementById("wrapper_big")).getElementsByTag("a"), media);
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
            if (title == null) {
                title = nextSiblingWithClass(number.parent(), "bold");
            }
            if (title != null) {
                moreMedia.setTitle(title.text());
            }
            Element caption = nextSiblingWithClass(title, "caption");
            if (caption == null && title != null) {
                caption = nextSiblingWithClass(title.parent(), "caption");
            }
            if (caption != null) {
                moreMedia.setCredits(caption.text());
            }
            Element imageMulti = nextSiblingWithClass(number, "more_image_multi");
            if (imageMulti == null && caption != null) {
                imageMulti = nextSiblingWithClass(caption, "more_image_multi");
            }
            if (imageMulti == null) {
                imageMulti = nextSiblingWithClass(number.parent(), "more_image_multi");
            }
            if (imageMulti != null) {
                Element img = imageMulti.getElementsByTag("img").first();
                if (img != null) {
                    moreMedia.setThumbnailUrl(newURL(newChandraUrl(url, img.attr("href"))));
                }
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
                Element moreCaptionCaption = moreCaption != null ? moreCaption.getElementsByClass("caption").first()
                        : null;
                String credits = moreCaptionCaption != null ? moreCaptionCaption.text() : "";
                if (!credits.isEmpty()) {
                    moreMedia.setCredits(credits);
                }
                if (moreCaption != null) {
                    moreMedia.setDescription(moreCaption.text().replace(credits, "").trim());
                }
                addMetadataFromLinks(url, image.getElementsByTag("a"), moreMedia);
            }
            if (moreMedia.hasMetadata()) {
                result.add(moreMedia);
            } else {
                LOGGER.warn("Skipped MORE media without image: {}", moreMedia);
            }
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
                case "Credit", "Illustration Credit", "Image Credit", "Video Credit", "Video Compilation Credit":
                    media.setCredits(text);
                    break;
                case "Release Date", "Model Release Date":
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
                case "Orientation":
                    media.setOrientation(text);
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
                case "Note":
                    media.setNote(text);
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
            if (!href.isEmpty() && !href.startsWith("mailto:") && !href.contains(".htm") && !href.contains(".xml")
                    && !href.contains(".aspx") && !href.contains(".ps") && !href.contains(".glb")
                    && !href.contains(".txt") && !href.contains(".php") && !href.contains(".obj")
                    && !href.contains("_description_audio.mp3") && !href.contains("/xml/")
                    && !href.contains("/openFITS/") && !href.contains("/blog/") && !href.contains("/podcasts/")
                    && !href.endsWith("_low.pdf") && !href.endsWith("_lores.pdf")) {
                String fileUrl = newChandraUrl(url, href);
                if (fileUrl.startsWith(BASE_URL) && fileUrl.lastIndexOf('.') > fileUrl.lastIndexOf('/')
                        && media.getMetadataStream().noneMatch(x -> fileUrl.equals(x.getAssetUri().toString()))) {
                    addMetadata(media, fileUrl, null);
                }
            }
        }
    }

    static String newChandraUrl(String url, String href) {
        return (href.contains("://") ? href
                : href.startsWith("/press/") || href.startsWith("/photo/") || href.startsWith("/graphics/")
                        || href.startsWith("/resources/") || href.startsWith("/fifth/") || href.startsWith("/ten/")
                        || href.startsWith("/corps/") || href.startsWith("/blackhole/") || href.startsWith("/deadstar/")
                        || href.startsWith("/edu/")
                        ? BASE_URL + href
                                : url.replace("more.html", "") + '/' + href)
                .replace("//", "/").replace(":/", "://");
    }

    private static LocalDate parseReleaseDate(String text) {
        return (LocalDate) Utils.extractDate(text, DATE_FORMATTERS).orElse(null);
    }

    @Override
    protected boolean checkBlocklist() {
        return false;
    }

    @Override
    protected Optional<String> getOtherFields(NasaChandraMedia media) {
        StringBuilder sb = new StringBuilder();
        addOtherField(sb, "About the Sound", media.getAboutTheSound());
        addOtherField(sb, "Also Known As", media.getAlsoKnownAs());
        addOtherField(sb, "Category", media.getCategory());
        addOtherField(sb, "Color Code", media.getColorCode());
        addOtherField(sb, "Constellation", media.getConstellation());
        addOtherField(sb, "Coordinates (J2000)", media.getCoordinates());
        addOtherField(sb, "Distance Estimate", media.getDistance());
        addOtherField(sb, "Orientation", media.getOrientation());
        addOtherField(sb, "Note", media.getNote());
        addOtherField(sb, "Observation Date(s)", media.getObservationDate());
        addOtherField(sb, "Observation ID(s)", media.getObservationIds());
        addOtherField(sb, "Observation Time", media.getObservationTime());
        addOtherField(sb, "References", media.getReferences());
        addOtherField(sb, "Scale", media.getScale());
        addOtherField(sb, "Instruments", media.getInstruments());
        String s = sb.toString();
        return s.isEmpty() ? Optional.empty() : Optional.of(s);
    }

    @Override
    public Set<String> findCategories(NasaChandraMedia media, FileMetadata metadata, boolean includeHidden) {
        Set<String> result = super.findCategories(media, metadata, includeHidden);
        boolean instrumentCat = false;
        if (media.getInstruments().contains("ACIS")) {
            instrumentCat = result.add("ACIS images");
        }
        if (media.getInstruments().contains("HRC")) {
            instrumentCat = result.add("HRC images");
        }
        if (media.getInstruments().contains("HETG") || media.getInstruments().contains("HETGS")) {
            instrumentCat = result.add("HETGS images");
        }
        if (media.getInstruments().contains("LETG") || media.getInstruments().contains("LETGS")) {
            instrumentCat = result.add("LETGS images");
        }
        if (!instrumentCat) {
            result.add("Images by the Chandra X-ray Observatory");
        }
        return result;
    }

    @Override
    public Set<String> findAfterInformationTemplates(NasaChandraMedia media, FileMetadata metadata) {
        Set<String> result = super.findAfterInformationTemplates(media, metadata);
        Set<String> instruments = media.getInstruments();
        result.add("NASA Photojournal/attribution|class=Chandra|mission=Chandra|name=Chandra"
                + (instruments.isEmpty() ? "" : "|credit=" + instruments.iterator().next()));
        return result;
    }

    @Override
    public Set<String> findLicenceTemplates(NasaChandraMedia media, FileMetadata metadata) {
        Set<String> result = super.findLicenceTemplates(media, metadata);
        result.add("PD-USGov-NASA");
        return result;
    }

    @Override
    protected SdcStatements getStatements(NasaChandraMedia media, FileMetadata metadata) {
        SdcStatements result = super.getStatements(media, metadata);
        char c = media.getIdUsedInOrg().charAt(0);
        if (c == '1' || c == '2') {
            result.instanceOf(metadata.isVideo() ? Q98069877_VIDEO : Q125191_PHOTOGRAPH)
                    .creator("Q49002") // Created by Chandra
                    .locationOfCreation("Q218056") // Created in high Earth orbit
                    .fabricationMethod("Q725252"); // Satellite imagery
        }
        if (isNotBlank(media.getConstellation())) {
            wikidata.searchConstellation(media.getConstellation()).map(Pair::getKey).ifPresent(result::constellation);
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
