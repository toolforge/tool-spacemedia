package org.wikimedia.commons.donvip.spacemedia.service.agencies;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.Temporal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.data.domain.eso.EsoFrontPageItem;
import org.wikimedia.commons.donvip.spacemedia.data.domain.eso.EsoMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.eso.EsoMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.eso.EsoMediaType;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.micrometer.core.instrument.util.StringUtils;

@Service
public class EsoService extends AbstractFullResSpaceAgencyService<EsoMedia, String> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EsoService.class);

    private static final Pattern SIZE_PATTERN = Pattern.compile("([0-9]+) x ([0-9]+) px");

    private static final String ESO_BASE_PUBLIC_URL = "https://www.eso.org/public/";

    private static final String ESO_IMAGES_PATH = "images/";

    private static final Pattern PATTERN_LOCALIZED_URL = Pattern
            .compile(ESO_BASE_PUBLIC_URL + "([a-z]+/)" + ESO_IMAGES_PATH + ".*");

    private DateTimeFormatter dateFormatter;
    private DateTimeFormatter dateTimeFormatter;

    @Value("${eso.date.pattern}")
    private String datePattern;

    @Value("${eso.datetime.pattern}")
    private String dateTimePattern;

    @Value("${eso.search.link}")
    private String searchLink;

    @Value("#{${eso.categories}}")
    private Map<String, String> esoCategories;

    @Autowired
    private ObjectMapper jackson;

    public EsoService(EsoMediaRepository repository) {
        super(repository);
    }

    @PostConstruct
    void init() {
        dateFormatter = DateTimeFormatter.ofPattern(datePattern, Locale.ENGLISH);
        dateTimeFormatter = DateTimeFormatter.ofPattern(dateTimePattern, Locale.ENGLISH);
    }

    @Scheduled(fixedDelay = 43200000L)
    public void checkEsoCategories() {
        checkCommonsCategories(esoCategories);
    }

    @Override
    public String getName() {
        return "ESO";
    }

    private static void scrapingError(String url) {
        throw new IllegalStateException("ESO scraping code must be updated, see " + url);
    }

    private Optional<EsoMedia> updateMediaForUrl(URL url, EsoFrontPageItem item)
            throws IOException, URISyntaxException {
        String imgUrlLink = url.getProtocol() + "://" + url.getHost() + item.getUrl();
        Matcher m = PATTERN_LOCALIZED_URL.matcher(imgUrlLink);
        if (m.matches()) {
            // Switch to international english url
            imgUrlLink = imgUrlLink.replace(m.group(1), "");
        }
        String id = item.getId();
        if (id.length() < 2) {
            scrapingError(imgUrlLink);
        }
        EsoMedia media;
        boolean save = false;
        Optional<EsoMedia> mediaInRepo = repository.findById(id);
        if (mediaInRepo.isPresent()) {
            media = mediaInRepo.get();
        } else {
            media = new EsoMedia();
            media.setId(id);
            save = true;
            LOGGER.info(imgUrlLink);
            Document html = Jsoup.connect(imgUrlLink).timeout(60_000).get();
            Element div = html.getElementsByClass("col-md-9 left-column").get(0);
            // Find title
            Elements h1s = div.getElementsByTag("h1");
            if (h1s.size() != 1 || h1s.get(0).text().isEmpty()) {
                scrapingError(imgUrlLink);
            }
            media.setTitle(h1s.get(0).html());
            // Find description
            StringBuilder description = new StringBuilder();
            for (Element p : div.getElementsByTag("div").get(1).nextElementSiblings()) {
                if ("script".equals(p.tagName())) {
                    continue;
                } else if (!"p".equals(p.tagName())) {
                    break;
                }
                description.append(p.html());
            }
            // Description CAN be empty, see https://www.eso.org/public/images/ann19041a/
            media.setDescription(description.toString());
            // Find credit
            Element credit = div.getElementsByClass("credit").get(0).getElementsByTag("p").get(0);
            Elements links = credit.getElementsByTag("a");
            for (Element a : links) {
                for (Iterator<Attribute> it = a.attributes().iterator(); it.hasNext();) {
                    if (!"href".equals(it.next().getKey())) {
                        it.remove();
                    }
                }
            }
            media.setCredit(links.isEmpty() ? credit.text()
                    : credit.html().replaceAll("<span[^>]*>", "").replace("</span>", ""));
            if (media.getCredit().startsWith("<") && !media.getCredit().startsWith("<a")) {
                scrapingError(imgUrlLink);
            }
            // Check copyright is standard (CC-BY-4.0)
            if (!"/public/outreach/copyright/"
                    .equals(div.getElementsByClass("copyright").get(0).getElementsByTag("a").get(0).attr("href"))) {
                LOGGER.error("Invalid copyright for " + imgUrlLink);
                return Optional.empty();
            }

            processObjectInfos(url, imgUrlLink, id, media, html);

            for (Element link : html.getElementsByClass("archive_dl_text")) {
                String assetUrlLink = link.getElementsByTag("a").get(0).attr("href");
                if (assetUrlLink.contains("/original/")) {
                    media.setFullResAssetUrl(buildAssetUrl(assetUrlLink, url));
                } else if (assetUrlLink.contains("/large/")) {
                    media.setAssetUrl(buildAssetUrl(assetUrlLink, url));
                }
            }

            // Try to detect pictures of identifiable people, as per ESO conditions
            if (media.getCategories() != null && media.getCategories().contains("People and Events")
                    && media.getCategories().size() == 1 && media.getTypes() != null
                    && media.getTypes().stream().allMatch(s -> s.startsWith("Unspecified : People"))) {
                media.setIgnored(Boolean.TRUE);
                media.setIgnoredReason(
                        "Image likely include a picture of an identifiable person, using that image for commercial purposes is not permitted.");
            }
        }
        if (mediaService.computeSha1(media)) {
            save = true;
        }
        if (mediaService.findCommonsFilesWithSha1(media)) {
            save = true;
        }
        if (save) {
            repository.save(media);
        }
        return Optional.of(media);
    }

    private static URL buildAssetUrl(String assetUrlLink, URL url) throws MalformedURLException {
        return new URL(
                assetUrlLink.startsWith("/") ? url.getProtocol() + "://" + url.getHost() + assetUrlLink : assetUrlLink);
    }

    protected void processObjectInfos(URL url, String imgUrlLink, String id, EsoMedia media, Document doc) {
        for (Element info : doc.getElementsByClass("object-info")) {
            for (Element h3 : info.getElementsByTag("h3")) {
                for (Element title : h3.nextElementSibling().getElementsByClass("title")) {
                    Element sibling = title.nextElementSibling();
                    String html = sibling.html();
                    String text = sibling.text();
                    switch (h3.text()) {
                    case "About the Image":
                        processAboutTheImage(url, imgUrlLink, id, media, title, sibling, text);
                        break;
                    case "About the Object":
                        processAboutTheObject(imgUrlLink, media, title, sibling, html, text);
                        break;
                    case "Coordinates":
                        processCoordinates(imgUrlLink, media, title, text);
                        break;
                    default:
                        scrapingError(imgUrlLink);
                    }
                }
                if ("Colours & filters".equals(h3.text())) {
                    processColooursAndFilters(imgUrlLink, media, h3);
                }
            }
        }
    }

    protected void processAboutTheImage(URL url, String imgUrlLink, String id, EsoMedia media, Element title,
            Element sibling, String text) {
        switch (title.text()) {
        case "Id:":
            if (!id.equals(text)) {
                scrapingError(imgUrlLink);
            }
            break;
        case "Type:":
            media.setImageType(EsoMediaType.valueOf(text));
            break;
        case "Release date:":
            try {
                media.setReleaseDate(LocalDateTime.parse(text, dateTimeFormatter));
            } catch (DateTimeParseException e) {
                media.setReleaseDate(LocalDate.parse(text, dateFormatter).atStartOfDay());
            }
            break;
        case "Size:":
            Matcher m = SIZE_PATTERN.matcher(text);
            if (!m.matches()) {
                scrapingError(imgUrlLink);
            }
            media.setWidth(Integer.parseInt(m.group(1)));
            media.setHeight(Integer.parseInt(m.group(2)));
            break;
        case "Field of View:":
            media.setFieldOfView(text);
            break;
        case "Related announcements:":
        case "Related science announcements:":
            media.setRelatedAnnouncements(parseExternalLinks(sibling, url));
            break;
        case "Related releases:":
            media.setRelatedReleases(parseExternalLinks(sibling, url));
            break;
        default:
            scrapingError(imgUrlLink);
        }
    }

    protected void processAboutTheObject(String imgUrlLink, EsoMedia media, Element title, Element sibling,
            String html,
            String text) {
        switch (title.text()) {
        case "Name:":
            media.setName(text);
            break;
        case "Type:":
            media.setTypes(Arrays.stream(html.split("<br>")).map(String::trim).collect(Collectors.toSet()));
            break;
        case "Category:":
            Elements categories = sibling.getElementsByTag("a");
            if (categories.isEmpty()) {
                scrapingError(imgUrlLink);
            }
            media.setCategories(categories.stream().map(Element::text).collect(Collectors.toSet()));
            break;
        case "Distance:":
            media.setDistance(text);
            break;
        case "Constellation:":
            media.setConstellation(text);
            break;
        default:
            scrapingError(imgUrlLink);
        }
    }

    protected void processCoordinates(String imgUrlLink, EsoMedia media, Element title, String text) {
        switch (title.text()) {
        case "Position (RA):":
            media.setPositionRa(text);
            break;
        case "Position (Dec):":
            media.setPositionDec(text);
            break;
        case "Field of view:":
            media.setFieldOfView(text);
            break;
        case "Orientation:":
            media.setOrientation(text);
            break;
        default:
            scrapingError(imgUrlLink);
        }
    }

    protected void processColooursAndFilters(String imgUrlLink, EsoMedia media, Element h3) {
        Element table = h3.nextElementSibling();
        int telescopeIndex = -1;
        int index = 0;
        Elements ths = table.getElementsByTag("th");
        for (Element th : ths) {
            if ("Telescope".equals(th.text())) {
                telescopeIndex = index;
                break;
            }
            index++;
        }
        if (telescopeIndex < 0) {
            scrapingError(imgUrlLink);
        }
        Set<String> telescopes = new HashSet<>();
        Elements tds = table.getElementsByTag("td");
        for (int i = telescopeIndex; i < tds.size(); i += ths.size()) {
            tds.get(i).getElementsByTag("a").forEach(a -> telescopes.add(a.text()));
        }
        if (!telescopes.isEmpty()) {
            // Can be empty, example: https://www.eso.org/public/images/potw1817a/
            media.setTelescopes(telescopes);
        }
    }

    protected Set<String> parseExternalLinks(Element sibling, URL url) {
        return sibling.getElementsByTag("a").stream().map(
                e -> e.outerHtml().replace("href=\"/", "href=\"" + url.getProtocol() + "://" + url.getHost() + "/"))
                .collect(Collectors.toSet());
    }

    @Override
    @Scheduled(fixedRateString = "${eso.update.rate}", initialDelayString = "${initial.delay}")
    public void updateMedia() throws IOException {
        LocalDateTime start = startUpdateMedia();
        int count = 0;
        boolean loop = true;
        int idx = 1;
        while (loop) {
            String urlLink = searchLink.replace("<idx>", Integer.toString(idx++));
            URL url = new URL(urlLink);
            try {
                Document document = Jsoup.connect(urlLink).timeout(60_000).get();
                for (Element script : document.getElementsByTag("script")) {
                    String html = script.html();
                    if (html.startsWith("var images = [")) {
                        String json = html.replaceFirst("var images = ", "").replace(",\n    \n];", "\n    \n]")
                                .replace("id: '", "\"id\": \"").replace("title: '", "\"title\": \"")
                                .replace("width:", "\"width\":").replace("height:", "\"height\":")
                                .replace("src: '", "\"src\": \"").replace("url: '", "\"url\": \"")
                                .replace("potw: '", "\"potw\": \"").replace("',\n", "\",\n").replace("'\n", "\"\n");
                        for (Iterator<EsoFrontPageItem> it = jackson.readerFor(EsoFrontPageItem.class)
                                .readValues(json); it.hasNext();) {
                            if (updateMediaForUrl(url, it.next()).isPresent()) {
                                count++;
                            }
                        }
                    }
                }
            } catch (HttpStatusException e) {
                // End of search when we receive an HTTP 404
                loop = false;
            } catch (IOException | URISyntaxException e) {
                LOGGER.error("Error when fetching " + url, e);
            }
        }
        endUpdateMedia(count, start);
    }

    @Override
    protected Optional<Temporal> getUploadDate(EsoMedia media) {
        return Optional.of(media.getReleaseDate());
    }

    @Override
    protected String getSource(EsoMedia media) throws MalformedURLException {
        return wikiLink(new URL(ESO_BASE_PUBLIC_URL + ESO_IMAGES_PATH + media.getId()), media.getTitle());
    }

    @Override
    protected String getAuthor(EsoMedia media) throws MalformedURLException {
        return media.getCredit();
    }

    @Override
    protected List<String> findTemplates(EsoMedia media) {
        List<String> result = super.findTemplates(media);
        result.add("ESO");
        return result;
    }

    @Override
    protected Set<String> findCategories(EsoMedia media) {
        Set<String> result = super.findCategories(media);
        if (media.getCategories() != null) {
            for (String cat : media.getCategories()) {
                String esoCat = esoCategories.get(cat);
                if (StringUtils.isNotBlank(esoCat)) {
                    result.add(esoCat);
                }
            }
        }
        return result;
    }
}
