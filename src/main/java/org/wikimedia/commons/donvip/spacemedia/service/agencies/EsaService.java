package org.wikimedia.commons.donvip.spacemedia.service.agencies;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
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
import org.wikimedia.commons.donvip.spacemedia.data.domain.Metadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.esa.EsaMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.esa.EsaMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.exception.UploadException;

import com.github.dozermapper.core.Mapper;

@Service
public class EsaService
        extends AbstractFullResAgencyService<EsaMedia, Integer, LocalDateTime, EsaMedia, Integer, LocalDateTime> {

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

    private Map<String, String> esaLocations;
    private Map<String, String> esaMissions;
    private Map<String, String> esaPeople;
    private Map<String, String> esaSystems;

    @Autowired
    public EsaService(EsaMediaRepository repository) {
        super(repository, "esa");
    }

    @Override
    @PostConstruct
    void init() throws IOException {
        super.init();
        dateFormatter = DateTimeFormatter.ofPattern(datePattern);
        esaLocations = loadCsvMapping("esa.locations.csv");
        esaMissions = loadCsvMapping("esa.missions.csv");
        esaPeople = loadCsvMapping("esa.people.csv");
        esaSystems = loadCsvMapping("esa.systems.csv");
    }

    @Scheduled(fixedDelay = 43200000L)
    public void checkEsaCategories() {
        checkCommonsCategories(esaLocations);
        checkCommonsCategories(esaMissions);
        checkCommonsCategories(esaPeople);
        checkCommonsCategories(esaSystems);
    }

    @Override
    protected Class<EsaMedia> getMediaClass() {
        return EsaMedia.class;
    }

    @Override
    protected final Integer getMediaId(String id) {
        return Integer.parseUnsignedInt(id);
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
            media = fetchMedia(url);
            if (media.getMetadata().getAssetUrl() == null) {
                return Optional.empty();
            }
            save = true;
        }
        if (!isCopyrightOk(media)) {
            problem(media.getUrl(), "Invalid copyright: " + media.getCopyright());
            return Optional.empty();
        }
        for (int i = 0; i < maxTries; i++) {
            try {
                if (doCommonUpdate(media)) {
                    save = true;
                }
                if (shouldUploadAuto(media, media.getCommonsFileNames())
                        || shouldUploadAuto(media, media.getFullResCommonsFileNames())) {
                    repository.save(upload(save ? repository.save(media) : media, true));
                    save = false;
                }
                break;
            } catch (IOException | IllegalArgumentException | UploadException e) {
                LOGGER.error(media.toString(), e);
                if (e.getMessage() != null &&
                        (e.getMessage().contains("tiffinfo command failed") || e.getMessage().contains("upstream request timeout"))) {
                    problem(media.getFullResMetadata().getAssetUrl(), e.getMessage());
                    ignoreFile(media, e.getMessage());
                    save = true;
                    break;
                }
            }
        }
        if (save) {
            repository.save(media);
        }
        return Optional.of(media);
    }

    private EsaMedia fetchMedia(URL url) {
        EsaMedia media = new EsaMedia();
        media.setUrl(url);
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
                    }
                }
                int size = files.size();
                if (size == 0) {
                    problem(media.getUrl(), "Image without any file");
                } else if (size == 1) {
                    media.getMetadata().setAssetUrl(files.get(0));
                } else if (size == 2) {
                    // When a file is a tif or png, the other is always a jpg (320+6 occurences),
                    // never something else
                    for (URL fileUrl : files) {
                        if (fileUrl.toExternalForm().endsWith(".tif") || fileUrl.toExternalForm().endsWith(".png")) {
                            media.getFullResMetadata().setAssetUrl(fileUrl);
                        } else {
                            media.getMetadata().setAssetUrl(fileUrl);
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
        return media;
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
            Metadata metadata = media.getMetadata();
            Metadata fullResMetadata = media.getFullResMetadata();
            if ("Envisat".equalsIgnoreCase(media.getMission()) && fullResMetadata.getAssetUrl() != null
                    && CollectionUtils.isEmpty(media.getCommonsFileNames())) {
                ignoreAndSaveFile(media, metadata.getAssetUrl(), "ENVISAT low resolution image", false);
            }
            if (media.isIgnored() == null) {
                // Ignore file from which we get an HTML error page instead of a real image
                if (SHA1_ERROR.equals(metadata.getSha1())) {
                    ignoreAndSaveFile(media, metadata.getAssetUrl(), "HTML error page", true);
                } else {
                    if (SHA1_ERROR.equals(fullResMetadata.getSha1())) {
                        ignoreAndSaveFile(media, fullResMetadata.getAssetUrl(), "HTML error page", true);
                    }
                }
            }
        }
    }

    @Override
    @Scheduled(fixedRateString = "${esa.update.rate}", initialDelayString = "${esa.initial.delay}")
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
    protected EsaMediaRepository getOriginalRepository() {
        return mediaRepository;
    }

    @Override
    protected Integer getOriginalId(String id) {
        return Integer.valueOf(id);
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
    protected Optional<String> getOtherFields(EsaMedia media) {
        StringBuilder sb = new StringBuilder();
        addOtherField(sb, "Action", media.getAction());
        addOtherField(sb, "Activity", media.getActivity());
        addOtherField(sb, "Keyword", media.getKeywords());
        addOtherField(sb, "Location", media.getLocations());
        addOtherField(sb, "Mission", media.getMission(), esaMissions);
        addOtherField(sb, "People", media.getPeople(), esaPeople);
        addOtherField(sb, "Set", media.getPhotoSet());
        addOtherField(sb, "System", media.getSystems(), esaSystems);
        String s = sb.toString();
        return s.isEmpty() ? Optional.empty() : Optional.of(s);
    }

    @Override
    public Set<String> findCategories(EsaMedia media, boolean includeHidden) {
        Set<String> result = super.findCategories(media, includeHidden);
        if (includeHidden) {
            result.add("ESA images (review needed)");
        }
        String mission = Optional.ofNullable(media.getMission()).orElse("");
        if (StringUtils.isNotBlank(mission)) {
            String cats = esaMissions.get(mission);
            if (StringUtils.isNotBlank(cats)) {
                Arrays.stream(cats.split(";")).forEach(result::add);
            }
        }
        if (media.getPeople() != null) {
            String cats = esaPeople.get(media.getPeople());
            if (StringUtils.isNotBlank(cats)) {
                Arrays.stream(cats.split(";")).forEach(result::add);
            }
        }
        if (media.getLocations() != null) {
            for (String location : media.getLocations()) {
                String cats = esaLocations.get(location);
                if (StringUtils.isNotBlank(cats)
                        && ("ESOC".equals(cats) || cats.endsWith(" Station") || (cats.startsWith("Satellite")
                        && (mission.contains("Sentinel") || mission.contains("Envisat") || media.getSystems().contains("Copernicus"))))) {
                    Arrays.stream(cats.split(";")).forEach(result::add);
                }
            }
        }
        enrichEsaCategories(result, media, media.getCopyright());
        return result;
    }

    public static void enrichEsaCategories(Set<String> categories, Media<?, ?> media, String author) {
        if (media.getDescription() != null) {
            enrichEnvisat(categories, media, author);
            enrichExoMars(categories, media, author);
            enrichMarsExpress(categories, media, author);
        }
    }

    private static void enrichEnvisat(Set<String> categories, Media<?, ?> media, String author) {
        String descLc = media.getDescription().toLowerCase(Locale.ENGLISH);
        String titleLc = media.getTitle().toLowerCase(Locale.ENGLISH);
        if (categories.contains("Envisat") &&
                (descLc.contains("medium resolution imaging spectrometer") || descLc.contains("(meris)"))) {
            categories.remove("Envisat");
            if (titleLc.contains("bloom")) {
                categories.add("Photos of phytoplankton by Envisat MERIS");
            } else {
                categories.add("Envisat MERIS images");
            }
        }
        if (categories.contains("Envisat") &&
                (descLc.contains("advanced synthetic aperture radar") || descLc.contains("(asar)"))) {
            categories.remove("Envisat");
            categories.add("Envisat Advanced Synthetic Aperture Radar images");
        }
        if (categories.contains("Envisat") &&
                (descLc.contains("this envisat") || descLc.contains("acquired by envisat"))) {
            categories.remove("Envisat");
            categories.add("Envisat images");
        }
    }

    private static void enrichExoMars(Set<String> categories, Media<?, ?> media, String author) {
        if (author != null && author.startsWith("ESA/Roscosmos/CaSSIS")) {
            categories.remove("ExoMars");
            categories.remove("ExoMars 2016");
            categories.add("Photos by CaSSIS");
        }
    }

    private static void enrichMarsExpress(Set<String> categories, Media<?, ?> media, String author) {
        if ((categories.contains("Mars Express") || categories.contains("Photos by Mars Express"))
                && (media.getDescription().contains("ESA/DLR/FU Berlin") || author.contains("ESA/DLR/FU Berlin"))) {
            categories.remove("Mars Express");
            categories.remove("Photos by Mars Express");
            categories.add("Photos by HRSC");
        }
        String titleLc = media.getTitle().toLowerCase(Locale.ENGLISH);
        if (categories.contains("Photos by HRSC")
                // Catch english and german
                && (titleLc.contains("topographi") || media.getDescription().contains("topographi"))) {
            categories.remove("Photos by HRSC");
            categories.add("Mars false-color topographic views by HRSC");
        }
        if (categories.contains("Photos by HRSC")
                // Catch english and german
                && (titleLc.contains("3d") || titleLc.contains("3-d") || titleLc.contains("anaglyph"))) {
            categories.remove("Photos by HRSC");
            categories.add("Mars 3D anaglyphs by HRSC");
        }
        if (categories.contains("Photos by HRSC")
                // Catch english and german
                && (titleLc.contains("perspectiv") || titleLc.contains("perspektiv"))) {
            categories.remove("Photos by HRSC");
            categories.add("Mars perspective views by HRSC");
        }
        if (categories.contains("Photos by HRSC")
                && (titleLc.contains("black") && titleLc.contains("white"))) {
            categories.remove("Photos by HRSC");
            categories.add("Black and white nadir views of Mars by HRSC");
        }
        if (categories.contains("Photos by HRSC")
                && (titleLc.contains("colour") && !titleLc.contains("false"))) {
            categories.remove("Photos by HRSC");
            categories.add("Colour nadir views of Mars by HRSC");
        }
        if (categories.contains("Photos by HRSC")
                && (titleLc.contains("colour") && titleLc.contains("false"))) {
            categories.remove("Photos by HRSC");
            categories.add("False-colour nadir views of Mars by HRSC");
        }
    }

    @Override
    public Set<String> findTemplates(EsaMedia media) {
        Set<String> result = super.findTemplates(media);
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
