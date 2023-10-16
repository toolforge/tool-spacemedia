package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import static java.util.Locale.ENGLISH;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toMap;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.newURL;

import java.io.IOException;
import java.net.URL;
import java.net.UnknownHostException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
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
import org.jsoup.HttpStatusException;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.CompositeMediaId;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.Media;
import org.wikimedia.commons.donvip.spacemedia.data.domain.webmil.WebMilMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.webmil.WebMilMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.exception.UploadException;
import org.wikimedia.commons.donvip.spacemedia.utils.UnitedStates;
import org.wikimedia.commons.donvip.spacemedia.utils.UnitedStates.VirinTemplates;

/**
 * Service fetching images from WEB.mil websites
 */
@Service
public abstract class AbstractOrgWebMilService extends AbstractOrgService<WebMilMedia> {

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
    public void updateMedia(String[] args) {
        LocalDateTime start = startUpdateMedia();
        List<WebMilMedia> uploadedMedia = new ArrayList<>();
        int count = 0;
        for (String website : getRepoIdsFromArgs(args)) {
            count += updateWebsite(website, uploadedMedia, start, count);
        }
        endUpdateMedia(count, uploadedMedia, uploadedMedia.stream().flatMap(Media::getMetadataStream).toList(), start,
                LocalDate.now().minusYears(1), true);
    }

    private int updateWebsite(String website, List<WebMilMedia> uploadedMedia, LocalDateTime start, int startCount) {
        int count = 0;
        String galleryUrl = galleryUrls.get(website);
        LocalDate doNotFetchEarlierThan = getRuntimeData().getDoNotFetchEarlierThan();
        LOGGER.info("Fetching {} media from {} ...", website, galleryUrl);
        for (int i = 1;; i++) {
            String pageUrl = galleryUrl + "/igpage/" + i;
            try {
                Document html = getWithJsoup(pageUrl, 10_000, 5);
                Elements elems = html.getElementsByClass("gallery_container");
                if (elems.isEmpty()) {
                    elems = html.getElementsByClass("AF2ImageGallerylvItem");
                }
                if (elems.isEmpty()) {
                    break;
                }
                for (Element elem : elems) {
                    try {
                        Elements links = elem.getElementsByClass("gallery-image-details-link");
                        if (links.isEmpty()) {
                            links = elem.getElementsByClass("aImageDetailsImgLink");
                        }
                        String[] items = links.first().attr("href").split("/");
                        WebMilMedia image = updateImage(website, items[items.length - 1], uploadedMedia);
                        count++;
                        ongoingUpdateMedia(start, startCount + count);
                        if (doNotFetchEarlierThan != null
                                && image.getPublicationDate().isBefore(doNotFetchEarlierThan)) {
                            return count;
                        }
                    } catch (UploadException | RuntimeException e) {
                        LOGGER.error("Error while processing {}", elem, e);
                    }
                }
            } catch (HttpStatusException e) {
                LOGGER.info(e.getMessage());
                break;
            } catch (UnknownHostException e) {
                LOGGER.error("Error while fetching {}", pageUrl, e);
                break;
            } catch (IOException e) {
                LOGGER.error("Error while fetching {}", pageUrl, e);
            }
        }
        return count;
    }

    private WebMilMedia updateImage(String website, String id, List<WebMilMedia> uploadedMedia)
            throws IOException, UploadException {
        boolean save = false;
        WebMilMedia media = null;
        Optional<WebMilMedia> imageInDb = repository.findById(new CompositeMediaId(website, id));
        if (imageInDb.isPresent()) {
            media = imageInDb.get();
        } else {
            media = fetchMediaFromApi(id, website);
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
        return save ? saveMedia(media) : media;
    }

    private WebMilMedia fetchMediaFromApi(String id, String website) throws IOException {
        WebMilMedia media = new WebMilMedia();
        media.setId(new CompositeMediaId(website, id));
        fillMediaWithHtml(getWithJsoup(getSourceUrl(media.getId()), 10_000, 5), media);
        return media;
    }

    void fillMediaWithHtml(Document html, WebMilMedia image) {
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
        if (image.getCredits() != null && image.getCredits().toLowerCase(ENGLISH).contains("courtesy")
                && findLicenceTemplates(image, image.getUniqueMetadata()).isEmpty()) {
            ignoreFile(image, "Courtesy photo not free");
        }
    }

    private static void mapField(Element div, String id, Consumer<String> setter) {
        Element elem = div.getElementById(id);
        if (elem != null) {
            setter.accept(elem.text().trim());
        }
    }

    @Override
    protected final WebMilMedia refresh(WebMilMedia media) throws IOException {
        return media.copyDataFrom(fetchMediaFromApi(media.getId().getMediaId(), media.getId().getRepoId()));
    }

    @Override
    public final URL getSourceUrl(WebMilMedia media, FileMetadata metadata) {
        return newURL(getSourceUrl(media.getId()));
    }

    public final String getSourceUrl(CompositeMediaId id) {
        return galleryUrls.get(id.getRepoId()) + "igphoto/" + id.getMediaId();
    }

    @Override
    protected final String getSource(WebMilMedia media, FileMetadata metadata) {
        URL sourceUrl = getSourceUrl(media, metadata);
        VirinTemplates t = UnitedStates.getUsVirinTemplates(media.getVirin(), sourceUrl);
        return t != null ? "{{" + t.virinTemplate() + "}}" : sourceUrl.toExternalForm();
    }

    @Override
    protected final Pair<String, Map<String, String>> getWikiFileDesc(WebMilMedia media, FileMetadata metadata) {
        return milim(media, metadata, media.getVirin(), Optional.empty(), Optional.empty());
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
