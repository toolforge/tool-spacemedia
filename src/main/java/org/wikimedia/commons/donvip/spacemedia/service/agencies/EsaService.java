package org.wikimedia.commons.donvip.spacemedia.service.agencies;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import javax.annotation.PostConstruct;

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
import org.springframework.util.CollectionUtils;
import org.wikimedia.commons.donvip.spacemedia.data.domain.esa.EsaMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.esa.EsaMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.exception.ImageDecodingException;
import org.wikimedia.commons.donvip.spacemedia.utils.Utils;

import com.github.dozermapper.core.Mapper;

@Service
public class EsaService extends AbstractFullResSpaceAgencyService<EsaMedia, Integer, LocalDateTime> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EsaService.class);

    /**
     * Resulting SHA-1 hash of an HTML error page.
     * See <a href="https://www.esa.int/var/esa/storage/images/esa_multimedia/images/2006/10/envisat_sees_madagascar/10084739-2-eng-GB/Envisat_sees_Madagascar.tiff">this example</a>
     */
    private static final String SHA1_ERROR = "860f6466c5f3da5d62b2065c33aa5548697d817c";

    @Autowired
    private EsaMediaRepository mediaRepository;

    @Value("${esa.search.link}")
    private String searchLink; 
    
    @Value("${esa.max.tries}")
    private int maxTries;

    @Value("${esa.date.pattern}")
    private String datePattern;

    @Autowired
    protected Mapper dozerMapper;

    private DateTimeFormatter dateFormatter;

    @Autowired
    public EsaService(EsaMediaRepository repository) {
        super(repository);
    }

    @PostConstruct
    void init() {
        dateFormatter = DateTimeFormatter.ofPattern(datePattern);
    }

    private static Optional<URL> getImageUrl(String src, URL imageUrl) throws MalformedURLException {
        if (src.startsWith("http://") || src.startsWith("https://")) {
            return Optional.of(new URL(src.replace("esamultimeda.esa.int", "esamultimedia.esa.int")));
        } else {
            return Optional.of(new URL(imageUrl.getProtocol(), imageUrl.getHost(), src));
        }
    }

    private static Optional<URL> getImageUrl(Element imageHtml, String klass, URL imageUrl) throws MalformedURLException {
        Elements elems = imageHtml.getElementsByClass(klass);
        return elems.isEmpty() ? Optional.empty() : getImageUrl(elems.get(0).attr("href"), imageUrl);
    }
    
    private void processDetails(EsaMedia image, Elements items) {
        for (Element li : items) {
            if ("li".equals(li.tagName())) {
                String tag = li.child(0).text();
                String label = li.text().replaceFirst(tag, "").trim();
                switch (tag) {
                    case "Title":
                        image.setTitle(label); break;
                    case "Released":
                        image.setDate(LocalDateTime.parse(label.toUpperCase(Locale.ENGLISH), dateFormatter)); break;
                    case "Copyright":
                        image.setCopyright(label); break;
                    case "Description":
                        image.setDescription(label); break;
                    case "Id":
                        image.setId(Integer.parseInt(label)); break;
                    default:
                        LOGGER.warn("Unknown detail: {}", tag);
                }
            } else if (!li.children().isEmpty()) {
                // Strange HTML in https://www.esa.int/spaceinimages/layout/set/html_npl/Images/2015/12/MTG_combined_antenna
                processDetails(image, li.children());
            }
        }
    }

    private void processTags(EsaMedia image, Elements items) {
        for (Element li : items) {
            if (!li.children().isEmpty()) {
                String tag = li.child(0).text();
                String label = li.text().replaceFirst(tag, "").trim();
                switch (tag) {
                case "Action":
                    image.setAction(label); break;
                case "Activity":
                    image.setActivity(label); break;
                case "Mission":
                    image.setMission(label); break;
                case "People":
                    image.setPeople(label); break;
                case "System":
                    image.setSystems(set(label)); break;
                case "Location":
                    image.setLocations(set(label)); break;
                case "Keywords":
                    image.setKeywords(set(label)); break;
                case "Set":
                    image.setPhotoSet(label); break;
                default:
                    LOGGER.warn("Unknown tag: {}", tag);
                }
            }
        }
    }

    private static boolean isCopyrightOk(EsaMedia image) {
        String copyrightUppercase = image.getCopyright().toUpperCase(Locale.ENGLISH);
        return (copyrightUppercase.contains("BY-SA") || copyrightUppercase.contains("COPERNICUS SENTINEL")
                || (copyrightUppercase.contains("COPERNICUS DATA")
                        && image.getDescription().toUpperCase(Locale.ENGLISH).contains(" SENTINEL")));
    }

    private Optional<EsaMedia> checkEsaImage(URL url) {
        EsaMedia media;
        boolean save = false;
        Optional<EsaMedia> mediaInRepo = mediaRepository.findByUrl(url);
        if (mediaInRepo.isPresent()) {
            media = mediaInRepo.get();
        } else {
            media = new EsaMedia();
            media.setUrl(url);
            save = true;
            boolean ok = false;
            for (int i = 0; i < maxTries && !ok; i++) {
                try {
                    Document html = Jsoup.connect(url.toExternalForm()).get();
                    List<URL> files = new ArrayList<>();
                    for (String format : Arrays.asList("gif", "jpg", "png", "tif")) {
                        getImageUrl(html, "download d-" + format + "-hi", url).ifPresent(files::add);
                    }
                    Optional<URL> lowRes = getImageUrl(
                            html.getElementsByClass("pi_container").get(0).getElementsByTag("img").get(0).attr("src"), url)
                            .filter(u -> !u.getPath().contains("extension/esadam/design/esadam/images/global/arw"));
                    if (lowRes.isPresent()) {
                        if (files.isEmpty()) {
                            // No hi-res for some missions (Gaia, Visual Monitoring Camera, OSIRIS...)
                            files.add(lowRes.get());
                        }
                        if (media.getThumbnailUrl() == null) {
                            media.setThumbnailUrl(lowRes.get());
                            save = true;
                        }
                    }
                    int size = files.size();
                    if (size == 0) {
                        problem(media.getUrl(), "Image without any file");
                        save = false;
                    } else if (size == 1) {
                        media.setAssetUrl(files.get(0));
                    } else if (size == 2) {
                        // When a file is a tif or png, the other is always a jpg (320+6 occurences), never something else
                        for (URL fileUrl : files) {
                            if (fileUrl.toExternalForm().endsWith(".tif")
                                    || fileUrl.toExternalForm().endsWith(".png")) {
                                media.setFullResAssetUrl(fileUrl);
                            } else {
                                media.setAssetUrl(fileUrl);
                            }
                        }
                    } else {
                        throw new IllegalStateException("Media with more than two files: " + media);
                    }
                    processDetails(media,
                            html.getElementById("pi_details_content").getElementsByTag("ul").get(0).children());
                    processTags(media,
                            html.getElementById("pi_tags_content").getElementsByTag("ul").get(0).children());
                    ok = true;
                } catch (SocketTimeoutException e) {
                    LOGGER.debug(url.toExternalForm(), e);
                } catch (IOException | IllegalStateException e) {
                    LOGGER.error(url.toExternalForm(), e);
                }
            }
        }
        if (!isCopyrightOk(media)) {
            problem(media.getUrl(), "Invalid copyright: " + media.getCopyright());
            return Optional.empty();
        }
        boolean ok = false;
        for (int i = 0; i < maxTries && !ok; i++) {
            try {
                if (mediaService.computeSha1(media)) {
                    save = true;
                }
                if (mediaService.findCommonsFilesWithSha1(media)) {
                    save = true;
                }
                ok = true;
            } catch (IOException | URISyntaxException e) {
                LOGGER.error(media.toString(), e);
            }
        }
        if (save) {
            repository.save(media);
        }
        return Optional.of(media);
    }

    private static final Set<String> set(String label) {
        return new TreeSet<>(Arrays.asList(label.replace(", ", ",").split(",")));
    }

    private EsaMedia ignoreAndSaveFile(EsaMedia file, URL url, String reason, boolean problem) {
        if (problem) {
            problem(url, reason);
        }
        file.setIgnored(Boolean.TRUE);
        file.setIgnoredReason(reason);
        return repository.save(file);
    }

    private void updateMissingImages() {
        for (EsaMedia media : listMissingMedia()) {
            // Envisat pictures are released in two versions: 1 hi-res TIFF file and 1 not-so-shi-res JPEG file
            // On wikimedia commons, the TIFF file has been uploaded in both TIFF and JPEG format to benefit from hi-res
            if ("Envisat".equalsIgnoreCase(media.getMission()) && media.getFullResAssetUrl() != null
                    && CollectionUtils.isEmpty(media.getCommonsFileNames())) {
                ignoreAndSaveFile(media, media.getAssetUrl(), "ENVISAT low resolution image", false);
            }
            if (media.isIgnored() == null) {
                // Ignore file from which we get an HTML error page instead of a real image
                if (SHA1_ERROR.equals(media.getSha1())) {
                    ignoreAndSaveFile(media, media.getAssetUrl(), "HTML error page", true);
                } else if (SHA1_ERROR.equals(media.getFullResSha1())) {
                    ignoreAndSaveFile(media, media.getFullResAssetUrl(), "HTML error page", true);
                } else {
                    // Check for corrupted ESA images (many of them...)
                    checkCorruptedMedia(media, media.getAssetUrl());
                    checkCorruptedMedia(media, media.getFullResAssetUrl());
                }
            }
        }
    }

    private void checkCorruptedMedia(EsaMedia media, URL url) {
        if (url != null) {
            try {
                BufferedImage bi = Utils.readImage(url, false);
                if (bi != null) {
                    bi.flush();
                    media.setIgnored(Boolean.FALSE);
                    repository.save(media);
                }
            } catch (ImageDecodingException e) {
                LOGGER.warn(media.toString(), e);
                ignoreAndSaveFile(media, url, e.getMessage(), true);
            } catch (IOException | URISyntaxException e) {
                problem(url, e);
            }
        }
    }

    @Override
    @Scheduled(fixedRateString = "${esa.update.rate}", initialDelayString = "${initial.delay}")
    public void updateMedia() throws IOException {
        LocalDateTime start = startUpdateMedia();
        updateMissingImages();
        final URL url = new URL(searchLink);
        final String proto = url.getProtocol();
        final String host = url.getHost();
        boolean moreImages = true;
        int count = 0;
        int index = 0;
        do {
            String searchUrl = searchLink.replace("<idx>", Integer.toString(index));
            try {
                boolean ok = false;
                for (int i = 0; i < maxTries && !ok; i++) {
                    try {
                        LOGGER.debug("Fetching ESA images: {}", searchUrl);
                        Document html = Jsoup.connect(searchUrl).timeout(15_000).get();
                        Elements divs = html.getElementsByClass("psr_item_grid");
                        for (Element div : divs) {
                            URL imageUrl = new URL(proto, host, div.select("a").get(0).attr("href"));
                            index++;
                            LOGGER.debug("Checking ESA image {}: {}", index, imageUrl);
                            if (checkEsaImage(imageUrl).isPresent()) {
                                count++;
                            }
                        }
                        moreImages = !html.getElementsByClass("next").isEmpty();
                        ok = true;
                    } catch (SocketTimeoutException e) {
                        LOGGER.debug(searchUrl, e);
                    }
                }
            } catch (IOException e) {
                LOGGER.error(searchUrl, e);
            }
        } while (moreImages);

        endUpdateMedia(count, start);
    }

    @Override
    public String getName() {
        return "ESA";
    }

    @Override
    public URL getSourceUrl(EsaMedia media) throws MalformedURLException {
        URL url = media.getUrl();
        String externalForm = url.toExternalForm();
        if (externalForm.contains("://www.esa.int/spaceinimages/layout/set/html_npl/Images/")) {
            url = new URL(externalForm.replace("layout/set/html_npl/", ""));
        }
        return url;
    }

    @Override
    protected String getAuthor(EsaMedia media) {
        if (media.getCopyright().contains("/")) {
            String authors = media.getCopyright().replace("CC BY-SA 3.0 IGO", "").trim();
            if (authors.endsWith(",") && authors.length() > 2) {
                return authors.replace(",", "").trim();
            }
        }
        return "European Space Agency";
    }

    @Override
    protected Optional<Temporal> getUploadDate(EsaMedia media) {
        return Optional.ofNullable(media.getDate());
    }
}
