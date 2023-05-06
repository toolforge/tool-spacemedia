package org.wikimedia.commons.donvip.spacemedia.service.agencies;

import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.data.domain.Metadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.NasaMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.NasaMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.sirs.NasaSirsImage;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.sirs.NasaSirsImageRepository;
import org.wikimedia.commons.donvip.spacemedia.exception.UploadException;
import org.wikimedia.commons.donvip.spacemedia.service.AbstractSocialMediaService;
import org.wikimedia.commons.donvip.spacemedia.service.nasa.NasaMediaProcessorService;

@Service
public class NasaSirsService
        extends AbstractAgencyService<NasaSirsImage, String, LocalDate, NasaMedia, String, ZonedDateTime> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NasaSirsService.class);

    private static final DateTimeFormatter usDateformatter = DateTimeFormatter.ofPattern("MM/dd/yyyy", Locale.ENGLISH);

    @Value("${nasa.sirs.categories.url}")
    private String categoriesUrl;

    @Value("${nasa.sirs.images.url}")
    private String imagesUrl;

    @Value("${nasa.sirs.details.url}")
    private String detailsUrl;

    @Value("${nasa.ssc.home.page}")
    private URL homePage;

    @Value("${nasa.ssc.name}")
    private String sscName;

    @Autowired
    private NasaMediaRepository<NasaMedia> nasaMediaRepository;

    public NasaSirsService(NasaSirsImageRepository repository) {
        super(repository, "nasa.sirs");
    }

    @Override
    protected Class<NasaSirsImage> getMediaClass() {
        return NasaSirsImage.class;
    }

    @Override
    public String getName() {
        return "NASA (SIRS)";
    }

    @Override
    protected NasaMediaRepository<NasaMedia> getOriginalRepository() {
        return nasaMediaRepository;
    }

    @Override
    protected String getOriginalId(String id) {
        return id;
    }

    @Override
    protected String getMediaId(String id) {
        return id;
    }

    @Override
    public URL getSourceUrl(NasaSirsImage media) throws MalformedURLException {
        return new URL(detailsUrl.replace("<id>", media.getId()));
    }

    @Override
    protected String getAuthor(NasaSirsImage media) {
        return wikiLink(homePage, sscName);
    }

    @Override
    protected Optional<Temporal> getCreationDate(NasaSirsImage media) {
        return Optional.of(media.getDate() != null ? media.getDate() : media.getYear());
    }

    @Override
    public void updateMedia() throws IOException, UploadException {
        LocalDateTime start = startUpdateMedia();
        int count = updateFromSirs();
        Set<String> processedImages = new HashSet<>();
        // SIRS doesn"t work anymore so make sure we're still able to handle existing files
        for (NasaSirsImage image : repository.findMissingInCommons()) {
            if (!processedImages.contains(image.getId())) {
                for (NasaSirsImage dupe : repository.findByMetadata_Sha1(image.getMetadata().getSha1())) {
                    if (!dupe.getId().equals(image.getId())) {
                        processedImages.add(dupe.getId());
                        LOGGER.warn("Deleting {} SIRS image (duplicate of {})", dupe.getId(), image.getId());
                        deleteMedia(dupe, "SIRS duplicate of " + image.getId());
                        count++;
                    }
                    if (doCommonUpdate(image)) {
                        image = saveMedia(image);
                    }
                    if (shouldUploadAuto(image, false)) {
                        saveMedia(upload(image, true, false).getLeft());
                    }
                }
            }
        }
        endUpdateMedia(count, emptyList(), emptyList(), start);
    }

    private int updateFromSirs() throws IOException {
        int count = 0;
        for (String category : loadCategories()) {
            LOGGER.info("{} medias update for '{}'...", getName(), category);
            int page = 0;
            boolean loop = true;
            while (loop) {
                List<String> imageLinks = loadImageLinks(category, page++);
                loop = !imageLinks.isEmpty();
                for (String imageLink : imageLinks) {
                    String id = imageLink.substring(imageLink.lastIndexOf('=') + 1);
                    URL url = new URL(imageLink);
                    if ("$id\\\"".equals(id)) {
                        problem(url, "invalid id");
                        return count;
                    }
                    NasaSirsImage media = null;
                    try {
                        boolean save = false;
                        Optional<NasaSirsImage> mediaInRepo = repository.findById(id);
                        if (mediaInRepo.isPresent()) {
                            media = mediaInRepo.get();
                        } else {
                            List<String> values = loadImageValues(imageLink);
                            media = new NasaSirsImage();
                            media.setId(id);
                            media.setTitle(values.get(0));
                            media.setCategory(values.get(1));
                            try {
                                media.setDate(LocalDate.parse(values.get(3), usDateformatter));
                                media.setYear(Year.of(media.getDate().getYear()));
                            } catch (DateTimeParseException e) {
                                media.setYear(Year.parse(values.get(3)));
                            }
                            media.setKeywords(NasaMediaProcessorService.normalizeKeywords(singleton(values.get(4))));
                            media.setThumbnailUrl(new URL(url.getProtocol(), url.getHost(), values.get(5)));
                            media.getMetadata().setAssetUrl(new URL(url.getProtocol(), url.getHost(), values.get(6)));
                            media.setDescription(values.get(7));
                            save = true;
                        }
                        if (media != null) {
                            if (doCommonUpdate(media)) {
                                save = true;
                            }
                            saveMediaOrCheckRemote(save, media);
                            count++;
                        }
                    } catch (HttpStatusException e) {
                        problem(url, e);
                    }
                }
            }
        }
        return count;
    }

    private static List<String> loadImageValues(String imageLink) throws IOException {
        Elements tds = Jsoup.connect(imageLink).timeout(15_000).get().getElementsByTag("td");
        List<String> result = new ArrayList<>();
        for (int i = 1; i < tds.size(); i += 2) {
            if (i == 11) {
                Elements links = tds.get(i).getElementsByTag("a");
                result.add(links.first().attr("href"));
                result.add(links.last().attr("href"));
            } else {
                result.add(tds.get(i).text());
            }
        }
        return result;
    }

    private List<String> loadImageLinks(String category, int page) throws IOException {
        String link = imagesUrl.replace("<cat>", category).replace("<idx>", Integer.toString(page));
        URL url = new URL(link);
        return Jsoup.connect(link).timeout(15_000).get().getElementsByTag("tr").stream()
                .map(e -> url.getProtocol() + "://" + url.getHost() + e.getElementsByTag("a").get(1).attr("href"))
                .toList();
    }

    private Set<String> loadCategories() throws IOException {
        return Jsoup.connect(categoriesUrl).timeout(15_000).get().getElementsByTag("tbody").get(0)
                .getElementsByTag("input").stream().map(e -> e.attr("value")).collect(Collectors.toSet());
    }

    @Override
    public Set<String> findCategories(NasaSirsImage media, Metadata metadata, boolean includeHidden) {
        Set<String> result = super.findCategories(media, metadata, includeHidden);
        if (includeHidden) {
            result.add("Stennis Space Center in " + media.getYear());
        }
        return result;
    }

    @Override
    protected String getSource(NasaSirsImage media) throws MalformedURLException {
        return super.getSource(media)
                + " ([" + media.getMetadata().getAssetUrl() + " direct link])\n"
                + "{{NASA-image|id=" + media.getId() + "|center=SSC}}";
    }

    @Override
    public Set<String> findLicenceTemplates(NasaSirsImage media) {
        Set<String> result = super.findLicenceTemplates(media);
        result.add("PD-USGov-NASA");
        return result;
    }

    @Override
    protected NasaSirsImage refresh(NasaSirsImage media) throws IOException {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    protected Set<String> getEmojis(NasaSirsImage uploadedMedia) {
        return AbstractSocialMediaService.getEmojis(uploadedMedia.getKeywords());
    }

    @Override
    protected Set<String> getTwitterAccounts(NasaSirsImage uploadedMedia) {
        return Set.of("@NASAStennis");
    }
}
