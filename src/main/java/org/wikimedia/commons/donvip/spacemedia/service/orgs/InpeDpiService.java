package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.wikimedia.commons.donvip.spacemedia.service.wikimedia.WikidataItem.Q725252_SATELLITE_IMAGERY;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.getWithJsoup;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.newURL;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Triple;
import org.jsoup.HttpStatusException;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.CompositeMediaId;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.inpe.dpi.InpeDpiMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.inpe.dpi.InpeDpiMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.exception.UploadException;
import org.wikimedia.commons.donvip.spacemedia.service.wikimedia.GlitchTip;
import org.wikimedia.commons.donvip.spacemedia.service.wikimedia.SdcStatements;
import org.wikimedia.commons.donvip.spacemedia.utils.Emojis;
import org.wikimedia.commons.donvip.spacemedia.utils.Utils;

@Service
public class InpeDpiService extends AbstractOrgService<InpeDpiMedia> {

    private static final String BASE_URL = "https://www.dpi.inpe.br";
    private static final String GALERIA_URL = BASE_URL + "/galeria/";

    private static final Pattern ID_WITH_BASICDATE = Pattern.compile(".+_(20\\d{6}).+");
    private static final Pattern ID_WITH_CUSTOMDATE = Pattern.compile(".+\\.(20\\d{2}_[01]\\d_[0-3]\\d).+");

    private static final DateTimeFormatter CUSTOM_DATE = DateTimeFormatter.ofPattern("yyyy_MM_dd");

    private static final Set<String> FILE_EXT_SET = Set.of("jpg", "png", "gif", "tif", "pdf", "txt", "zip");

    private static final Logger LOGGER = LoggerFactory.getLogger(InpeDpiService.class);

    public InpeDpiService(InpeDpiMediaRepository repository) {
        super(repository, "inpe.dpi", Set.of("dpi"));
    }

    @Override
    public Set<String> findCategories(InpeDpiMedia media, FileMetadata metadata, boolean includeHidden) {
        Set<String> result = super.findCategories(media, metadata, includeHidden);
        if (includeHidden) {
            result.add("Images by INPE");
        }
        InpeFlickrService.doForSatellites(() -> Stream.of(metadata.getOriginalFileName()),
                p -> result.add(p.getValue()));
        return result;
    }

    @Override
    public Set<String> findLicenceTemplates(InpeDpiMedia media, FileMetadata metadata) {
        Set<String> result = super.findLicenceTemplates(media, metadata);
        // https://www.dpi.inpe.br/galeria/
        // Available images can be copied and redistributed since the source is mentioned (INPE) - Creative Commons
        result.add("Cc-by-sa-4.0");
        return result;
    }

    @Override
    protected SdcStatements getStatements(InpeDpiMedia media, FileMetadata metadata) {
        SdcStatements result = super.getStatements(media, metadata);
        InpeFlickrService.doForSatellites(() -> Stream.of(metadata.getOriginalFileName()),
                p -> result.creator(p.getKey()));
        return result.locationOfCreation("Q663611") // Created in low earth orbit
                .fabricationMethod(Q725252_SATELLITE_IMAGERY);
    }

    @Override
    protected boolean isSatellitePicture(InpeDpiMedia media, FileMetadata metadata) {
        return true;
    }

    @Override
    protected boolean checkBlocklist() {
        return false;
    }

    @Override
    protected String hiddenUploadCategory() {
        return "Spacemedia INPE files uploaded by " + commonsService.getAccount();
    }

    @Override
    protected String getAuthor(InpeDpiMedia media, FileMetadata metadata) {
        return "INPE/OBT/DPI: Divisão de Processamento de Imagens, Coordenação Geral de Observação da Terra, Instituto Nacional de Pesquisas Espaciais";
    }

    @Override
    public String getName() {
        return "INPE (DPI)";
    }

    @Override
    protected String getLanguage(InpeDpiMedia media) {
        return "pt-br";
    }

    @Override
    protected Set<String> getEmojis(InpeDpiMedia uploadedMedia) {
        return Set.of(Emojis.FLAG_BRA);
    }

    @Override
    protected Set<String> getTwitterAccounts(InpeDpiMedia uploadedMedia) {
        return Set.of("@inpe_mcti");
    }

    @Override
    public URL getSourceUrl(InpeDpiMedia media, FileMetadata metadata, String ext) {
        return newURL(GALERIA_URL);
    }

    @Override
    protected InpeDpiMedia refresh(InpeDpiMedia media) throws IOException {
        return media;
    }

    @Override
    protected Class<InpeDpiMedia> getMediaClass() {
        return InpeDpiMedia.class;
    }

