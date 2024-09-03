package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import static java.util.regex.Pattern.compile;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.newURL;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.CompositeMediaId;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.copernicus.gallery.CopernicusGalleryMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.copernicus.gallery.CopernicusGalleryMediaRepository;

@Service
public class CopernicusGalleryService extends AbstractOrgHtmlGalleryService<CopernicusGalleryMedia> {

    private static final String BASE_URL = "https://www.copernicus.eu";

    static final String BY = ".*(?:by|from|using) ";
    static final String ON = ",? ?(?:on|of|for|from|between) ?";
    static final String ACQ = " (?:to )?(?:acquired?|acquisitions?|captured?|provided?|measured?|collected|retrieved?|observed?|taken?|imaged?|obtained?|sensed?|seen?|produced?|created?)(?: using data| from measurements)? ";
    static final String DAY = "(?:[1-9]|\\d{2})(?:st|rd|th)?";
    static final String MONTH = "(?:January|February|March|April|May|June|July|August|September|October|November|December)";
    static final String DATE = "(?:the )?(?:" + DAY + ", )?(?:" + DAY + " (?:,|and|to) )?(" + DAY + " " + MONTH
            + " 20\\d{2})";
    static final String NO_YEAR = "(?:\\.|,? \\(?[a-z])";
    static final String DAY_MONTH = "(?:" + DAY
            + " and )?(?:Monday |Tuesday |Wednesday |Thursday |Friday |Saturday |Sunday )?(" + DAY + " " + MONTH + ")";
    static final String MONTH_DAY_YEAR = "(" + MONTH + " " + DAY + ", 20\\d{2})";
    static final String MONTH_DAY_NO_YEAR = "(" + MONTH + " " + DAY + ")" + NO_YEAR;
    static final String SATS = "(?:the OLCI \\(Ocean Land and Colour Instrument\\) of )?(?:the Tropomi instrument on ?board )?(?:the radar on ?board )?(?:the SLSTR radiometer of )?(?:a |one of )?(?:the )?(?:two )?(?:satellites of the )?(?:Copernicus )?Sentie?nel[- ]?\\d ?[A-P]? ?(?:and Sentinel-\\d ?[A-P]?)?(?: [mM]ission| [sS]atellites?)?(?: data)?";

