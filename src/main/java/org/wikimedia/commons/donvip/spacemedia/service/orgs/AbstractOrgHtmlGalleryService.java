package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.getWithJsoup;

import java.io.IOException;
import java.net.URL;
import java.net.UnknownHostException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
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

    protected abstract Elements getGalleryItems(String repoId, Element html);

    protected Optional<ZonedDateTime> extractDateFromGalleryItem(Element result) {
        return empty();
    }

    protected abstract String extractIdFromGalleryItem(Element result);

    @Override
    public final URL getSourceUrl(T media, FileMetadata metadata) {
        return ofNullable(getSourceUrl(media.getId())).map(Utils::newURL).orElse(null);
    }

    protected abstract String getSourceUrl(CompositeMediaId id);

    abstract void fillMediaWithHtml(String url, Document html, T media) throws IOException;

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
                Elements results = getGalleryItems(repoId, getWithJsoup(pageUrl, 10_000, 3));
                for (Element result : results) {
                    CompositeMediaId id = new CompositeMediaId(repoId, extractIdFromGalleryItem(result));
                    Optional<ZonedDateTime> date = extractDateFromGalleryItem(result);
                    if (date.isPresent() && doNotFetchEarlierThan != null
                            && date.get().toLocalDate().isBefore(doNotFetchEarlierThan)) {
                        loop = false;
                    }
                    if (loop) {
                        try {
                            T media = updateImage(id, date, uploadedMedia);
                            if (doNotFetchEarlierThan != null && media != null
                                    && media.getPublicationDate().isBefore(doNotFetchEarlierThan)) {
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

    private T updateImage(CompositeMediaId id, Optional<ZonedDateTime> date, List<T> uploadedMedia)
            throws IOException, UploadException {
        boolean save = false;
        T media = null;
        Optional<T> imageInDb = repository.findById(id);
        if (imageInDb.isPresent()) {
            media = imageInDb.get();
        } else {
            media = fetchMedia(id, date);
            save = media != null;
        }
        if (media != null) {
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
        return media;
    }

    protected T fetchMedia(CompositeMediaId id, Optional<ZonedDateTime> date) throws IOException {
        try {
            String url = getSourceUrl(id);
            if (url == null) {
                LOGGER.error("Can't find valid source URL for {}", id);
                return null;
            }
            T media = getMediaClass().getConstructor().newInstance();
            media.setId(id);
            date.ifPresent(media::setPublicationDateTime);
            fillMediaWithHtml(url, getWithJsoup(url, 10_000, 5), media);
            return media;
        } catch (ReflectiveOperationException | IllegalArgumentException | SecurityException e) {
            throw new IllegalStateException(e);
        }
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
