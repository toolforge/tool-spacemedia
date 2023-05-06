package org.wikimedia.commons.donvip.spacemedia.service.agencies;

import static java.util.Collections.emptyList;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.Temporal;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionException;
import org.wikimedia.commons.donvip.spacemedia.data.domain.Metadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.kari.KariMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.kari.KariMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.service.MediaService.MediaUpdateResult;
import org.wikimedia.commons.donvip.spacemedia.utils.Emojis;

@Service
public class KariService extends AbstractAgencyService<KariMedia, Integer, LocalDate, KariMedia, Integer, LocalDate> {

    private static final Logger LOGGER = LoggerFactory.getLogger(KariService.class);

    @Autowired
    public KariService(KariMediaRepository repository) {
        super(repository, "kari");
    }

    @Value("${kari.view.link}")
    private String viewLink;

    @Value("${kari.kogl.type1}")
    private String koglType1Icon;

    @Value("${kari.max.failures:50}")
    private int maxFailures;

    @Override
    protected final Integer getMediaId(String id) {
        return Integer.parseUnsignedInt(id);
    }

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
        return Optional.ofNullable(media.getCreationDate());
    }

    @Override
    protected Optional<Temporal> getUploadDate(KariMedia media) {
        return Optional.ofNullable(media.getDate());
    }

    @Override
    public final URL getSourceUrl(KariMedia media) throws MalformedURLException {
        return new URL(getViewUrl(media.getId()));
    }

    @Override
    public final String getSource(KariMedia media) throws MalformedURLException {
        return "{{KARI-source|" + media.getId() + "|" + media.getKariId() + "}}";
    }

    @Override
    protected final String getAuthor(KariMedia media) {
        return "{{Creator:KARI}}";
    }

    private String getViewUrl(int id) {
        return viewLink.replace("<id>", Integer.toString(id));
    }

    @Override
    protected boolean checkBlocklist() {
        return false;
    }

    @Override
    public Set<String> findLicenceTemplates(KariMedia media) {
        Set<String> result = super.findLicenceTemplates(media);
        result.add("KOGL");
        return result;
    }

    @Override
    public void updateMedia() throws IOException {
        LocalDateTime start = startUpdateMedia();
        int consecutiveFailures = 0;
        int count = 0;
        int id = 1;
        while (consecutiveFailures < maxFailures) {
            boolean save = false;
            String viewUrl = getViewUrl(id);
            URL view = new URL(viewUrl);
            Optional<KariMedia> mediaInRepo = repository.findById(id);
            KariMedia media = mediaInRepo.orElse(null);
            if (media == null) {
                KariUpdateResult result = fetchMedia(id);
                media = result.getMedia();
                save = result.getResult();
                if (result.isResetConsecutiveFailures()) {
                    consecutiveFailures = 0;
                } else if (result.isIncrementConsecutiveFailures()) {
                    consecutiveFailures++;
                }
            }
            if (media != null) {
                try {
                    processMedia(save, media, view, mediaInRepo.isPresent());
                    count++;
                } catch (TransactionException e) {
                    LOGGER.error("Transaction error when saving {}", media, e);
                }
            }
            id++;
        }
        endUpdateMedia(count, emptyList(), emptyList(), start);
    }

    static class KariUpdateResult extends MediaUpdateResult {

        private final KariMedia media;
        private final boolean resetConsecutiveFailures;
        private final boolean incrementConsecutiveFailures;

        public KariUpdateResult(KariMedia media, boolean result, boolean resetConsecutiveFailures,
                boolean incrementConsecutiveFailures, Exception exception) {
            super(result, exception);
            this.media = media;
            this.resetConsecutiveFailures = resetConsecutiveFailures;
            this.incrementConsecutiveFailures = incrementConsecutiveFailures;
        }

        public KariMedia getMedia() {
            return media;
        }

        public boolean isResetConsecutiveFailures() {
            return resetConsecutiveFailures;
        }

        public boolean isIncrementConsecutiveFailures() {
            return incrementConsecutiveFailures;
        }
    }

    private KariUpdateResult fetchMedia(int id) throws MalformedURLException {
        String viewUrl = getViewUrl(id);
        URL view = new URL(viewUrl);
        KariMedia media = null;
        boolean resetConsecutiveFailures = false;
        boolean incrementConsecutiveFailures = false;
        Exception ex = null;
        try {
            Element div = Jsoup.connect(viewUrl).timeout(60_000).get().getElementsByClass("board_view").get(0);
            String title = div.getElementsByTag("h4").get(0).text();
            if (!title.isEmpty()) {
                resetConsecutiveFailures = true;
                Element infos = div.getElementsByClass("photo_infor").get(0);
                Elements lis = infos.getElementsByTag("li");
                if (lis.get(lis.size() - 1).getElementsByTag("img").attr("src").endsWith(koglType1Icon)) {
                    media = buildMedia(id, view, div, title, infos, lis);
                } else {
                    LOGGER.info("Media {} exists but appears to be non-free (Not KOGL type 1)", id);
                }
            } else {
                incrementConsecutiveFailures = true;
            }
        } catch (DateTimeParseException e) {
            LOGGER.error("Cannot parse HTML", e);
            ex = e;
        } catch (IOException e) {
            problem(view, e);
            incrementConsecutiveFailures = true;
            ex = e;
        }
        return new KariUpdateResult(media, media != null, resetConsecutiveFailures, incrementConsecutiveFailures, ex);
    }

    protected KariMedia processMedia(boolean save, KariMedia media, URL view, boolean mediaInRepo) throws IOException {
        if (StringUtils.isBlank(media.getDescription())) {
            problem(view, "Empty description");
        }
        Metadata metadata = media.getMetadata();
        if (metadata.getAssetUrl() != null) {
            String mediaUrl = metadata.getAssetUrl().toExternalForm();
            if (StringUtils.isBlank(mediaUrl) || "https://www.kari.re.kr".equals(mediaUrl)) {
                metadata.setAssetUrl(null);
            }
        }
        if (metadata.getAssetUrl() == null) {
            problem(view, "No download link");
            save = false;
            if (mediaInRepo) {
                deleteMedia(media, "No download link");
            }
        }
        if (doCommonUpdate(media)) {
            save = true;
        }
        return saveMediaOrCheckRemote(save, media);
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
            media.getMetadata().setAssetUrl(new URL(view.getProtocol(), view.getHost(), href));
        }
        String src = div.getElementsByClass("board_txt").get(0).getElementsByTag("img").attr("src").replace("/view/",
                "/lst/");
        if (StringUtils.isNotBlank(src)) {
            media.setThumbnailUrl(new URL(view.getProtocol(), view.getHost(), src));
        }
        return media;
    }

    @Override
    protected boolean isPermittedFileType(Metadata metadata) {
        return metadata.getAssetUrl() != null && metadata.getAssetUrl().toExternalForm()
                .startsWith("https://www.kari.re.kr/image/kari_image_down.do?");
    }

    @Override
    protected KariMedia refresh(KariMedia media) throws IOException {
        KariMedia mediaFromApi = fetchMedia(media.getId()).getMedia();
        return mediaFromApi != null ? media.copyDataFrom(mediaFromApi) : null;
    }

    @Override
    protected Set<String> getEmojis(KariMedia uploadedMedia) {
        return Set.of(Emojis.FLAG_KOR);
    }

    @Override
    protected Set<String> getTwitterAccounts(KariMedia uploadedMedia) {
        return Set.of("@kari2030");
    }
}