    private static final Map<DateTimeFormatter, List<Pattern>> ACQUIRED_PATTERNS = Map
            .of(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH),
                    List.of(compile(".*" + ACQ + ON + DATE + ".*"),
                            compile(".*" + ACQ + ON + DAY_MONTH + ".*"),
                            compile(".*" + ACQ + BY + SATS + ".*" + ON + DATE + ".*"),
                            compile(".*" + ACQ + BY + SATS + ".*" + ON + DAY_MONTH + ".*"),
                            compile(".*" + SATS + " image(?:s|ry)?(?: shows?)?" + ON + DATE + ".*"),
                            compile(".*" + SATS + " image(?:s|ry)?(?: shows?)?" + ON + DAY_MONTH + ".*"),
                            compile(".*" + SATS + ACQ + ".*" + ON + DATE + ".*"),
                            compile(".*" + SATS + ACQ + ".*" + ON + DAY_MONTH + ".*"),
                            compile(".* " + DATE + ".*" + ACQ + BY + SATS + ".*"),
                            compile(".* " + DATE + ".*" + SATS + ACQ + ".*"),
                            compile(".* " + DATE + ".*" + SATS + " image" + ".*"),
                            compile(".* " + DAY_MONTH + ".*" + ACQ + "by " + SATS + ".*"),
                            compile(".* " + DAY_MONTH + ".*" + SATS + ACQ + ".*"),
                            compile(".* " + DAY_MONTH + ".*" + SATS + " image" + ".*"),
                            compile(".* acquisition of this image" + ON + DATE + ".*"),
                            compile(".* acquisition of this image" + ON + DAY_MONTH + ".*"),
                            compile(".* acquisitions from " + SATS + ON + DATE + ".*"),
                            compile(".* acquisitions from " + SATS + ON + DAY_MONTH + ".*"),
                            compile(".* forecast.*" + ON + DATE + ".*"),
                            compile(".* forecast.*" + ON + DAY_MONTH + ".*")),
                    DateTimeFormatter.ofPattern("yyyy MMMM d", Locale.ENGLISH),
                    List.of(compile(".*" + ACQ + BY + SATS + ON + MONTH_DAY_NO_YEAR + ".*")),
                    DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH),
                    List.of(compile(".*" + ACQ + BY + SATS + ON + MONTH_DAY_YEAR + ".*")));

    @Value("${copernicus.max.tries:5}")
    private int maxTries;

    @Autowired
    public CopernicusGalleryService(CopernicusGalleryMediaRepository repository) {
        super(repository, "copernicus", Set.of("copernicus"));
    }

    @Override
    public String getName() {
        return "Copernicus";
    }

    @Override
    protected List<String> fetchGalleryUrls(String repoId) {
        return List.of(BASE_URL + "/en/media/image-day");
    }

    @Override
    protected String getGalleryPageUrl(String galleryUrl, int page) {
        return galleryUrl + "?page=" + page;
    }

    @Override
    protected Elements getGalleryItems(String repoId, String url, Element html) {
        return html.getElementsByClass("search-results-item-details");
    }

    @Override
    public Set<String> findLicenceTemplates(CopernicusGalleryMedia media, FileMetadata metadata) {
        return Set.of("Attribution-Copernicus|" + media.getYear().getValue());
    }

    @Override
    public Set<String> findCategories(CopernicusGalleryMedia media, FileMetadata metadata, boolean includeHidden) {
        Set<String> result = super.findCategories(media, metadata, includeHidden);
        categorizationService.findCategoriesForSentinels(media, result);
        return result;
    }

    @Override
    protected boolean checkBlocklist(CopernicusGalleryMedia media) {
        return false;
    }

    @Override
    protected boolean isSatellitePicture(CopernicusGalleryMedia media, FileMetadata metadata) {
        return true;
    }

    @Override
    protected Map<String, String> getLegends(CopernicusGalleryMedia media, Map<String, String> descriptions) {
        return replaceLegendByCorrectSentence("en", "Sentinel-", descriptions);
    }

    @Override
    protected Optional<Temporal> extractDateFromGalleryItem(Element result) {
        return Optional.of(ZonedDateTime.parse(result.getElementsByTag("time").first().attr("datetime")));
    }

    @Override
    protected String extractIdFromGalleryItem(String url, Element result) {
        String href = result.getElementsByTag("a").first().attr("href");
        return href.substring(href.lastIndexOf('/') + 1);
    }

    @Override
    protected String getSourceUrl(CompositeMediaId id) {
        return BASE_URL + "/en/media/image-day-gallery/" + id.getMediaId();
    }

    @Override
    List<CopernicusGalleryMedia> fillMediaWithHtml(String url, Document html, Element galleryItem,
            CopernicusGalleryMedia media) {
        Element section = html.getElementsByTag("main")
                .first().getElementsByTag("section").first();
        media.setTitle(section.getElementsByTag("h1").first().text());
        media.setThumbnailUrl(newURL(BASE_URL
                + section.getElementsByClass("field_image_day").first().getElementsByTag("img").first().attr("src")));

        String prevLine = null;
        for (String line : section.getElementsByClass("col-12 col-md-6 order-md-first m-0").first()
                .getElementsByClass("mb-1").first().wholeText().strip().split("\n")) {
            if ("Location:".equals(prevLine)) {
                media.setLocation(line.strip());
            } else if ("Credit:".equals(prevLine)) {
                media.setCredits(line.strip());
            }
            if (isNotBlank(line)) {
                prevLine = line.strip();
            }
        }

        media.setKeywords(section.getElementsByClass("col-12 col-md-6 order-md-first m-0").first()
                .getElementsByClass("search-tag-btn").stream()
                .map(Element::text).filter(x -> !"Image of The Day".equals(x)).collect(toSet()));
        media.setDescription(section.getElementsByClass("ec-content").text());
        media.setCreationDate(extractAcquisitionDate(media));
        String href = section.getElementsByClass("card-body").get(1).getElementsByTag("a").first().attr("href");
        addMetadata(media, BASE_URL + href.substring(href.lastIndexOf('=') + 1), null);
        return List.of(media);
    }

    LocalDate extractAcquisitionDate(CopernicusGalleryMedia image) {
        String text = image.getDescription();
        for (Entry<DateTimeFormatter, List<Pattern>> e : ACQUIRED_PATTERNS.entrySet()) {
            for (Pattern p : e.getValue()) {
                Matcher m = p.matcher(text);
                if (m.matches()) {
                    StringBuilder sb = new StringBuilder(m.group(1).replace("rd", "").replace("th", ""));
                    if (e.getKey().toString().startsWith("Value(YearOfEra,4") && !sb.toString().startsWith("20")) {
                        sb.insert(0, image.getPublicationDate().getYear()).insert(4, ' ');
                    } else if (e.getKey().toString().contains("' 'Value(YearOfEra,4")
                            && !sb.toString().contains(" 20")) {
                        sb.append(' ').append(image.getPublicationDate().getYear());
                    }
                    return LocalDate.parse(sb.toString(), e.getKey());
                }
            }
        }
        return null;
    }

    @Override
    protected CopernicusGalleryMedia refresh(CopernicusGalleryMedia media) throws IOException {
        return media.copyDataFrom(fetchMedia(media.getId(), Optional.empty()));
    }

    @Override
    protected Class<CopernicusGalleryMedia> getMediaClass() {
        return CopernicusGalleryMedia.class;
    }

    @Override
    protected String hiddenUploadCategory(String repoId) {
        return "Spacemedia Copernicus files uploaded by " + commonsService.getAccount();
    }
}
