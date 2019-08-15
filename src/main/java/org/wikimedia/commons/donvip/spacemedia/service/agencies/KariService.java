package org.wikimedia.commons.donvip.spacemedia.service.agencies;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionException;
import org.wikimedia.commons.donvip.spacemedia.data.local.ProblemRepository;
import org.wikimedia.commons.donvip.spacemedia.data.local.kari.KariMedia;
import org.wikimedia.commons.donvip.spacemedia.data.local.kari.KariMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.service.MediaService;

@Service
public class KariService extends SpaceAgencyService<KariMedia, Integer> {

    private static final Logger LOGGER = LoggerFactory.getLogger(KariService.class);

    @Autowired
    public KariService(KariMediaRepository repository, ProblemRepository problemrepository, MediaService mediaService) {
        super(repository, problemrepository, mediaService);
    }

    @Value("${kari.view.link}")
    private String viewLink;

    @Value("${kari.kogl.type1}")
    private String koglType1Icon;

    @Value("${kari.max.failures:10}")
    private int maxFailures;

    @Override
    public String getName() {
        return "KARI";
    }

    @Override
    @Scheduled(fixedRateString = "${kari.update.rate}", initialDelayString = "${initial.delay}")
    public List<KariMedia> updateMedia() throws IOException {
        LocalDateTime start = LocalDateTime.now();
        LOGGER.info("Starting {} medias update...", getName());
        List<KariMedia> medias = new ArrayList<>();
        int consecutiveFailures = 0;
        int id = 1;
        while (consecutiveFailures < maxFailures) {
            boolean save = false;
            KariMedia media = null;
            String viewUrl = viewLink.replace("<id>", Integer.toString(id));
            URL view = new URL(viewUrl);
            Optional<KariMedia> mediaInRepo = repository.findById(id);
            if (mediaInRepo.isPresent()) {
                media = mediaInRepo.get();
            } else {
                try {
                    Document html = Jsoup.connect(viewUrl).timeout(60_000).get();
                    Element div = html.getElementsByClass("board_view").get(0);
                    String title = div.getElementsByTag("h4").get(0).text();
                    if (!title.isEmpty()) {
                        consecutiveFailures = 0;
                        Element infos = div.getElementsByClass("photo_infor").get(0);
                        Elements lis = infos.getElementsByTag("li");
                        if (lis.get(lis.size() - 1).getElementsByTag("img").attr("src").endsWith(koglType1Icon)) {
                            media = buildMedia(id, view, div, title, infos, lis);
                            save = true;
                        }
                    } else {
                        consecutiveFailures++;
                    }
                } catch (DateTimeParseException e) {
                    LOGGER.error("Cannot parse HTML", e);
                } catch (IOException e) {
                    problem(view, e);
                }
            }
            if (media != null) {
                try {
                    if (StringUtils.isBlank(media.getDescription())) {
                        problem(view, "Empty description");
                    }
                    if (media.getUrl() != null) {
                        String mediaUrl = media.getUrl().toExternalForm();
                        if (StringUtils.isBlank(mediaUrl) || "https://www.kari.re.kr".equals(mediaUrl)) {
                            media.setUrl(null);
                        }
                    }
                    if (media.getUrl() == null) {
                        problem(view, "No download link");
                        save = false;
                        if (mediaInRepo.isPresent()) {
                            repository.delete(media);
                        }
                    }
                    if (mediaService.computeSha1(media, media.getUrl())) {
                        save = true;
                    }
                    if (mediaService.findCommonsFilesWithSha1(media)) {
                        save = true;
                    }
                    medias.add(save ? repository.save(media) : media);
                } catch (URISyntaxException e) {
                    LOGGER.error("Cannot compute SHA-1 of " + media, e);
                } catch (TransactionException e) {
                    LOGGER.error("Transaction error when saving " + media, e);
                }
            }
            id++;
        }
        LOGGER.info("{} medias update completed: {} medias in {}", getName(), medias.size(),
                Duration.between(LocalDateTime.now(), start));
        return medias;
    }

    private static KariMedia buildMedia(int id, URL view, Element div, String title, Element infos, Elements lis)
            throws MalformedURLException {
        KariMedia media = new KariMedia();
        media.setId(id);
        media.setKariId(lis.get(0).getElementsByClass("txt").get(0).text());
        media.setTitle(title);
        media.setDate(LocalDate.parse(div.getElementsByClass("infor").get(0).getElementsByTag("li")
                .get(0).getElementsByClass("txt").get(0).text()));
        media.setDescription(div.getElementsByClass("photo_txt").get(0).text());
        String href = infos.getElementsByTag("a").attr("href");
        if (StringUtils.isNotBlank(href)) {
            media.setUrl(new URL(view.getProtocol(), view.getHost(), href));
        }
        return media;
    }
}
