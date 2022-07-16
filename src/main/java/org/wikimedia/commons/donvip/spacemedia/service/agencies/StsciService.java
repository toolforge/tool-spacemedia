package org.wikimedia.commons.donvip.spacemedia.service.agencies;

import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.net.URL;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.hubble.HubbleNasaImageFiles;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.hubble.HubbleNasaImageResponse;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.hubble.HubbleNasaNewsReleaseResponse;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.hubble.HubbleNasaNewsResponse;

@Service
public class StsciService {

    public static final DateTimeFormatter releaseDateformatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy h:mma (zz)",
            Locale.ENGLISH);

    private static final Pattern FILE_DOWNLOAD_TEXT = Pattern
            .compile("(?:(?:(\\d+) X (\\d+))|(?:Text Description)), (PDF|PNG|TIF) \\((\\d+\\.\\d+) (KB|MB|GB)\\)");

    private static final Logger LOGGER = LoggerFactory.getLogger(StsciService.class);

    public HubbleNasaNewsResponse[] fetchNewsByScrapping(String urlLink) throws IOException {
        Document html = fetchHtml(new URL(urlLink));
        Elements divs = html.getElementsByClass("news-listing");
        HubbleNasaNewsResponse[] result = new HubbleNasaNewsResponse[divs.size()];
        int i = 0;
        for (Element div : divs) {
            HubbleNasaNewsResponse news = new HubbleNasaNewsResponse();
            news.setId(div.getElementsByClass("news-release-id").first().ownText().substring(12));
            news.setName(div.getElementsByTag("h3").first().ownText());
            news.setUrl(div.getElementsByTag("a").first().ownText());
            result[i++] = news;
        }
        return result;
    }

    @Cacheable("stsciNewsDetailsByScrapping")
    public HubbleNasaNewsReleaseResponse getNewsDetailsByScrapping(String urlLink) throws IOException {
        URL url = new URL(urlLink);
        Document html = fetchHtml(url);
        Element details = html.getElementsByClass("news-listing-details").first();
        HubbleNasaNewsReleaseResponse result = new HubbleNasaNewsReleaseResponse();
        result.setUrl(urlLink);
        result.setMission(getMission(url.getHost()));
        result.setName(html.getElementsByClass("news-listing__intro").first().getElementsByTag("h2").first().text());
        result.setId(details.getElementsByClass("news-release-id").first().ownText().substring(12));
        result.setPublication(ZonedDateTime.parse(details.getElementsByClass("news-release-date").first().text(),
                releaseDateformatter));
        result.setAbstract(html.getElementsByClass("page-intro__main").first().text());
        Element releaseImages = html.getElementsByClass("news-release-images").first();
        if (releaseImages != null) {
            result.setReleaseImages(extractReleases(releaseImages));
        }
        Element releaseVideos = html.getElementsByClass("news-release-videos").first();
        if (releaseVideos != null) {
            result.setReleaseVideos(extractReleases(releaseVideos));
        }
        return result;
    }

    public HubbleNasaImageResponse getImageDetailsByScrapping(String urlLink) throws IOException {
        URL url = new URL(urlLink);
        Document html = fetchHtml(url);
        Element main = html.getElementById("main-content");
        HubbleNasaImageResponse result = new HubbleNasaImageResponse();
        result.setMission(getMission(url.getHost()));
        result.setName(main.getElementsByTag("h2").first().text());
        result.setCredits(main.getElementsByTag("footer").first().getElementsByTag("p").first().ownText().trim());
        result.setDescription(main.getElementsByClass("col-md-8").first().getElementsByTag("p").first().text());
        List<HubbleNasaImageFiles> files = new ArrayList<>();
        for (Element a : main.getElementsByClass("media-library-links-list").first().getElementsByTag("a")) {
            files.add(extractFile(urlLink, a.attr("href"), a.ownText()));
        }
        result.setImageFiles(files);
        return result;
    }

    protected static HubbleNasaImageFiles extractFile(String urlLink, String href, String text) throws IOException {
        HubbleNasaImageFiles file = new HubbleNasaImageFiles();
        file.setFileUrl("https:" + href);
        Matcher m = FILE_DOWNLOAD_TEXT.matcher(text);
        if (m.matches()) {
            if (!"PDF".equals(m.group(3))) {
                file.setWidth(Integer.parseInt(m.group(1)));
                file.setHeight(Integer.parseInt(m.group(2)));
            }
            double size = Double.parseDouble(m.group(4));
            switch (m.group(5)) {
            case "KB":
                size *= 1024;
                break;
            case "MB":
                size *= 1024 * 1024;
                break;
            case "GB":
                size *= 1024 * 1024 * 1024;
                break;
            default:
                throw new IOException("Unsupported file size unit: '" + m.group(4) + "' at " + urlLink);
            }
            file.setFileSize((int) size);
        } else {
            throw new IOException("Unsupported file download text: '" + text + "' at " + urlLink);
        }
        return file;
    }

    private static List<String> extractReleases(Element releaseImages) {
        return releaseImages.getElementsByClass("grid-view").first().getElementsByTag("a").stream()
                .map(a -> a.attr("href").replace("?news=true", "")
                        .replace("/contents/media/images/", "")
                        .replace("/contents/media/videos/", ""))
                .collect(toList());
    }

    public static Document fetchHtml(URL sourceUrl) throws IOException {
        String sourceLink = sourceUrl.toExternalForm();
        LOGGER.info(sourceLink);
        return Jsoup.connect(sourceLink).timeout(60_000).get();
    }

    public static String getWebsite(String mission) {
        switch (mission) {
        case "hubble":
            return "hubblesite.org";
        case "james_webb":
            return "webbtelescope.org";
        default:
            return null;
        }
    }

    public static String getMission(String website) {
        switch (website) {
        case "hubblesite.org":
            return "hubble";
        case "webbtelescope.org":
            return "james_webb";
        default:
            return null;
        }
    }
}
