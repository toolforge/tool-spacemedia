package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.Locale.ENGLISH;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.getWithJsoup;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.isTemporalBefore;

import java.io.IOException;
import java.net.URL;
import java.net.UnknownHostException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.jsoup.HttpStatusException;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.CompositeMediaId;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.ImageDimensions;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.Media;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.MediaRepository;
import org.wikimedia.commons.donvip.spacemedia.exception.UploadException;
import org.wikimedia.commons.donvip.spacemedia.service.wikimedia.GlitchTip;
import org.wikimedia.commons.donvip.spacemedia.utils.Utils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

public abstract class AbstractOrgHtmlGalleryService<T extends Media> extends AbstractOrgService<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractOrgHtmlGalleryService.class);

    protected static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(ofPattern("yyyy MMMM dd HHmmz", ENGLISH),
            ofPattern("yyyy MMMM d HHmmz", ENGLISH), ofPattern("yyyy MMMM dd HHmmX", ENGLISH),
            ofPattern("yyyy MMMM d HHmmX", ENGLISH), ofPattern("yyyy MMMM dd HHmm z", ENGLISH),
            ofPattern("yyyy MMMM d HHmm z", ENGLISH), ofPattern("yyyy MMMM d HHmmss", ENGLISH),
            ofPattern("yyyy MMMM d HHmm", ENGLISH), ofPattern("yyyy MMMM dd hhmm a", ENGLISH),
            ofPattern("dd MMM yy", ENGLISH), ofPattern("MMMM dd yyyy", ENGLISH), ofPattern("MMMM d yyyy", ENGLISH),
            ofPattern("MMM dd yyyy", ENGLISH), ofPattern("yyyy MMMM dd", ENGLISH), ofPattern("yyyy MMMM d", ENGLISH),
            ofPattern("yyyy MMMM d z", ENGLISH), ofPattern("MMMM yyyy", ENGLISH), ofPattern("yyyy MMMM", ENGLISH),
            ofPattern("yyyy", ENGLISH));

    protected AbstractOrgHtmlGalleryService(MediaRepository<T> repository, String id, Set<String> repoIds) {
        super(repository, id, repoIds);
    }

    protected abstract List<String> fetchGalleryUrls(String repoId) throws IOException;

    protected abstract String getGalleryPageUrl(String galleryUrl, int page);

    protected abstract Elements getGalleryItems(String repoId, String url, Element html);

    protected Optional<Temporal> extractDateFromGalleryItem(Element result) {
        return empty();
    }

    protected abstract String extractIdFromGalleryItem(String url, Element result);

    @Override
    public final URL getSourceUrl(T media, FileMetadata metadata, String ext) {
        return ofNullable(getSourceUrl(media.getId())).map(Utils::newURL).orElse(null);
    }

    protected abstract String getSourceUrl(CompositeMediaId id);

    protected List<T> fillMediaWithHtml(String url, Element galleryItem, T media) throws IOException {
        return fillMediaWithHtml(url, fetchUrl(url), galleryItem, media);
    }

    abstract List<T> fillMediaWithHtml(String url, Document html, Element galleryItem, T media) throws IOException;

    @Override
    public final void updateMedia(String[] args) throws IOException, UploadException {
        int count = 0;
        LocalDateTime start = startUpdateMedia();
        List<T> uploadedMedia = new ArrayList<>();
        for (String repoId : getRepoIdsFromArgs(args)) {
            LOGGER.info("Updating media for repo {}", repoId);
            for (String galleryUrl : fetchGalleryUrls(repoId)) {
                LOGGER.info("Updating media for gallery {}", galleryUrl);
                count += updateGallery(repoId, galleryUrl, uploadedMedia, start, count);
            }
        }
        endUpdateMedia(count, uploadedMedia, uploadedMedia.stream().flatMap(Media::getMetadataStream).toList(), start);
    }

    protected boolean loop(String repoId, Elements results) {
        return !results.isEmpty();
    }

    private int updateGallery(String repoId, String galleryUrl, List<T> uploadedMedia, LocalDateTime start,
            int startCount) throws IOException, UploadException {
        int idx = 1;
        int count = 0;
        boolean loop = true;
        do {
            LocalDate doNotFetchEarlierThan = getRuntimeData().getDoNotFetchEarlierThan();
            String pageUrl = getGalleryPageUrl(galleryUrl, idx++);
            try {
                Elements results = getGalleryItems(repoId, pageUrl, getWithJsoup(pageUrl, 10_000, 3));
                LOGGER.debug("Detected {} {} gallery items at {}", results.size(), repoId, pageUrl);
                if (idx == 2 && results.isEmpty()) {
                    LOGGER.warn("First {} gallery page is empty! {}", repoId, pageUrl);
                }
                for (Element result : results) {
                    try {
                        String extractedId = extractIdFromGalleryItem(pageUrl, result);
                        if (isNotBlank(extractedId)) {
                            CompositeMediaId id = new CompositeMediaId(repoId, extractedId);
                            Optional<Temporal> date = extractDateFromGalleryItem(result);
                            if (date.isPresent() && doNotFetchEarlierThan != null
                                    && isTemporalBefore(date.get(), doNotFetchEarlierThan)) {
                                loop = false;
                            }
                            if (loop) {
                                try {
                                    List<T> medias = updateImages(id, date, result, uploadedMedia);
                                    if (doNotFetchEarlierThan != null && medias.stream().anyMatch(
                                            media -> media.getPublicationDate() != null
                                                    && media.getPublicationDate().isBefore(doNotFetchEarlierThan))) {
                                        loop = false;
                                    }
                                } catch (IOException | RuntimeException e) {
                                    LOGGER.error("Error while updating {} => {} => {}", id,
                                            e.getClass().getSimpleName(), e.getMessage());
                                    LOGGER.debug("Error stacktrace:", e);
                                    GlitchTip.capture(e);
                                }
                            }
                        } else {
                            LOGGER.warn("Failed to extract item id from {}", pageUrl);
                        }
                    } catch (RuntimeException e) {
                        LOGGER.error("Failed to extract item id from {} => {} => {}", pageUrl,
                                e.getClass().getSimpleName(), e.getMessage());
                        LOGGER.debug("Error stacktrace:", e);
                        GlitchTip.capture(e);
                    }
                    ongoingUpdateMedia(start, startCount + count++);
                }
                loop = loop && loop(repoId, results);
            } catch (HttpStatusException e) {
                LOGGER.info(e.getMessage());
                break;
            } catch (UnknownHostException e) {
                LOGGER.error("Error while fetching {}", pageUrl, e);
                GlitchTip.capture(e);
                break;
            } catch (IOException e) {
                LOGGER.error("Error while fetching {}", pageUrl, e);
                GlitchTip.capture(e);
            }
        } while (loop);
        return count;
    }

    private List<T> updateImages(CompositeMediaId id, Optional<Temporal> date, Element galleryItem,
            List<T> uploadedMedia) throws IOException, UploadException {
        boolean saveAll = false;
        List<T> medias = null;
        Optional<T> imageInDb = repository.findById(id);
        if (imageInDb.isPresent()) {
            medias = List.of(imageInDb.get());
        } else {
            medias = fetchMedias(id, date, galleryItem);
            saveAll = !medias.isEmpty();
        }
        for (T media : medias) {
            boolean save = saveAll;
            save |= doCommonUpdate(media);
            save |= ignoreNonFreeFiles(media);
            if (shouldUploadAuto(media, false)) {
                uploadedMedia.add(saveMedia(upload(save ? saveMedia(media) : media, true, false).getLeft()));
            } else if (save) {
                saveMedia(media);
            }
        }
        return medias;
    }

    protected boolean ignoreNonFreeFiles(T media) {
        return false;
    }

    protected T fetchMedia(CompositeMediaId id, Optional<Temporal> date) throws IOException {
        return fetchMedias(id, date, null).stream().filter(m -> m.getId().equals(id)).findFirst().orElseThrow();
    }

    protected List<T> fetchMedias(CompositeMediaId id, Optional<Temporal> date, Element galleryItem)
            throws IOException {
        try {
            String url = getSourceUrl(id);
            if (url == null) {
                throw new IllegalArgumentException("Can't find valid source URL for " + id);
            }
            T media = getMediaClass().getConstructor().newInstance();
            media.setId(id);
            date.ifPresent(media::setPublication);
            return fillMediaWithHtml(url, galleryItem, media);
        } catch (ReflectiveOperationException | IllegalArgumentException | SecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    protected Document fetchUrl(String url) throws IOException {
        return getWithJsoup(url, 10_000, 5);
    }

    protected final void addZoomifyFileMetadata(T media, Element html, String baseUrl) throws JsonProcessingException {
        for (Element e : html.getElementsByClass("olZoomify")) {
            DataFiZoomify data = jackson.readValue(e.attr("data-fi-zoomify"), DataFiZoomify.class);
            if (isNotBlank(data.ptifName())) {
                String fName = data.ptifName() + ".tif";
                addMetadata(media, baseUrl + "/ptif/download_file?fName=" + fName, fm -> {
                    fm.setExtension("tif");
                    fm.setOriginalFileName(fName);
                    fm.setImageDimensions(new ImageDimensions(data.width(), data.height()));
                });
            } else {
                LOGGER.error("Failed to retrieve ptifName in data-fi-zoomify from {}", e);
            }
        }
    }

    @JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static record DataFiZoomify(String ptifName, int width, int height, double leftX, double rightX,
            double topY, double bottomY, Point center, int zoomLevel) {

        private static record Point(int x, int y) {
        }
    }
}
