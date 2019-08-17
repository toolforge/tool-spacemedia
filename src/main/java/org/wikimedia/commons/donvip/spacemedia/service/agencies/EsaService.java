package org.wikimedia.commons.donvip.spacemedia.service.agencies;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

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
import org.wikimedia.commons.donvip.spacemedia.data.domain.esa.EsaFile;
import org.wikimedia.commons.donvip.spacemedia.data.domain.esa.EsaFileRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.esa.EsaImage;
import org.wikimedia.commons.donvip.spacemedia.data.domain.esa.EsaImageRepository;
import org.wikimedia.commons.donvip.spacemedia.exception.ImageDecodingException;
import org.wikimedia.commons.donvip.spacemedia.utils.Utils;

@Service
public class EsaService extends AbstractSpaceAgencyService<EsaFile, String> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EsaService.class);

    /**
     * Resulting SHA-1 hash of an HTML error page.
     * See <a href="https://www.esa.int/var/esa/storage/images/esa_multimedia/images/2006/10/envisat_sees_madagascar/10084739-2-eng-GB/Envisat_sees_Madagascar.tiff">this example</a>
     */
    private static final String SHA1_ERROR = "860f6466c5f3da5d62b2065c33aa5548697d817c";

    @Autowired
    private EsaImageRepository imageRepository;

    @Value("${esa.search.link}")
    private String searchLink; 
    
    @Value("${esa.max.tries}")
    private int maxTries;

    @Value("${esa.date.pattern}")
    private String datePattern;

    private DateTimeFormatter dateFormatter;

    @Autowired
    public EsaService(EsaFileRepository repository) {
        super(repository);
    }

    @PostConstruct
    void init() {
        dateFormatter = DateTimeFormatter.ofPattern(datePattern);
    }

    public Iterable<EsaImage> listAllImages() {
        return imageRepository.findAll();
    }

    public List<EsaImage> listMissingImages() {
        return imageRepository.findMissingInCommons();
    }

    public List<EsaImage> listIgnoredImages() {
        return imageRepository.findByIgnoredTrue();
    }

    public List<EsaImage> listDuplicateImages() {
        return imageRepository.findDuplicateInCommons();
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
    
    private void processDetails(EsaImage image, Elements items) {
        for (Element li : items) {
            if ("li".equals(li.tagName())) {
                String tag = li.child(0).text();
                String label = li.text().replaceFirst(tag, "").trim();
                switch (tag) {
                    case "Title":
                        image.setTitle(label); break;
                    case "Released":
                        image.setReleased(LocalDateTime.parse(label.toUpperCase(Locale.ENGLISH), dateFormatter)); break;
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

    private void processTags(EsaImage image, Elements items) {
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

    private EsaFile findCommonsFileBySha1(EsaFile f, boolean save) {
        if (mediaService.findCommonsFilesWithSha1(f) && save) {
            return repository.save(f);
        }
        return f;
    }

    private EsaImage processMissingCommonsFileNames(EsaImage image) {
        image.getFiles().forEach(f -> findCommonsFileBySha1(f, true));
        return image;
    }

    private static boolean isCopyrightOk(EsaImage image) {
        String copyrightUppercase = image.getCopyright().toUpperCase(Locale.ENGLISH);
        return (copyrightUppercase.contains("BY-SA") || copyrightUppercase.contains("COPERNICUS SENTINEL")
                || (copyrightUppercase.contains("COPERNICUS DATA")
                        && image.getDescription().toUpperCase(Locale.ENGLISH).contains(" SENTINEL")));
    }

    private EsaImage processCopyrightAndSave(EsaImage image) {
        if (isCopyrightOk(image)) {
            for (EsaFile f : image.getFiles()) {
                URL url = f.getAssetUrl();
                if (url != null) {
                    boolean sha1ok = false;
                    for (int j = 0; j < maxTries && !sha1ok; j++) {
                        try {
                            mediaService.computeSha1(f);
                            f = repository.save(findCommonsFileBySha1(f, false));
                            sha1ok = true;
                        } catch (IOException | URISyntaxException e) {
                            LOGGER.error(url.toExternalForm(), e);
                        }
                    }
                }
            }
            image = imageRepository.save(image);
            if (CollectionUtils.isEmpty(image.getFiles())) {
                problem(image.getUrl(), "Image without any file");
            } else {
                LOGGER.debug("New image: {}", image);
            }
        } else {
            LOGGER.debug("Non free image? {} => {}", image.getUrl(), image.getCopyright());
        }
        return image;
    }

    private Optional<EsaImage> checkEsaImage(URL imageUrl) {
        List<EsaImage> existingImages = imageRepository.findByUrl(imageUrl);
        if (!existingImages.isEmpty() && !CollectionUtils.isEmpty(existingImages.get(0).getFiles())) {
            return Optional.of(processMissingCommonsFileNames(existingImages.get(0)));
        } else {
            List<EsaFile> files = new ArrayList<>();
            EsaImage image = new EsaImage();
            image.setFiles(files);
            image.setUrl(imageUrl);
            boolean ok = false;
            for (int i = 0; i < maxTries && !ok; i++) {
                try {
                    Document imageHtml = Jsoup.connect(imageUrl.toExternalForm()).get();
                    for (String format : Arrays.asList("gif", "jpg", "png", "tif")) {
                        getImageUrl(imageHtml, "download d-"+format+"-hi", imageUrl).map(EsaFile::new).ifPresent(files::add);
                    }
                    if (files.isEmpty()) {
                        // No hi-res for some missions (Gaia, Visual Monitoring Camera, OSIRIS...)
                        getImageUrl(imageHtml.getElementsByClass("pi_container").get(0).getElementsByTag("img").get(0).attr("src"), imageUrl)
                            .filter(u -> !u.getPath().contains("extension/esadam/design/esadam/images/global/arw"))
                            .map(EsaFile::new).ifPresent(files::add);
                    }
                    processDetails(image, imageHtml.getElementById("pi_details_content").getElementsByTag("ul").get(0).children());
                    processTags(image, imageHtml.getElementById("pi_tags_content").getElementsByTag("ul").get(0).children());
                    image = processCopyrightAndSave(image);
                    ok = true;
                } catch (SocketTimeoutException e) {
                    LOGGER.debug(imageUrl.toExternalForm(), e);
                } catch (IOException | IllegalStateException e) {
                    LOGGER.error(imageUrl.toExternalForm(), e);
                }
            }
            return ok ? Optional.of(image) : Optional.empty();
        }
    }

    private static final Set<String> set(String label) {
        return new TreeSet<>(Arrays.asList(label.replace(", ", ",").split(",")));
    }

    private EsaFile ignoreAndSaveFile(EsaFile file, String reason, boolean problem) {
        if (problem) {
            problem(file.getAssetUrl(), reason);
        }
        file.setIgnored(Boolean.TRUE);
        file.setIgnoredReason(reason);
        return repository.save(file);
    }

    private void updateMissingImages() {
        for (EsaImage image: listMissingImages()) {
            List<EsaFile> files = image.getFiles();
            // Envisat pictures are released in two versions: 1 hi-res TIFF file and 1 not-so-shi-res JPEG file
            // On wikimedia commons, the TIFF file has been uploaded in both TIFF and JPEG format to benefit from hi-res
            if ("Envisat".equalsIgnoreCase(image.getMission()) && files.size() == 2) {
                boolean empty0 = CollectionUtils.isEmpty(files.get(0).getCommonsFileNames());
                boolean empty1 = CollectionUtils.isEmpty(files.get(1).getCommonsFileNames());
                if (empty0 && !empty1) {
                    ignoreAndSaveFile(files.get(0), "ENVISAT low resolution image", false);
                } else if (!empty0 && empty1) {
                    ignoreAndSaveFile(files.get(1), "ENVISAT low resolution image", false);
                }
            }
            // Ignore files on a case by case
            for (EsaFile file : files) {
                if (file.isIgnored() == null) {
                    // Ignore file from which we get an HTML error page instead of a real image
                    if (SHA1_ERROR.equals(file.getSha1())) {
                        ignoreAndSaveFile(file, "HTML error page", true);
                    } else {
                        // Check for corrupted ESA images (many of them...)
                        try {
                            BufferedImage bi = Utils.readImage(file.getAssetUrl(), false);
                            if (bi != null) {
                                bi.flush();
                                file.setIgnored(Boolean.FALSE);
                                /* file = */ repository.save(file);
                            }
                        } catch (ImageDecodingException e) {
                            LOGGER.warn(file.toString(), e);
                            ignoreAndSaveFile(file, e.getMessage(), true);
                        } catch (IOException | URISyntaxException e) {
                            problem(file.getAssetUrl(), e);
                        }
                    }
                }
            }
        }
    }

    @Scheduled(fixedRateString = "${esa.update.rate}", initialDelayString = "${initial.delay}")
    public List<EsaImage> updateImages() throws IOException {
        LocalDateTime start = LocalDateTime.now();
        LOGGER.info("Starting ESA image updates...");
        updateMissingImages();
        final URL url = new URL(searchLink);
        final String proto = url.getProtocol();
        final String host = url.getHost();
        final List<EsaImage> images = new ArrayList<>();
        boolean moreImages = true;
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
                            checkEsaImage(imageUrl).ifPresent(images::add);
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

        LOGGER.info("ESA images update completed: {} images in {}", images.size(), Duration.between(LocalDateTime.now(), start));
        return images;
    }

    @Override
    public List<EsaFile> updateMedia() throws IOException {
        return updateImages().stream().flatMap(i -> i.getFiles().stream()).collect(Collectors.toList());
    }

    private EsaImage getEsaImageForFile(EsaFile media) {
        List<EsaImage> images = imageRepository.findByFile(media);
        if (images.size() != 1) { // TODO handle multiple images
            throw new IllegalStateException(images.toString());
        }
        return images.get(0);
    }

    @Override
    protected void checkUploadPreconditions(EsaFile media) {
        super.checkUploadPreconditions(media);
        getEsaImageForFile(media);
    }

    @Override
    public String getName() {
        return "ESA";
    }

    @Override
    protected String getDescription(EsaFile media) {
        return getEsaImageForFile(media).getDescription();
    }

    @Override
    protected String getSource(EsaFile media) throws MalformedURLException {
        EsaImage image = getEsaImageForFile(media);
        URL url = image.getUrl();
        String externalForm = url.toExternalForm();
        if (externalForm.contains("://www.esa.int/spaceinimages/layout/set/html_npl/Images/")) {
            url = new URL(externalForm.replace("layout/set/html_npl/", ""));
        }
        return wikiLink(url, image.getTitle());
    }

    @Override
    protected String getAuthor(EsaFile media) {
        EsaImage image = getEsaImageForFile(media);
        if (image.getCopyright().contains("/")) {
            String authors = image.getCopyright().replace("CC BY-SA 3.0 IGO", "").trim();
            if (authors.endsWith(",") && authors.length() > 2) {
                return authors.replace(",", "").trim();
            }
        }
        return "European Space Agency";
    }
}
