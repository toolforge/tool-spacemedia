package org.wikimedia.commons.donvip.spacemedia.service.agencies;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.data.domain.ImageDimensions;
import org.wikimedia.commons.donvip.spacemedia.data.domain.Media;
import org.wikimedia.commons.donvip.spacemedia.data.domain.Metadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.NasaMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.NasaMediaType;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.sdo.NasaSdoDataType;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.sdo.NasaSdoMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.sdo.NasaSdoMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.exception.UploadException;
import org.wikimedia.commons.donvip.spacemedia.exception.WrappedIOException;
import org.wikimedia.commons.donvip.spacemedia.exception.WrappedUploadException;
import org.wikimedia.commons.donvip.spacemedia.utils.Emojis;

@Service
public class NasaSdoService
        extends AbstractAgencyService<NasaSdoMedia, String, LocalDateTime, NasaMedia, String, ZonedDateTime> {

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
    public URL getSourceUrl(NasaSdoMedia media) throws MalformedURLException {
        if (media.isVideo()) {
            return new URL(BASE_URL + "/data/dailymov/movie.php?q=" + media.getId());
        }
        return media.getMetadata().getAssetUrl();
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
    public void updateMedia() throws IOException, UploadException {
        LocalDateTime start = startUpdateMedia();
        List<NasaSdoMedia> uploadedMedia = new ArrayList<>();
        int count = 0;
        for (LocalDate date = LocalDate.now(); date.getYear() >= 2010; date = date.minusDays(1)) {
            String dateStringPath = DateTimeFormatter.ISO_LOCAL_DATE.format(date).replace('-', '/');
            updateImages(dateStringPath, date, uploadedMedia);
            if (videosEnabled) {
                updateVideos(dateStringPath, date, uploadedMedia);
            }
        }
        endUpdateMedia(count, uploadedMedia, uploadedMedia.stream().map(Media::getMetadata).toList(), start);
    }

    // First image of each day
    private void updateImages(String dateStringPath, LocalDate date, List<NasaSdoMedia> uploadedMedia)
            throws IOException {
        updateImagesOrVideos(dateStringPath, date, 4096, NasaMediaType.image, "browse", "jpg", uploadedMedia,
                NasaSdoService::parseImageDateTime);
    }

    static LocalDateTime parseImageDateTime(String filename) {
        return LocalDateTime.parse(filename.substring(0, 15), DATE_TIME_FORMAT);
    }

    // Daily movies
    private void updateVideos(String dateStringPath, LocalDate date, List<NasaSdoMedia> uploadedMedia)
            throws IOException {
        updateImagesOrVideos(dateStringPath, date, 1024, NasaMediaType.video, "dailymov", "ogv", uploadedMedia,
                NasaSdoService::parseMovieDateTime);
    }

    static LocalDateTime parseMovieDateTime(String filename) {
        return LocalDate.parse(filename.substring(0, 8), DateTimeFormatter.BASIC_ISO_DATE).atStartOfDay();
    }

    private void updateImagesOrVideos(String dateStringPath, LocalDate date, int dim, NasaMediaType mediaType,
            String browse, String ext, List<NasaSdoMedia> uploadedMedia,
            Function<String, LocalDateTime> dateTimeExtractor) throws IOException {
        ImageDimensions dimensions = new ImageDimensions(dim, dim);
        if (sdoRepository.countByMediaTypeAndDimensionsAndDate(mediaType, dimensions,
                date) < NasaSdoDataType.values().length) {
            String browseUrl = String.join("/", BASE_URL_IMG, browse, dateStringPath);
            List<String> files = Jsoup.connect(browseUrl).timeout(30_000).get().getElementsByTag("a").stream()
                    .map(e -> e.attr("href")).filter(href -> href.contains("_" + dimensions.getWidth() + "_")).sorted()
                    .toList();
            for (NasaSdoDataType dataType : NasaSdoDataType.values()) {
                files.stream().filter(i -> i.endsWith(dataType.name() + '.' + ext)).findFirst().ifPresent(firstFile -> {
                    try {
                        updateMedia(firstFile.replace("." + ext, ""), dateTimeExtractor.apply(firstFile), dimensions,
                                new URL(browseUrl + '/' + firstFile), mediaType, dataType, uploadedMedia);
                    } catch (IOException e) {
                        throw new WrappedIOException(e);
                    } catch (UploadException e) {
                        throw new WrappedUploadException(e);
                    }
                });
            }
        }
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
            media.setId(id);
            media.setTitle(id);
            media.setDate(date);
            media.setDataType(dataType);
            media.setInstrument(dataType.getInstrument());
            media.getMetadata().setAssetUrl(url);
            if (dimensions.getWidth() == 4096) {
                media.setThumbnailUrl(new URL(url.toExternalForm().replace("_4096_", "_1024_")));
            }
            media.setDimensions(dimensions);
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
    public Set<String> findCategories(NasaSdoMedia media, Metadata metadata, boolean includeHidden) {
        Set<String> result = super.findCategories(media, metadata, includeHidden);
        if (media.isVideo()) {
            result.add("Videos from the Solar Dynamics Observatory");
        } else if (media.isImage()) {
            result.add("Photos by Solar Dynamics Observatory");
        }
        return result;
    }

    @Override
    protected String getSource(NasaSdoMedia media) throws MalformedURLException {
        return super.getSource(media)
                + " ([" + media.getMetadata().getAssetUrl() + " direct link])\n"
                + "{{NASA-image|id=" + media.getId() + "|center=GSFC}}";
    }

    @Override
    public Set<String> findTemplates(NasaSdoMedia media) {
        Set<String> result = super.findTemplates(media);
        result.add("NASA Photojournal/attribution|class=SDO|mission=SDO|name=SDO|credit=" + media.getInstrument());
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
