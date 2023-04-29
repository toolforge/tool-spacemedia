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
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.data.domain.ImageDimensions;
import org.wikimedia.commons.donvip.spacemedia.data.domain.Media;
import org.wikimedia.commons.donvip.spacemedia.data.domain.Metadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.NasaMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.aster.NasaAsterImage;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.aster.NasaAsterImageRepository;
import org.wikimedia.commons.donvip.spacemedia.exception.UploadException;
import org.wikimedia.commons.donvip.spacemedia.utils.Emojis;
import org.wikimedia.commons.donvip.spacemedia.utils.Utils;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class NasaAsterService
        extends AbstractAgencyService<NasaAsterImage, String, LocalDate, NasaMedia, String, ZonedDateTime> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NasaAsterService.class);

    private static final Pattern ADDED = Pattern
            .compile(".*Added: (\\d{1,2}/\\d{1,2}/\\d{4} \\d{1,2}:\\d{1,2}:\\d{1,2} [AP]M).*");

    private static final DateTimeFormatter addedDateTimeFormat = DateTimeFormatter.ofPattern("M/d/yyyy h:m:s a",
            Locale.ENGLISH);

    private static final Pattern ACQUIRED = Pattern
            .compile(".*image was acquired(?: on)? ([A-Z][a-z]+ \\d{1,2}, \\d{4}).*");

    private static final DateTimeFormatter acqDateFormat = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH);

    private static final Pattern SIZE = Pattern.compile(".*Size: \\(([\\d,]+) (KB|bytes)\\).*");

    private static final Pattern RESOLUTION = Pattern.compile(".*Resolution \\( ([\\d,]+) x ([\\d,]+) \\).*");

    @Value("${nasa.aster.gallery.url}")
    private String galleryUrl;

    @Value("${nasa.aster.details.url}")
    private String detailsUrl;

    @Autowired
    private ObjectMapper jackson;

    public NasaAsterService(NasaAsterImageRepository repository) {
        super(repository, "nasa.aster");
    }

    @Override
    protected Class<NasaAsterImage> getMediaClass() {
        return NasaAsterImage.class;
    }

    @Override
    public String getName() {
        return "NASA (ASTER)";
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
    public URL getSourceUrl(NasaAsterImage media) throws MalformedURLException {
        return new URL(detailsUrl.replace("<id>", media.getId()));
    }

    @Override
    protected String getAuthor(NasaAsterImage media) {
        return "NASA/METI/AIST/Japan Space Systems, and U.S./Japan ASTER Science Team";
    }

    @Override
    protected Optional<Temporal> getCreationDate(NasaAsterImage media) {
        return Optional.of(media.getDate());
    }

    @Override
    public void updateMedia() throws IOException, UploadException {
        LocalDateTime start = startUpdateMedia();
        List<NasaAsterImage> uploadedMedia = new ArrayList<>();
        int count = 0;
        for (AsterItem item : Utils.restTemplateSupportingAll(jackson).getForObject(galleryUrl, AsterItem[].class)) {
            updateImage(item, uploadedMedia);
            ongoingUpdateMedia(start, "ASTER", count++);
        }
        endUpdateMedia(count, uploadedMedia, uploadedMedia.stream().map(Media::getMetadata).toList(), start);
    }

    private NasaAsterImage updateImage(AsterItem item, List<NasaAsterImage> uploadedMedia)
            throws IOException, UploadException {
        boolean save = false;
        NasaAsterImage media = null;
        Optional<NasaAsterImage> imageInDb = repository.findById(item.getName());
        if (imageInDb.isPresent()) {
            media = imageInDb.get();
        } else {
            media = fetchMedia(item);
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
    public Set<String> findCategories(NasaAsterImage media, Metadata metadata, boolean includeHidden) {
        Set<String> result = super.findCategories(media, metadata, includeHidden);
        result.add("Photos by ASTER");
        return result;
    }

    @Override
    protected String getSource(NasaAsterImage media) throws MalformedURLException {
        return super.getSource(media)
                + " ([" + media.getMetadata().getAssetUrl() + " direct link])\n"
                + "{{NASA-image|id=" + media.getId() + "|center=JPL}}";
    }

    @Override
    public Set<String> findTemplates(NasaAsterImage media) {
        Set<String> result = super.findTemplates(media);
        result.add("PD-USGov-NASA");
        return result;
    }

    @Override
    protected NasaAsterImage refresh(NasaAsterImage media) throws IOException {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    protected Set<String> getEmojis(NasaAsterImage uploadedMedia) {
        return Set.of(Emojis.SATELLITE, Emojis.EARTH_AMERICA);
    }

    @Override
    protected Set<String> getTwitterAccounts(NasaAsterImage uploadedMedia) {
        return Set.of("@NASAEarth");
    }

    private NasaAsterImage fetchMedia(AsterItem item) throws IOException {
        NasaAsterImage image = new NasaAsterImage();
        image.setId(item.getName());
        image.setLongName(item.getLname());
        image.setCategory(item.getCat());
        image.setIcon(item.getIcon());
        image.setLatitude(item.getLat());
        image.setLongitude(item.getLng());
        String imgUrlLink = detailsUrl.replace("<id>", item.getName());
        LOGGER.info(imgUrlLink);
        Document html = Jsoup.connect(imgUrlLink).timeout(15_000).get();
        fillMediaWithHtml(html, image);
        return image;
    }

    static void fillMediaWithHtml(Document html, NasaAsterImage image) throws MalformedURLException {
        Element bodyText = Objects.requireNonNull(html.getElementsByClass("BodyText").first(), "BodyText");
        Elements tables = bodyText.getElementsByTag("table");
        image.setTitle(Objects.requireNonNull(bodyText.getElementsByTag("font").first(), "font").text());
        image.setThumbnailUrl(
                new URL(tables.get(0).getElementsByTag("img").first().attr("src").replace("http://", "https://")));
        image.setDescription(tables.get(1).getElementsByTag("td").text());
        Matcher m = ACQUIRED.matcher(image.getDescription());
        if (m.matches()) {
            image.setDate(LocalDate.parse(m.group(1), acqDateFormat));
        } else {
            throw new IllegalStateException("No acquisition date found: " + image.getDescription());
        }
        image.getMetadata().setAssetUrl(
                new URL(tables.get(2).getElementsByTag("a").first().attr("href").replace("http://", "https://")));
        String meta = tables.get(2).getElementsByTag("td").get(1).text();
        m = SIZE.matcher(meta);
        if (m.matches()) {
            image.getMetadata().setSize(Long.valueOf(numval(m.group(1))) * ("KB".equals(m.group(2)) ? 1024 : 1));
        } else {
            throw new IllegalStateException("No size found: " + meta);
        }
        m = RESOLUTION.matcher(meta);
        if (m.matches()) {
            image.setDimensions(
                    new ImageDimensions(Integer.valueOf(numval(m.group(1))), Integer.valueOf(numval(m.group(2)))));
        } else {
            throw new IllegalStateException("No resolution found: " + meta);
        }
        meta = html.getElementsByTag("tr").get(1).getElementsByTag("table").get(0).nextElementSibling()
                .getElementsByTag("tr").first().nextElementSibling().nextElementSibling().getElementsByTag("tr").get(0)
                .getElementsByTag("td").first().getElementsByTag("td").get(2).text();
        m = ADDED.matcher(meta);
        if (m.matches()) {
            image.setPublicationDate(LocalDateTime.parse(m.group(1), addedDateTimeFormat));
        } else {
            throw new IllegalStateException("No publication date found: " + meta);
        }
    }

    private static String numval(String s) {
        return s.replace(",", "");
    }

    static class AsterItem {
        private double lat;
        private double lng;
        private String name;
        private String lname;
        @JsonProperty("Cat")
        private String cat;
        private String icon;

        public double getLat() {
            return lat;
        }

        public void setLat(double lat) {
            this.lat = lat;
        }

        public double getLng() {
            return lng;
        }

        public void setLng(double lng) {
            this.lng = lng;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getLname() {
            return lname;
        }

        public void setLname(String lname) {
            this.lname = lname;
        }

        public String getCat() {
            return cat;
        }

        public void setCat(String cat) {
            this.cat = cat;
        }

        public String getIcon() {
            return icon;
        }

        public void setIcon(String icon) {
            this.icon = icon;
        }

        @Override
        public String toString() {
            return "AsterItem [lat=" + lat + ", lng=" + lng + ", name=" + name + ", lname=" + lname + ", cat=" + cat
                    + ", icon=" + icon + "]";
        }
    }
}
