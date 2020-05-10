package org.wikimedia.commons.donvip.spacemedia.service.agencies;

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
import java.util.Collections;
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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.NasaMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.NasaMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.sirs.NasaSirsImage;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.sirs.NasaSirsImageRepository;

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
    public URL getSourceUrl(NasaSirsImage media) throws MalformedURLException {
        return new URL(detailsUrl.replace("<id>", media.getId()));
    }

    @Override
    protected String getAuthor(NasaSirsImage media) {
        return wikiLink(homePage, sscName);
    }

    @Override
    protected Optional<Temporal> getCreationDate(NasaSirsImage media) {
        return Optional.of(media.getDate());
    }

    @Override
    @Scheduled(fixedRateString = "${nasa.sirs.update.rate}", initialDelayString = "${nasa.sirs.initial.delay}")
    public void updateMedia() throws IOException {
        LocalDateTime start = startUpdateMedia();
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
                    NasaSirsImage media = null;
                    try {
                        boolean save = false;
                        Optional<NasaSirsImage> mediaInRepo = repository.findById(id);
                        if (mediaInRepo.isPresent()) {
                            media = mediaInRepo.get();
                        }
                        if (media == null || media.getThumbnailUrl() == null) {
                            List<String> values = loadImageValues(imageLink);
                            boolean newMedia = null == media; // FIXME migration code, to remove later
                            if (newMedia) {
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
                                media.setKeywords(NasaService.normalizeKeywords(Collections.singleton(values.get(4))));
                            }
                            media.setThumbnailUrl(new URL(url.getProtocol(), url.getHost(), values.get(5)));
                            if (newMedia) {
                                media.getMetadata().setAssetUrl(new URL(url.getProtocol(), url.getHost(), values.get(6)));
                                media.setDescription(values.get(7));
                            }
                            save = true;
                        }
                        if (doCommonUpdate(media)) {
                            save = true;
                        }
                        if (save) {
                            repository.save(media);
                        }
                        count++;
                    } catch (HttpStatusException e) {
                        problem(url, e);
                    }
                }
            }
        }
        endUpdateMedia(count, start);
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
                .collect(Collectors.toList());
    }

    private Set<String> loadCategories() throws IOException {
        return Jsoup.connect(categoriesUrl).timeout(15_000).get().getElementsByTag("tbody").get(0)
                .getElementsByTag("input").stream().map(e -> e.attr("value")).collect(Collectors.toSet());
    }
}
