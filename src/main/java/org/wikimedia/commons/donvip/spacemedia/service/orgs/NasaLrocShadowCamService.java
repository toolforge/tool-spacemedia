package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.wikimedia.commons.donvip.spacemedia.service.wikimedia.WikidataItem.Q125191_PHOTOGRAPH;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.CompositeMediaId;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.lroc.NasaLrocMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.lroc.NasaLrocMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.exception.IgnoreException;
import org.wikimedia.commons.donvip.spacemedia.service.nasa.NasaMappingService;
import org.wikimedia.commons.donvip.spacemedia.service.wikimedia.SdcStatements;
import org.wikimedia.commons.donvip.spacemedia.utils.CsvHelper;

@Service
public class NasaLrocShadowCamService extends AbstractOrgHtmlGalleryService<NasaLrocMedia> {

    private static final String LROC_BASE_URL = "https://www.lroc.asu.edu";
    private static final String SHAD_BASE_URL = "https://www.shadowcam.asu.edu";

    private static final DateTimeFormatter LROC_DATE_PATTERN = ofPattern("MMMM d, yyyy HH:mm z.", Locale.ENGLISH);
    private static final DateTimeFormatter SHAD_DATE_PATTERN = ofPattern("d MMMM yyyy", Locale.ENGLISH);

    private static final Pattern CREDIT_PATTERN = Pattern.compile(".* \\[(.+)\\].?");

    @Lazy
    @Autowired
    private NasaMappingService mappings;

    private Map<String, String> moonKeywords;

    @Autowired
    public NasaLrocShadowCamService(NasaLrocMediaRepository repository) {
        super(repository, "nasa.lroc.shadowcam", Set.of("lroc", "shadowcam"));
    }

    @Override
    @PostConstruct
    void init() throws IOException {
        super.init();
        moonKeywords = CsvHelper.loadCsvMapping("moon.keywords.csv");
    }

    @Override
    public String getName() {
        return "NASA (LROC / ShadowCam)";
    }

    @Override
    protected boolean checkBlocklist() {
        return false;
    }

    private static <T> T lrocOrShadowcam(String repoId, T lroc, T shadowcam) {
        return "shadowcam".equals(repoId) ? shadowcam : lroc;
    }

    @Override
    protected List<String> fetchGalleryUrls(String repoId) {
        return List.of(getBaseUrl(repoId));
    }

    @Override
    protected String getGalleryPageUrl(String galleryUrl, int page) {
        return galleryUrl + "?page=" + page;
    }

    @Override
    protected Elements getGalleryItems(String repoId, Element html) {
        return html.getElementsByClass(lrocOrShadowcam(repoId, "block-link", "img-link"));
    }

    @Override
    protected String getAuthor(NasaLrocMedia media, FileMetadata metadata) {
        if (metadata.getDescription() != null) {
            Matcher m = CREDIT_PATTERN.matcher(metadata.getDescription());
            if (m.matches()) {
                String credit = m.group(1);
                if (!credit.contains("NASA")) {
                    throw new IgnoreException("Non-NASA picture: " + credit);
                }
                return credit;
            }
        }
        return "NASA/" + lrocOrShadowcam(media.getId().getRepoId(), "GSFC", "KARI") + "/Arizona State University";
    }

    private String getBaseUrl(String repoId) {
        return lrocOrShadowcam(repoId, LROC_BASE_URL + "/posts", SHAD_BASE_URL + "/images");
    }

    @Override
    protected String getSourceUrl(CompositeMediaId id) {
        return getBaseUrl(id.getRepoId()) + "/" + id.getMediaId();
    }

    @Override
    protected NasaLrocMedia refresh(NasaLrocMedia media) throws IOException {
        return media.copyDataFrom(fetchMedia(media.getId(), empty()));
    }

    @Override
    protected Class<NasaLrocMedia> getMediaClass() {
        return NasaLrocMedia.class;
    }

    @Override
    public Set<String> findCategories(NasaLrocMedia media, FileMetadata metadata, boolean includeHidden) {
        Set<String> result = super.findCategories(media, metadata, includeHidden);
        result.addAll(media.getKeywordStream().map(moonKeywords::get).filter(Objects::nonNull).toList());
        result.addAll(media.getKeywordStream().map(mappings.getNasaKeywords()::get).filter(Objects::nonNull).toList());
        if (media.getPublicationDate().isAfter(LocalDate.of(2009, 7, 1))) {
            result.add("Photos of the Moon by "
                    + lrocOrShadowcam(media.getId().getRepoId(), "Lunar Reconnaissance Orbiter", "ShadowCam")
                    + (metadata.getImageDimensions() != null && metadata.getImageDimensions().getAspectRatio() < 0.25
                            ? " (raw frames)"
                            : ""));
        }
        return result;
    }

