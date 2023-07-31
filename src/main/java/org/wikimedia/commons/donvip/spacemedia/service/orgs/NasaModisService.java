package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.newURL;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.apache.commons.lang3.tuple.Pair;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Safelist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.modis.NasaModisMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.modis.NasaModisMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.exception.UploadException;
import org.wikimedia.commons.donvip.spacemedia.utils.Emojis;

@Service
public class NasaModisService
        extends AbstractOrgService<NasaModisMedia, String, LocalDate> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NasaModisService.class);

    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("M/d/yyyy", Locale.ENGLISH);

    @Value("${nasa.modis.details.url}")
    private String detailsUrl;

    private final NasaModisMediaRepository modisRepository;

    public NasaModisService(NasaModisMediaRepository repository) {
        super(repository, "nasa.modis");
        this.modisRepository = repository;
    }

    @Override
    protected Class<NasaModisMedia> getMediaClass() {
        return NasaModisMedia.class;
    }

    @Override
    public String getName() {
        return "NASA (MODIS)";
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
    public URL getSourceUrl(NasaModisMedia media, FileMetadata metadata) {
        return newURL(detailsUrl.replace("<date>", media.getPublicationDate().toString()));
    }

    @Override
    protected String getAuthor(NasaModisMedia media) {
        return media.getCredit();
    }

    @Override
    protected Optional<Temporal> getCreationDate(NasaModisMedia media) {
        return Optional.ofNullable(media.getDate());
    }

    @Override
    protected final Optional<Temporal> getUploadDate(NasaModisMedia media) {
        return Optional.of(media.getPublicationDate());
    }

    @Override
    public void updateMedia() throws IOException, UploadException {
        LocalDateTime start = startUpdateMedia();
        List<NasaModisMedia> uploadedMedia = new ArrayList<>();
        int count = 0;
        LocalDate doNotFetchEarlierThan = getRuntimeData().getDoNotFetchEarlierThan();
        for (LocalDate date = LocalDate.now(); date.getYear() >= 1999
                && (doNotFetchEarlierThan == null || date.isAfter(doNotFetchEarlierThan)); date = date.minusDays(1)) {
            try {
                updateImage(date, uploadedMedia);
            } catch (IOException | UploadException | RuntimeException e) {
                LOGGER.error(e.getMessage(), e);
            } catch (UpdateFinishedException e) {
                LOGGER.info("Detected end of medias at date {}", date);
                break;
            }
            ongoingUpdateMedia(start, "MODIS", count++);
        }
        endUpdateMedia(count, uploadedMedia, start);
    }

    private NasaModisMedia updateImage(LocalDate date, List<NasaModisMedia> uploadedMedia)
            throws IOException, UploadException, UpdateFinishedException {
        boolean save = false;
        NasaModisMedia media = null;
        Optional<NasaModisMedia> imageInDb = modisRepository.findByPublicationDate(date);
        if (imageInDb.isPresent()) {
            media = imageInDb.get();
        } else {
            media = fetchMedia(date);
            save = true;
        }
        if (media.getDescription().contains("ÔÇÖ")) {
            media.setDescription(media.getDescription().replace("ÔÇÖ", "'"));
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
    public Set<String> findCategories(NasaModisMedia media, FileMetadata metadata, boolean includeHidden) {
        Set<String> result = super.findCategories(media, metadata, includeHidden);
        String whichModis = terraOrAqua(media, () -> "Terra (satellite) MODIS", () -> "Aqua (satellite) MODIS");
        result.addAll(findCategoriesForEarthObservationImage(media, x -> "Photos of " + x + " by the " + whichModis,
                "Photos by the " + whichModis));
        return result;
    }

    @Override
    protected String getSource(NasaModisMedia media, FileMetadata metadata) {
        return super.getSource(media, metadata) + " ([" + metadata.getAssetUrl() + " direct link])\n"
                + "{{NASA-image|id=" + media.getId() + "|center=GSFC}}";
    }

    @Override
    public Set<String> findInformationTemplates(NasaModisMedia media) {
        Set<String> result = super.findInformationTemplates(media);
        terraOrAqua(media,
                () -> result.add("NASA Photojournal/attribution|class=Terra|mission=Terra|name=Terra|credit=MODIS"),
                () -> result.add("NASA Photojournal/attribution|class=Aqua|mission=Aqua|name=Aqua|credit=MODIS"));
        return result;
    }

    @Override
    public Set<String> findLicenceTemplates(NasaModisMedia media) {
        Set<String> result = super.findLicenceTemplates(media);
        result.add("PD-USGov-NASA");
        return result;
    }

    @Override
    protected Map<String, Pair<Object, Map<String, Object>>> getStatements(NasaModisMedia media,
            FileMetadata metadata) {
        Map<String, Pair<Object, Map<String, Object>>> result = super.getStatements(media, metadata);
        terraOrAqua(media,
                () -> result.put("P170", Pair.of("Q584697", null)),
                () -> result.put("P170", Pair.of("Q17397", null))); // Created by Terra or Aqua
        result.put("P1071", Pair.of("Q663611", null)); // Created in low earth orbit
        result.put("P2079", Pair.of("Q725252", null)); // Satellite imagery
        result.put("P4082", Pair.of("Q676840", null)); // Taken with MODIS instrument
        return result;
    }

    private static <T> T terraOrAqua(NasaModisMedia media, Supplier<T> terra, Supplier<T> aqua) {
        if ("Terra".equals(media.getSatellite())) {
            return terra.get();
        } else if ("Aqua".equals(media.getSatellite())) {
            return aqua.get();
        }
        return null;
    }

    @Override
    protected NasaModisMedia refresh(NasaModisMedia media) throws IOException {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    protected Set<String> getEmojis(NasaModisMedia uploadedMedia) {
        return Set.of(Emojis.SATELLITE, Emojis.EARTH_AMERICA);
    }

    @Override
    protected Set<String> getTwitterAccounts(NasaModisMedia uploadedMedia) {
        return Set.of("@NASAEarth");
    }

    private NasaModisMedia fetchMedia(LocalDate date) throws IOException, UpdateFinishedException {
        NasaModisMedia image = new NasaModisMedia();
        image.setId(date.toString());
        image.setPublicationDate(date);
        String imgUrlLink = detailsUrl.replace("<date>", date.toString());
        LOGGER.info(imgUrlLink);
        Document html = Jsoup.connect(imgUrlLink).timeout(10_000).get();
        fillMediaWithHtml(html, image);
        return image;
    }

    void fillMediaWithHtml(Document html, NasaModisMedia image) throws UpdateFinishedException {
        Element div = requireNonNull(html.getElementsByClass("secondary_rt").first(), "secondary_rt");
        // Title
        String h5 = requireNonNull(div.getElementsByTag("h5").first(), "h5").text();
        if ("-".equals(h5)) {
            throw new UpdateFinishedException("Done");
        }
        image.setTitle(h5.substring(h5.indexOf('-') + 1).strip());
        // Image at top is used as thumbnail
        String thumb = div.getElementsByTag("img").first().attr("src").replace("http://", "https://");
        if (!thumb.startsWith("https://")) {
            thumb = "https://modis.gsfc.nasa.gov/gallery/" + thumb;
        }
        image.setThumbnailUrl(newURL(thumb));
        // Description and acquisition date
        image.setDescription(
                div.getElementsByTag("p").stream().map(p -> p.text().strip())
                        .filter(x -> isNotBlank(x) && !x.startsWith("Image Facts")).collect(joining("\n\n")));
        for (String fact : div.getElementsByTag("p").stream().map(p -> p.html().strip())
                .filter(x -> x.contains("<b>Image Facts")).map(x -> x.split("<br>"))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("metadata"))) {
            String cleanFact = fact.replace("</b> ", "").replace("\n", "").strip();
            extractFact(cleanFact, "Satellite:", image::setSatellite);
            extractFact(cleanFact, "Date Acquired:", x -> image.setDate(LocalDate.parse(x, dateFormatter)));
            extractFact(cleanFact, "Bands Used:", image::setBands);
            extractFact(cleanFact, "Image Credit:", image::setCredit);
        }
        if (image.getSatellite() == null) {
            boolean terra = image.getDescription().contains("Terra");
            boolean aqua = image.getDescription().contains("Aqua");
            if (terra && !aqua) {
                image.setSatellite("Terra");
            } else if (!terra && aqua) {
                image.setSatellite("Aqua");
            } else {
                LOGGER.error("Unable to determine satellite for {}", h5);
            }
        }
        // Image URL
        String url0250 = null;
        String url0500 = null;
        String url1000 = null;
        for (String url : div.getElementsByTag("a").stream().map(x -> x.attr("href"))
                .filter(x -> x.contains("://modis.gsfc.nasa.gov/gallery/images/image")).toList()) {
            if (url.endsWith("_250m.jpg")) {
                url0250 = url;
            } else if (url.endsWith("_500m.jpg")) {
                url0500 = url;
            } else if (url.endsWith("_1km.jpg")) {
                url1000 = url;
            }
        }
        if (url0250 != null) {
            addMetadata(image, url0250, null);
        } else if (url0500 != null) {
            addMetadata(image, url0500, null);
        } else if (url1000 != null) {
            addMetadata(image, url1000, null);
        }
    }

    private static void extractFact(String fact, String prefix, Consumer<String> setter) {
        if (fact.equals(prefix)) {
            LOGGER.warn("Unable to find a value for this fact: {}", fact);
        } else if (fact.startsWith(prefix)) {
            setter.accept(
                    Jsoup.clean(fact.substring(fact.indexOf(prefix) + prefix.length() + 1).strip(), Safelist.none()));
        }
    }
}
