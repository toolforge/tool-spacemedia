package org.wikimedia.commons.donvip.spacemedia.harvester.esa.website.service;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
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
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.core.Harvester;
import org.wikimedia.commons.donvip.spacemedia.data.jpa.Depot;
import org.wikimedia.commons.donvip.spacemedia.data.jpa.DepotRepository;
import org.wikimedia.commons.donvip.spacemedia.data.jpa.MediaPublication;
import org.wikimedia.commons.donvip.spacemedia.data.jpa.MediaPublicationRepository;
import org.wikimedia.commons.donvip.spacemedia.data.jpa.Metadata;
import org.wikimedia.commons.donvip.spacemedia.data.jpa.MetadataRepository;
import org.wikimedia.commons.donvip.spacemedia.data.jpa.PublicationKey;

@Service
public class EsaWebsiteScrappingService implements Harvester {

    private static final Logger LOGGER = LoggerFactory.getLogger(EsaWebsiteScrappingService.class);

    private static final String ACTION = "ACTION";
    private static final String ACTIVITY = "ACTIVITY";
    private static final String MISSION = "MISSION";
    private static final String PEOPLE = "PEOPLE";
    private static final String PHOTOSET = "PHOTOSET";
    private static final String SYSTEMS = "SYSTEMS";
    private static final String LOCATIONS = "LOCATIONS";
    private static final String KEYWORDS = "KEYWORDS";

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
    private MediaPublicationRepository mediaPublicationRepository;

    @Autowired
    private MetadataRepository metadataRepository;

    @Autowired
    private DepotRepository depotRepository;

    @Value("${esa.search.link}")
    private String searchLink;

    @Value("${esa.max.tries}")
    private int maxTries;

    @Value("${esa.date.pattern}")
    private String datePattern;

    private DateTimeFormatter dateFormatter;

    @PostConstruct
    void init() throws IOException {
        dateFormatter = DateTimeFormatter.ofPattern(datePattern);
    }

    @Override
    public void harvestMedia() throws IOException {
        final URL url = new URL(searchLink);
        final String proto = url.getProtocol();
        final String host = url.getHost();
        boolean moreImages = true;
        int index = 0;
        Depot depot = depotRepository.findById("esa-website").orElseThrow();
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
                            checkEsaImage(imageUrl, depot);
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
    }

    private void checkEsaImage(URL url, Depot depot) {
        MediaPublication pub;
        boolean save = false;
        Optional<MediaPublication> pubInRepo = mediaPublicationRepository.findByUrl(url);
        if (pubInRepo.isPresent()) {
            pub = pubInRepo.get();
        } else {
            pub = fetchMedia(url, depot);
            save = true;
        }
        if (!isCopyrightOk(pub)) {
            problem(pub.getUrl(), "Invalid copyright: " + pub.getCredit());
            return;
        }
        if (save) {
            mediaPublicationRepository.save(pub);
        }
    }

    private static final Set<String> set(String label) {
        return new TreeSet<>(Arrays.asList(label.replace(", ", ",").split(",")));
    }

    private void addMetadata(MediaPublication pub, String key, Set<String> values) {
        values.forEach(v -> addMetadata(pub, key, v));
    }

    private void addMetadata(MediaPublication pub, String key, String value) {
        Metadata metadata = new Metadata(key, value);
        Optional<Metadata> opt = metadataRepository.findById(metadata);
        pub.addMetadata(opt.isPresent() ? opt.get() : metadataRepository.save(metadata));
    }

    private MediaPublication fetchMedia(URL url, Depot depot) {
        MediaPublication media = new MediaPublication();
        media.setDepot(depot);
        media.setUrl(url);
        boolean ok = false;
        for (int i = 0; i < maxTries && !ok; i++) {
            try {
                Document html = Jsoup.connect(url.toExternalForm()).get();
                List<URL> files = new ArrayList<>();
                Optional<URL> lowRes = Optional.empty();
                for (Element element : html.getElementsByClass("dropdown__item")) {
                    Optional<URL> urlOpt = getImageUrl(element.attr("href"), url);
                    urlOpt.ifPresent(files::add);
                    String text = element.text().toUpperCase(Locale.ENGLISH);
                    if (!text.startsWith("HI-RES") && !text.startsWith("SOURCE")) {
                        lowRes = urlOpt;
                    }
                }
                if (lowRes.isPresent() && media.getThumbnailUrl() == null) {
                    media.setThumbnailUrl(lowRes.get());
                }
                if (files.isEmpty()) {
                    problem(media.getUrl(), "Image without any file");
                }
                // TODO persist file publications
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

    private static Optional<URL> getImageUrl(String src, URL imageUrl) throws MalformedURLException {
        if (src.startsWith("http://") || src.startsWith("https://")) {
            return Optional.of(new URL(src.replace("esamultimeda.esa.int", "esamultimedia.esa.int")));
        } else {
            return Optional.of(new URL(imageUrl.getProtocol(), imageUrl.getHost(), src));
        }
    }

    private void processHeader(MediaPublication image, Element element) {
        image.setTitle(element.getElementsByClass("heading").get(0).text());
        image.setPublicationDateTime(LocalDate
                .parse(element.getElementsByClass("meta").get(0).getElementsByTag("span").get(0).text(), dateFormatter)
                .atStartOfDay(ZoneOffset.UTC));
    }

    private void processShare(MediaPublication image, Element element) {
        String id = element.getElementsByClass("btn ezsr-star-rating-enabled").get(0).attr("id").replace("ezsr_", "");
        image.setId(new PublicationKey(image.getDepot().getId(), id.substring(0, id.indexOf('_'))));
    }

    private void processExtra(MediaPublication image, Element element) {
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
                    image.setCredit(label); break;
                case "action":
                    addMetadata(image, ACTION, label); break;
                case "activity":
                case "landmark":
                    addMetadata(image, ACTIVITY, label); break;
                case "mission":
                case "rocket":
                    addMetadata(image, MISSION, label); break;
                case "people":
                    addMetadata(image, PEOPLE, label); break;
                case "system":
                case "book":
                    addMetadata(image, SYSTEMS, set(label)); break;
                case "location":
                    addMetadata(image, LOCATIONS, set(label)); break;
                case "keywords":
                    addMetadata(image, KEYWORDS, set(label)); break;
                case "set":
                case "tags":
                    addMetadata(image, PHOTOSET, label); break;
                default:
                    LOGGER.warn("Unknown title for {}: {}", image, title);
                }
            } else {
                LOGGER.warn("Strange item for {}: {}", image, li);
            }
        }
    }

    private static boolean isCopyrightOk(MediaPublication pub) {
        if (pub.getCredit() == null)
            return false;
        String mission = pub.getMetadataValues(MISSION).stream().findFirst().orElse(null);
        String creditUC = pub.getCredit().toUpperCase(Locale.ENGLISH);
        String descriUC = pub.getDescription().toUpperCase(Locale.ENGLISH);
        return (creditUC.contains("BY-SA") || creditUC.contains("COPERNICUS SENTINEL")
                || (creditUC.contains("COPERNICUS DATA") && descriUC.contains(" SENTINEL")))
                || ((creditUC.equals("ESA") || creditUC.equals("SEE BELOW"))
                        && (descriUC.contains("BY-SA")
                                || (mission != null && mission.toUpperCase(Locale.ENGLISH).contains("SENTINEL"))));
    }

    protected final void problem(URL problematicUrl, String errorMessage) {
        // TODO persist
        LOGGER.error("{}: {}", problematicUrl, errorMessage);
    }
}
