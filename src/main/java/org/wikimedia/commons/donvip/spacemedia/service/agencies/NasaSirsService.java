package org.wikimedia.commons.donvip.spacemedia.service.agencies;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.data.local.ProblemRepository;
import org.wikimedia.commons.donvip.spacemedia.data.local.nasa.sirs.NasaSirsImage;
import org.wikimedia.commons.donvip.spacemedia.data.local.nasa.sirs.NasaSirsImageRepository;
import org.wikimedia.commons.donvip.spacemedia.service.MediaService;

@Service
public class NasaSirsService extends SpaceAgencyService<NasaSirsImage, String> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NasaSirsService.class);

    private static final DateTimeFormatter usDateformatter = DateTimeFormatter.ofPattern("MM/dd/yyyy", Locale.ENGLISH);

    @Value("${nasa.sirs.categories.url}")
    private String categoriesUrl;

    @Value("${nasa.sirs.images.url}")
    private String imagesUrl;

    public NasaSirsService(NasaSirsImageRepository repository, ProblemRepository problemrepository,
            MediaService mediaService) {
        super(repository, problemrepository, mediaService);
    }

    @Override
    public String getName() {
        return "NASA (SIRS)";
    }

    @Override
    @Scheduled(fixedRateString = "${nasa.sirs.update.rate}", initialDelayString = "${initial.delay}")
    public List<NasaSirsImage> updateMedia() throws IOException {
        LocalDateTime start = LocalDateTime.now();
        LOGGER.info("Starting {} medias update...", getName());
        List<NasaSirsImage> medias = new ArrayList<>();
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
                        } else {
                            media = new NasaSirsImage();
                            media.setNasaId(id);
                            List<String> values = loadImageValues(imageLink);
                            media.setTitle(values.get(0));
                            media.setCategory(values.get(1));
                            try {
                                media.setPhotoDate(LocalDate.parse(values.get(3), usDateformatter));
                                media.setPhotoYear(Year.of(media.getPhotoDate().getYear()));
                            } catch (DateTimeParseException e) {
                                media.setPhotoYear(Year.parse(values.get(3)));
                            }
                            media.setKeywords(NasaService.normalizeKeywords(Collections.singleton(values.get(4))));
                            media.setAssetUrl(new URL(url.getProtocol(), url.getHost(), values.get(5)));
                            media.setDescription(values.get(6));
                            save = true;
                        }
                        if (mediaService.computeSha1(media)) {
                            save = true;
                        }
                        if (mediaService.findCommonsFilesWithSha1(media)) {
                            save = true;
                        }
                        medias.add(save ? repository.save(media) : media);
                    } catch (URISyntaxException e) {
                        LOGGER.error("Cannot compute SHA-1 of " + media, e);
                    } catch (HttpStatusException e) {
                        problem(url, e);
                    }
                }
            }
        }
        LOGGER.info("{} medias update completed: {} medias in {}", getName(), medias.size(),
                Duration.between(LocalDateTime.now(), start));
        return medias;
    }

    private static List<String> loadImageValues(String imageLink) throws IOException {
        Elements tds = Jsoup.connect(imageLink).timeout(15_000).get().getElementsByTag("td");
        List<String> result = new ArrayList<>();
        for (int i = 1; i < tds.size(); i += 2) {
            if (i == 11) {
                result.add(tds.get(i).getElementsByTag("a").last().attr("href"));
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