    @Override
    public void updateMedia(String[] args) throws IOException, UploadException {
        LocalDateTime start = startUpdateMedia();
        List<FileMetadata> uploadedMetadata = new ArrayList<>();
        List<InpeDpiMedia> uploadedMedia = new ArrayList<>();
        int count = processWebPage(GALERIA_URL, uploadedMetadata, uploadedMedia, start, new TreeSet<>(), 0, 0);
        endUpdateMedia(count, uploadedMedia, uploadedMetadata, start);
    }

    private int processWebPage(String pageUrl, List<FileMetadata> uploadedMetadata, List<InpeDpiMedia> uploadedMedia,
            LocalDateTime start, Set<String> known, int count, int level) throws IOException, UploadException {
        known.add(pageUrl);
        int localCount = 0;
        LOGGER.info("{}Scrapping {} ...", ">".repeat(level), pageUrl);
        for (Element link : getWithJsoup(pageUrl, 30_000, 3).getElementsByTag("a")) {
            String href = link.attr("href");
            if (isNotBlank(href)) {
                href = href.replace("http:", "https:");
                if (!href.startsWith("http")) {
                    String part = href;
                    href = part.startsWith("/") ? BASE_URL + part
                            : FILE_EXT_SET.stream().anyMatch(x -> part.endsWith("." + x)) ? GALERIA_URL + part
                                    : pageUrl + (pageUrl.endsWith("/") ? "" : "/") + part;
                }
                if (href.startsWith(GALERIA_URL) && !known.contains(href) && !href.contains("?C=")
                        && !href.equals(pageUrl + "/")) {
                    int idx = href.replace(BASE_URL, "").lastIndexOf('.');
                    if (idx == -1) {
                        localCount += processWebPage(href, uploadedMetadata, uploadedMedia, start, known,
                                count + localCount, level + 1);
                    } else {
                        switch (href.replace(BASE_URL, "").substring(idx + 1).toLowerCase(Locale.ENGLISH)) {
                        case "php", "html":
                            try {
                                localCount += processWebPage(href, uploadedMetadata, uploadedMedia, start, known,
                                        count + localCount, level + 1);
                            } catch (HttpStatusException e) {
                                LOGGER.warn(e.getMessage());
                            }
                            break;
                        case "jpg", "png", "gif", "tif", "pdf":
                            try {
                                localCount += processFile(newURL(href), uploadedMetadata, uploadedMedia);
                                ongoingUpdateMedia(start, count + localCount);
                            } catch (RuntimeException | URISyntaxException e) {
                                LOGGER.error("Error: {} => {}", href, e.getMessage());
                                GlitchTip.capture(e);
                            }
                            break;
                        case "txt", "zip":
                            break;
                        default:
                            throw new UnsupportedOperationException(href);
                        }
                    }
                }
            }
        }
        return localCount;
    }

    private int processFile(URL fileUrl, List<FileMetadata> uploadedMetadata, List<InpeDpiMedia> uploadedMedia)
            throws UploadException, IOException, URISyntaxException {
        InpeDpiMedia media;
        boolean save = false;
        String filename = Utils.getFilename(fileUrl);
        CompositeMediaId id = new CompositeMediaId("dpi", filename);
        Optional<InpeDpiMedia> mediaInRepo = repository.findById(id);
        if (mediaInRepo.isPresent()) {
            media = mediaInRepo.get();
        } else {
            media = new InpeDpiMedia();
            media.setId(id);
            media.setTitle(id.getMediaId());
            Matcher m = ID_WITH_BASICDATE.matcher(id.getMediaId());
            if (m.matches()) {
                media.setCreationDate(LocalDate.parse(m.group(1), DateTimeFormatter.BASIC_ISO_DATE));
            } else {
                m = ID_WITH_CUSTOMDATE.matcher(id.getMediaId());
                if (m.matches()) {
                    media.setCreationDate(LocalDate.parse(m.group(1), CUSTOM_DATE));
                }
            }
            media.setPublicationDate(media.getCreationDate());
            save = true;
        }
        URI fileUri = fileUrl.toURI();
        if (media.getMetadataStream().noneMatch(x -> x.getAssetUri().equals(fileUri))) {
            addMetadata(media, fileUrl, null);
        }
        if (doCommonUpdate(media)) {
            save = true;
        }
        if (shouldUploadAuto(media, false)) {
            Triple<InpeDpiMedia, Collection<FileMetadata>, Integer> upload = upload(save ? saveMedia(media) : media,
                    true, false);
            uploadedMedia.add(saveMedia(upload.getLeft()));
            uploadedMetadata.addAll(upload.getMiddle());
            save = false;
        }
        saveMediaOrCheckRemote(save, media);
        return 1;
    }
}
