package org.wikimedia.commons.donvip.spacemedia.service.orgs;

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
import java.time.Year;
import java.time.YearMonth;
import java.time.ZonedDateTime;
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
import org.wikimedia.commons.donvip.spacemedia.utils.Utils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

public abstract class AbstractOrgHtmlGalleryService<T extends Media> extends AbstractOrgService<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractOrgHtmlGalleryService.class);

    protected AbstractOrgHtmlGalleryService(MediaRepository<T> repository, String id, Set<String> repoIds) {
        super(repository, id, repoIds);
    }

    protected abstract List<String> fetchGalleryUrls(String repoId);

    protected abstract String getGalleryPageUrl(String repoId, int page);

    protected abstract Elements getGalleryItems(String repoId, String url, Element html);

    protected Optional<Temporal> extractDateFromGalleryItem(Element result) {
        return empty();
    }

    protected abstract String extractIdFromGalleryItem(String url, Element result);

    @Override
    public final URL getSourceUrl(T media, FileMetadata metadata) {
        return ofNullable(getSourceUrl(media.getId())).map(Utils::newURL).orElse(null);
    }

    protected abstract String getSourceUrl(CompositeMediaId id);

    abstract List<T> fillMediaWithHtml(String url, Document html, T media) throws IOException;

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
                    CompositeMediaId id = new CompositeMediaId(repoId, extractIdFromGalleryItem(pageUrl, result));
                    Optional<Temporal> date = extractDateFromGalleryItem(result);
                    if (date.isPresent() && doNotFetchEarlierThan != null
                            && isTemporalBefore(date.get(), doNotFetchEarlierThan)) {
                        loop = false;
                    }
                    if (loop) {
                        try {
                            List<T> medias = updateImages(id, date, uploadedMedia);
                            if (doNotFetchEarlierThan != null && medias.stream()
                                    .anyMatch(media -> media.getPublicationDate().isBefore(doNotFetchEarlierThan))) {
                                loop = false;
                            }
                        } catch (IOException | RuntimeException e) {
                            LOGGER.error("Error while updating {}", id, e);
                        }
                    }
                    ongoingUpdateMedia(start, startCount + count++);
                }
                loop = loop && loop(repoId, results);
            } catch (HttpStatusException e) {
                LOGGER.info(e.getMessage());
                break;
            } catch (UnknownHostException e) {
                LOGGER.error("Error while fetching {}", pageUrl, e);
                break;
            } catch (IOException e) {
                LOGGER.error("Error while fetching {}", pageUrl, e);
            }
        } while (loop);
        return count;
    }

    private List<T> updateImages(CompositeMediaId id, Optional<Temporal> date, List<T> uploadedMedia)
            throws IOException, UploadException {
        boolean save = false;
        List<T> medias = null;
        Optional<T> imageInDb = repository.findById(id);
        if (imageInDb.isPresent()) {
            medias = List.of(imageInDb.get());
        } else {
            medias = fetchMedias(id, date);
            save = !medias.isEmpty();
        }
        for (T media : medias) {
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
        }
        return medias;
    }

    protected T fetchMedia(CompositeMediaId id, Optional<Temporal> date) throws IOException {
        return fetchMedias(id, date).stream().filter(m -> m.getId().equals(id)).findFirst().orElseThrow();
    }

    protected List<T> fetchMedias(CompositeMediaId id, Optional<Temporal> date) throws IOException {
        try {
            String url = getSourceUrl(id);
            if (url == null) {
                LOGGER.error("Can't find valid source URL for {}", id);
                return null;
            }
            T media = getMediaClass().getConstructor().newInstance();
            media.setId(id);
            date.ifPresent(t -> {
                if (t instanceof LocalDate d) {
                    media.setPublicationDate(d);
                } else if (t instanceof ZonedDateTime dt) {
                    media.setPublicationDateTime(dt);
                } else if (t instanceof YearMonth m) {
                    media.setPublicationMonth(m);
                } else if (t instanceof Year y) {
                    media.setPublicationYear(y);
                } else {
                    throw new IllegalArgumentException("Unsupported temporal: " + t);
                }
            });
            return fillMediaWithHtml(url, fetchUrl(url), media);
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
