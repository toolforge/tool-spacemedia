package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import static java.util.regex.Pattern.compile;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.newURL;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.restTemplateSupportingAll;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.tuple.Pair;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.ImageDimensions;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.aster.NasaAsterMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.aster.NasaAsterMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.library.NasaMediaType;
import org.wikimedia.commons.donvip.spacemedia.exception.UploadException;
import org.wikimedia.commons.donvip.spacemedia.service.GeometryService;
import org.wikimedia.commons.donvip.spacemedia.utils.Emojis;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class NasaAsterService
        extends AbstractOrgService<NasaAsterMedia, String, LocalDate> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NasaAsterService.class);

    private static final Map<DateTimeFormatter, Pattern> ADDED = Map.of(
            DateTimeFormatter.ofPattern("M/d/yyyy h:m:s a", Locale.ENGLISH),
            compile(".*Added: (\\d{1,2}/\\d{1,2}/\\d{4} \\d{1,2}:\\d{1,2}:\\d{1,2} [AP]M)$"),
            DateTimeFormatter.ofPattern("M/d/yyyy", Locale.ENGLISH), compile(".*Added: (\\d{1,2}/\\d{1,2}/\\d{4})$"));

    private static final Map<DateTimeFormatter, List<Pattern>> ACQUIRED = Map.of(
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
            List.of(compile(".*ASTER image was acquired (\\d{1,2} [A-Z][a-z]+, \\d{4}).*")),
            DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH),
            List.of(compile(".*ASTER image (?:on|from) (\\d{1,2} [A-Z][a-z]+ \\d{4}).*"),
                    compile(".*acquired (?:on )?(\\d{1,2} [A-Za-z]+ \\d{4}).*")),
            DateTimeFormatter.ofPattern("yyyy MMMM d", Locale.ENGLISH),
            List.of(compile(".*(\\d{4})\\..+([A-Z][a-z]+ \\d{1,2}) ASTER .*"),
                    compile(".*(\\d{4}) \\(ASTER image from ([A-Z][a-z]+ \\d{1,2})\\).*"),
                    compile(".*(\\d{4}).+ ASTER image (?:was )?acquired on [A-Za-z]+, ([A-Z][a-z]+ \\d{1,2}).*")));

    private static final Pattern SIZE = compile(".*Size: \\( ?([\\d,.]+)(?: (MB|KB|bytes))?\\).*");

    private static final Pattern RESOLUTION = compile(".*Resolution \\( ([\\d,]+) x ([\\d,]+) \\).*");

    @Value("${nasa.aster.gallery.url}")
    private String galleryUrl;

    @Value("${nasa.aster.details.url}")
    private String detailsUrl;

    @Autowired
    private ObjectMapper jackson;

    @Autowired
    private GeometryService geometry;

    public NasaAsterService(NasaAsterMediaRepository repository) {
        super(repository, "nasa.aster");
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
    protected String getMediaId(String id) {
        return id;
    }

    @Override
    protected boolean checkBlocklist() {
        return false;
    }

    @Override
    public URL getSourceUrl(NasaAsterMedia media, FileMetadata metadata) {
        return newURL(detailsUrl.replace("<id>", media.getId()));
    }

    @Override
    protected String getAuthor(NasaAsterMedia media) {
        return "NASA/METI/AIST/Japan Space Systems, and U.S./Japan ASTER Science Team";
    }

    @Override
    protected Optional<Temporal> getCreationDate(NasaAsterMedia media) {
        return Optional.ofNullable(media.getDate());
    }

    @Override
    protected final Optional<Temporal> getUploadDate(NasaAsterMedia media) {
        return Optional.of(media.getPublicationDate());
    }

    @Override
    public void updateMedia() throws IOException, UploadException {
        LocalDateTime start = startUpdateMedia();
        List<NasaAsterMedia> uploadedMedia = new ArrayList<>();
        int count = 0;
        for (AsterItem item : jackson.readValue(
                restTemplateSupportingAll(Charset.forName("Windows-1252")).getForObject(galleryUrl, String.class),
                AsterItem[].class)) {
            try {
                updateImage(item, uploadedMedia);
            } catch (IOException | UploadException | RuntimeException e) {
                LOGGER.error(e.getMessage(), e);
            }
            ongoingUpdateMedia(start, "ASTER", count++);
        }
        endUpdateMedia(count, uploadedMedia, start);
    }

    private NasaAsterMedia updateImage(AsterItem item, List<NasaAsterMedia> uploadedMedia)
            throws IOException, UploadException {
        boolean save = false;
        NasaAsterMedia media = null;
        Optional<NasaAsterMedia> imageInDb = repository.findById(item.getName());
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
    public Set<String> findCategories(NasaAsterMedia media, FileMetadata metadata, boolean includeHidden) {
        Set<String> result = super.findCategories(media, metadata, includeHidden);
        String continent = geometry.getContinent(media.getLatitude(), media.getLongitude());
        result.add(continent != null ? "Photos of " + continent + " by ASTER" : "Photos by ASTER");
        return result;
    }

    @Override
    protected String getSource(NasaAsterMedia media, FileMetadata metadata) {
        return super.getSource(media, metadata) + " ([" + metadata.getAssetUrl() + " direct link])\n"
                + "{{NASA-image|id=" + media.getId() + "|center=JPL}}";
    }

    @Override
    public Set<String> findInformationTemplates(NasaAsterMedia media) {
        Set<String> result = super.findInformationTemplates(media);
        result.add("NASA Photojournal/attribution|class=Terra|mission=Terra|name=Terra|credit=ASTER");
        return result;
    }

    @Override
    public Set<String> findLicenceTemplates(NasaAsterMedia media) {
        Set<String> result = super.findLicenceTemplates(media);
        result.add("PD-USGov-NASA");
        return result;
    }

    @Override
    protected Map<String, Pair<Object, Map<String, Object>>> getStatements(NasaAsterMedia media,
            FileMetadata metadata) {
        Map<String, Pair<Object, Map<String, Object>>> result = super.getStatements(media, metadata);
        result.put("P170", Pair.of("Q584697", null)); // Created by Terra
        result.put("P1071", Pair.of("Q663611", null)); // Created in low earth orbit
        result.put("P2079", Pair.of("Q725252", null)); // Satellite imagery
        result.put("P4082", Pair.of("Q298019", null)); // Taken with ASTER instrument
        return result;
    }

    @Override
    protected NasaAsterMedia refresh(NasaAsterMedia media) throws IOException {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    protected Set<String> getEmojis(NasaAsterMedia uploadedMedia) {
        return Set.of(Emojis.SATELLITE, Emojis.EARTH_AMERICA);
    }

    @Override
    protected Set<String> getTwitterAccounts(NasaAsterMedia uploadedMedia) {
        return Set.of("@NASAEarth");
    }

    private NasaAsterMedia fetchMedia(AsterItem item) throws IOException {
        NasaAsterMedia image = new NasaAsterMedia();
        image.setId(item.getName());
        image.setLongName(item.getLname());
        image.setCategory(item.getCat());
        image.setIcon(item.getIcon());
        image.setLatitude(item.getLat());
        image.setLongitude(item.getLng());
        String imgUrlLink = detailsUrl.replace("<id>", item.getName());
        LOGGER.info(imgUrlLink);
        Document html = Jsoup.connect(imgUrlLink).timeout(15_000).get();
        fillMediaWithHtml(html, image);
        return image;
    }

    static LocalDate extractAcquisitionDate(NasaAsterMedia image) {
        String text = image.getDescription();
        for (Entry<DateTimeFormatter, List<Pattern>> e : ACQUIRED.entrySet()) {
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
                    }
                    return LocalDate.parse(sb.toString(), e.getKey());
                }
            }
        }
        LOGGER.warn("No acquisition date found for {}", image);
        return null;
    }

    static LocalDateTime extractPublicationDate(Document html) {
        String meta = html.getElementsByTag("tr").get(1).getElementsByTag("table").get(0).nextElementSibling()
                .getElementsByTag("tr").first().nextElementSibling().nextElementSibling().getElementsByTag("tr").get(0)
                .getElementsByTag("td").first().getElementsByTag("td").get(2).text();
        for (Entry<DateTimeFormatter, Pattern> e : ADDED.entrySet()) {
            Matcher m = e.getValue().matcher(meta);
            if (m.matches()) {
                String text = m.group(1);
                DateTimeFormatter format = e.getKey();
                return format.toString().contains("Value(MinuteOfHour)") ? LocalDateTime.parse(text, format)
                        : LocalDate.parse(text, format).atStartOfDay();
            }
        }
        throw new IllegalStateException("No publication date found: " + meta);
    }

    void fillMediaWithHtml(Document html, NasaAsterMedia image) {
        Element bodyText = Objects.requireNonNull(html.getElementsByClass("BodyText").first(), "BodyText");
        Elements tables = bodyText.getElementsByTag("table");
        image.setPublicationDate(extractPublicationDate(html));
        // Title
        image.setTitle(Objects.requireNonNull(bodyText.getElementsByTag("font").first(), "font").text());
        // Image at top is used as thumbnail
        image.setThumbnailUrl(
                newURL(tables.get(0).getElementsByTag("img").first().attr("src").replace("http://", "https://")));
        // Description and acquisition date
        image.setDescription(tables.get(1).getElementsByTag("td").text());
        image.setDate(extractAcquisitionDate(image));
        // FIXME ASTER entries can be videos too
        // https://asterweb.jpl.nasa.gov/gallery-detail.asp?name=fuji
        image.setMediaType(NasaMediaType.image);
        tables.get(1).siblingElements();
        for (int i = 2; i < tables.size(); i++) {
            Element table = tables.get(i);
            String meta = table.getElementsByTag("td").get(1).text();
            Matcher mr = RESOLUTION.matcher(meta);
            if (mr.matches()) {
                FileMetadata metadata = addMetadata(image,
                        table.getElementsByTag("a").first().attr("href").replace("http://", "https://"),
                        md -> md.setImageDimensions(new ImageDimensions(intval(mr.group(1)), intval(mr.group(2)))));
                Matcher ms = SIZE.matcher(meta);
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
                    throw new IllegalStateException("No size found: " + meta);
                }
            } else {
                throw new IllegalStateException("No resolution found: " + meta);
            }
        }
    }

    private static Integer intval(String s) {
        return Integer.valueOf(numval(s));
    }

    private static String numval(String s) {
        return s.replace(",", "");
    }

    static class AsterItem {
        private double lat;
        private double lng;
        private String name;
        private String lname;
        @JsonProperty("Cat")
        private String cat;
        private String icon;

        public double getLat() {
            return lat;
        }

        public void setLat(double lat) {
            this.lat = lat;
        }

        public double getLng() {
            return lng;
        }

        public void setLng(double lng) {
            this.lng = lng;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getLname() {
            return lname;
        }

        public void setLname(String lname) {
            this.lname = lname;
        }

        public String getCat() {
            return cat;
        }

        public void setCat(String cat) {
            this.cat = cat;
        }

        public String getIcon() {
            return icon;
        }

        public void setIcon(String icon) {
            this.icon = icon;
        }

        @Override
        public String toString() {
            return "AsterItem [lat=" + lat + ", lng=" + lng + ", name=" + name + ", lname=" + lname + ", cat=" + cat
                    + ", icon=" + icon + "]";
        }
    }
}