    @Override
    public Set<String> findAfterInformationTemplates(NasaLrocMedia media, FileMetadata metadata) {
        Set<String> result = super.findAfterInformationTemplates(media, metadata);
        if ("lroc".equals(media.getId().getRepoId())) {
            result.add(
                    "Template:NASA Photojournal/attribution|class=LRO|mission=LRO|name=Lunar Reconnaissance Orbiter Camera|credit=LROC");
            result.add("NASA-image|id=" + media.getId() + "|center=GSFC");
        }
        return result;
    }

    @Override
    public Set<String> findLicenceTemplates(NasaLrocMedia media, FileMetadata metadata) {
        Set<String> result = super.findLicenceTemplates(media, metadata);
        result.add("PD-USGov-NASA");
        return result;
    }

    @Override
    protected SdcStatements getStatements(NasaLrocMedia media, FileMetadata metadata) {
        SdcStatements result = super.getStatements(media, metadata).instanceOf(Q125191_PHOTOGRAPH);
        if (media.getPublicationDate().isAfter(LocalDate.of(2009, 7, 1))) {
            String repoId = media.getId().getRepoId();
            result.creator(lrocOrShadowcam(repoId, "Q331778", "Q30749609")) // Created by LRO / Danuri
                    .depicts("Q405") // Depicts the Moon
                    .locationOfCreation("Q210448") // Created in lunar orbit
                    .fabricationMethod("Q725252") // Satellite imagery
                    .capturedWith(lrocOrShadowcam(repoId, "Q124653753", "Q106473449")); // Taken with LROC / ShadowCam
        }
        return result;
    }

    @Override
    protected String extractIdFromGalleryItem(Element result) {
        String[] items = (result.hasAttr("href") ? result.attr("href")
                : result.getElementsByTag("a").first().attr("href")).split("/");
        return items[items.length - 1];
    }

    @Override
    void fillMediaWithHtml(String url, Document html, NasaLrocMedia media) throws IOException {
        String repoId = media.getId().getRepoId();
        Element article = ofNullable(html.getElementsByTag("article").first()).orElse(html);
        media.setTitle(article.getElementsByTag("h1").first().text());
        String[] byline = article.getElementsByClass(lrocOrShadowcam(repoId, "byline", "mt-2")).first().text()
                .split(" on ");
        if ("shadowcam".equals(repoId)) {
            media.setPublicationDate(LocalDate.parse(byline[byline.length - 1], SHAD_DATE_PATTERN));
        } else {
            media.setPublicationDateTime(ZonedDateTime.parse(byline[byline.length - 1], LROC_DATE_PATTERN));
        }
        if (article.getElementsByTag("figcaption").isEmpty()
                && article.getElementsByClass("serendipity_imageComment_txt").isEmpty()) {
            media.setDescription(article.getElementById("post-body").text());
        }
        Element tags = article.getElementsByClass("tags").first();
        if (tags != null) {
            for (Element tag : tags.getElementsByTag("a")) {
                media.getKeywords().add(tag.text());
            }
        }
        String baseUrl = lrocOrShadowcam(media.getId().getRepoId(), LROC_BASE_URL, SHAD_BASE_URL);
        for (Element e : article.getElementsByClass("img-polaroid")) {
            String src = e.getElementsByTag("img").attr("src");
            addMetadata(media, src.startsWith("http") ? src : baseUrl + src,
                    fm -> {
                        String figcaption = e.getElementsByTag("figcaption").text();
                        String imgComment = e.getElementsByClass("serendipity_imageComment_txt").text();
                        fm.setDescription(isNotBlank(figcaption) ? figcaption
                                : isNotBlank(imgComment) ? imgComment : e.getElementsByTag("font").text());
                    });
        }
        for (Element e : article.getElementsByClass("olZoomify")) {
            addZoomifyFileMetadata(media, e, baseUrl);
        }
    }

    @Override
    protected Set<String> getTwitterAccounts(NasaLrocMedia uploadedMedia) {
        return Set.of("@NASAMoon");
    }
}
