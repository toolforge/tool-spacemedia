package org.wikimedia.commons.donvip.spacemedia.harvester.esa.website.service;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.core.AbstractHarvesterService;
import org.wikimedia.commons.donvip.spacemedia.data.jpa.entity.Depot;
import org.wikimedia.commons.donvip.spacemedia.data.jpa.entity.FilePublication;
import org.wikimedia.commons.donvip.spacemedia.data.jpa.entity.Licence;
import org.wikimedia.commons.donvip.spacemedia.data.jpa.entity.MediaPublication;
import org.wikimedia.commons.donvip.spacemedia.data.jpa.entity.PublicationKey;
import org.wikimedia.commons.donvip.spacemedia.harvester.esa.website.data.EsaMetadataCategory;

@Service
public class EsaWebsiteScrappingService extends AbstractHarvesterService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EsaWebsiteScrappingService.class);

    /**
     * Resulting SHA-1 hash of an HTML error page.
     * See <a href="https://www.esa.int/var/esa/storage/images/esa_multimedia/images/2006/10/envisat_sees_madagascar/10084739-2-eng-GB/Envisat_sees_Madagascar.tiff">this example</a>
     */
    private static final String SHA1_ERROR = "860f6466c5f3da5d62b2065c33aa5548697d817c";

    protected static final Pattern COPERNICUS_CREDIT = Pattern.compile(
                    ".*Copernicus[ -](?:Sentinel[ -])?dat(?:a|en)(?:/ESA)? [\\(\\[](2[0-9]{3}(?:[-–/][0-9]{2,4})?)[\\)\\]].*",
                    Pattern.CASE_INSENSITIVE);

    protected static final List<Pattern> COPERNICUS_PROCESSED_BY = Arrays.asList(
            Pattern.compile(
                    ".*Copernicus.*data [\\(\\[]2[0-9]{3}(?:[-–/][0-9]{2,4})?[\\)\\]][ ]?(?:/|,)[ ]?(?:Processed by )?(.*)",
                    Pattern.CASE_INSENSITIVE),
            Pattern.compile(
                    "(?:Basierend auf|Modifizierte und) von der (.*) (?:modifizierten|bearbeitete) Copernicus[ -]Sentinel[ -]Daten [\\(\\[]2[0-9]{3}(?:[-–/][0-9]{2,4})?[\\)\\]]",
                    Pattern.CASE_INSENSITIVE),
            Pattern.compile(
                    "Erstellt mit modifizierten Copernicus[ -]Sentinel[ -]Daten [\\(\\[]2[0-9]{3}(?:[-–/][0-9]{2,4})?[\\)\\]][,]? bearbeitet von (.*)",
                    Pattern.CASE_INSENSITIVE));

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
        final String depotId = "esa-website";
        LocalDateTime start = startUpdateMedia(depotId);
        final URL url = new URL(searchLink);
        final String proto = url.getProtocol();
        final String host = url.getHost();
        boolean moreMedia = true;
        int count = 0;
        int index = 0;
        Depot depot = depotRepository.findById(depotId).orElseThrow();
        do {
            String searchUrl = searchLink.replace("<idx>", Integer.toString(index));
            try {
                boolean ok = false;
                for (int i = 0; i < maxTries && !ok; i++) {
                    try {
                        LOGGER.debug("Fetching {} media: {}", depotId, searchUrl);
                        Document html = Jsoup.connect(searchUrl).timeout(15_000).get();
                        Elements divs = html.getElementsByClass("grid-item");
                        for (Element div : divs) {
                            URL mediaUrl = new URL(proto, host, div.select("a").get(0).attr("href"));
                            index++;
                            LOGGER.debug("Checking {} media {}: {}", depotId, index, mediaUrl);
                            if (checkEsaMedia(mediaUrl, depot) != null) {
                                count++;
                            }
                        }
                        moreMedia = !html.getElementsByClass("paging").get(0)
                                .getElementsByAttributeValue("title", "Next").isEmpty();
                        ok = true;
                    } catch (SocketTimeoutException e) {
                        LOGGER.debug(searchUrl, e);
                    }
                }
            } catch (IOException | RuntimeException e) {
                LOGGER.error(searchUrl, e);
                moreMedia = false;
            }
        } while (moreMedia);
        endUpdateMedia(depotId, count, start);
    }

    private MediaPublication checkEsaMedia(URL url, Depot depot) {
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
            return null;
        }
        if (save) {
            mediaPublicationRepository.save(pub);
        }
        return pub;
    }

    private MediaPublication fetchMedia(URL url, Depot depot) {
        MediaPublication media = new MediaPublication();
        media.setDepot(depot);
        media.setUrl(url);
        boolean ok = false;
        for (int i = 0; i < maxTries && !ok; i++) {
            try {
                Document html = Jsoup.connect(url.toExternalForm()).get();
                media.setLang(html.getElementsByTag("html").attr("lang"));
                List<URL> files = new ArrayList<>();
                Optional<URL> lowRes = Optional.empty();
                for (Element element : html.getElementsByClass("dropdown__item")) {
                    Optional<URL> urlOpt = getMediaUrl(element.attr("href"), url);
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
                    problem(media.getUrl(), "Media without any file");
                }
                for (URL fileUrl : files) {
                    Optional<FilePublication> filePubInRepo = filePublicationRepository.findByUrl(fileUrl);
                    media.addFilePublication(filePubInRepo.isPresent() ? filePubInRepo.get()
                            : filePublicationRepository
                                    .save(new FilePublication(depot, getFileId(fileUrl.toExternalForm()), fileUrl)));
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

    protected static final String getFileId(String fileUrl) {
        String[] parts = fileUrl.split("/");
        return parts[parts.length - 2].contains("-")
                ? parts[parts.length - 2].split("-")[0]
                : parts[parts.length - 1].split("\\.")[0];
    }

    protected static Optional<URL> getMediaUrl(String src, URL mediaUrl) throws MalformedURLException {
        if (src.startsWith("http://") || src.startsWith("https://")) {
            return Optional.of(new URL(src.replace("esamultimeda.esa.int", "esamultimedia.esa.int")));
        } else {
            return Optional.of(new URL(mediaUrl.getProtocol(), mediaUrl.getHost(), src));
        }
    }

    private void processHeader(MediaPublication media, Element element) {
        media.setTitle(element.getElementsByClass("heading").get(0).text());
        media.setPublicationDateTime(LocalDate
                .parse(element.getElementsByClass("meta").get(0).getElementsByTag("span").get(0).text(), dateFormatter)
                .atStartOfDay(ZoneOffset.UTC));
    }

    private void processShare(MediaPublication media, Element element) {
        String id = element.getElementsByClass("btn ezsr-star-rating-enabled").get(0).attr("id").replace("ezsr_", "");
        media.setId(new PublicationKey(media.getDepot().getId(), id.substring(0, id.indexOf('_'))));
    }

    private void processExtra(MediaPublication media, Element element) {
        Element details = element.getElementById("modal__tab-content--details");
        media.setDescription(details.getElementsByClass("modal__tab-description").get(0).getElementsByTag("p").stream()
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
                    processCopyright(media, label);
                    break;
                default:
                    processDefault(media, title, label);
                }
            } else {
                LOGGER.warn("Strange item for {}: {}", media, li);
            }
        }
    }

    private void processCopyright(MediaPublication media, String label) {
        media.setCredit(label);
        String strippedLabel = label.replace(" ", "").replace("-", "");
        if (strippedLabel.contains("CCBYSA3.0IGO") || strippedLabel.contains("CCBYSAIGO3.0")) {
            media.setLicence(Licence.CC_BY_SA_3_0_IGO);
        } else if (strippedLabel.contains("CCBYSA4.0")) {
            media.setLicence(Licence.CC_BY_SA_4_0);
        } else if (strippedLabel.contains("CCBYSA2.0")) {
            media.setLicence(Licence.CC_BY_SA_2_0);
        } else {
            LOGGER.warn("Unknown licence for {}: {}", media, label);
        }
    }

    private void processDefault(MediaPublication media, String title, String label) {
        for (EsaMetadataCategory cat : EsaMetadataCategory.values()) {
            if (cat.getMarkers().contains(title)) {
                if (cat.isMultiValues()) {
                    addMetadata(media, cat.name(), set(label));
                } else {
                    addMetadata(media, cat.name(), label);
                }
                return;
            }
        }
        LOGGER.warn("Unknown title for {}: {}", media, title);
    }

    private static boolean isCopyrightOk(MediaPublication pub) {
        if (pub.getCredit() == null)
            return false;
        String mission = pub.getMetadataValues(EsaMetadataCategory.MISSION.name()).stream().findFirst().orElse(null);
        String creditUC = pub.getCredit().toUpperCase(Locale.ENGLISH);
        String descriUC = pub.getDescription().toUpperCase(Locale.ENGLISH);
        return (creditUC.contains("BY-SA") || creditUC.contains("COPERNICUS SENTINEL")
                || (creditUC.contains("COPERNICUS DATA") && descriUC.contains(" SENTINEL")))
                || ((creditUC.equals("ESA") || creditUC.equals("SEE BELOW"))
                        && (descriUC.contains("BY-SA")
                                || (mission != null && mission.toUpperCase(Locale.ENGLISH).contains("SENTINEL"))));
    }
}
