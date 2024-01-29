package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import static java.util.Collections.emptyList;
import static org.wikimedia.commons.donvip.spacemedia.utils.CsvHelper.loadCsvMapping;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.getWithJsoup;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.newURL;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.CompositeMediaId;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.Media;
import org.wikimedia.commons.donvip.spacemedia.data.domain.esa.EsaMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.esa.EsaMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.exception.UploadException;
import org.wikimedia.commons.donvip.spacemedia.utils.Emojis;

@Service
public class EsaService extends AbstractOrgService<EsaMedia> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EsaService.class);

    static final List<Pattern> COPERNICUS_PROCESSED_BY = Arrays.asList(
            Pattern.compile(
                    ".*Copernicus.*data [\\(\\[]2\\d{3}(?:[-–/]\\d{2,4})?[\\)\\]] ?(?:/|,) ?(?:Processed by )?(.*)",
                    Pattern.CASE_INSENSITIVE),
            Pattern.compile(
                    "(?:Basierend auf|Modifizierte und) von der (.*) (?:modifizierten|bearbeitete) Copernicus[ -]Sentinel[ -]Daten [\\(\\[]2\\d{3}(?:[-–/]\\d{2,4})?[\\)\\]]",
                    Pattern.CASE_INSENSITIVE),
            Pattern.compile(
                    "Erstellt mit modifizierten Copernicus[ -]Sentinel[ -]Daten [\\(\\[]2\\d{3}(?:[-–/]\\d{2,4})?[\\)\\]],? bearbeitet von (.*)",
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

    private DateTimeFormatter dateFormatter;

    private Map<String, String> esaLocations;
    private Map<String, String> esaMissions;
    private Map<String, String> esaPeople;
    private Map<String, String> esaSystems;

    @Autowired
    public EsaService(EsaMediaRepository repository) {
        super(repository, "esa", Set.of("esa"));
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
    protected boolean checkBlocklist() {
        return false;
    }

    private static Optional<URL> getImageUrl(String src, URL imageUrl) {
        if (src.startsWith("http://") || src.startsWith("https://")) {
            return Optional.of(newURL(src.replace("esamultimeda.esa.int", "esamultimedia.esa.int")));
        } else {
            return Optional.of(newURL(imageUrl.getProtocol(), imageUrl.getHost(), src));
        }
    }

    private void processHeader(EsaMedia image, Element element) {
        image.setTitle(element.getElementsByClass("heading").get(0).text());
        image.setPublicationDate(LocalDate.parse(
                element.getElementsByClass("meta").get(0).getElementsByTag("span").get(0).text(), dateFormatter));
    }

    private void processShare(EsaMedia image, Element element) {
        String id = element.getElementsByClass("btn ezsr-star-rating-enabled").get(0).attr("id").replace("ezsr_", "");
        image.setId(new CompositeMediaId("esa", id.substring(0, id.indexOf('_'))));
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
                    image.setCredits(label); break;
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
        if (image.getCredits() == null)
            return false;
        String copyrightUppercase = image.getCredits().toUpperCase(Locale.ENGLISH);
        String descriptionUppercase = image.getDescription().toUpperCase(Locale.ENGLISH);
        return (copyrightUppercase.contains("BY-SA") || copyrightUppercase.contains("COPERNICUS SENTINEL")
                || (copyrightUppercase.contains("COPERNICUS DATA") && descriptionUppercase.contains(" SENTINEL")))
                || ((copyrightUppercase.equals("ESA") || copyrightUppercase.equals("SEE BELOW"))
                        && (descriptionUppercase.contains("BY-SA") || (image.getMission() != null
                                && image.getMission().toUpperCase(Locale.ENGLISH).contains("SENTINEL"))));
    }

    private Triple<Optional<EsaMedia>, Collection<FileMetadata>, Integer> checkEsaImage(URL url) {
        EsaMedia media;
        boolean save = false;
        Optional<EsaMedia> mediaInRepo = mediaRepository.findByUrl(url);
        if (mediaInRepo.isPresent()) {
            media = mediaInRepo.get();
        } else {
            media = fetchMedia(url);
            if (!media.hasMetadata()) {
                return Triple.of(Optional.empty(), emptyList(), 0);
            }
            if (!isCopyrightOk(media)) {
                problem(media.getUrl(), "Invalid copyright: " + media.getCredits());
                return Triple.of(Optional.empty(), emptyList(), 0);
            }
            save = true;
        }
        int uploadCount = 0;
        List<FileMetadata> uploadedMetadata = new ArrayList<>();
        for (int i = 0; i < maxTries; i++) {
            try {
                if (doCommonUpdate(media)) {
                    save = true;
                }
                if (shouldUploadAuto(media, false)) {
                    Triple<EsaMedia, Collection<FileMetadata>, Integer> upload = upload(save ? saveMedia(media) : media,
                            true, false);
                    uploadCount += upload.getRight();
                    uploadedMetadata.addAll(upload.getMiddle());
                    saveMedia(upload.getLeft());
                    save = false;
                }
                break;
            } catch (IOException | IllegalArgumentException | UploadException e) {
                LOGGER.error(media.toString(), e);
                if (e.getMessage() != null && e.getMessage().contains("tiffinfo command failed")) {
                    save = mediaService.ignoreMedia(media, media.getMetadata().toString(), e);
                    break;
                }
            }
        }
        return Triple.of(Optional.of(saveMediaOrCheckRemote(save, media)), uploadedMetadata, uploadCount);
    }

    private EsaMedia fetchMedia(URL url) {
        EsaMedia media = new EsaMedia();
        media.setUrl(url);
        try {
            Document html = getWithJsoup(url.toExternalForm(), 10_000, maxTries);
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
            }
            for (URL file : files) {
                addMetadata(media, file, null);
            }
            if (size > 1) {
                checkAssetUrlCorrectness(media);
            }
            processHeader(media, html.getElementsByClass("modal__header").get(0));
            processShare(media, html.getElementsByClass("modal__share").get(0));
            processExtra(media, html.getElementsByClass("modal__extra").get(0));
        } catch (IOException | IllegalStateException e) {
            LOGGER.error(url.toExternalForm(), e);
        }
        return media;
    }

    private void checkAssetUrlCorrectness(EsaMedia media) {
        if (media.getMetadataStream().filter(m -> m.getAssetUrl() != null).count() < 2) {
            problem(media.getUrl(), "Image without two asset URLs");
        }
    }

    private static final Set<String> set(String label) {
        return new TreeSet<>(Arrays.asList(label.replace(", ", ",").split(",")));
    }

    @Override
    public void updateMedia(String[] args) throws IOException {
        LocalDateTime start = startUpdateMedia();
        final URL url = newURL(searchLink);
        final String proto = url.getProtocol();
        final String host = url.getHost();
        boolean moreImages = true;
        int count = 0;
        int index = 0;
        List<FileMetadata> uploadedMetadata = new ArrayList<>();
        List<EsaMedia> uploadedMedia = new ArrayList<>();
        do {
            String searchUrl = searchLink.replace("<idx>", Integer.toString(index));
            try {
                Document html = getWithJsoup(searchUrl, 15_000, maxTries);
                Elements divs = html.getElementsByClass("grid-item");
                for (Element div : divs) {
                    URL imageUrl = newURL(proto, host, div.select("a").get(0).attr("href"));
                    index++;
                    LOGGER.debug("Checking ESA image {}: {}", index, imageUrl);
                    Triple<Optional<EsaMedia>, Collection<FileMetadata>, Integer> check = checkEsaImage(imageUrl);
                    Optional<EsaMedia> optionalMedia = check.getLeft();
                    if (optionalMedia.isPresent()) {
                        count++;
                        if (check.getRight() > 0) {
                            uploadedMetadata.addAll(check.getMiddle());
                            uploadedMedia.add(optionalMedia.get());
                        }
                    }
                }
                moreImages = !html.getElementsByClass("paging").get(0).getElementsByAttributeValue("title", "Next")
                        .isEmpty();
            } catch (IOException | RuntimeException e) {
                LOGGER.error(searchUrl, e);
                moreImages = false;
            }
        } while (moreImages);

        endUpdateMedia(count, uploadedMedia, uploadedMetadata, start);
    }

    @Override
    public String getName() {
        return "ESA";
    }

    @Override
    public URL getSourceUrl(EsaMedia media, FileMetadata metadata) {
        URL url = media.getUrl();
        String externalForm = url.toExternalForm();
        if (externalForm.contains("://www.esa.int/spaceinimages/layout/set/html_npl/Images/")) {
            url = newURL(externalForm.replace("layout/set/html_npl/", ""));
        }
        return url;
    }

    @Override
    protected String getAuthor(EsaMedia media) {
        if (media.getCredits().contains("/")) {
            String authors = media.getCredits().replace("CC BY-SA 3.0 IGO", "").trim();
            if (authors.endsWith(",") && authors.length() > 2) {
                return authors.replace(",", "").trim();
            }
        }
        return "European Space Agency";
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
    public Set<String> findCategories(EsaMedia media, FileMetadata metadata, boolean includeHidden) {
        Set<String> result = super.findCategories(media, metadata, includeHidden);
        if (includeHidden) {
            result.add("ESA images (review needed)");
        }
        String mission = Optional.ofNullable(media.getMission()).orElse("");
        if (StringUtils.isNotBlank(mission)) {
            String cats = esaMissions.get(mission);
            if (StringUtils.isNotBlank(cats)) {
                Arrays.stream(cats.split(";")).forEach(result::add);
            }
            if (mission.matches("Sentinel-\\d.?")) {
                result.addAll(findCategoriesForEarthObservationImage(media, x -> "Photos of " + x + " by " + mission,
                        mission + " images", true, true, true));
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
        enrichEsaCategories(result, media, media.getCredits());
        return result;
    }

    public static void enrichEsaCategories(Set<String> categories, Media media, String author) {
        if (media.getDescription() != null) {
            enrichEnvisat(categories, media, author);
            enrichExoMars(categories, media, author);
            enrichMarsExpress(categories, media, author);
        }
    }

    private static void enrichEnvisat(Set<String> categories, Media media, String author) {
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

    private static void enrichExoMars(Set<String> categories, Media media, String author) {
        if (author != null && author.startsWith("ESA/Roscosmos/CaSSIS")) {
            categories.remove("ExoMars");
            categories.remove("ExoMars 2016");
            categories.add("Photos by CaSSIS");
        }
    }

    private static void enrichMarsExpress(Set<String> categories, Media media, String author) {
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
    public Set<String> findLicenceTemplates(EsaMedia media, FileMetadata metadata) {
        Set<String> result = super.findLicenceTemplates(media, metadata);
        String credit = media.getCredits();
        for (String spelling : CC_BY_SA_SPELLINGS) {
            credit = credit.replace(", " + spelling, "").replace("; " + spelling, "").replace(" " + spelling, "").trim();
        }
        String copernicusTemplate = getCopernicusTemplate(credit);
        if (copernicusTemplate != null) {
            result.add(copernicusTemplate);
            credit = getCopernicusProcessedBy(credit).orElse("ESA");
        }
        result.add("ESA|" + credit);
        return result;
    }

    @Override
    protected EsaMedia refresh(EsaMedia media) throws IOException {
        return media.copyDataFrom(fetchMedia(media.getUrl()));
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

    @Override
    protected Set<String> getEmojis(EsaMedia uploadedMedia) {
        Set<String> result = super.getEmojis(uploadedMedia);
        fillSet(result, uploadedMedia.getMission(),
                Map.of("Ariel", Emojis.STARS, "BepiColombo", Emojis.PLANET_WITH_RINGS, "CHEOPS",
                        Emojis.PLANET_WITH_RINGS, "Euclid", Emojis.STARS, "ExoMars", Emojis.PLANET_WITH_RINGS, "Gaia",
                        Emojis.STARS, "Juice", Emojis.PLANET_WITH_RINGS, "XMM-Newton", Emojis.STARS));
        Set<String> systems = uploadedMedia.getSystems();
        if (systems != null) {
            for (String system : systems) {
                fillSet(result, system, Map.of("Copernicus", Emojis.FLAG_EUR));
            }
        }
        fillSet(result, uploadedMedia.getActivity(),
                Map.of("Human Spaceflight", Emojis.ASTRONAUT, "Observing the Earth", Emojis.EARTH_EUROPE, "Operations",
                        Emojis.ANTENNA, "Space Science", Emojis.SATELLITE, "Space Transportation", Emojis.ROCKET));
        return result;
    }

    @Override
    protected Set<String> getTwitterAccounts(EsaMedia uploadedMedia) {
        Set<String> result = new HashSet<>();
        fillSet(result, uploadedMedia.getMission(),
                Map.of("Ariel", "@ESAArielMission", "BepiColombo", "@BepiColombo", "CHEOPS", "@ESA_CHEOPS", "Euclid",
                        "@ESA_Euclid", "ExoMars", "@ExoMars_CaSSIS", "Gaia", "@ESAGaia", "Juice", "@ESA_JUICE",
                        "XMM-Newton", "@ESA_XMM"));
        Set<String> systems = uploadedMedia.getSystems();
        if (systems != null) {
            for (String system : systems) {
                fillSet(result, system, Map.of("Copernicus", "@CopernicusEU"));
            }
        }
        fillSet(result, uploadedMedia.getActivity(),
                Map.of("Human Spaceflight", "@esaspaceflight", "Observing the Earth", "@ESA_EO", "Operations",
                        "@esaoperations", "Photo Archive (ESA Publications)", "@ESA_History", "Space Science",
                        "@esascience", "Technology", "@esa_tech", "Space Transportation", "@ESA_transport"));
        if (result.isEmpty()) {
            result.add("@esa");
        }
        return result;
    }

    private static void fillSet(Set<String> result, String text, Map<String, String> map) {
        if (text != null) {
            for (Entry<String, String> e : map.entrySet()) {
                if (text.contains(e.getKey())) {
                    result.add(e.getValue());
                }
            }
        }
    }
}
