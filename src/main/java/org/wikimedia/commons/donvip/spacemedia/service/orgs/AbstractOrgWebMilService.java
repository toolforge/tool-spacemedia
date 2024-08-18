package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import static java.util.Locale.ENGLISH;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toMap;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.CompositeMediaId;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.webmil.WebMilMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.webmil.WebMilMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.service.wikimedia.SdcStatements;
import org.wikimedia.commons.donvip.spacemedia.utils.UnitedStates;
import org.wikimedia.commons.donvip.spacemedia.utils.UnitedStates.VirinTemplates;

/**
 * Service fetching images from WEB.mil websites
 */
@Service
public abstract class AbstractOrgWebMilService extends AbstractOrgHtmlGalleryService<WebMilMedia> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractOrgWebMilService.class);

    private static final Pattern URL_PATTERN = Pattern
            .compile("https://.*\\.defense\\.gov/(\\d{4}/[A-Za-z]{3}/\\d{2})/.*");

    private static final DateTimeFormatter URL_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy/MMM/dd", ENGLISH);
    private static final DateTimeFormatter VIRIN_DATE_FORMAT = DateTimeFormatter.ofPattern("yyMMdd", ENGLISH);
    private static final DateTimeFormatter VIRIN_BAD_DATE_FORMAT = DateTimeFormatter.ofPattern("MMddyy", ENGLISH);

    private static final String PHOTO_BY = "Photo by:";
    private static final String VIRIN = "VIRIN:";

    private final Map<String, String> galleryUrls;

    protected AbstractOrgWebMilService(WebMilMediaRepository repository, String id, Set<String> websites) {
        super(repository, id, setFromSet(websites, 0));
        galleryUrls = mapFromSet(websites, 1, x -> "https://" + x);
    }

    private static Set<String> setFromSet(Set<String> set, int idx) {
        return set.stream().map(x -> x.split(":")[idx]).collect(toCollection(TreeSet::new));
    }

    private static Map<String, String> mapFromSet(Set<String> set, int idx, UnaryOperator<String> mapper) {
        return set.stream().map(x -> x.split(":")).collect(toMap(x -> x[0], x -> mapper.apply(x[idx])));
    }

    @Override
    protected Class<WebMilMedia> getMediaClass() {
        return WebMilMedia.class;
    }

    @Override
    protected String hiddenUploadCategory(String repoId) {
        return "WEB.mil files uploaded by " + commonsService.getAccount();
    }

    @Override
    protected List<String> fetchGalleryUrls(String repoId) {
        return List.of(galleryUrls.get(repoId));
    }

    @Override
    protected String getGalleryPageUrl(String galleryUrl, int page) {
        return galleryUrl + "/igpage/" + page;
    }

    @Override
    protected Elements getGalleryItems(String repoId, String url, Element html) {
        for (String klass : new String[] { "gallery_container", "AF2ImageGallerylvItem", "DGOVImageGallerylvItem" }) {
            Elements elems = html.getElementsByClass(klass);
            if (!elems.isEmpty()) {
                return elems;
            }
        }
        return new Elements();
    }

    @Override
    protected String extractIdFromGalleryItem(String url, Element result) {
        Elements links = result.getElementsByClass("gallery-image-details-link");
        if (links.isEmpty()) {
            links = result.getElementsByClass("aImageDetailsImgLink");
        }
        String[] items = links.first().attr("href").split("/");
        return items[items.length - 1];
    }

    @Override
    List<WebMilMedia> fillMediaWithHtml(String url, Document html, Element galleryItem, WebMilMedia image) {
        Element div = html.getElementsByClass("details-content").first();
        if (div == null) {
            div = html.getElementsByClass("AF2ImageDiv").first();
        }
        if (div == null) {
            LOGGER.error("{} => {}", image.getId(), html);
            throw new IllegalArgumentException("No div element found for " + image.getId());
        }
        mapField(div, "spanTitle", image::setTitle);
        mapField(div, "spanCaption", image::setDescription);
        mapField(div, "author", image::setCredits);
        mapField(div, "virin", x -> image.setVirin(x.substring(0, x.lastIndexOf('.'))));
        if (image.getCredits() == null || image.getVirin() == null) {
            Element legacy = div.getElementsByClass("AF2LeftPaneItemLast AF2LeftPaneItem").first();
            if (legacy != null) {
                String text = legacy.text();
                int idxAuthor = text.indexOf(PHOTO_BY);
                int idxVirin = text.indexOf(VIRIN);
                image.setCredits(text.substring(idxAuthor + PHOTO_BY.length() + 1, idxVirin));
                image.setVirin(text.substring(idxVirin + VIRIN.length() + 1, text.lastIndexOf('.')));
            }
        }
        String virinDate = image.getVirin().substring(0, 6);
        try {
            image.setCreationDate(LocalDate.parse(virinDate, VIRIN_DATE_FORMAT));
        } catch (DateTimeParseException e1) {
            LOGGER.warn(e1.getMessage());
            try {
                image.setCreationDate(LocalDate.parse(virinDate, VIRIN_BAD_DATE_FORMAT));
            } catch (DateTimeParseException e2) {
                LOGGER.warn(e2.getMessage());
            }
        }
        String assetUrl = div.getElementById("aDownloadLrg").attr("href");
        addMetadata(image, assetUrl, null);
        Matcher m = URL_PATTERN.matcher(assetUrl);
        if (m.matches()) {
            image.setPublicationDate(LocalDate.parse(m.group(1), URL_DATE_FORMAT));
        } else {
            throw new IllegalArgumentException(assetUrl);
        }
        if (image.getCredits() != null && isCourtesy(image.getCredits().toLowerCase(ENGLISH))
                && findLicenceTemplates(image, image.getUniqueMetadata()).isEmpty()) {
            mediaService.ignoreMedia(image, "Courtesy photo not free");
        }
        return List.of(image);
    }

    private static void mapField(Element div, String id, Consumer<String> setter) {
        Element elem = div.getElementById(id);
        if (elem != null) {
            setter.accept(elem.text().trim());
        }
    }

    @Override
    protected final WebMilMedia refresh(WebMilMedia media) throws IOException {
        return media.copyDataFrom(fetchMedia(media.getId(), Optional.empty()));
    }

    @Override
    protected final String getSourceUrl(CompositeMediaId id) {
        return galleryUrls.get(id.getRepoId()) + "igphoto/" + id.getMediaId();
    }

    @Override
    protected final String getSource(WebMilMedia media, FileMetadata metadata) {
        URL sourceUrl = getSourceUrl(media, metadata, metadata.getExtension());
        VirinTemplates t = UnitedStates.getUsVirinTemplates(media.getVirin(), sourceUrl);
        return t != null ? "{{" + t.virinTemplate() + "}}" : sourceUrl.toExternalForm();
    }

    @Override
    protected final Pair<String, Map<String, String>> getWikiFileDesc(WebMilMedia media, FileMetadata metadata) {
        return milim(media, metadata, media.getVirin(), Optional.empty(), Optional.empty());
    }

    @Override
    protected List<String> getReviewCategories(WebMilMedia media) {
        return getMilitaryReviewCategories(media);
    }

    @Override
    public Set<String> findCategories(WebMilMedia media, FileMetadata metadata, boolean includeHidden) {
        Set<String> result = super.findCategories(media, metadata, includeHidden);
        if (metadata.isVideo()) {
            VirinTemplates t = UnitedStates.getUsVirinTemplates(media.getVirin(),
                    media.getUniqueMetadata().getAssetUrl());
            if (t != null && StringUtils.isNotBlank(t.videoCategory())) {
                result.add(t.videoCategory());
            }
        }
        return result;
    }

    @Override
    public Set<String> findLicenceTemplates(WebMilMedia media, FileMetadata metadata) {
        Set<String> result = super.findLicenceTemplates(media, metadata);
        VirinTemplates t = UnitedStates.getUsVirinTemplates(media.getVirin(),
                media.getUniqueMetadata().getAssetUrl());
        if (t != null && StringUtils.isNotBlank(t.pdTemplate())) {
            result.add(t.pdTemplate());
        }
        return result;
    }

    @Override
    protected SdcStatements getStatements(WebMilMedia media, FileMetadata metadata) {
        SdcStatements result = super.getStatements(media, metadata);
        UnitedStates.getUsMilitaryCreator(media).ifPresent(result::creator);
        return result;
    }

    @Override
    protected Set<String> getEmojis(WebMilMedia uploadedMedia) {
        Set<String> result = super.getEmojis(uploadedMedia);
        result.add(UnitedStates.getUsMilitaryEmoji(uploadedMedia));
        return result;
    }

    @Override
    protected Set<String> getTwitterAccounts(WebMilMedia uploadedMedia) {
        Set<String> result = super.getEmojis(uploadedMedia);
        result.add(UnitedStates.getUsMilitaryTwitterAccount(uploadedMedia));
        return result;
    }
}
