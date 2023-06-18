package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import static java.util.stream.Collectors.toSet;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.newURL;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.copernicus.gallery.CopernicusGalleryMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.copernicus.gallery.CopernicusGalleryMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.exception.UploadException;

@Service
public class CopernicusGalleryService extends AbstractOrgService<CopernicusGalleryMedia, String, ZonedDateTime> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CopernicusGalleryService.class);

    private static final String BASE_URL = "https://www.copernicus.eu";

    @Value("${copernicus.max.tries:5}")
    private int maxTries;

    @Autowired
    public CopernicusGalleryService(CopernicusGalleryMediaRepository repository) {
        super(repository, "copernicus");
    }

    @Override
    public String getName() {
        return "Copernicus";
    }

    @Override
    public URL getSourceUrl(CopernicusGalleryMedia media, FileMetadata metadata) {
        return newURL(BASE_URL + "/en/media/image-day-gallery/" + media.getId());
    }

    @Override
    protected String getAuthor(CopernicusGalleryMedia media) throws MalformedURLException {
        return media.getCredit();
    }

    @Override
    public Set<String> findLicenceTemplates(CopernicusGalleryMedia media) {
        return Set.of("Attribution-Copernicus|" + media.getYear().getValue());
    }

    @Override
    protected boolean checkBlocklist() {
        return false;
    }

    @Override
    public void updateMedia() throws IOException, UploadException {
        int i = 0;
        int count = 0;
        boolean loop = true;
        LocalDateTime start = LocalDateTime.now();
        List<CopernicusGalleryMedia> uploadedMedia = new ArrayList<>();
        LocalDate doNotFetchEarlierThan = getRuntimeData().getDoNotFetchEarlierThan();
        do {
            Elements results = Jsoup.connect(BASE_URL + "/en/media/image-day?page=" + i++).get()
                    .getElementsByClass("search-results-item-details");
            loop = !results.isEmpty();
            if (loop) {
                for (Element result : results) {
                    try {
                        ZonedDateTime date = ZonedDateTime
                                .parse(result.getElementsByTag("time").first().attr("datetime"));
                        loop = doNotFetchEarlierThan == null || date.toLocalDate().isAfter(doNotFetchEarlierThan);
                        if (loop) {
                            String href = result.getElementsByTag("a").first().attr("href");
                            String id = href.substring(href.lastIndexOf('/') + 1);
                            updateImage(id, date, uploadedMedia);
                            ongoingUpdateMedia(start, count++);
                        }
                    } catch (RuntimeException | IOException e) {
                        LOGGER.error("Unable to process {}", result, e);
                    }
                }
            }
        } while (loop);
        endUpdateMedia(count, uploadedMedia, uploadedMedia.stream().flatMap(m -> m.getMetadata().stream()).toList(),
                start);
    }

    private CopernicusGalleryMedia updateImage(String id, ZonedDateTime date,
            List<CopernicusGalleryMedia> uploadedMedia) throws IOException, UploadException {
        boolean save = false;
        CopernicusGalleryMedia media = null;
        Optional<CopernicusGalleryMedia> imageInDb = repository.findById(id);
        if (imageInDb.isPresent()) {
            media = imageInDb.get();
        } else {
            media = fetchMedia(id, date);
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

    private CopernicusGalleryMedia fetchMedia(String id, ZonedDateTime date) throws IOException {
        CopernicusGalleryMedia media = new CopernicusGalleryMedia();
        media.setId(id);
        media.setDate(date);
        String url = BASE_URL + "/en/media/image-day-gallery/" + id;
        LOGGER.info("GET {}", url);
        boolean ok = false;
        for (int i = 0; i < maxTries && !ok; i++) {
            try {
                fillMediaWithHtml(Jsoup.connect(url).get(), media);
                ok = true;
            } catch (IOException e) {
                LOGGER.error(media.toString(), e);
            }
        }
        return media;
    }

    void fillMediaWithHtml(Document html, CopernicusGalleryMedia media) {
        Element section = html.getElementsByTag("main")
                .first().getElementsByTag("section").first();
        media.setTitle(section.getElementsByTag("h1").first().text());
        media.setThumbnailUrl(newURL(BASE_URL
                + section.getElementsByClass("field_image_day").first().getElementsByTag("img").first().attr("src")));

        String prevLine = null;
        for (String line : section.getElementsByClass("col-12 col-md-6 order-md-first m-0").first()
                .getElementsByClass("mb-1").first().wholeText().strip().split("\n")) {
            if ("Location:".equals(prevLine)) {
                media.setLocation(line.strip());
            } else if ("Credit:".equals(prevLine)) {
                media.setCredit(line.strip());
            }
            prevLine = line.strip();
        }

        media.setKeywords(section.getElementsByClass("col-12 col-md-6 order-md-first m-0").first()
                .getElementsByClass("search-tag-btn").stream()
                .map(Element::text).filter(x -> !"Image of The Day".equals(x)).collect(toSet()));
        media.setDescription(section.getElementsByClass("ec-content").text());
        String href = section.getElementsByClass("card-body").get(1).getElementsByTag("a").first().attr("href");
        addMetadata(media, BASE_URL + href.substring(href.lastIndexOf('=') + 1), null);
    }

    @Override
    protected CopernicusGalleryMedia refresh(CopernicusGalleryMedia media) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected Class<CopernicusGalleryMedia> getMediaClass() {
        return CopernicusGalleryMedia.class;
    }

    @Override
    protected String getMediaId(String id) {
        return id;
    }

    @Override
    protected Set<String> getTwitterAccounts(CopernicusGalleryMedia uploadedMedia) {
        return Set.of("@CopernicusEU");
    }
}
