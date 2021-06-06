package org.wikimedia.commons.donvip.spacemedia.repo.eso;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
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
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.core.AbstractHarvesterService;
import org.wikimedia.commons.donvip.spacemedia.data.jpa.entity.Depot;
import org.wikimedia.commons.donvip.spacemedia.data.jpa.entity.FilePublication;
import org.wikimedia.commons.donvip.spacemedia.data.jpa.entity.Licence;
import org.wikimedia.commons.donvip.spacemedia.data.jpa.entity.MediaPublication;
import org.wikimedia.commons.donvip.spacemedia.data.jpa.entity.Organization;
import org.wikimedia.commons.donvip.spacemedia.data.jpa.entity.PublicationKey;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class EsoHarvesterService extends AbstractHarvesterService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EsoHarvesterService.class);

    private static final Pattern SIZE_PATTERN = Pattern.compile("([0-9]+) x ([0-9]+) px");

    private static final String ESO_CONTEXT = "ESO";

    @Value("${eso.depot.id}")
    private String depotId;

    @Value("${eso.organization.id}")
    private String orgId;

    @Value("${eso.search.link}")
    private String searchLink;

    @Value("${eso.copyright.path}")
    private String copyrightPath;

    @Value("${eso.object.info.class}")
    private String objectInfoClass;

    @Value("${eso.object.info.title.class}")
    private String objectInfoTitleClass;

    @Value("${eso.localized.url.pattern}")
    private Pattern localizedUrlPattern;

    @Value("${eso.date.pattern}")
    private String datePattern;

    @Value("${eso.datetime.pattern}")
    private String dateTimePattern;

    @Value("${eso.forbidden.categories}")
    private Set<String> forbiddenCategories;

    @Autowired
    private ObjectMapper jackson;

    private DateTimeFormatter dateFormatter;
    private DateTimeFormatter dateTimeFormatter;

    @PostConstruct
    void init() throws IOException {
        dateFormatter = DateTimeFormatter.ofPattern(datePattern, Locale.ENGLISH);
        dateTimeFormatter = DateTimeFormatter.ofPattern(dateTimePattern, Locale.ENGLISH);
    }

    @Override
    public void harvestMedia() throws IOException {
        LocalDateTime start = startUpdateMedia(depotId);
        int count = 0;
        boolean loop = true;
        int idx = 1;
        Depot depot = depotRepository.findById(depotId).orElseThrow();
        Organization org = organizationRepository.findById(orgId).orElseThrow();
        while (loop) {
            String urlLink = searchLink.replace("<idx>", Integer.toString(idx++));
            URL url = new URL(urlLink);
            try {
                for (Iterator<EsoFrontPageItem> it = findFrontPageItems(
                        Jsoup.connect(urlLink).timeout(60_000).get()); it.hasNext();) {
                    if (updateMediaForUrl(url, it.next(), depot, org) != null) {
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
        endUpdateMedia(depotId, count, start);
    }

    protected Set<String> parseExternalLinks(Element sibling, URL url) {
        return sibling.getElementsByTag("a").stream().map(
                e -> e.outerHtml().replace("href=\"/", "href=\"" + url.getProtocol() + "://" + url.getHost() + "/"))
                .collect(Collectors.toSet());
    }

    protected Iterator<EsoFrontPageItem> findFrontPageItems(Document document) throws IOException {
        // eso-website and esa-hubble define their images in a javascript tag
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
        // iau-website defines its images in HTML, see IauHarvesterService impl.
        return Collections.emptyIterator();
    }

    private MediaPublication updateMediaForUrl(URL url, EsoFrontPageItem item, Depot depot, Organization org)
            throws IOException, ReflectiveOperationException {
        String imgUrlLink = url.getProtocol() + "://" + url.getHost() + item.getUrl();
        Matcher m = localizedUrlPattern.matcher(imgUrlLink);
        if (m.matches()) {
            // Switch to international english url
            imgUrlLink = imgUrlLink.replace(m.group(1), "");
        }
        String id = item.getId();
        if (id.length() < 1) {
            scrapingError(imgUrlLink, id);
        }
        MediaPublication media;
        boolean save = false;
        PublicationKey key = new PublicationKey(depotId, id);
        Optional<MediaPublication> mediaInRepo = mediaPublicationRepository.findById(key);
        if (mediaInRepo.isPresent()) {
            media = mediaInRepo.get();
        } else {
            media = fetchMedia(url, key, imgUrlLink, depot, org);
            if (media == null) {
                return null;
            }
            save = true;
        }
        Set<String> categories = media.getMetadataValues(EsoMetadata.CATEGORY.name());
        Set<String> types = media.getMetadataValues(EsoMetadata.TYPE.name());
        // Try to detect pictures of identifiable people, as per ESO conditions
        if (categories.size() == 1 && categories.iterator().next().contains("People")
                && types.stream().allMatch(s -> s.startsWith("Unspecified : People"))) {
            save = ignoreFile(media,
                    "Image likely include a picture of an identifiable person, using that image for commercial purposes is not permitted.");
        } else if (categories.stream().anyMatch(c -> forbiddenCategories.contains(c))) {
            save = ignoreFile(media, "Forbidden category.");
        }
        if (save) {
            media = mediaPublicationRepository.save(media);
        }
        return media;
    }

    private MediaPublication fetchMedia(URL url, PublicationKey key, String imgUrlLink, Depot depot, Organization org)
            throws ReflectiveOperationException, IOException {
        MediaPublication media = new MediaPublication();
        media.setUrl(new URL(imgUrlLink));
        media.setDepot(depot);
        media.addAuthor(org);
        media.setId(key);
        LOGGER.info(imgUrlLink);
        Document html = Jsoup.connect(imgUrlLink).timeout(60_000).get();
        media.setLang(html.getElementsByTag("html").attr("lang"));
        Element div = html.getElementsByClass("col-md-9").last();
        // Find title
        Elements h1s = div.getElementsByTag("h1");
        if (h1s.isEmpty() || h1s.get(0).text().isEmpty()) {
            scrapingError(imgUrlLink, h1s.toString());
        }
        media.setTitle(h1s.get(0).html());
        // Description CAN be empty, see https://www.eso.org/public/images/ann19041a/
        media.setDescription(findDescription(div));
        // Find credit
        media.setCredit(findCredit(div));
        if (media.getCredit().startsWith("<") && !media.getCredit().startsWith("<a")) {
            scrapingError(imgUrlLink, media.getCredit());
        }
        // Check copyright is standard (CC-BY-4.0)
        Elements copyrights = div.getElementsByClass("copyright");
        if (!copyrights.isEmpty()
                && !copyrightPath.equals(copyrights.get(0).getElementsByTag("a").get(0).attr("href"))) {
            LOGGER.error("Invalid copyright for {}", imgUrlLink);
            return null;
        } else {
            media.setLicence(Licence.CC_BY_4_0);
        }

        processObjectInfos(url, imgUrlLink, key.getId(), media, html);

        for (Element link : html.getElementsByClass("archive_dl_text")) {
            Elements innerLinks = link.getElementsByTag("a");
            if (innerLinks.isEmpty()) {
                // Invalid page, see https://www.spacetelescope.org/images/heic1103a/
                innerLinks = link.parent().nextElementSiblings();
            }
            String assetUrlLink = innerLinks.get(0).attr("href");
            // Skip zoomable tiled images, example:
            // https://www.spacetelescope.org/images/potw2122a/
            if (!"zoomable/".equals(assetUrlLink)) {
                URL assetUrl = buildAssetUrl(assetUrlLink, url);
                media.addFilePublication(
                        filePublicationRepository.save(new FilePublication(depot, assetUrlLink, assetUrl)));
                if (assetUrlLink.contains("/screen/") && media.getThumbnailUrl() == null) {
                    media.setThumbnailUrl(assetUrl);
                }
            }
        }
        return media;
    }

    protected static URL buildAssetUrl(String assetUrlLink, URL url) throws MalformedURLException {
        return new URL(
                assetUrlLink.startsWith("/") ? url.getProtocol() + "://" + url.getHost() + assetUrlLink : assetUrlLink);
    }

    protected String findDescription(Element div) {
        // Different implementation in IauHarvesterService
        StringBuilder description = new StringBuilder();
        for (Element p : div.getElementsByTag("div").get(1).nextElementSiblings()) {
            if ("script".equals(p.tagName())) {
                continue;
            } else if (!"p".equals(p.tagName())) {
                break;
            }
            description.append(p.html());
        }
        return description.toString();
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

    protected void processObjectInfos(URL url, String imgUrlLink, String id, MediaPublication media, Document doc)
            throws MalformedURLException {
        for (Element info : doc.getElementsByClass(objectInfoClass)) {
            for (Element h3 : info.getElementsByTag("h3")) {
                for (Element title : h3.nextElementSibling().getElementsByClass(objectInfoTitleClass)) {
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

    protected void processAboutTheImage(URL url, String imgUrlLink, String id, MediaPublication media,
            Element title, Element sibling, String text) throws MalformedURLException {
        switch (title.text()) {
        case "Id:":
            if (!id.equals(text)) {
                problem(new URL(imgUrlLink), "Different ids: " + id + " <> " + text);
            }
            break;
        case "Type:":
            addEsoMetadata(media, EsoMetadata.MEDIA_TYPE, EsoMediaType.valueOf(text).name());
            break;
        case "Release date:":
            try {
                media.setPublicationDateTime(LocalDateTime.parse(text, dateTimeFormatter).atZone(ZoneOffset.UTC));
            } catch (DateTimeParseException e) {
                media.setPublicationDateTime(LocalDate.parse(text, dateFormatter).atStartOfDay(ZoneOffset.UTC));
            }
            break;
        case "Size:":
            Matcher m = SIZE_PATTERN.matcher(text);
            if (!m.matches()) {
                scrapingError(imgUrlLink, text);
            }
            addEsoMetadata(media, EsoMetadata.WIDTH, Integer.valueOf(m.group(1)).toString());
            addEsoMetadata(media, EsoMetadata.HEIGHT, Integer.valueOf(m.group(2)).toString());
            break;
        case "Field of View:":
            addEsoMetadata(media, EsoMetadata.FIELD_OF_VIEW, text);
            break;
        case "Related announcements:":
        case "Related science announcements:":
            addEsoMetadata(media, EsoMetadata.RELATED_ANNOUNCEMENT, parseExternalLinks(sibling, url));
            break;
        case "Related releases:":
            addEsoMetadata(media, EsoMetadata.RELATED_RELEASE, parseExternalLinks(sibling, url));
            break;
        case "Language:":
            // Ignored
            break;
        default:
            scrapingError(imgUrlLink, title.text());
        }
    }

    protected void processAboutTheObject(String imgUrlLink, MediaPublication media, Element title,
            Element sibling, String html, String text) {
        switch (title.text()) {
        case "Name:":
            addEsoMetadata(media, EsoMetadata.NAME, text);
            break;
        case "Type:":
            addEsoMetadata(media, EsoMetadata.TYPE,
                    Arrays.stream(html.split("<br>")).map(String::trim).collect(Collectors.toSet()));
            break;
        case "Category:":
            Elements categories = sibling.getElementsByTag("a");
            if (categories.isEmpty()) {
                scrapingError(imgUrlLink, sibling.html());
            }
            addEsoMetadata(media, EsoMetadata.CATEGORY,
                    categories.stream().map(Element::text).collect(Collectors.toSet()));
            break;
        case "Distance:":
            addEsoMetadata(media, EsoMetadata.DISTANCE, text);
            break;
        case "Constellation:":
            addEsoMetadata(media, EsoMetadata.CONSTELLATION, text);
            break;
        default:
            scrapingError(imgUrlLink, title.text());
        }
    }

    protected void processCoordinates(String imgUrlLink, MediaPublication media, Element title, String text) {
        switch (title.text()) {
        case "Position (RA):":
            addEsoMetadata(media, EsoMetadata.POSITION_RA, text);
            break;
        case "Position (Dec):":
            addEsoMetadata(media, EsoMetadata.POSITION_DEC, text);
            break;
        case "Field of view:":
            addEsoMetadata(media, EsoMetadata.FIELD_OF_VIEW, text);
            break;
        case "Orientation:":
            addEsoMetadata(media, EsoMetadata.ORIENTATION, text);
            break;
        default:
            scrapingError(imgUrlLink, title.text());
        }
    }

    protected void processColoursAndFilters(String imgUrlLink, MediaPublication media, Element h3) {
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
        // Can be empty, example: https://www.eso.org/public/images/potw1817a/
        addEsoMetadata(media, EsoMetadata.TELESCOPE, telescopes);
    }

    protected void addEsoMetadata(MediaPublication media, EsoMetadata key, Collection<String> values) {
        values.forEach(x -> addEsoMetadata(media, key, x));
    }

    protected void addEsoMetadata(MediaPublication media, EsoMetadata key, String value) {
        media.addMetadata(metadataRepository.findOrCreate(ESO_CONTEXT, key.name(), value));
    }

    protected static void scrapingError(String url, String details) {
        throw new IllegalStateException("ESO scraping code must be updated, see " + url + " - Details: " + details);
    }
}
