package org.wikimedia.commons.donvip.spacemedia.service.agencies;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
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
import org.wikimedia.commons.donvip.spacemedia.data.domain.Media;
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

    static final Pattern COPERNICUS_CREDIT = Pattern.compile(
                    ".*Copernicus[ -](?:Sentinel[ -])?dat(?:a|en)(?:/ESA)? [\\(\\[](2[0-9]{3}(?:[-–/][0-9]{2,4})?)[\\)\\]].*",
                    Pattern.CASE_INSENSITIVE);

    static final List<Pattern> COPERNICUS_PROCESSED_BY = Arrays.asList(
            Pattern.compile(
                    ".*Copernicus.*data [\\(\\[]2[0-9]{3}(?:[-–/][0-9]{2,4})?[\\)\\]][ ]?(?:/|,)[ ]?(?:Processed by )?(.*)",
                    Pattern.CASE_INSENSITIVE),
            Pattern.compile(
                    "(?:Basierend auf|Modifizierte und) von der (.*) (?:modifizierten|bearbeitete) Copernicus[ -]Sentinel[ -]Daten [\\(\\[]2[0-9]{3}(?:[-–/][0-9]{2,4})?[\\)\\]]",
                    Pattern.CASE_INSENSITIVE),
            Pattern.compile(
                    "Erstellt mit modifizierten Copernicus[ -]Sentinel[ -]Daten [\\(\\[]2[0-9]{3}(?:[-–/][0-9]{2,4})?[\\)\\]][,]? bearbeitet von (.*)",
                    Pattern.CASE_INSENSITIVE));

    static final List<String> CC_BY_SA_SPELLINGS = Arrays.asList(
            "CC-BY-SA-3.0 IGO", "CC BY-SA IGO 3.0", "CC BY-SA 3.0 IGO", "(CC BY-SA 2.0)", "(CC BY-SA 4.0)");

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

	private Map<String, String> esaMissions;
	private Map<String, String> esaPeople;

    @Autowired
    public EsaService(EsaMediaRepository repository) {
        super(repository);
    }

    @Override
    @PostConstruct
    void init() throws IOException {
        super.init();
        dateFormatter = DateTimeFormatter.ofPattern(datePattern);
		esaMissions = loadCsvMapping("esa.missions.csv");
		esaPeople = loadCsvMapping("esa.people.csv");
    }

	@Scheduled(fixedDelay = 43200000L)
	public void checkEsaCategories() {
		checkCommonsCategories(esaMissions);
		checkCommonsCategories(esaPeople);
	}

    @Override
    protected Class<EsaMedia> getMediaClass() {
        return EsaMedia.class;
    }

    private static Optional<URL> getImageUrl(String src, URL imageUrl) throws MalformedURLException {
        if (src.startsWith("http://") || src.startsWith("https://")) {
            return Optional.of(new URL(src.replace("esamultimeda.esa.int", "esamultimedia.esa.int")));
        } else {
            return Optional.of(new URL(imageUrl.getProtocol(), imageUrl.getHost(), src));
        }
    }

    private void processHeader(EsaMedia image, Element element) {
        image.setTitle(element.getElementsByClass("heading").get(0).text());
        image.setDate(LocalDate.parse(
                element.getElementsByClass("meta").get(0).getElementsByTag("span").get(0).text(), dateFormatter)
                .atStartOfDay());
    }

    private void processShare(EsaMedia image, Element element) {
        String id = element.getElementsByClass("btn ezsr-star-rating-enabled").get(0).attr("id").replace("ezsr_", "");
        image.setId(Integer.parseInt(id.substring(0, id.indexOf('_'))));
    }

    private void processExtra(EsaMedia image, Element element) {
        Element details = element.getElementById("modal__tab-content--details");
        image.setDescription(details.getElementsByClass("modal__tab-description").get(0).getElementsByTag("p").stream()
                .map(Element::text).collect(Collectors.joining("<br>")));
        for (Element li : element.getElementsByClass("modal__meta").get(0).children()) {
            if (li.children().size() == 1 && li.child(0).children().size() > 1) {
                // Weird HTML code for https://www.esa.int/ESA_Multimedia/Images/2015/12/MTG_combined_antenna
                li = li.child(0);
            }
            if (li.children().size() > 1) {
                String title = li.child(0).child(0).attr("title").toLowerCase(Locale.ENGLISH);
                String label = li.child(1).text().trim();
                switch (title) {
                case "copyright":
                    image.setCopyright(label); break;
                case "action":
                    image.setAction(label); break;
                case "activity":
                case "landmark":
                    image.setActivity(label); break;
                case "mission":
                case "rocket":
                    image.setMission(label); break;
                case "people":
                    image.setPeople(label); break;
                case "system":
                case "book":
                    image.setSystems(set(label)); break;
                case "location":
                    image.setLocations(set(label)); break;
                case "keywords":
                    image.setKeywords(set(label)); break;
                case "set":
                case "tags":
                    image.setPhotoSet(label); break;
                default:
                    LOGGER.warn("Unknown title for {}: {}", image, title);
                }
            } else {
                LOGGER.warn("Strange item for {}: {}", image, li);
            }
        }
    }

    private static boolean isCopyrightOk(EsaMedia image) {
        if (image.getCopyright() == null)
            return false;
        String copyrightUppercase = image.getCopyright().toUpperCase(Locale.ENGLISH);
        String descriptionUppercase = image.getDescription().toUpperCase(Locale.ENGLISH);
        return (copyrightUppercase.contains("BY-SA") || copyrightUppercase.contains("COPERNICUS SENTINEL")
                || (copyrightUppercase.contains("COPERNICUS DATA") && descriptionUppercase.contains(" SENTINEL")))
                || ((copyrightUppercase.equals("ESA") || copyrightUppercase.equals("SEE BELOW"))
                        && (descriptionUppercase.contains("BY-SA") || (image.getMission() != null
                                && image.getMission().toUpperCase(Locale.ENGLISH).contains("SENTINEL"))));
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
        }
        if (!mediaInRepo.isPresent() || media.getThumbnailUrl() == null) { // FIXME migration code to remove later
            save = true;
            boolean ok = false;
            for (int i = 0; i < maxTries && !ok; i++) {
                try {
                    Document html = Jsoup.connect(url.toExternalForm()).get();
                    List<URL> files = new ArrayList<>();
                    Optional<URL> lowRes = Optional.empty();
                    for (Element element : html.getElementsByClass("dropdown__item")) {
                        String text = element.text().toUpperCase(Locale.ENGLISH);
                        if (text.startsWith("HI-RES") || text.startsWith("SOURCE")) {
                            getImageUrl(element.attr("href"), url).ifPresent(files::add);
                        } else {
                            lowRes = getImageUrl(element.attr("href"), url);
                        }
                    }
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
                    processHeader(media, html.getElementsByClass("modal__header").get(0));
                    processShare(media, html.getElementsByClass("modal__share").get(0));
                    processExtra(media, html.getElementsByClass("modal__extra").get(0));
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
                if (mediaService.updateMedia(media)) {
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
			} catch (IOException | URISyntaxException | RuntimeException e) {
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
                        Elements divs = html.getElementsByClass("grid-item");
                        for (Element div : divs) {
                            URL imageUrl = new URL(proto, host, div.select("a").get(0).attr("href"));
                            index++;
                            LOGGER.debug("Checking ESA image {}: {}", index, imageUrl);
                            if (checkEsaImage(imageUrl).isPresent()) {
                                count++;
                            }
                        }
                        moreImages = !html.getElementsByClass("paging").get(0)
                                .getElementsByAttributeValue("title", "Next").isEmpty();
                        ok = true;
                    } catch (SocketTimeoutException e) {
                        LOGGER.debug(searchUrl, e);
                    }
                }
			} catch (IOException | RuntimeException e) {
                LOGGER.error(searchUrl, e);
                moreImages = false;
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

    @Override
	public Set<String> findCategories(EsaMedia media, boolean includeHidden) {
		Set<String> result = super.findCategories(media, includeHidden);
		if (includeHidden) {
			result.add("ESA images (review needed)");
		}
		if (media.getMission() != null) {
			String missionCats = esaMissions.get(media.getMission());
			if (StringUtils.isNotBlank(missionCats)) {
				Arrays.stream(missionCats.split(";")).forEach(result::add);
			}
		}
		if (media.getPeople() != null) {
			String peopleCats = esaPeople.get(media.getPeople());
			if (StringUtils.isNotBlank(peopleCats)) {
				Arrays.stream(peopleCats.split(";")).forEach(result::add);
			}
		}
        enrichEsaCategories(result, media);
        return result;
    }

    public static void enrichEsaCategories(Set<String> categories, Media<?, ?> media) {
        if (media.getDescription() != null) {
            if (categories.contains("Photos by Mars Express") && media.getDescription().contains("ESA/DLR/FU Berlin")) {
                categories.remove("Photos by Mars Express");
                categories.add("Photos by HRSC");
            }
            if (categories.contains("Photos by HRSC")
                    // Catch lowercase, normalcase, english and german
                    && (media.getTitle().contains("opographi") || media.getDescription().contains("opographi"))) {
                categories.remove("Photos by HRSC");
                categories.add("Mars false-color topographic views by HRSC");
            }
            if (categories.contains("Photos by HRSC")
                    // Catch lowercase, normalcase, english and german
                    && (media.getTitle().contains("3D") || media.getTitle().contains("3-D") || media.getTitle().contains("naglyph"))) {
                categories.remove("Photos by HRSC");
                categories.add("Mars 3D anaglyphs by HRSC");
            }
            if (categories.contains("Photos by HRSC")
                    // Catch lowercase, normalcase, english and german
                    && (media.getTitle().contains("erspectiv") || media.getTitle().contains("erspektiv"))) {
                categories.remove("Photos by HRSC");
                categories.add("Mars perspective views by HRSC");
            }
        }
    }

    @Override
    public List<String> findTemplates(EsaMedia media) {
        List<String> result = super.findTemplates(media);
        String credit = media.getCopyright();
        for (String spelling : CC_BY_SA_SPELLINGS) {
            credit = credit.replace(", " + spelling, "").replace("; " + spelling, "").replace(" " + spelling, "").trim();
        }
        Matcher m = COPERNICUS_CREDIT.matcher(credit);
        if (m.matches()) {
            result.add("Attribution-Copernicus |year=" + m.group(1));
            credit = getCopernicusProcessedBy(credit).orElse("ESA");
        }
        result.add("ESA|" + credit);
        return result;
    }

    static Optional<String> getCopernicusProcessedBy(String credit) {
        for (Pattern p : COPERNICUS_PROCESSED_BY) {
            Matcher m = p.matcher(credit);
            if (m.matches()) {
                return Optional.of(m.group(1));
            }
        }
        return Optional.empty();
    }
}
