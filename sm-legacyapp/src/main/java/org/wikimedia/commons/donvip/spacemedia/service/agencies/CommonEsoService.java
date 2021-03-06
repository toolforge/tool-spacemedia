package org.wikimedia.commons.donvip.spacemedia.service.agencies;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.Temporal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
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
import org.wikimedia.commons.donvip.spacemedia.data.domain.Metadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.eso.CommonEsoMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.eso.CommonEsoMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.repo.eso.EsoFrontPageItem;
import org.wikimedia.commons.donvip.spacemedia.repo.eso.EsoMediaType;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.micrometer.core.instrument.util.StringUtils;

public abstract class CommonEsoService<T extends CommonEsoMedia>
        extends AbstractFullResAgencyService<T, String, LocalDateTime, T, String, LocalDateTime> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommonEsoService.class);

    private static final Pattern SIZE_PATTERN = Pattern.compile("([0-9]+) x ([0-9]+) px");

    private final Class<T> mediaClass;
    private final String searchLink;

    private DateTimeFormatter dateFormatter;
    private DateTimeFormatter dateTimeFormatter;

    @Value("${eso.date.pattern}")
    private String datePattern;

    @Value("${eso.datetime.pattern}")
    private String dateTimePattern;

    private Map<String, String> esoCategories;
    private Map<String, String> esoNames;
    private Map<String, String> esoTypes;

    @Autowired
    private ObjectMapper jackson;

    public CommonEsoService(CommonEsoMediaRepository<T> repository, String id, String searchLink, Class<T> mediaClass) {
        super(repository, id);
        this.searchLink = Objects.requireNonNull(searchLink);
        this.mediaClass = Objects.requireNonNull(mediaClass);
    }

    @Override
    @PostConstruct
    void init() throws IOException {
        super.init();
        dateFormatter = DateTimeFormatter.ofPattern(datePattern, Locale.ENGLISH);
        dateTimeFormatter = DateTimeFormatter.ofPattern(dateTimePattern, Locale.ENGLISH);
        esoCategories = loadCsvMapping("eso.categories.csv");
        esoNames = loadCsvMapping("eso.names.csv");
        esoTypes = loadCsvMapping("eso.types.csv");
    }

    @Scheduled(fixedDelay = 43200000L)
    public void checkEsoCategories() {
        checkCommonsCategories(esoCategories);
        checkCommonsCategories(esoTypes);
    }

    @Override
    protected final String getMediaId(String id) {
        return id;
    }

    private static void scrapingError(String url, String details) {
        throw new IllegalStateException("ESO scraping code must be updated, see " + url + " - Details: " + details);
    }

    protected abstract Matcher getLocalizedUrlMatcher(String imgUrlLink);

    private Optional<T> updateMediaForUrl(URL url, EsoFrontPageItem item)
            throws IOException, ReflectiveOperationException {
        String imgUrlLink = url.getProtocol() + "://" + url.getHost() + item.getUrl();
        Matcher m = getLocalizedUrlMatcher(imgUrlLink);
        if (m.matches()) {
            // Switch to international english url
            imgUrlLink = imgUrlLink.replace(m.group(1), "");
        }
        String id = item.getId();
        if (id.length() < 1) {
            scrapingError(imgUrlLink, id);
        }
        T media;
        boolean save = false;
        Optional<T> mediaInRepo = repository.findById(id);
        if (mediaInRepo.isPresent()) {
            media = mediaInRepo.get();
        } else {
            media = fetchMedia(url, id, imgUrlLink);
            if (media == null) {
                return Optional.empty();
            }
            save = true;
        }
        if (media.getCategories() != null) {
            // Try to detect pictures of identifiable people, as per ESO conditions
            if (media.getCategories().size() == 1 && media.getCategories().iterator().next().contains(
                    "People")
                    && media.getTypes() != null
                    && media.getTypes().stream().allMatch(s -> s.startsWith("Unspecified : People"))) {
                save = ignoreFile(media,
                        "Image likely include a picture of an identifiable person, using that image for commercial purposes is not permitted.");
            } else if (media.getCategories().stream().anyMatch(c -> getForbiddenCategories().contains(c))) {
                save = ignoreFile(media, "Forbidden category.");
            }
        }
        if (doCommonUpdate(media)) {
            save = true;
        }
        if (save) {
            media = repository.save(media);
        }
        return Optional.of(media);
    }

    private T fetchMedia(URL url, String id, String imgUrlLink) throws ReflectiveOperationException, IOException {
        T media = mediaClass.getConstructor().newInstance();
        media.setId(id);
        LOGGER.info(imgUrlLink);
        Document html = Jsoup.connect(imgUrlLink).timeout(60_000).get();
        Element div = html.getElementsByClass("col-md-9").last();
        // Find title
        Elements h1s = div.getElementsByTag("h1");
        if (h1s.isEmpty() || h1s.get(0).text().isEmpty()) {
            scrapingError(imgUrlLink, h1s.toString());
        }
        media.setTitle(h1s.get(0).html());
        // Description CAN be empty, see https://www.eso.org/public/images/ann19041a/
        media.setDescription(findDescription(div).toString());
        // Find credit
        media.setCredit(findCredit(div));
        if (media.getCredit().startsWith("<") && !media.getCredit().startsWith("<a")) {
            scrapingError(imgUrlLink, media.getCredit());
        }
        // Check copyright is standard (CC-BY-4.0)
        Elements copyrights = div.getElementsByClass("copyright");
        if (!copyrights.isEmpty()
                && !getCopyrightLink().equals(copyrights.get(0).getElementsByTag("a").get(0).attr("href"))) {
            LOGGER.error("Invalid copyright for {}", imgUrlLink);
            return null;
        }

        processObjectInfos(url, imgUrlLink, id, media, html);
        Metadata metadata = media.getMetadata();
        Metadata frMetadata = media.getFullResMetadata();

        for (Element link : html.getElementsByClass("archive_dl_text")) {
            Elements innerLinks = link.getElementsByTag("a");
            if (innerLinks.isEmpty()) {
                // Invalid page, see https://www.spacetelescope.org/images/heic1103a/
                innerLinks = link.parent().nextElementSiblings();
            }
            String assetUrlLink = innerLinks.get(0).attr("href");
            if (assetUrlLink.endsWith(".psb")) {
                continue; // format not supported by Wikimedia Commons
            } else if (assetUrlLink.contains("/original/")) {
                frMetadata.setAssetUrl(buildAssetUrl(assetUrlLink, url));
            } else if (assetUrlLink.contains("/large/")) {
                metadata.setAssetUrl(buildAssetUrl(assetUrlLink, url));
            } else if (assetUrlLink.contains("/screen/")) {
                media.setThumbnailUrl(buildAssetUrl(assetUrlLink, url));
            } else if (frMetadata.getAssetUrl() == null && assetUrlLink.contains(".tif")) {
                frMetadata.setAssetUrl(buildAssetUrl(assetUrlLink, url));
            } else if (metadata.getAssetUrl() == null && assetUrlLink.contains(".jp")) {
                metadata.setAssetUrl(buildAssetUrl(assetUrlLink, url));
            } else if (metadata.getAssetUrl() != null && frMetadata.getAssetUrl() != null
                    && media.getThumbnailUrl() != null) {
                break;
            }
        }
        return media;
    }

    protected Collection<String> getForbiddenCategories() {
        return Collections.emptyList();
    }

    protected String findCredit(Element div) {
        Element credit = div.getElementsByClass("credit").get(0);
        Elements children = credit.children();
        if (children.size() == 1
                && ("p".equals(children.get(0).tagName()) || "div".equals(children.get(0).tagName()))) {
            credit = children.get(0);
        }
        Elements links = credit.getElementsByTag("a");
        for (Element a : links) {
            for (Iterator<Attribute> it = a.attributes().iterator(); it.hasNext();) {
                if (!"href".equals(it.next().getKey())) {
                    it.remove();
                }
            }
        }
        return links.isEmpty() ? credit.text()
                : credit.html().replaceAll("</?span[^>]*>", "").replace("<br>", ". ").replaceAll("</?p[^>]*>", "")
                        .replaceAll("</?strong>", "");
    }

    protected StringBuilder findDescription(Element div) {
        StringBuilder description = new StringBuilder();
        for (Element p : div.getElementsByTag("div").get(1).nextElementSiblings()) {
            if ("script".equals(p.tagName())) {
                continue;
            } else if (!"p".equals(p.tagName())) {
                break;
            }
            description.append(p.html());
        }
        return description;
    }

    protected abstract String getCopyrightLink();

    private static URL buildAssetUrl(String assetUrlLink, URL url) throws MalformedURLException {
        return new URL(
                assetUrlLink.startsWith("/") ? url.getProtocol() + "://" + url.getHost() + assetUrlLink : assetUrlLink);
    }

    protected String getObjectInfoClass() {
        return "object-info";
    }

    protected String getObjectInfoTitleClass() {
        return "title";
    }

    protected void processObjectInfos(URL url, String imgUrlLink, String id, T media, Document doc)
            throws MalformedURLException {
        for (Element info : doc.getElementsByClass(getObjectInfoClass())) {
            for (Element h3 : info.getElementsByTag("h3")) {
                for (Element title : h3.nextElementSibling().getElementsByClass(getObjectInfoTitleClass())) {
                    Element sibling = title.nextElementSibling();
                    if (!sibling.tagName().equals(title.tagName())) {
                        sibling = sibling.nextElementSibling();
                    }
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
                        scrapingError(imgUrlLink, h3.text());
                    }
                }
                if ("Colours & filters".equals(h3.text())) {
                    processColoursAndFilters(imgUrlLink, media, h3);
                }
            }
        }
    }

    protected void processAboutTheImage(URL url, String imgUrlLink, String id, T media, Element title,
            Element sibling, String text) throws MalformedURLException {
        switch (title.text()) {
        case "Id:":
            if (!id.equals(text)) {
                problem(new URL(imgUrlLink), "Different ids: " + id + " <> " + text);
            }
            break;
        case "Type:":
            media.setImageType(EsoMediaType.valueOf(text));
            break;
        case "Release date:":
            try {
                media.setDate(LocalDateTime.parse(text, dateTimeFormatter));
            } catch (DateTimeParseException e) {
                media.setDate(LocalDate.parse(text, dateFormatter).atStartOfDay());
            }
            break;
        case "Size:":
            Matcher m = SIZE_PATTERN.matcher(text);
            if (!m.matches()) {
                scrapingError(imgUrlLink, text);
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
        case "Language:":
            // Ignored
            break;
        default:
            scrapingError(imgUrlLink, title.text());
        }
    }

    protected void processAboutTheObject(String imgUrlLink, T media, Element title, Element sibling,
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
                scrapingError(imgUrlLink, sibling.html());
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
            scrapingError(imgUrlLink, title.text());
        }
    }

    protected void processCoordinates(String imgUrlLink, T media, Element title, String text) {
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
            scrapingError(imgUrlLink, title.text());
        }
    }

    protected void processColoursAndFilters(String imgUrlLink, T media, Element h3) {
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
            scrapingError(imgUrlLink, table.html());
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

    protected Iterator<EsoFrontPageItem> findFrontPageItems(Document document) throws IOException {
        for (Element script : document.getElementsByTag("script")) {
            String html = script.html();
            if (html.startsWith("var images = [")) {
                String json = html.replaceFirst("var images = ", "").replace(",\n    \n];", "\n    \n]")
                        .replace("id: '", "\"id\": \"").replace("title: '", "\"title\": \"")
                        .replace("width:", "\"width\":").replace("height:", "\"height\":")
                        .replace("src: '", "\"src\": \"").replace("url: '", "\"url\": \"")
                        .replace("potw: '", "\"potw\": \"").replace("',\n", "\",\n").replace("'\n", "\"\n");
                return jackson.readerFor(EsoFrontPageItem.class).readValues(json);
            }
        }
        return Collections.emptyIterator();
    }

    protected void doUpdateMedia() throws IOException {
        LocalDateTime start = startUpdateMedia();
        int count = 0;
        boolean loop = true;
        int idx = 1;
        while (loop) {
            String urlLink = searchLink.replace("<idx>", Integer.toString(idx++));
            URL url = new URL(urlLink);
            try {
                for (Iterator<EsoFrontPageItem> it = findFrontPageItems(
                        Jsoup.connect(urlLink).timeout(60_000).get()); it.hasNext();) {
                    if (updateMediaForUrl(url, it.next()).isPresent()) {
                        count++;
                    }
                }
            } catch (HttpStatusException e) {
                // End of search when we receive an HTTP 404
                loop = false;
            } catch (IOException | ReflectiveOperationException | RuntimeException e) {
                LOGGER.error("Error when fetching " + url, e);
            }
        }
        endUpdateMedia(count, start);
    }

    @Override
    protected final Optional<Temporal> getUploadDate(T media) {
        return Optional.of(media.getDate());
    }

    @Override
    protected String getAuthor(T media) throws MalformedURLException {
        return media.getCredit();
    }

    @Override
    public Set<String> findCategories(T media, boolean includeHidden) {
        Set<String> result = super.findCategories(media, includeHidden);
        if (media.getCategories() != null) {
            result.addAll(media.getCategories().stream().map(esoCategories::get).filter(StringUtils::isNotBlank)
                    .collect(Collectors.toSet()));
        }
        if (media.getCategories() != null) {
            result.addAll(media.getTypes().stream().map(esoTypes::get).filter(StringUtils::isNotBlank)
                    .collect(Collectors.toSet()));
        }
        if (media.getName() != null) {
            String catName = esoNames.get(media.getName());
            if (StringUtils.isNotBlank(catName)) {
                result.add(catName);
            }
        }
        return result;
    }
}
