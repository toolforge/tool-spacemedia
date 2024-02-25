package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.wikimedia.commons.donvip.spacemedia.service.wikimedia.WikidataItem.Q125191_PHOTOGRAPH;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.CompositeMediaId;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.lroc.NasaLrocMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.lroc.NasaLrocMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.service.nasa.NasaMappingService;
import org.wikimedia.commons.donvip.spacemedia.service.wikimedia.SdcStatements;

@Service
public class NasaLrocService extends AbstractOrgHtmlGalleryService<NasaLrocMedia> {

    private static final String BASE_URL = "https://www.lroc.asu.edu";

    private static final DateTimeFormatter DATE_PATTERN = DateTimeFormatter.ofPattern("MMMM d, yyyy HH:mm z.",
            Locale.ENGLISH);

    private static final Pattern CREDIT_PATTERN = Pattern.compile(".* \\[(.+)\\]");

    @Autowired
    private NasaMappingService mappings;

    @Autowired
    public NasaLrocService(NasaLrocMediaRepository repository) {
        super(repository, "nasa.lroc", Set.of("lroc"));
    }

    @Override
    public String getName() {
        return "NASA (LROC)";
    }

    @Override
    protected boolean checkBlocklist() {
        return false;
    }

    @Override
    protected List<String> fetchGalleryUrls(String repoId) {
        return List.of(BASE_URL + "/posts");
    }

    @Override
    protected String getGalleryPageUrl(String galleryUrl, int page) {
        return galleryUrl + "?page=" + page;
    }

    @Override
    protected Elements getGalleryItems(Element html) {
        return html.getElementsByClass("post-card");
    }

    @Override
    protected String getAuthor(NasaLrocMedia media, FileMetadata metadata) {
        if (metadata.getDescription() != null) {
            Matcher m = CREDIT_PATTERN.matcher(metadata.getDescription());
            if (m.matches()) {
                return m.group(1);
            }
        }
        return "NASA/GSFC/Arizona State University";
    }

    @Override
    protected String getSourceUrl(CompositeMediaId id) {
        return BASE_URL + "/posts/" + id.getMediaId();
    }

    @Override
    protected NasaLrocMedia refresh(NasaLrocMedia media) throws IOException {
        return media.copyDataFrom(fetchMedia(media.getId(), Optional.empty()));
    }

    @Override
    protected Class<NasaLrocMedia> getMediaClass() {
        return NasaLrocMedia.class;
    }

    @Override
    public Set<String> findCategories(NasaLrocMedia media, FileMetadata metadata, boolean includeHidden) {
        Set<String> result = super.findCategories(media, metadata, includeHidden);
        result.addAll(media.getKeywordStream().map(mappings.getNasaKeywords()::get).filter(Objects::nonNull).toList());
        if (media.getPublicationDate().isAfter(LocalDate.of(2009, 7, 1))) {
            result.add("Photos of the Moon by Lunar Reconnaissance Orbiter");
        }
        return result;
    }

    @Override
    public Set<String> findAfterInformationTemplates(NasaLrocMedia media, FileMetadata metadata) {
        Set<String> result = super.findAfterInformationTemplates(media, metadata);
        result.add(
                "Template:NASA Photojournal/attribution|class=LRO|mission=LRO|name=Lunar Reconnaissance Orbiter Camera|credit=LROC");
        result.add("NASA-image|id=" + media.getId() + "|center=GSFC");
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
            result.creator("Q331778") // Created by LRO
                    .depicts("Q405") // Depicts the Moon
                    .locationOfCreation("Q210448") // Created in lunar orbit
                    .fabricationMethod("Q725252") // Satellite imagery
                    .capturedWith("Q124653753"); // Taken with LROC instrument
        }
        return result;
    }

    @Override
    protected String extractIdFromGalleryItem(Element result) {
        String[] items = result.getElementsByTag("a").first().attr("href").split("/");
        return items[items.length - 1];
    }

    @Override
    void fillMediaWithHtml(String url, Document html, NasaLrocMedia media) throws IOException {
        Element article = html.getElementsByTag("article").first();
        media.setTitle(article.getElementsByTag("h1").first().text());
        String[] byline = article.getElementsByClass("byline").first().text().split(" on ");
        media.setPublicationDateTime(ZonedDateTime.parse(byline[byline.length - 1], DATE_PATTERN));
        if (article.getElementsByTag("figcaption").isEmpty()
                && article.getElementsByClass("serendipity_imageComment_txt").isEmpty()) {
            media.setDescription(article.getElementById("post-body").text());
        }
        Element tags = article.getElementsByClass("tags").first();
        if (tags != null) {
            for (Element tag : tags.getElementsByClass("nonya")) {
                media.getKeywords().add(tag.text());
            }
        }
        for (Element e : article.getElementsByClass("img-polaroid")) {
            String src = e.getElementsByTag("img").attr("src");
            addMetadata(media, src.startsWith("http") ? src : BASE_URL + src,
                    fm -> {
                        String figcaption = e.getElementsByTag("figcaption").text();
                        String imgComment = e.getElementsByClass("serendipity_imageComment_txt").text();
                        fm.setDescription(isNotBlank(figcaption) ? figcaption
                                : isNotBlank(imgComment) ? imgComment : e.getElementsByTag("font").text());
                    });
        }
        for (Element e : article.getElementsByClass("olZoomify")) {
            addZoomifyFileMetadata(media, e, BASE_URL);
        }
    }

    @Override
    protected Set<String> getTwitterAccounts(NasaLrocMedia uploadedMedia) {
        return Set.of("@LRO_NASA");
    }
}
