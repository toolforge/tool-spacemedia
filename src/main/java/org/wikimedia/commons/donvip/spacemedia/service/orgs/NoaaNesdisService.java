package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.getFilename;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import org.wikimedia.commons.donvip.spacemedia.service.wikimedia.GlitchTip;

@Service
public class NoaaNesdisService extends AbstractOrgHtmlGalleryService<NoaaNesdisMedia> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NoaaNesdisService.class);

    private static final String BASE_URL = "https://www.nesdis.noaa.gov/";

    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH);

    private static final Pattern WEBP_PATTERN = Pattern
            .compile("https://www\\.nesdis\\.noaa\\.gov/s3/styles/webp/s3/(.+\\..+)\\.webp");

    public NoaaNesdisService(NoaaNesdisMediaRepository repository) {
        super(repository, "noaa.nesdis", Set.of("nesdis"));
    }

    @Override
    public String getName() {
        return "NOAA (NESDIS)";
    }

    @Override
    protected String hiddenUploadCategory(String repoId) {
        return "Spacemedia NESDIS files uploaded by " + commonsService.getAccount();
    }

    @Override
    protected List<String> fetchGalleryUrls(String repoId) {
        List<String> result = new ArrayList<>();
        result.addAll(fetchGalleryUrls(repoId, "real-time-imagery/imagery-collections"));
        result.addAll(fetchGalleryUrls(repoId, "real-time-imagery/videos-animations-media/argos-4-image-gallery"));
        result.add("real-time-imagery/videos-animations-media/satellite-launches");
        return result;
    }

    private List<String> fetchGalleryUrls(String repoId, String path) {
        String pageUrl = (path.startsWith("http") ? "" : BASE_URL) + path;
        try {
            return getGalleryItems(repoId, pageUrl, getWithJsoup(pageUrl, 30_000, 3)).stream()
                    .map(item -> item.getElementsByTag("a").first().attr("href"))
                    .filter(x -> !x.contains("://") || x.startsWith(BASE_URL))
                    .toList();
        } catch (IOException | RuntimeException e) {
            LOGGER.error("Error while fetching {}", pageUrl, e);
            GlitchTip.capture(e);
            return List.of();
        }
    }

    @Override
    protected String getGalleryPageUrl(String galleryUrl, int page) {
        return (galleryUrl.startsWith("http") ? "" : BASE_URL) + galleryUrl + "?page=" + (page - 1);
    }

    @Override
    protected Elements getGalleryItems(String repoId, String url, Element html) {
        Element itemList = html.getElementsByClass("item-list").first();
        return itemList == null ? html.getElementsByClass("cards--media") : itemList.getElementsByTag("li");
    }

    @Override
    protected String extractIdFromGalleryItem(String url, Element result) {
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
    protected List<NoaaNesdisMedia> fillMediaWithHtml(String url, Document html, Element galleryItem,
            NoaaNesdisMedia media) throws IOException {
        try {
            media.setTitle(html.getElementsByTag("h1").first().text());
            media.setPublicationDate(
                    LocalDate.parse(html.getElementsByClass("news-date").first().text(), dateFormatter));
            html.getElementsByClass("call-to-action blue")
                    .stream().filter(x -> x.text().contains("Download"))
                    .forEach(x -> addMetadata(media, nesdisUrl(x.attr("href")), null));
            html.getElementsByClass("paragraph--type--image").forEach(img -> {
                String imgUrl = nesdisUrl(img.getElementsByTag("img").first().attr("src").split("\\?")[0]);
                if (!media.containsMetadata(imgUrl) && !media.containsMetadataWithFilename(getFilename(imgUrl))) {
                    addMetadata(media, imgUrl, fm -> ofNullable(img.getElementsByClass("caption").first())
                            .ifPresent(c -> fm.setDescription(c.text())));
                }
            });
            Element descItem = html.getElementsByClass("field--type-text-long").first();
            if (descItem == null) {
                descItem = html.getElementsByClass("paragraph--type--map-locator").first();
            }
            if (descItem == null) {
                descItem = html.getElementsByClass("paragraph--type--text-block").first();
            }
            if (descItem != null) {
                media.setDescription(descItem.text());
            }
            return List.of(media);
        } catch (RuntimeException e) {
            LOGGER.error("Failed to parse HTML for {} => {}", media, html.html());
            GlitchTip.capture(e);
            throw e;
        }
    }

    private static String nesdisUrl(String href) {
        String result = href.contains("://") ? href : (BASE_URL + href).replace("//", "/").replace(":/", "://");
        Matcher m = WEBP_PATTERN.matcher(result);
        return m.matches() ? "https://nesdis-prod.s3.amazonaws.com/" + m.group(1)
                : result.replace("/s3dl?path=/s3/", "/s3/");
    }

    @Override
    protected boolean checkBlocklist() {
        return false;
    }

    @Override
    protected boolean isSatellitePicture(NoaaNesdisMedia media, FileMetadata metadata) {
        return true;
    }

    @Override
    public Set<String> findCategories(NoaaNesdisMedia media, FileMetadata metadata, boolean includeHidden) {
        Set<String> result = super.findCategories(media, metadata, includeHidden);
        boolean viirs = media.containsInTitleOrDescriptionOrKeywords("VIIRS",
                "Visible Infrared Imaging Radiometer Suite");
        boolean npp = media.containsInTitleOrDescriptionOrKeywords("NPP", "National Polar-orbiting");
        boolean noaa20 = media.containsInTitleOrDescriptionOrKeywords("NOAA-20");
        boolean noaa21 = media.containsInTitleOrDescriptionOrKeywords("NOAA-21");
        if (viirs) {
            if (!npp && !noaa20 && !noaa21) {
                result.add("Photos by VIIRS");
            }
            if (npp) {
                result.add("Photos by VIIRS (Suomi NPP)");
            }
            if (noaa20) {
                result.add("Photos by VIIRS (NOAA-20)");
            }
            if (noaa21) {
                result.add("Photos by VIIRS (NOAA-21)");
            }
        } else {
            if (npp) {
                result.add("Photos by Suomi NPP");
            }
            if (noaa20) {
                result.add("Satellite pictures by NOAA-20");
            }
            if (noaa21) {
                result.add("Satellite pictures by NOAA-21");
            }
        }
        if (media.containsInTitleOrDescriptionOrKeywords("GOES")) {
            result.add("GOES pictures");
            for (int i = 1; i <= 20; i++) {
                if (media.containsInTitleOrDescriptionOrKeywords("GOES " + i, "GOES-" + i)) {
                    result.add("GOES " + i + " pictures");
                    result.remove("GOES pictures");
                }
            }
        }
        if (media.containsInTitleOrDescriptionOrKeywords("Himawari")) {
            result.add("Images by Himawari satellites");
            for (int i = 1; i <= 10; i++) {
                if (media.containsInTitleOrDescriptionOrKeywords("Himawari " + i, "Himawari-" + i)) {
                    result.add("Himawari " + i + " images");
                    result.remove("Images by Himawari satellites");
                }
            }
        }
        return result;
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
}
