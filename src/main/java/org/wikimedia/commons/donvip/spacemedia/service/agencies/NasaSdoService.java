package org.wikimedia.commons.donvip.spacemedia.service.agencies;

import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.newURL;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.ImageDimensions;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.library.NasaMediaType;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.sdo.NasaSdoAiaKeywords;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.sdo.NasaSdoDataType;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.sdo.NasaSdoMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.sdo.NasaSdoMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.exception.UploadException;
import org.wikimedia.commons.donvip.spacemedia.utils.Emojis;

@Service
public class NasaSdoService
        extends AbstractAgencyService<NasaSdoMedia, String, LocalDateTime> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NasaSdoService.class);

    private static final String BASE_URL = "https://sdo.gsfc.nasa.gov";
    private static final String BASE_URL_IMG = BASE_URL + "/assets/img";

    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    @Autowired
    private NasaSdoMediaRepository sdoRepository;

    public NasaSdoService(NasaSdoMediaRepository repository) {
        super(repository, "nasa.sdo");
    }

    @Override
    protected Class<NasaSdoMedia> getMediaClass() {
        return NasaSdoMedia.class;
    }

    @Override
    public String getName() {
        return "NASA (SDO)";
    }

    @Override
    protected String getMediaId(String id) {
        return id;
    }

    @Override
    protected boolean checkBlocklist() {
        return false;
    }

    @Override
    protected boolean includeByPerceptualHash() {
        return false;
    }

    @Override
    public URL getSourceUrl(NasaSdoMedia media) {
        if (media.isVideo()) {
            return newURL(BASE_URL + "/data/dailymov/movie.php?q=" + media.getId());
        }
        return media.getMetadata().get(0).getAssetUrl();
    }

    @Override
    protected String getAuthor(NasaSdoMedia media) {
        // https://sdo.gsfc.nasa.gov/data/rules.php
        return "Courtesy of NASA/SDO and the AIA, EVE, and HMI science teams.";
    }

    @Override
    protected Optional<Temporal> getCreationDate(NasaSdoMedia media) {
        return Optional.of(media.getDate());
    }

    @Override
    protected final Optional<Temporal> getUploadDate(NasaSdoMedia media) {
        return Optional.of(media.getDate());
    }

    @Override
    protected Map<String, Pair<Object, Map<String, Object>>> getStatements(NasaSdoMedia media, FileMetadata metadata) {
        Map<String, Pair<Object, Map<String, Object>>> result = super.getStatements(media, metadata);
        result.put("P170", Pair.of("Q382494", null)); // Created by SDO
        result.put("P180", Pair.of("Q525", null)); // Depicts the Sun
        result.put("P1071", Pair.of("Q472251", null)); // Created in geosynchronous orbit
        result.put("P2079", Pair.of("Q725252", null)); // Satellite imagery
        result.put("P4082", Pair.of(media.getInstrument().getQid(), null)); // Taken with SDO instrument
        Optional.ofNullable(media.getAiaKeywords()).map(NasaSdoAiaKeywords::getExpTime) // Exposure time in seconds
                .ifPresent(exp -> result.put("P6757", Pair.of(exp.toString(), null)));
        return result;
    }

    @Override
    public void updateMedia() throws IOException, UploadException {
        LocalDateTime start = startUpdateMedia();
        List<NasaSdoMedia> uploadedMedia = new ArrayList<>();
        int count = 0;
        LocalDate doNotFetchEarlierThan = getRuntimeData().getDoNotFetchEarlierThan();
        for (LocalDate date = LocalDate.now(); date.getYear() >= 2010
                && (doNotFetchEarlierThan == null || date.isAfter(doNotFetchEarlierThan)); date = date.minusDays(1)) {
            String dateStringPath = DateTimeFormatter.ISO_LOCAL_DATE.format(date).replace('-', '/');
            count += updateImages(dateStringPath, date, uploadedMedia, start, count);
            if (videosEnabled) {
                count += updateVideos(dateStringPath, date, uploadedMedia, start, count);
            }
        }
        endUpdateMedia(count, uploadedMedia, start);
    }

    // First image of each day
    private int updateImages(String dateStringPath, LocalDate date, List<NasaSdoMedia> uploadedMedia,
            LocalDateTime start, int count) throws IOException, UploadException {
        return updateImagesOrVideos(dateStringPath, date, 4096, NasaMediaType.image, "browse", "jpg", uploadedMedia,
                start, count, NasaSdoService::parseImageDateTime);
    }

    static LocalDateTime parseImageDateTime(String filename) {
        return LocalDateTime.parse(filename.substring(0, 15), DATE_TIME_FORMAT);
    }

    // Daily movies
    private int updateVideos(String dateStringPath, LocalDate date, List<NasaSdoMedia> uploadedMedia,
            LocalDateTime start, int count) throws IOException, UploadException {
        return updateImagesOrVideos(dateStringPath, date, 1024, NasaMediaType.video, "dailymov", "ogv", uploadedMedia,
                start, count, NasaSdoService::parseMovieDateTime);
    }

    static LocalDateTime parseMovieDateTime(String filename) {
        return LocalDate.parse(filename.substring(0, 8), DateTimeFormatter.BASIC_ISO_DATE).atStartOfDay();
    }

    private int updateImagesOrVideos(String dateStringPath, LocalDate date, int dim, NasaMediaType mediaType,
            String browse, String ext, List<NasaSdoMedia> uploadedMedia, LocalDateTime start, int count,
            Function<String, LocalDateTime> dateTimeExtractor) throws IOException, UploadException {
        ImageDimensions dimensions = new ImageDimensions(dim, dim);
        int localCount = 0;
        if (mediaType == NasaMediaType.image && date.getDayOfMonth() == 1) {
            LOGGER.info("Current update date: {} ...", date);
        }
        long imagesInDatabase = sdoRepository.countByMediaTypeAndDimensionsAndDate(mediaType, dimensions, date);
        int expectedCount = mediaType == NasaMediaType.video ? NasaSdoDataType.values().length - 1
                : NasaSdoDataType.values().length;
        if (imagesInDatabase < expectedCount) {
            String browseUrl = String.join("/", BASE_URL_IMG, browse, dateStringPath);
            LOGGER.info("Fetching {} ({}<{})", browseUrl, imagesInDatabase, expectedCount);
            try {
                List<String> files = Jsoup.connect(browseUrl).header("Accept", "text/html").timeout(30_000).get()
                        .getElementsByTag("a").stream().map(e -> e.attr("href"))
                        .filter(href -> href.contains("_" + dimensions.getWidth() + "_")).sorted().toList();
                for (NasaSdoDataType dataType : NasaSdoDataType.values()) {
                    Optional<String> opt = files.stream().filter(i -> i.endsWith(dataType.name() + '.' + ext))
                            .findFirst();
                    if (opt.isPresent()) {
                        String firstFile = opt.get();
                        updateMedia(firstFile.replace("." + ext, ""), dateTimeExtractor.apply(firstFile), dimensions,
                                newURL(browseUrl + '/' + firstFile), mediaType, dataType, uploadedMedia);
                        ongoingUpdateMedia(start, count + localCount++);
                    }
                }
            } catch (HttpStatusException e) {
                // https://sdo.gsfc.nasa.gov/assets/img/browse/2016/12/25/ => HTTP 404
                LOGGER.error(e.getMessage(), e);
            }
        }
        return localCount;
    }

    private NasaSdoMedia updateMedia(String id, LocalDateTime date, ImageDimensions dimensions, URL url,
            NasaMediaType mediaType, NasaSdoDataType dataType, List<NasaSdoMedia> uploadedMedia)
            throws IOException, UploadException {
        boolean save = false;
        NasaSdoMedia media = null;
        Optional<NasaSdoMedia> imageInDb = repository.findById(id);
        if (imageInDb.isPresent()) {
            media = imageInDb.get();
        } else {
            media = new NasaSdoMedia();
            FileMetadata metadata = media.getUniqueMetadata();
            media.setId(id);
            media.setTitle(id);
            media.setDate(date);
            media.setDataType(dataType);
            media.setInstrument(dataType.getInstrument());
            metadata.setAssetUrl(url);
            if (dimensions.getWidth() == 4096) {
                media.setThumbnailUrl(newURL(url.toExternalForm().replace("_4096_", "_1024_")));
            }
            metadata.setImageDimensions(dimensions);
            metadataRepository.save(metadata);
            media.setMediaType(mediaType);
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
        if (save) {
            media = saveMedia(media);
        }
        return media;
    }

    @Override
    public Set<String> findCategories(NasaSdoMedia media, FileMetadata metadata, boolean includeHidden) {
        Set<String> result = super.findCategories(media, metadata, includeHidden);
        if (media.isVideo()) {
            result.add("Videos from the Solar Dynamics Observatory");
        }
        return result;
    }

    @Override
    protected Pair<String, Map<String, String>> getWikiFileDesc(NasaSdoMedia media, FileMetadata metadata)
            throws IOException {
        return Pair.of("{{NASA SDO|instrument=" + media.getInstrument() + "|band="
                + media.getDataType().name().replace("_", "") + "|type=" + metadata.getFileExtension() + "|id="
                + media.getId() + "}}", Map.of());
    }

    @Override
    public Set<String> findLicenceTemplates(NasaSdoMedia media) {
        Set<String> result = super.findLicenceTemplates(media);
        result.add("PD-USGov-NASA");
        return result;
    }

    @Override
    protected NasaSdoMedia refresh(NasaSdoMedia media) throws IOException {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    protected Set<String> getEmojis(NasaSdoMedia uploadedMedia) {
        return Set.of(Emojis.SUN);
    }

    @Override
    protected Set<String> getTwitterAccounts(NasaSdoMedia uploadedMedia) {
        return Set.of("@NASASun");
    }
}
