package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import static java.lang.Double.parseDouble;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static java.util.Locale.ENGLISH;
import static java.util.Optional.empty;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.extractDate;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.getWithJsoup;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.newURL;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.replace;

import java.io.IOException;
import java.net.MalformedURLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.geo.Point;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.CompositeMediaId;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.ImageDimensions;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.photojournal.NasaPhotojournalMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.photojournal.NasaPhotojournalMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.service.nasa.NasaMappingService;
import org.wikimedia.commons.donvip.spacemedia.service.wikimedia.CommonsService;
import org.wikimedia.commons.donvip.spacemedia.service.wikimedia.SdcStatements;

@Service
public class NasaPhotojournalService extends AbstractOrgHtmlGalleryService<NasaPhotojournalMedia> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NasaPhotojournalService.class);

    static final Pattern ANIMATION_PATTERN = Pattern.compile(
            ".*<a href=\"(https?://[^\"]+\\.(?:gif|mp4))\".*");

    static final Pattern AUDIO_PATTERN = Pattern.compile(
            ".*<a href=\"(https?://[^\"]+\\.(?:wav|mp3|flac|midi))\".*");

    static final Pattern QTVR_PATTERN = Pattern.compile(
            ".*<a href=\"(https?://[^\"]+\\.mov)\".*");

    static final Pattern FIGURE_PATTERN = Pattern.compile(
            "<a href=\"(https?://[^\"]+/(?:figures|archive)/[^\"]+\\.(?:jpg|png|tiff))\"");

    static final Pattern ACQ_PATTERN = Pattern.compile(
            ".*acquired ((?:January|February|March|April|May|June|July|August|September|October|November|December) \\d{1,2}, [1-2]\\d{3}).*");

    static final Pattern LOCATION_PATTERN = Pattern.compile(
            ".*located at (\\d+\\.?\\d*) degrees (north|south), (\\d+\\.?\\d*) degrees (west|east).*");

    private static final DateTimeFormatter ACQ_DATE_FORMAT = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.US);

    private static final String BASE_URL = "https://photojournal.jpl.nasa.gov";

    @Lazy
    @Autowired
    private NasaMappingService mappings;

    @Value("${nasa.photojournal.geohack.globes}")
    private Set<String> globes;

    @Lazy
    @Autowired
    private NasaAsterService asterService;

    @Autowired
    public NasaPhotojournalService(NasaPhotojournalMediaRepository repository) {
        super(repository, "nasa.photojournal", Set.of("photojournal"));
    }

    @Override
    public String getName() {
        return "NASA (Photojournal)";
    }

    @Override
    protected String hiddenUploadCategory(String repoId) {
        return "Spacemedia Photojournal files uploaded by " + commonsService.getAccount();
    }

    @Override
    protected boolean checkBlocklist() {
        return false;
    }

    @Override
    protected List<AbstractOrgService<?>> getSimilarOrgServices(NasaPhotojournalMedia media) {
        if (media.getInstrument() != null) {
            switch (media.getInstrument().toLowerCase(ENGLISH)) {
                case "aster": return List.of(asterService);
            }
        }
        return List.of();
    }

    @Override
    protected List<String> fetchGalleryUrls(String repoId) throws IOException {
        return getWithJsoup(BASE_URL + "/Help/ImageGallery.html", 10_000, 3).getElementById("middle_2_border")
                .getElementsByTag("h2").first().nextElementSibling().nextElementSibling()
                .getElementsByTag("a").stream().map(a -> BASE_URL + a.attr("href")).toList();
    }

    @Override
    protected String getGalleryPageUrl(String galleryUrl, int page) {
        return galleryUrl + "?start=" + 100 * (page - 1);
    }

    @Override
    protected Elements getGalleryItems(String repoId, String url, Element html) {
        Elements result = new Elements();
        Elements trs = html.getElementById("middle_2_border").child(1).getElementsByTag("caption").get(1)
                .nextElementSibling().getElementsByTag("tr");
        for (int i = 1; i < trs.size() - 1; i += 2) {
            Element item = new Element("fake");
            item.appendChildren(List.of(trs.get(i), trs.get(i + 1)));
            // https://photojournal.jpl.nasa.gov/keywords/dp?start=100
            // PIA19856 breaks the table with a weird additional row :'(
            if (item.child(1).childrenSize() == 1) {
                item.appendChild(trs.get(i++ + 2)); // NOSONAR
            }
            result.add(item);
        }
        return result;
    }

    @Override
    protected Optional<Temporal> extractDateFromGalleryItem(Element result) {
        return extractDate(result.child(0).child(1).text(), List.of(ISO_LOCAL_DATE));
    }

    @Override
    protected String extractIdFromGalleryItem(String url, Element result) {
        return result.child(result.childrenSize() - 1).getElementsByTag("dt").first().text().replace(":", "");
    }

    @Override
    protected boolean ignoreNonFreeFiles(NasaPhotojournalMedia media) {
        String credit = media.getCredits();
        return !media.isIgnored() && !credit.contains("NASA") && !credit.contains("JPL")
                && !credit.contains("Jet Propulsion Laboratory") && !credit.contains("USSF")
                && mediaService.ignoreMedia(media, "Non-free content");
    }

    @Override
    List<NasaPhotojournalMedia> fillMediaWithHtml(String url, Document doc, Element galleryItem,
            NasaPhotojournalMedia media) throws IOException {
        Elements dds = doc.getElementsByTag("dd");
        String caption = dds.get(0).html();
        media.setDescription(caption);
        media.setCredits(dds.get(1).text());
        Element cap = doc.getElementsByTag("caption").first();
        media.setTitle(cap.text().split(":")[1].trim());
        media.setThumbnailUrl(newURL(BASE_URL + "/thumb/" + media.getIdUsedInOrg() + ".jpg"));
        ImageDimensions dims = null;
        for (Element tr : cap.nextElementSibling().child(0).child(1)
                .getElementsByTag("tr")) {
            String key = tr.child(0).text().replaceAll("[\\h:\\-]", "");
            String val = StringUtils.strip(tr.child(1).text());
            switch (key) {
            case "TargetName":
                media.setTarget(val);
                break;
            case "Mission":
                media.setMission(val);
                break;
            case "Spacecraft":
                media.setSpacecraft(val);
                break;
            case "Instrument":
                media.setInstrument(val);
                break;
            case "ProducedBy":
                media.setProducer(val);
                break;
            case "ProductSize":
                String[] tab = val.split(" ");
                dims = new ImageDimensions(Integer.parseInt(tab[0]), Integer.parseInt(tab[2]));
                break;
            case "FullResTIFF", "FullResJPEG":
                ImageDimensions idims = dims;
                addMetadata(media, BASE_URL + tr.child(1).getElementsByTag("a").first().attr("href"),
                        m -> m.setImageDimensions(idims));
                break;
            default:
                LOGGER.info("Ignored key: {}", key);
            }
        }
        boolean isAnimation = media.containsInTitleOrDescriptionOrKeywords("animation");
        boolean isQtvr = media.containsInTitleOrDescriptionOrKeywords("qtvr");
        if (isAnimation || isQtvr) {
            addMetadataFromPattern(isAnimation ? ANIMATION_PATTERN : QTVR_PATTERN, caption, media);
        }
        addMetadataFromPattern(AUDIO_PATTERN, caption, media);
        detectFigures(media);
        detectCreationDate(media);
        String href = doc.getElementsByClass("browseView").first().getElementsByTag("a").first().attr("href");
        if (href.contains("/animation/")) {
            getWithJsoup(href.startsWith("/") ? BASE_URL + href : href, 10_000, 3).getElementsByTag("li").stream()
                    .map(li -> li.getElementsByTag("a").first().attr("href")).forEach(link -> {
                        String animUrl = link.startsWith("/") ? BASE_URL + link : link;
                        if (!media.containsMetadata(animUrl)) {
                            addMetadata(media, animUrl, null);
                        }
                    });
        }
        return List.of(media);
    }

    private boolean addMetadataFromPattern(Pattern pattern, String caption, NasaPhotojournalMedia media) {
        boolean result = false;
        Matcher m = pattern.matcher(caption);
        while (m.find()) {
            String url = m.group(1);
            if (!media.containsMetadata(url)) {
                addMetadata(media, url, null);
                result = true;
            }
        }
        return result;
    }

    boolean detectFigures(NasaPhotojournalMedia media) {
        String caption = media.getDescription();
        return caption.contains("<img ") && addMetadataFromPattern(FIGURE_PATTERN, caption, media);
    }

    static boolean detectCreationDate(NasaPhotojournalMedia media) {
        boolean result = false;
        if (media.getCreationDate() == null) {
            Matcher m = ACQ_PATTERN.matcher(media.getDescription());
            if (m.matches()) {
                media.setCreationDate(ACQ_DATE_FORMAT.parse(m.group(1), LocalDate::from));
                result = true;
            }
        }
        return result;
    }

    @Override
    protected String getSourceUrl(CompositeMediaId id) {
        return "https://photojournal.jpl.nasa.gov/catalog/" + id.getMediaId();
    }

    @Override
    protected final Pair<String, Map<String, String>> getWikiFileDesc(NasaPhotojournalMedia media, FileMetadata metadata)
            throws MalformedURLException {
        // https://commons.wikimedia.org/wiki/Template:NASA_Photojournal/attribution/mission
        String lang = getLanguage(media);
        String desc = getDescription(media, metadata);
        StringBuilder sb = new StringBuilder("{{NASA Photojournal\n| catalog = ").append(media.getId().getMediaId())
                .append("\n| image= ").append(media.isImage()).append("\n| video= ").append(media.isVideo())
                .append("\n| animation= ").append("gif".equals(metadata.getFileExtensionOnCommons()))
                .append("\n| mission= ").append(media.getMission())
                .append("\n| instrument= ").append(media.getInstrument()).append("\n| caption = ").append("{{")
                .append(lang).append("|1=").append(CommonsService.formatWikiCode(desc))
                .append("}}\n| credit= ").append(media.getCredits());
        getUploadDate(media).ifPresent(s -> sb.append("\n| addition_date = ").append(toIso8601(s)));
        sb.append("\n| creation_date = ");
        getCreationDate(media).ifPresent(sb::append);
        if (globes.contains(media.getTarget())) {
            sb.append("\n| globe= ").append(media.getTarget());
        }
        getLocation(media)
                .ifPresent(p -> sb.append("\n| lat= ").append(p.getX()).append("\n| long= ").append(p.getY()));
        appendWikiOtherVersions(sb, media, metadata, "gallery");
        if (List.of("gif", "mp4").contains(metadata.getFileExtension())) {
            sb.append("\n| link= ").append(metadata.getAssetUrl());
        }
        sb.append("\n}}");
        return Pair.of(sb.toString(), Map.of(lang, desc));
    }

    protected final Optional<Point> getLocation(NasaPhotojournalMedia media) {
        Matcher m = LOCATION_PATTERN.matcher(media.getDescription());
        return m.matches()
                ? Optional.of(new Point(
                        parseDouble(m.group(1)) * ("north".equalsIgnoreCase(m.group(2)) ? 1 : -1),
                        parseDouble(m.group(3)) * ("east".equalsIgnoreCase(m.group(4)) ? 1 : -1)))
                : Optional.empty();
    }

    @Override
    public Set<String> findCategories(NasaPhotojournalMedia media, FileMetadata metadata, boolean includeHidden) {
        Set<String> result = super.findCategories(media, metadata, includeHidden);
        result.add("NASA Photojournal entries from " + media.getYear() + '|' + media.getId().getMediaId());
        if (media.containsInTitleOrDescriptionOrKeywords("anaglyph")) {
            result.add("Moon".equalsIgnoreCase(media.getTarget()) ? "Anaglyphs of the Moon" : "Anaglyphs");
        }
        if ("gif".equals(metadata.getFileExtensionOnCommons())) {
            result.add("Mars".equalsIgnoreCase(media.getTarget()) ? "Animated GIF of Mars" : "Animated GIF files");
        }
        if ("mov".equals(metadata.getFileExtension())) {
            // Not sure what to do about these files
        }
        findCategoryFromMapping(media.getInstrument(), "instrument", mappings.getNasaInstruments())
                .ifPresent(result::add);
        findCategoryFromMapping(media.getMission(), "mission", mappings.getNasaMissions()).ifPresent(result::add);
        if ("Mars".equalsIgnoreCase(media.getTarget())) {
            replace(result, "2001 Mars Odyssey", "Photos of Mars by 2001 Mars Odyssey");
            replace(result, "Photos by THEMIS", "Photos of Mars by THEMIS");
        }
        return result;
    }

    @Override
    public Set<String> findLicenceTemplates(NasaPhotojournalMedia media, FileMetadata metadata) {
        Set<String> result = super.findLicenceTemplates(media, metadata);
        result.add("JPL Image Copyright");
        return result;
    }

    @Override
    protected NasaPhotojournalMedia refresh(NasaPhotojournalMedia media) throws IOException {
        return media.copyDataFrom(fetchMedia(media.getId(), empty()));
    }

    @Override
    protected Class<NasaPhotojournalMedia> getMediaClass() {
        return NasaPhotojournalMedia.class;
    }

    @Override
    protected Map<String, String> getLegends(NasaPhotojournalMedia media, Map<String, String> descriptions) {
        Map<String, String> result = new TreeMap<>(super.getLegends(media, descriptions));
        String legend = result.get("en");
        if (legend != null && legend.startsWith("<")) {
            if (legend.contains("Today's")) {
                result.put("en", legend.substring(legend.indexOf("Today's")));
            } else if (legend.contains("This VIS ")) {
                result.put("en", legend.substring(legend.indexOf("This VIS ")));
            }
        }
        return result;
    }

    @Override
    protected SdcStatements getStatements(NasaPhotojournalMedia media, FileMetadata metadata) {
        SdcStatements result = super.getStatements(media, metadata);
        wikidataStatementMapping(media.getInstrument(), mappings.getNasaInstruments(), "P4082", result); // Taken with
        wikidataStatementMapping(media.getSpacecraft(), mappings.getNasaMissions(), "P170", result); // Created by
        return result;
    }
}
