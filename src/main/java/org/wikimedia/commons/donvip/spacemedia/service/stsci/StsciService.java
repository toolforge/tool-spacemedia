package org.wikimedia.commons.donvip.spacemedia.service.stsci;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.data.domain.Metadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.stsci.StsciImageFiles;
import org.wikimedia.commons.donvip.spacemedia.data.domain.stsci.StsciMedia;

@Service
public class StsciService {

    private static final DateTimeFormatter exposureDateformatter = DateTimeFormatter.ofPattern("MMM dd, yyyy",
            Locale.ENGLISH);

    public static final DateTimeFormatter releaseDateformatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy h:mma (zz)",
            Locale.ENGLISH);

    private static final Pattern FILE_DIMENSIONS = Pattern.compile("(\\d+) X (\\d+)");

    private static final Pattern FILE_TYPE_SIZE = Pattern
            .compile("(?:JPE?G|PDF|PNG|TIFF?) \\((\\d+\\.\\d+) (KB|MB|GB)\\)");

    private static final Logger LOGGER = LoggerFactory.getLogger(StsciService.class);

    public String[] fetchImagesByScrapping(String urlLink) throws IOException {
        URL url = new URL(urlLink);
        Document html = fetchHtml(url);
        Elements divs = html.getElementsByClass("ad-research-box");
        String[] result = new String[divs.size()];
        int i = 0;
        for (Element div : divs) {
            String href = div.getElementsByTag("a").first().attr("href").replace("/contents/media/images/", "");
            result[i++] = href.substring(0, href.indexOf('?'));
        }
        return result;
    }

    public StsciMedia getImageDetailsByScrapping(String id, String urlLink) throws IOException {
        URL url = new URL(urlLink);
        return getImageDetailsByScrapping(id, urlLink, url, fetchHtml(url));
    }

    public StsciMedia getImageDetailsByScrapping(String id, String urlLink, URL url, Document html) throws IOException {
        Element main = html.getElementById("main-content");
        Element md8 = main.getElementsByClass("col-md-8").first();
        StsciMedia result = new StsciMedia();
        result.setId(id);
        result.setMission(getMission(url.getHost()));
        result.setTitle(main.getElementsByTag("h1").first().text());
        result.setCredits(
                md8.getElementsByTag("h3").select(":contains(Credits)").first().nextElementSibling().ownText().trim());
        result.setDescription(md8.getElementsByTag("p").eachText().stream().filter(s -> !s.equals(result.getCredits()))
                .collect(joining("\n")));
        List<StsciImageFiles> files = new ArrayList<>();
        for (Element a : main.getElementsByClass("media-library-links-list").first().getElementsByTag("a")) {
            files.add(extractFile(urlLink, a.attr("href"), a.ownText()));
        }
        if (files.isEmpty()) {
            throw new IOException("No file found at " + urlLink);
        }
        fillMetadata(result, files);

        result.setKeywords(html.getElementsByClass("keyword-tag").stream().map(Element::text).collect(toSet()));
        Elements tds = html.getElementsByTag("td");
        findTd(tds, "Object Name").ifPresent(result::setObjectName);
        findTd(tds, "Constellation").ifPresent(result::setConstellation);
        findTd(tds, "Exposure Dates").ifPresent(dates -> {
            try {
                result.setExposureDate(LocalDate.parse(dates, exposureDateformatter));
            } catch (DateTimeParseException e) {
                LOGGER.debug(dates, e);
            }
        });
        Elements h3s = html.getElementsByTag("h3");
        List<Element> elems = h3s.stream().filter(h -> "Release Date".equals(h.text())).toList();
        if (elems.size() == 1) {
            String date = elems.get(0).parent().ownText().trim();
            try {
                result.setDate(ZonedDateTime.parse(date, StsciService.releaseDateformatter));
            } catch (DateTimeParseException e) {
                LOGGER.debug(date, e);
            }
        }
        elems = h3s.stream().filter(p -> p.text().equals("Read the Release")).toList();
        if (elems.size() == 1) {
            result.setNewsId(elems.get(0).nextElementSibling().text());
        }

        return result;
    }

    private static void fillMetadata(StsciMedia result, List<StsciImageFiles> files) throws MalformedURLException {
        int width = -1;
        Metadata metadata = result.getMetadata();
        Metadata frMetadata = result.getFullResMetadata();
        for (StsciImageFiles imageFile : files) {
            String fileUrl = imageFile.getFileUrl();
            URL assetUrl = toUrl(fileUrl);
            if (fileUrl.endsWith(".tif") || fileUrl.endsWith(".tiff")) {
                frMetadata.setAssetUrl(assetUrl);
                frMetadata.setSize((long) imageFile.getFileSize());
            } else if ((fileUrl.endsWith(".png") || fileUrl.endsWith(".jpg") || fileUrl.endsWith(".pdf"))
                    && (metadata.getAssetUrl() == null || (metadata.getSize().intValue() < imageFile.getFileSize()
                            && width <= imageFile.getWidth()))) {
                metadata.setAssetUrl(assetUrl);
                metadata.setSize((long) imageFile.getFileSize());
                width = imageFile.getWidth();
            }
        }
        int finalWidth = width;
        if (frMetadata.getAssetUrl() == null && files.size() > 1) {
            files.stream().filter(f -> f.getWidth() >= finalWidth)
                    .max(Comparator.comparingInt(StsciImageFiles::getFileSize))
                    .map(StsciImageFiles::getFileUrl).ifPresent(max -> {
                        try {
                            frMetadata.setAssetUrl(toUrl(max));
                        } catch (MalformedURLException e) {
                            LOGGER.error(max, e);
                        }
                    });
        }
        if (endsWith(frMetadata.getAssetUrl(), ".png", ".jpg") && endsWith(metadata.getAssetUrl(), ".png", ".jpg")) {
            metadata.setAssetUrl(frMetadata.getAssetUrl());
            frMetadata.setAssetUrl(null);
        }
        files.stream().filter(f -> !f.getFileUrl().endsWith(".pdf"))
                .min(Comparator.comparingInt(StsciImageFiles::getFileSize)).map(StsciImageFiles::getFileUrl)
                .ifPresent(min -> {
                    try {
                        result.setThumbnailUrl(toUrl(min));
                    } catch (MalformedURLException e) {
                        LOGGER.error(min, e);
                    }
                });
    }

    private static URL toUrl(String fileUrl) throws MalformedURLException {
        if (fileUrl.startsWith("//")) {
            fileUrl = "https:" + fileUrl;
        }
        if (fileUrl.startsWith("https://imgsrc.hubblesite.org/")) {
            // Broken https, redirected anyway to https://hubblesite.org/ without hvi folder
            fileUrl = fileUrl.replace("imgsrc.", "").replace("/hvi/", "/");
        }
        return new URL(fileUrl);
    }

    private static boolean endsWith(URL url, String... exts) {
        if (url != null) {
            String externalForm = url.toExternalForm();
            for (String ext : exts) {
                if (externalForm.endsWith(ext)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static Optional<String> findTd(Elements tds, String label) {
        List<Element> matches = tds.stream().filter(x -> label.equalsIgnoreCase(x.text())).toList();
        if (matches.size() == 2) {
            return Optional.ofNullable(matches.get(0).nextElementSibling()).map(Element::text);
        }
        return Optional.empty();
    }

    protected static StsciImageFiles extractFile(String urlLink, String href, String text) throws IOException {
        StsciImageFiles file = new StsciImageFiles();
        file.setFileUrl("https:" + href);
        for (String segment : text.split(", ")) {
            Matcher m = FILE_DIMENSIONS.matcher(segment);
            if (m.matches()) {
                file.setWidth(Integer.parseInt(m.group(1)));
                file.setHeight(Integer.parseInt(m.group(2)));
            } else {
                m = FILE_TYPE_SIZE.matcher(segment);
                if (m.matches()) {
                    double size = Double.parseDouble(m.group(1));
                    switch (m.group(2)) {
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
                        throw new IOException("Unsupported file size unit: '" + m.group(2) + "' at " + urlLink);
                    }
                    file.setFileSize((int) size);
                }
            }
        }
        return file;
    }

    private static Document fetchHtml(URL sourceUrl) throws IOException {
        String sourceLink = sourceUrl.toExternalForm();
        LOGGER.info(sourceLink);
        return Jsoup.connect(sourceLink).timeout(60_000).get();
    }

    public static String getWebsite(String mission) {
        switch (mission) {
        case "hubble":
            return "hubblesite.org";
        case "webb":
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
            return "webb";
        default:
            return null;
        }
    }
}
