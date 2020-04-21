package org.wikimedia.commons.donvip.spacemedia.service.agencies;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.Temporal;
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
import org.wikimedia.commons.donvip.spacemedia.data.domain.kari.KariMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.kari.KariMediaRepository;

@Service
public class KariService extends AbstractSpaceAgencyService<KariMedia, Integer, LocalDate> {

    private static final Logger LOGGER = LoggerFactory.getLogger(KariService.class);

    @Autowired
    public KariService(KariMediaRepository repository) {
        super(repository);
    }

    @Value("${kari.view.link}")
    private String viewLink;

    @Value("${kari.kogl.type1}")
    private String koglType1Icon;

    @Value("${kari.max.failures:50}")
    private int maxFailures;

    @Override
    public final String getName() {
        return "KARI";
    }

    @Override
    protected String getLanguage(KariMedia media) {
        return "ko";
    }

    @Override
    protected Class<KariMedia> getMediaClass() {
        return KariMedia.class;
    }

    @Override
    protected Optional<Temporal> getCreationDate(KariMedia media) {
        return Optional.ofNullable(media.getDate());
    }

    @Override
    public final URL getSourceUrl(KariMedia media) throws MalformedURLException {
        return new URL(getViewUrl(media.getId()));
    }

    @Override
    protected final String getAuthor(KariMedia media) {
        return "Korea Aerospace Research Institute";
    }

    private String getViewUrl(int id) {
        return viewLink.replace("<id>", Integer.toString(id));
    }

    @Override
    public List<String> findTemplates(KariMedia media) {
        List<String> result = super.findTemplates(media);
        result.add("KOGL");
        return result;
    }

    @Override
    @Scheduled(fixedRateString = "${kari.update.rate}", initialDelayString = "${initial.delay}")
    public void updateMedia() throws IOException {
        LocalDateTime start = startUpdateMedia();
        int consecutiveFailures = 0;
        int count = 0;
        int id = 1;
        while (consecutiveFailures < maxFailures) {
            boolean save = false;
            KariMedia media = null;
            String viewUrl = getViewUrl(id);
            URL view = new URL(viewUrl);
            Optional<KariMedia> mediaInRepo = repository.findById(id);
            if (mediaInRepo.isPresent()) {
                media = mediaInRepo.get();
            }
            if (media == null || media.getThumbnailUrl() == null) {
                try {
                    Document html = Jsoup.connect(viewUrl).timeout(60_000).get();
                    Element div = html.getElementsByClass("board_view").get(0);
                    String title = div.getElementsByTag("h4").get(0).text();
                    if (!title.isEmpty()) {
                        consecutiveFailures = 0;
                        Element infos = div.getElementsByClass("photo_infor").get(0);
                        Elements lis = infos.getElementsByTag("li");
                        if (media == null && lis.get(lis.size() - 1).getElementsByTag("img").attr("src").endsWith(koglType1Icon)) {
                            media = buildMedia(id, view, div, title, infos, lis);
                            save = true;
                        }
                        if (media != null && media.getThumbnailUrl() == null) {
                            String src = div.getElementsByClass("board_txt").get(0).getElementsByTag("img").attr("src")
                                    .replace("/view/", "/lst/");
                            if (StringUtils.isNotBlank(src)) {
                                media.setThumbnailUrl(new URL(view.getProtocol(), view.getHost(), src));
                                save = true;
                            }
                        }
                    } else {
                        consecutiveFailures++;
                    }
                } catch (DateTimeParseException e) {
                    LOGGER.error("Cannot parse HTML", e);
                } catch (IOException e) {
                    problem(view, e);
                    consecutiveFailures++;
                }
            }
            if (media != null) {
                try {
                    processMedia(save, media, view, mediaInRepo.isPresent());
                    count++;
                } catch (URISyntaxException e) {
                    LOGGER.error("Cannot compute SHA-1 of " + media, e);
                } catch (TransactionException e) {
                    LOGGER.error("Transaction error when saving " + media, e);
                }
            }
            id++;
        }
        endUpdateMedia(count, start);
    }

    protected KariMedia processMedia(boolean save, KariMedia media, URL view, boolean mediaInRepo)
            throws IOException, URISyntaxException {
        if (StringUtils.isBlank(media.getDescription())) {
            problem(view, "Empty description");
        }
        if (media.getAssetUrl() != null) {
            String mediaUrl = media.getAssetUrl().toExternalForm();
            if (StringUtils.isBlank(mediaUrl) || "https://www.kari.re.kr".equals(mediaUrl)) {
                media.setAssetUrl(null);
            }
        }
        if (media.getAssetUrl() == null) {
            problem(view, "No download link");
            save = false;
            if (mediaInRepo) {
                repository.delete(media);
            }
        }
        if (mediaService.updateMedia(media)) {
            save = true;
        }
        return save ? repository.save(media) : media;
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
            media.setAssetUrl(new URL(view.getProtocol(), view.getHost(), href));
        }
        return media;
    }
}
