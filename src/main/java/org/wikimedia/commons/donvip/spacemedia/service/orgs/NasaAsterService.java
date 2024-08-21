package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import static java.util.regex.Pattern.compile;
import static org.wikimedia.commons.donvip.spacemedia.service.wikimedia.WikidataItem.Q725252_SATELLITE_IMAGERY;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.extractDate;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.getWithJsoup;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.newURL;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.restTemplateSupportingAll;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.codec.binary.StringUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.CompositeMediaId;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.ImageDimensions;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.aster.NasaAsterMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.aster.NasaAsterMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.exception.UploadException;
import org.wikimedia.commons.donvip.spacemedia.service.InternetArchiveService;
import org.wikimedia.commons.donvip.spacemedia.service.wikimedia.GlitchTip;
import org.wikimedia.commons.donvip.spacemedia.service.wikimedia.SdcStatements;
import org.wikimedia.commons.donvip.spacemedia.utils.Emojis;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;

@Service
public class NasaAsterService extends AbstractOrgService<NasaAsterMedia> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NasaAsterService.class);

    private static final Map<DateTimeFormatter, Pattern> ADDED_PATTERNS = Map.of(
            DateTimeFormatter.ofPattern("M/d/yyyy h:m:s a", Locale.ENGLISH),
            compile(".*Added: (\\d{1,2}/\\d{1,2}/\\d{4} \\d{1,2}:\\d{1,2}:\\d{1,2} [AP]M)$"),
            DateTimeFormatter.ofPattern("M/d/yyyy", Locale.ENGLISH), compile(".*Added: (\\d{1,2}/\\d{1,2}/\\d{4})$"));

    private static final Map<DateTimeFormatter, List<Pattern>> ACQUIRED_PATTERNS = Map.of(
            DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH),
            List.of(
                    compile(".*(?:ASTER|acquired)(?: on)? (?:20 )?(?:[A-Z][a-z]+, )?([A-Z][a-z]+ \\d{1,2}, \\d{4}).*"),
                    compile(".*ASTER image from ([A-Z][a-z]+ \\d{1,2}, \\d{4}).*"),
                    compile(".* ([A-Z][a-z]+ \\d{1,2}, \\d{4}) (?:by )?\\(?ASTER\\)?.*"),
                    compile(".* ([A-Z][a-z]+ \\d{1,2}, \\d{4}), acquired by ASTER.*"),
                    compile(".*ASTER captured th.+ image.* on ([A-Z][a-z]+ \\d{1,2}, \\d{4}).*"),
                    compile(".* ([A-Z][a-z]+ \\d{1,2}, \\d{4}),? when (?:the )?ASTER (?:image was )?acquired.*"),
                    compile(".* ([A-Z][a-z]+ \\d{1,2}, \\d{4}) (?:ASTER|image|sub-scene).*"),
                    compile(".*ASTER image .+ as seen on ([A-Z][a-z]+ \\d{1,2}, \\d{4}),.*"),
                    compile(".*ASTER .* on ([A-Z][a-z]+ \\d{1,2}, \\d{4}).*"),
                    compile(".* ([A-Z][a-z]+ \\d{1,2}, \\d{4}).+ acquired by ASTER.*"),
                    compile(".*[Oo]n ([A-Z][a-z]+ \\d{1,2}, \\d{4}),.*"),
                    compile(".*acquired .+ ([A-Z][a-z]+ \\d{1,2},) .+ (\\d{4}) image.*"),
                    compile(".* ([A-Z][a-z]+ \\d{1,2}), when .+(?:ASTER)? (?:thermal )?image was acquired.*"),
                    compile(".* ([A-Z][a-z]+ \\d{1,2}), ASTER (?:captured|acquired).*"),
                    compile(".* ([A-Z][a-z]+ \\d{1,2}), .+ can be seen.*"),
                    compile(".*ASTER captured this image .*on (?:[A-Z][a-z]+, )?([A-Z][a-z]+ \\d{1,2})(?:\\.| at).*"),
                    compile(".*ASTER image .+ was captured .+ ([A-Z][a-z]+ \\d{1,2}) [a-z].*"),
                    compile(".*acquired(?: by ASTER)?(?: on)? ([A-Z][a-z]+ \\d{1,2}).*")),
            DateTimeFormatter.ofPattern("MMMM d yyyy", Locale.ENGLISH),
            List.of(compile(".*ASTER .*image .+ ([A-Z][a-z]+ \\d{1,2} \\d{4}).*")),
            DateTimeFormatter.ofPattern("MMMMd, yyyy", Locale.ENGLISH),
            List.of(compile(".*(?:ASTER|acquired)(?: on)? ([A-Z][a-z]+\\d{1,2}, \\d{4}).*")),
            DateTimeFormatter.ofPattern("d MMMM, yyyy", Locale.ENGLISH),
            List.of(compile(".*ASTER image was acquired (\\d{1,2} [A-Z][a-z]+, \\d{4}).*"),
                    compile(".*acquired(?: by ASTER)?(?: on)? ([A-Z][a-z]+, \\d{4}).*")),
            DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH),
            List.of(compile(".*ASTER image (?:on|from) (\\d{1,2} [A-Z][a-z]+ \\d{4}).*"),
                    compile(".*acquired (?:on )?(\\d{1,2} [A-Za-z]+ \\d{4}).*")),
            DateTimeFormatter.ofPattern("yyyy MMMM d", Locale.ENGLISH),
            List.of(compile(".*(\\d{4})\\..+([A-Z][a-z]+ \\d{1,2}) ASTER .*"),
                    compile(".*(\\d{4}) \\(ASTER image from ([A-Z][a-z]+ \\d{1,2})\\).*"),
                    compile(".*(\\d{4}).+ ASTER image (?:was )?acquired on [A-Za-z]+, ([A-Z][a-z]+ \\d{1,2}).*")));

    private static final Pattern SIZE = compile(".*Size: \\( ?([\\d,.]+)(?: (MB|KB|bytes))?\\).*");

    private static final Pattern RESOLUTION = compile(".*Resolution \\( ([\\d,.]+) x ([\\d,.]+) \\).*");

    @Value("${nasa.aster.gallery.url}")
    private String galleryUrl;

    @Value("${nasa.aster.details.url}")
    private String detailsUrl;

    @Lazy
    @Autowired
    private InternetArchiveService internetArchive;

    public NasaAsterService(NasaAsterMediaRepository repository) {
        super(repository, "nasa.aster", Set.of("aster"));
    }

    @Override
    protected Class<NasaAsterMedia> getMediaClass() {
        return NasaAsterMedia.class;
    }

    @Override
    public String getName() {
        return "NASA (ASTER)";
    }

    @Override
    protected String hiddenUploadCategory(String repoId) {
        return "Spacemedia ASTER files uploaded by " + commonsService.getAccount();
    }

    @Override
    protected boolean checkBlocklist() {
        return false;
    }

    @Override
    public URL getSourceUrl(NasaAsterMedia media, FileMetadata metadata, String ext) {
        return newURL(detailsUrl.replace("<id>", media.getId().getMediaId()));
    }

    @Override
    protected String getAuthor(NasaAsterMedia media, FileMetadata metadata) {
        return "NASA/METI/AIST/Japan Space Systems, and U.S./Japan ASTER Science Team";
    }

    @Override
    public void updateMedia(String[] args) throws IOException, UploadException {
        LocalDateTime start = startUpdateMedia();
        List<NasaAsterMedia> uploadedMedia = new ArrayList<>();
        int count = 0;
        for (AsterItem item : fetchItems()) {
            try {
                updateImage(item, uploadedMedia);
            } catch (IOException | UploadException | RuntimeException e) {
                LOGGER.error(e.getMessage(), e);
                GlitchTip.capture(e);
            }
            ongoingUpdateMedia(start, "ASTER", count++);
        }
        endUpdateMedia(count, uploadedMedia, start);
    }

    private AsterItem[] fetchItems() throws JsonProcessingException, RestClientException {
        return jackson.readValue(
                restTemplateSupportingAll(Charset.forName("Windows-1252")).getForObject(galleryUrl, String.class),
                AsterItem[].class);
    }

    private NasaAsterMedia updateImage(AsterItem item, List<NasaAsterMedia> uploadedMedia)
            throws IOException, UploadException {
        boolean save = false;
        NasaAsterMedia media = null;
        Optional<NasaAsterMedia> imageInDb = repository.findById(new CompositeMediaId("aster", item.name()));
        if (imageInDb.isPresent()) {
            media = imageInDb.get();
        } else {
            media = fetchMedia(item);
            save = true;
        }
        if (media.getDescription().contains("ÔÇÖ")) {
            media.setDescription(media.getDescription().replace("ÔÇÖ", "'"));
            save = true;
        }
        if (doCommonUpdate(media)) {
            save = true;
        }
        if (shouldUploadAuto(media, false)) {
            media = saveMedia(upload(save ? saveMedia(media) : media, true, false).getLeft());
            uploadedMedia.add(media);
            save = false;
        }
        if (save) {
            media = saveMedia(media);
        }
        return media;
    }

    @Override
    protected boolean isSatellitePicture(NasaAsterMedia media, FileMetadata metadata) {
        return true;
    }

    @Override
    public Set<String> findCategories(NasaAsterMedia media, FileMetadata metadata, boolean includeHidden) {
        Set<String> result = super.findCategories(media, metadata, includeHidden);
        String continent = geometry.getContinent(media.getLatitude(), media.getLongitude());
        result.add(continent != null ? "Photos of " + continent + " by ASTER" : "Photos by ASTER");
        return result;
    }

    @Override
    protected String getSource(NasaAsterMedia media, FileMetadata metadata) {
        return super.getSource(media, metadata) + " ([" + metadata.getAssetUrl() + " direct link])\n"
                + "{{NASA-image|id=" + media.getId().getMediaId() + "|center=JPL}}";
    }

    @Override
    public Set<String> findAfterInformationTemplates(NasaAsterMedia media, FileMetadata metadata) {
        Set<String> result = super.findAfterInformationTemplates(media, metadata);
        result.add("NASA Photojournal/attribution|class=Terra|mission=Terra|name=Terra|credit=ASTER");
        return result;
    }

    @Override
    public Set<String> findLicenceTemplates(NasaAsterMedia media, FileMetadata metadata) {
        Set<String> result = super.findLicenceTemplates(media, metadata);
        result.add("PD-USGov-NASA");
        return result;
    }

    @Override
    protected SdcStatements getStatements(NasaAsterMedia media, FileMetadata metadata) {
        return super.getStatements(media, metadata).creator("Q584697") // Created by Terra
                .locationOfCreation("Q663611") // Created in low earth orbit
                .fabricationMethod(Q725252_SATELLITE_IMAGERY)
                .capturedWith("Q298019"); // Taken with ASTER instrument
    }

    @Override
    protected NasaAsterMedia refresh(NasaAsterMedia media) throws IOException {
        return media.copyDataFrom(fetchMedia(
                Arrays.stream(fetchItems()).filter(x -> StringUtils.equals(x.name(), media.getId().getMediaId()))
                        .findAny().orElseThrow(() -> new IOException("ASTER item not found"))));
    }

    @Override
    protected Set<String> getEmojis(NasaAsterMedia uploadedMedia) {
        return Set.of(Emojis.SATELLITE, Emojis.EARTH_AMERICA);
    }

    private NasaAsterMedia fetchMedia(AsterItem item) throws IOException {
        NasaAsterMedia image = new NasaAsterMedia();
        image.setId(new CompositeMediaId("aster", item.name()));
        image.setLongName(item.lname());
        image.setCategory(item.cat());
        image.setIcon(item.icon());
        image.setLatitude(item.lat());
        image.setLongitude(item.lng());
        try {
            String url = detailsUrl.replace("<id>", item.name());
            Document document = getWithJsoup(url, 15_000, 3, false);
            if ("Object moved".equals(document.getElementsByTag("head").text())) {
                url = internetArchive.retrieveOldestUrl(url)
                        .orElseThrow(() -> new IllegalStateException(
                                item + " cannot be retrieved from ASTER website nor Internet Archive"))
                        .toExternalForm();
                fillMediaWithHtml(getWithJsoup(url, 15_000, 3, true), image, url);
            } else {
                fillMediaWithHtml(document, image, url);
            }
        } catch (IOException | RuntimeException e) {
            LOGGER.error("Error while fetching {}: {}", image, e.getMessage());
            GlitchTip.capture(e);
            throw e;
        }
        return image;
    }

    static LocalDate extractAcquisitionDate(NasaAsterMedia image) {
        String text = image.getDescription();
        for (Entry<DateTimeFormatter, List<Pattern>> e : ACQUIRED_PATTERNS.entrySet()) {
            for (Pattern p : e.getValue()) {
                Matcher m = p.matcher(text);
                if (m.matches()) {
                    StringBuilder sb = new StringBuilder(
                            m.group(1).replace("june", "June").replace("Jan ", "January "));
                    for (int i = 2; i <= m.groupCount(); i++) {
                        sb.append(' ').append(m.group(i));
                    }
                    if (e.getKey().toString().contains("','' 'Value(YearOfEra,4") && !sb.toString().contains(",")) {
                        sb.append(", ").append(image.getPublicationDate().getYear());
                    } else if (e.getKey().toString().startsWith("Value(DayOfMonth)") && sb.charAt(0) >= 'A'
                            && sb.charAt(0) <= 'Z') {
                        sb.insert(0, "1 ");
                    }
                    return LocalDate.parse(sb.toString(), e.getKey());
                }
            }
        }
        LOGGER.warn("No acquisition date found for {}", image);
        return null;
    }

    ZonedDateTime extractPublicationDate(Document html, String url) {
        String meta = html.getElementsByTag("center").first()
                .getElementsByTag("tr").get(1).getElementsByTag("table").get(0).nextElementSibling()
                .getElementsByTag("tr").first().nextElementSibling().nextElementSibling().getElementsByTag("tr").get(0)
                .getElementsByTag("td").first().getElementsByTag("td").get(2).text();
        return extractDate(meta, ADDED_PATTERNS).or(() -> {
            return url.contains("://web.archive.org/web/") ? Optional.of(internetArchive.extractTimestamp(url))
                    : Optional.empty();
        }).orElseThrow(() -> new IllegalStateException("No publication date found for " + url + " => " + meta));
    }

    void fillMediaWithHtml(Document html, NasaAsterMedia image, String url) {
        Element bodyText = Objects.requireNonNull(html.getElementsByClass("BodyText").first(), "BodyText");
        Elements tables = bodyText.getElementsByTag("table");
        Element img = tables.get(0).getElementsByTag("img").first();
        image.setPublicationDateTime(extractPublicationDate(html, url));
        // Title
        image.setTitle(Objects.requireNonNull(bodyText.getElementsByTag("font").first(), "font").text());
        // Image at top is used as thumbnail
        image.setThumbnailUrl(newURL(img.attr("src").replace("http://", "https://")));
        // Description and acquisition date
        for (Element table : tables) {
            if ("5".equals(table.attr("cellspacing"))) {
                image.setDescription(table.getElementsByTag("td").text());
                break;
            }
        }
        image.setCreationDate(extractAcquisitionDate(image));
        // FIXME ASTER entries can be MOV videos too
        // https://asterweb.jpl.nasa.gov/gallery-detail.asp?name=fuji
        for (int i = 2; i < tables.size(); i++) {
            Element table = tables.get(i);
            Elements tds = table.getElementsByTag("td");
            if (tds.size() > 1) {
                String meta = tds.get(1).text();
                Matcher mr = RESOLUTION.matcher(meta);
                Matcher ms = SIZE.matcher(meta);
                if (mr.matches() || ms.matches()) {
                    FileMetadata metadata = addMetadata(image,
                            table.getElementsByTag("a").first().attr("href").replace("http://", "https://"),
                            null);
                    fillMetadataResolution(image, meta, mr, metadata);
                    fillMetadataSize(image, meta, ms, metadata);
                } else {
                    LOGGER.warn("No size and no resolution found for {}: {}", image, meta);
                }
            }
        }
        if (!image.hasMetadata()) {
            String href = img.parent().attr("href").replace("http://", "https://");
            if (href.contains("://web.archive.org/web/")) {
                href = href.substring(href.indexOf("/http") + 1);
            }
            addMetadata(image, href, null);
        }
    }

    private static void fillMetadataResolution(NasaAsterMedia image, String meta, Matcher mr, FileMetadata metadata) {
        if (mr.matches()) {
            metadata.setImageDimensions(new ImageDimensions(intval(mr.group(1)), intval(mr.group(2))));
        } else {
            LOGGER.warn("No resolution found for {}: {}", image, meta);
        }
    }

    private static void fillMetadataSize(NasaAsterMedia image, String meta, Matcher ms, FileMetadata metadata) {
        if (ms.matches()) {
            String unit = ms.groupCount() >= 2 ? ms.group(2) : null;
            Long size = "MB".equals(unit) ? 1024 * 1024 : "KB".equals(unit) ? 1024L : 1L;
            if (ms.group(1).contains(".")) {
                size = (long) (size * Double.valueOf(ms.group(1)));
            } else {
                size *= Long.valueOf(numval(ms.group(1)));
            }
            metadata.setSize(size);
        } else {
            LOGGER.warn("No size found for {}: {}", image, meta);
        }
    }

    private static Integer intval(String s) {
        return Integer.valueOf(numval(s));
    }

    private static String numval(String s) {
        return s.replace(",", "").replace(".", "");
    }

    static record AsterItem(double lat, double lng, String name, String lname, @JsonProperty("Cat") String cat,
            String icon) {
    }
}
