package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toMap;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.newURL;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.CompositeMediaId;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.stac.StacMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.stac.StacMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.exception.UploadException;
import org.wikimedia.commons.donvip.spacemedia.utils.Emojis;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * Service fetching satellite images from a static STAC catalog
 */
public abstract class AbstractOrgStacService extends AbstractOrgService<StacMedia> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractOrgStacService.class);

    private final StacMediaRepository stacRepository;

    private final Map<String, URL> catalogUrls;

    protected AbstractOrgStacService(StacMediaRepository repository, String id, Set<String> catalogs) {
        super(repository, id, decodeMap(catalogs).keySet());
        this.stacRepository = repository;
        this.catalogUrls = decodeMap(catalogs);
    }

    private static Map<String, URL> decodeMap(Set<String> values) {
        return values.stream().map(x -> x.split("\\|")).collect(toMap(x -> x[0], x -> newURL(x[1])));
    }

    @Override
    protected boolean isSatellitePicture(StacMedia media, FileMetadata metadata) {
        return true;
    }

    @Override
    protected boolean checkBlocklist() {
        return false;
    }

    @Override
    public void updateMedia(String[] args) throws IOException, UploadException {
        LocalDateTime start = startUpdateMedia();
        List<StacMedia> uploadedMedia = new ArrayList<>();
        int count = 0;
        for (String repoId : getRepoIdsFromArgs(args)) {
            Pair<Integer, Collection<StacMedia>> update = updateStacMedia(repoId);
            uploadedMedia.addAll(update.getRight());
            count += update.getLeft();
            ongoingUpdateMedia(start, count);
        }
        endUpdateMedia(count, uploadedMedia, allMetadata(uploadedMedia), start, LocalDate.now().minusYears(1), true);
    }

    @Override
    public URL getSourceUrl(StacMedia media, FileMetadata metadata) {
        return media.getUrl();
    }

    @Override
    protected StacMedia refresh(StacMedia media) throws IOException {
        // TODO
        return media.copyDataFrom(media);
    }

    @Override
    protected Class<StacMedia> getMediaClass() {
        return StacMedia.class;
    }

    @Override
    protected Set<String> getEmojis(StacMedia uploadedMedia) {
        Set<String> result = super.getEmojis(uploadedMedia);
        result.add(Emojis.SATELLITE);
        return result;
    }

    protected Pair<Integer, Collection<StacMedia>> updateStacMedia(String repoId) throws MalformedURLException {
        List<StacMedia> uploadedMedia = new ArrayList<>();
        int count = 0;
        Set<String> processedItems = new HashSet<>();
        LocalDateTime start = LocalDateTime.now();
        LocalDate doNotFetchEarlierThan = getRuntimeData().getDoNotFetchEarlierThan();

        try {
            count += processStacCatalog(catalogUrls.get(repoId), start, doNotFetchEarlierThan, repoId, uploadedMedia,
                    processedItems, count);
        } catch (IOException e) {
            LOGGER.error("Unable to process STAC catalog", e);
        }

        return Pair.of(count, uploadedMedia);
    }

    protected int processStacCatalog(URL catalogUrl, LocalDateTime start, LocalDate doNotFetchEarlierThan,
            String repoId, List<StacMedia> uploadedMedia, Set<String> processedItems, int startCount)
            throws IOException {
        int count = 0;
        LOGGER.info("Processing STAC catalog {}", catalogUrl);
        for (StacLink link : jackson.readValue(catalogUrl, StacCatalog.class).links()) {
            try {
                if ("child".equals(link.rel())) {
                    count += processStacCatalog(link.absoluteHref(catalogUrl), start, doNotFetchEarlierThan, repoId,
                            uploadedMedia, processedItems, startCount + count);
                } else if ("item".equals(link.rel()) && !isStacItemIgnored(link.href())
                        && (doNotFetchEarlierThan == null || !isStacItemBefore(link.href(), doNotFetchEarlierThan))) {
                    Pair<StacMedia, Integer> result = processStacItem(link.absoluteHref(catalogUrl),
                            repoId, this::fetchStacMedia, processedItems);
                    if (result != null && result.getValue() > 0) {
                        uploadedMedia.add(result.getKey());
                    }
                    ongoingUpdateMedia(start, repoId, startCount + count++);
                }
            } catch (IOException | RuntimeException | UploadException e) {
                LOGGER.error("Unable to process STAC link {}", link, e);
            }
        }
        return count;
    }

    protected Pair<StacMedia, Integer> processStacItem(URL itemUrl, String repoId,
            BiFunction<String, URL, StacMedia> worker,
            Set<String> processedItems) throws IOException, UploadException {
        if (processedItems.contains(itemUrl.toExternalForm())) {
            return null;
        }
        StacMedia media = null;
        boolean save = false;
        Optional<StacMedia> mediaInDb = stacRepository.findByUrl(itemUrl);
        if (mediaInDb.isPresent()) {
            media = mediaInDb.get();
        } else {
            media = worker.apply(repoId, itemUrl);
            save = true;
        }
        save |= doCommonUpdate(media);
        int uploadCount = 0;
        if (shouldUploadAuto(media, false)) {
            Triple<StacMedia, Collection<FileMetadata>, Integer> upload = upload(media, true, false);
            uploadCount = upload.getRight();
            media = upload.getLeft();
            save = true;
        }
        if (save) {
            saveMedia(media);
        }
        processedItems.add(itemUrl.toExternalForm());
        return Pair.of(media, uploadCount);
    }

    private StacMedia fetchStacMedia(String repoId, URL itemUrl) {
        try {
            StacItem item = jackson.readValue(itemUrl, StacItem.class);
            StacProperties properties = item.properties();
            StacMedia media = new StacMedia();
            media.setUrl(itemUrl);
            media.setId(new CompositeMediaId(repoId, item.id()));
            media.setCreationDateTime(properties.datetime());
            media.setPublicationDateTime(properties.endDatetime());
            media.setLongitude(properties.projCentroid().get(0));
            media.setLatitude(properties.projCentroid().get(1));
            StacAssets assets = item.assets();
            Arrays.stream(new StacAsset[] { assets.HH(), assets.HV(), assets.VV(), assets.VH() })
                    .filter(x -> x != null && x.href().toExternalForm().contains(".tif"))
                    .forEach(x -> addMetadata(media, x.href(), null));
            if (!media.hasMetadata() && assets.preview() != null) {
                addMetadata(media, assets.preview().href(), null);
            }
            ofNullable(assets.thumbnail()).ifPresent(x -> media.setThumbnailUrl(x.href()));
            enrichStacMedia(media);
            return media;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    protected abstract void enrichStacMedia(StacMedia media);

    protected abstract boolean isStacItemBefore(String itemHref, LocalDate doNotFetchEarlierThan);

    protected abstract boolean isStacItemIgnored(String itemHref);

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @JsonIgnoreProperties(ignoreUnknown = true)
    protected static record StacCatalog(String type, String id, String stacVersion, String description,
            List<StacLink> links, List<URL> stacExtensions) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @JsonIgnoreProperties(ignoreUnknown = true)
    protected static record StacLink(String rel, String href, String type, String title) {
        public URL absoluteHref(String parent) throws MalformedURLException {
            return new URL(newURL(parent.substring(0, parent.lastIndexOf('/') + 1)), href);
        }

        public URL absoluteHref(URL parent) throws MalformedURLException {
            return absoluteHref(parent.toExternalForm());
        }
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @JsonIgnoreProperties(ignoreUnknown = true)
    protected static record StacItem(String type, String stacVersion, String id, StacProperties properties,
            StacGeometry geometry, List<StacLink> links, StacAssets assets, Double[] bbox, List<String> stacExtensions,
            String collection) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @JsonIgnoreProperties(ignoreUnknown = true)
    protected static record StacProperties(ZonedDateTime datetime, ZonedDateTime startDatetime,
            ZonedDateTime endDatetime, String platform, String constellation, List<String> instruments,
            @JsonProperty("proj:centroid") List<Double> projCentroid,
            @JsonProperty("proj:epsg") Integer projEpsg,
            @JsonProperty("proj:shape") List<Integer> projShape,
            @JsonProperty("proj:transform") List<Double> projTransform,
            @JsonProperty("sar:center_frequency") String sarCenterFrequency,
            @JsonProperty("sar:frequency_band") String sarFrequencyBand,
            @JsonProperty("sar:instrument_mode") String sarInstrumentMode,
            @JsonProperty("sar:looks_azimuth") Integer sarLooksAzimuth,
            @JsonProperty("sar:looks_equivalent_number") Integer sarLooksEquivalentNumber,
            @JsonProperty("sar:looks_range") Integer sarLooksRange,
            @JsonProperty("sar:observation_direction") String sarObservationDirection,
            @JsonProperty("sar:pixel_spacing_azimuth") String sarPixelSpacingAzimuth,
            @JsonProperty("sar:pixel_spacing_range") String sarPixelSpacingRange,
            @JsonProperty("sar:polarizations") List<String> sarPolarizations,
            @JsonProperty("sar:product_type") String sarProductType,
            @JsonProperty("sar:resolution_azimuth") Double sarResolutionAzimuth,
            @JsonProperty("sar:resolution_range") Double sarResolutionRange,
            @JsonProperty("sat:orbit_state") String satOrbitState,
            @JsonProperty("view:incidence_angle") Double viewIncidenceAngle,
            @JsonProperty("view:look_angle") Double viewLookAngle) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    protected static record StacGeometry(String type, List<List<List<Double>>> coordinates) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @JsonIgnoreProperties(ignoreUnknown = true)
    protected static record StacAssets(@JsonProperty("HH") StacAsset HH, @JsonProperty("VV") StacAsset VV,
            @JsonProperty("HV") StacAsset HV, @JsonProperty("VH") StacAsset VH, StacAsset metadata,
            StacAsset preview, StacAsset thumbnail) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @JsonIgnoreProperties(ignoreUnknown = true)
    protected static record StacAsset(URL href, String type, String title,
            @JsonProperty("sar:polarizations") List<String> sarPolarizations, List<String> roles) {
    }
}
