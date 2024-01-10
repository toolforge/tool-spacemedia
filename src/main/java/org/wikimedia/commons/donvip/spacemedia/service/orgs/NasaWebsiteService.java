package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import static java.lang.Integer.parseInt;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toSet;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.getWithJsoup;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.newURL;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.uriExists;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.HttpStatusException;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.CompositeMediaId;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.ImageDimensions;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.website.NasaWebsiteMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.website.NasaWebsiteMediaRepository;

@Service
public class NasaWebsiteService extends AbstractOrgHtmlGalleryService<NasaWebsiteMedia> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NasaWebsiteService.class);

    private static final String BASE_URL = "https://www.nasa.gov/";

    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH);

    private static final Pattern SIZE_PATTERN = Pattern.compile("(\\d+)x(\\d+)px");

    private static final String IMAGE_ARTICLE = "image-article";
    private static final String IMAGE_DETAIL = "image-detail";
    private static final String VIDEO_DETAIL = "video-detail";

    private static final Pattern DATE_LINK_PATTERN = Pattern
            .compile(BASE_URL + "wp-content/uploads/([12]\\d{3})/(\\d{2})/.*");

    // To ignore blank value in Excel date systems...
    private static final Set<LocalDate> IGNORED_DATES = Set.of(LocalDate.of(1900, 1, 1), LocalDate.of(1904, 1, 1));

    public NasaWebsiteService(NasaWebsiteMediaRepository repository) {
        super(repository, "nasa.website", Set.of(IMAGE_ARTICLE, IMAGE_DETAIL, VIDEO_DETAIL));
    }

    @Override
    public String getName() {
        return "NASA (Website)";
    }

    @Override
    protected List<String> fetchGalleryUrls(String repoId) {
        if (IMAGE_ARTICLE.equals(repoId)) {
            return List.of(BASE_URL + repoId);
        }
        int idx = 1;
        boolean loop = true;
        List<String> result = new ArrayList<>();
        do {
            String pageUrl = BASE_URL + "gallery/page/" + idx++;
            try {
                Elements items = getGalleryItems(getWithJsoup(pageUrl, 30_000, 3));
                items.forEach(item -> result.add(item.attr("href")));
                loop = !items.isEmpty();
            } catch (HttpStatusException e) {
                if (e.getStatusCode() == 404) {
                    break;
                }
                LOGGER.error("Error while fetching {}", pageUrl, e);
            } catch (IOException | RuntimeException e) {
                LOGGER.error("Error while fetching {}", pageUrl, e);
            }
        } while (loop);
        return result;
    }

    @Override
    protected String getGalleryPageUrl(String galleryUrl, int page) {
        return galleryUrl.startsWith("https://www.nasa.gov/gallery/") ? galleryUrl : galleryUrl + "/page/" + page;
    }

    @Override
    protected Elements getGalleryItems(Element html) {
        Elements headings = html.getElementsByClass("hds-content-item-heading");
        return headings.isEmpty() ? html.getElementsByClass("hds-gallery-item-link") : headings;
    }

    @Override
    protected String extractIdFromGalleryItem(Element result) {
        String href = result.attr("href");
        if (href.endsWith("/")) {
            href = href.substring(0, href.length() - 1);
        }
        return href.substring(href.lastIndexOf('/') + 1);
    }

    @Override
    protected boolean loop(String repoId, Elements results) {
        return IMAGE_ARTICLE.equals(repoId) && !results.isEmpty();
    }

    @Override
    protected String getSourceUrl(CompositeMediaId id) {
        String url = BASE_URL + IMAGE_DETAIL + '/' + id.getMediaId() + '/';
        if (uriExists(url)) {
            return url;
        }
        url = BASE_URL + id.getRepoId() + '/' + id.getMediaId() + '/';
        if (uriExists(url)) {
            return url;
        }
        return null;
    }

    @Override
    protected void fillMediaWithHtml(String url, Document html, NasaWebsiteMedia media) throws IOException {
        try {
            if (url.startsWith(BASE_URL + IMAGE_ARTICLE + '/')) {
                Element detail = html.getElementsByAttributeValueMatching("href", BASE_URL + IMAGE_DETAIL + "/.*")
                        .first();
                if (detail != null) {
                    fillMediaWithHtmlDetail(IMAGE_DETAIL, detail, media);
                } else {
                    detail = html.getElementsByAttributeValueMatching("href", BASE_URL + VIDEO_DETAIL + "/.*").first();
                    if (detail != null) {
                        fillMediaWithHtmlDetail(VIDEO_DETAIL, detail, media);
                    } else {
                        fillMediaWithHtmlImageArticle(html, media);
                    }
                }
            } else {
                fillMediaWithHtmlImageDetail(html, media);
            }
        } catch (RuntimeException e) {
            LOGGER.error("Failed to parse HTML for {} => {}", media, html.html());
            throw e;
        }
    }

    private void fillMediaWithHtmlDetail(String repoId, Element detail, NasaWebsiteMedia media) throws IOException {
        media.setId(new CompositeMediaId(repoId, extractIdFromGalleryItem(detail)));
        String url = detail.attr("href");
        if (uriExists(url)) {
            fillMediaWithHtmlImageDetail(getWithJsoup(url, 10_000, 5), media);
        }
    }

    protected void fillMediaWithHtmlImageArticle(Document html, NasaWebsiteMedia media) {
        Element section = html.getElementsByTag("section").first();
        media.setPublicationDateTime(
                ZonedDateTime.parse((html.getElementsByAttributeValue("name", "parsely-pub-date").attr("content"))));
        media.setTitle(section.getElementsByTag("h1").first().text());
        Element content = section.getElementsByClass("usa-article-content").first();
        media.setThumbnailUrl(
                newURL(content.getElementsByTag("figure").first().getElementsByTag("img").first().attr("src")));
        media.setDescription(content.getElementsByClass("entry-content").first().text());
        int idx = media.getDescription().lastIndexOf("redit:");
        if (idx > -1) {
            media.setCredits(media.getDescription().substring(idx + 6).trim());
        }
        for (Element figure : content.getElementsByTag("figure")) {
            String src = figure.getElementsByTag("img").first().attr("src");
            idx = src.indexOf('?');
            addMetadata(media, idx > -1 ? src.substring(0, idx) : src, null);
        }
    }

    protected void fillMediaWithHtmlImageDetail(Document html, NasaWebsiteMedia media) {
        Element section = html.getElementsByTag("section").first();
        ofNullable(html.getElementsByAttributeValue("property", "og:updated_time").first())
                .map(x -> ZonedDateTime.parse(x.attr("content")))
                .or(() -> ofNullable(html.getElementsByAttributeValue("name", "parsely-pub-date").first())
                        .map(x -> LocalDateTime.parse(x.attr("content")).atZone(ZoneId.of("UTC-4"))))
                .ifPresent(media::setPublicationDateTime);
        media.setTitle(section.getElementsByClass("hds-attachment-single__title").first().text());
        ofNullable(section.getElementsByTag("img").first())
                .ifPresent(x -> media.setThumbnailUrl(newURL(x.attr("src"))));
        ofNullable(section.getElementsByClass("hds-attachment-single__caption").first())
                .ifPresent(x -> media.setDescription(x.text()));
        ofNullable(section.getElementsByClass("hds-tags").first()).ifPresent(
                tags -> media.setKeywords(tags.getElementsByTag("a").stream().map(Element::text).collect(toSet())));
        String assetUri = section.getElementsByClass("hds-button-download").attr("href");
        FileMetadata fm = addMetadata(media, assetUri, null);
        for (Element item : section.getElementsByClass("hds-attachment-single__meta-item")) {
            String value = item.getElementsByClass("hds-attachment-single__meta-item-value").first().text();
            switch (item.getElementsByClass("hds-attachment-single__meta-item-key").first().text()) {
            case "Taken":
                LocalDate date = LocalDate.parse(value, dateFormatter);
                if (!IGNORED_DATES.contains(date)) {
                    media.setCreationDate(date);
                }
                if (media.getPublicationDate() == null) {
                    media.setPublicationDate(date);
                }
                break;
            case "Image Credit", "Producer":
                media.setCredits(value);
                break;
            case "Size":
                Matcher m = SIZE_PATTERN.matcher(value);
                if (m.matches()) {
                    fm.setImageDimensions(new ImageDimensions(parseInt(m.group(1)), parseInt(m.group(2))));
                }
                break;
            default:
                break;
            }
        }
        if (media.getPublicationDate() == null) {
            Matcher m = DATE_LINK_PATTERN.matcher(assetUri);
            if (m.matches()) {
                media.setPublicationDate(LocalDate.of(parseInt(m.group(1)), parseInt(m.group(2)), 1));
            }
        }
    }

    @Override
    public URL getSourceUrl(NasaWebsiteMedia media, FileMetadata metadata) {
        return newURL(BASE_URL + media.getId().getRepoId() + "/" + media.getIdUsedInOrg());
    }

    @Override
    public Set<String> findLicenceTemplates(NasaWebsiteMedia media, FileMetadata metadata) {
        Set<String> result = super.findLicenceTemplates(media, metadata);
        result.add("PD-USGov-NASA");
        return result;
    }

    @Override
    protected NasaWebsiteMedia refresh(NasaWebsiteMedia media) throws IOException {
        return media.copyDataFrom(fetchMedia(media.getId(), empty()));
    }

    @Override
    protected Class<NasaWebsiteMedia> getMediaClass() {
        return NasaWebsiteMedia.class;
    }

    @Override
    protected Set<String> getTwitterAccounts(NasaWebsiteMedia uploadedMedia) {
        return Set.of("@NASA");
    }
}
