package org.wikimedia.commons.donvip.spacemedia.service.agencies;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.Temporal;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.wikimedia.commons.donvip.spacemedia.data.domain.Metadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.hubble.HubbleNasaImageFiles;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.hubble.HubbleNasaImageResponse;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.hubble.HubbleNasaImagesResponse;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.hubble.HubbleNasaMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.hubble.HubbleNasaMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.hubble.HubbleNasaNewsReleaseResponse;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.hubble.HubbleNasaNewsResponse;

/**
 * Service harvesting images from NASA Hubble / Jame Webb websites.
 */
@Service
public class HubbleNasaService extends
        AbstractFullResAgencyService<HubbleNasaMedia, Integer, ZonedDateTime, HubbleNasaMedia, Integer, ZonedDateTime> {

    private static final DateTimeFormatter exposureDateformatter = DateTimeFormatter
            .ofPattern("MMM dd, yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter releaseDateformatter = DateTimeFormatter
            .ofPattern("MMMM dd, yyyy h:mma (zz)", Locale.ENGLISH);

    private static final Logger LOGGER = LoggerFactory.getLogger(HubbleNasaService.class);

    private static final String NO_KEYWORD = "NONE";

    @Value("${hubble.nasa.news.link}")
    private String newsEndpoint;

    @Value("${hubble.nasa.news.detail.link}")
    private String newsDetailEndpoint;

    @Value("${hubble.nasa.search.link}")
    private String searchEndpoint;

    @Value("${hubble.nasa.detail.link}")
    private String detailEndpoint;

    @Value("${hubble.nasa.image.link}")
    private String imageLink;

    @Value("${hubble.nasa.news.image.link}")
    private String newsImageLink;

    private Map<String, String> hubbleCategories;

    @Autowired
    public HubbleNasaService(HubbleNasaMediaRepository repository) {
        super(repository, "hubble.nasa");
    }

    @Override
    @PostConstruct
    void init() throws IOException {
        super.init();
        hubbleCategories = loadCsvMapping("hubblenasa.categories.csv");
    }

    @Scheduled(fixedDelay = 43200000L)
    public void checkHubbleCategories() {
        checkCommonsCategories(hubbleCategories);
    }

    @Override
    protected Class<HubbleNasaMedia> getMediaClass() {
        return HubbleNasaMedia.class;
    }

    @Override
    protected final Integer getMediaId(String id) {
        return Integer.parseUnsignedInt(id);
    }

    private String getImageDetailsLink(int imageId) {
        return detailEndpoint.replace("<id>", Integer.toString(imageId));
    }

    private HubbleNasaImageResponse getImageDetails(RestTemplate rest, int imageId) {
        return rest.getForObject(getImageDetailsLink(imageId), HubbleNasaImageResponse.class);
    }

    @Override
    @Scheduled(fixedRateString = "${hubble.nasa.update.rate}", initialDelayString = "${hubble.nasa.initial.delay}")
    public void updateMedia() throws IOException {
        LocalDateTime start = startUpdateMedia();
        RestTemplate rest = new RestTemplate();
        int count = 0;
        // Step 1: loop over news, as they include more details than what is returned by images API
        boolean loop = true;
        int idx = 1;
        while (loop) {
            String urlLink = newsEndpoint.replace("<idx>", Integer.toString(idx++));
            HubbleNasaNewsResponse[] response = rest.getForObject(urlLink, HubbleNasaNewsResponse[].class);
            loop = response != null && response.length > 0;
            if (loop) {
                for (HubbleNasaNewsResponse news : response) {
                    HubbleNasaNewsReleaseResponse details = rest.getForObject(
                            newsDetailEndpoint.replace("<id>", news.getId()), HubbleNasaNewsReleaseResponse.class);
                    if (details.getReleaseImages() != null) {
                        for (int imageId : details.getReleaseImages()) {
                            try {
                                count += doUpdateMedia(imageId, getImageDetails(rest, imageId), details);
                            } catch (HttpStatusException e) {
                                LOGGER.error("Error {} while requesting {}: {}", e.getStatusCode(), e.getUrl(), e.getMessage());
                                problem(e.getUrl(), e);
                            } catch (IOException | RuntimeException e) {
                                LOGGER.error(
                                        "Error while fetching image " + imageId + " via Hubble news " + news.getId(),
                                        e);
                            }
                        }
                    }
                }
            }
        }
        // Step 2: loop over images to find ones not news-related
        loop = true;
        idx = 1;
        while (loop) {
            String urlLink = searchEndpoint.replace("<idx>", Integer.toString(idx++));
            HubbleNasaImagesResponse[] response = rest.getForObject(urlLink, HubbleNasaImagesResponse[].class);
            loop = response != null && response.length > 0;
            if (loop) {
                for (HubbleNasaImagesResponse image : response) {
                    try {
                        count += doUpdateMedia(image.getId(), getImageDetails(rest, image.getId()), null);
                    } catch (HttpStatusException e) {
                        LOGGER.error("Error while requesting {}: {}", e.getUrl(), e.getMessage());
                        problem(e.getUrl(), e);
                    } catch (IOException | RuntimeException e) {
                        LOGGER.error("Error while fetching image " + image.getId() + " via Hubble images", e);
                    }
                }
            }
        }
        endUpdateMedia(count, start);
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

    private int doUpdateMedia(int id, HubbleNasaImageResponse image, HubbleNasaNewsReleaseResponse news)
            throws IOException {
        if (image.getImageFiles() == null) {
            problem(getImageDetailsLink(id), "No image files");
            return 0;
        }
        boolean save = false;
        HubbleNasaMedia media;
        Optional<HubbleNasaMedia> mediaInRepo = repository.findById(id);
        if (mediaInRepo.isPresent()) {
            media = mediaInRepo.get();
        } else {
            media = new HubbleNasaMedia();
            media.setId(id);
            media.setDescription(image.getDescription());
            media.setTitle(image.getName());
            media.setCredits(image.getCredits());
            media.setMission(image.getMission());
            Metadata metadata = media.getMetadata();
            Metadata frMetadata = media.getFullResMetadata();
            for (HubbleNasaImageFiles imageFile : image.getImageFiles()) {
                String fileUrl = imageFile.getFileUrl();
                URL url = toUrl(fileUrl);
                if (fileUrl.endsWith(".tif") || fileUrl.endsWith(".tiff")) {
                    frMetadata.setAssetUrl(url);
                } else if (fileUrl.endsWith(".png") || fileUrl.endsWith(".jpg") || fileUrl.endsWith(".pdf")) {
                    metadata.setAssetUrl(url);
                }
            }
            if (frMetadata.getAssetUrl() == null && image.getImageFiles().size() > 1) {
                image.getImageFiles().stream().max(Comparator.comparingInt(HubbleNasaImageFiles::getFileSize))
                        .map(HubbleNasaImageFiles::getFileUrl).ifPresent(max -> {
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
            save = true;
        }
        if (media.getDate() == null && news != null) {
            media.setDate(news.getPublication());
            save = true;
        }
        if (media.getNewsId() == null && news != null) {
            media.setNewsId(news.getId());
            save = true;
        }
        if (CollectionUtils.isEmpty(media.getKeywords())) {
            URL sourceUrl = getImageUrl(media, true);
            if (sourceUrl != null) {
                fetchMetadata(media, sourceUrl);
                save = true;
            }
        }
        if (doCommonUpdate(media)) {
            save = true;
        }
        if (save) {
            repository.save(media);
        }
        return save ? 1 : 0;
    }

    private void fetchMetadata(HubbleNasaMedia media, URL sourceUrl) throws IOException {
        Document html = null;
        try {
            html = fetchHtml(sourceUrl);
        } catch (HttpStatusException e) {
            if (e.getStatusCode() == HttpStatus.SC_NOT_FOUND && media.getNewsId() != null) {
                sourceUrl = getImageUrl(media, false);
                if (sourceUrl != null) {
                    html = fetchHtml(sourceUrl);
                }
            } else {
                throw e;
            }
        }
        if (html == null) {
            throw new IllegalStateException("Unable to fetch HTML page for image " + media.getId());
        }
        media.setKeywords(
            html.getElementsByClass("keyword-tag").stream().map(Element::text).collect(Collectors.toSet()));
        if (media.getKeywords().isEmpty()) {
            media.getKeywords().add(NO_KEYWORD); // To avoid fetching HTML again on next update
        }
        Elements tds = html.getElementsByTag("td");
        if (StringUtils.isEmpty(media.getObjectName())) {
            findTd(tds, "Object Name").ifPresent(media::setObjectName);
        }
        if (media.getExposureDate() == null) {
            findTd(tds, "Exposure Dates").ifPresent(dates -> {
                try {
                    media.setExposureDate(LocalDate.parse(dates, exposureDateformatter));
                } catch (DateTimeParseException e) {
                    LOGGER.debug(dates, e);
                }
            });
        }
        if (media.getDate() == null) {
            List<Element> elems = html.getElementsByTag("p").stream()
                    .filter(p -> p.text().startsWith("Release Date:")).collect(Collectors.toList());
            if (elems.size() == 1) {
                String date = elems.get(0).text().replace("Release Date:", "").trim();
                try {
                    media.setDate(ZonedDateTime.parse(date, releaseDateformatter));
                } catch (DateTimeParseException e) {
                    LOGGER.debug(date, e);
                }
            }
        }
    }

    private static Document fetchHtml(URL sourceUrl) throws IOException {
        String sourceLink = sourceUrl.toExternalForm();
        LOGGER.debug(sourceLink);
        return Jsoup.connect(sourceLink).timeout(60_000).get();
    }

    private static Optional<String> findTd(Elements tds, String label) {
        List<Element> matches = tds.stream().filter(x -> label.equalsIgnoreCase(x.text())).collect(Collectors.toList());
        if (matches.size() == 2) {
            return Optional.ofNullable(matches.get(0).nextElementSibling()).map(Element::text);
        }
        return Optional.empty();
    }

    @Override
    public String getName() {
        return "Hubble (NASA)";
    }

    @Override
    public Set<String> findTemplates(HubbleNasaMedia media) {
        Set<String> result = super.findTemplates(media);
        result.add("PD-Hubble");
        return result;
    }

    @Override
    public Set<String> findCategories(HubbleNasaMedia media, boolean includeHidden) {
        Set<String> result = super.findCategories(media, includeHidden);
        if (media.getKeywords() != null) {
            result.addAll(media.getKeywords().stream().map(hubbleCategories::get).filter(StringUtils::isNotBlank)
                    .collect(Collectors.toSet()));
        }
        return result;
    }

    @Override
    public URL getSourceUrl(HubbleNasaMedia media) throws MalformedURLException {
        return getImageUrl(media, true);
    }

    private URL getImageUrl(HubbleNasaMedia media, boolean allowNews) throws MalformedURLException {
        if (media.getMission() != null) {
            String website = getWebsite(media.getMission());
            if (website != null) {
                if (allowNews && media.getNewsId() != null) {
                    return new URL(newsImageLink.replace("<website>", website)
                            .replace("<news_id>", media.getNewsId().replace('-', '/'))
                            .replace("<img_id>", media.getId().toString()));
                } else {
                    return new URL(imageLink
                            .replace("<website>", website)
                            .replace("<img_id>", media.getId().toString()));
                }
            }
        }
        return null;
    }

    private String getWebsite(String mission) {
        switch (mission) {
        case "hubble":
            return "hubblesite.org";
        case "james_webb":
            return "webbtelescope.org";
        default:
            return null;
        }
    }

    @Override
    protected String getAuthor(HubbleNasaMedia media) throws MalformedURLException {
        return media.getCredits();
    }

    @Override
    protected Optional<Temporal> getCreationDate(HubbleNasaMedia media) {
        return Optional.ofNullable(media.getExposureDate());
    }

    @Override
    protected Optional<Temporal> getUploadDate(HubbleNasaMedia media) {
        return Optional.of(media.getDate());
    }
}
