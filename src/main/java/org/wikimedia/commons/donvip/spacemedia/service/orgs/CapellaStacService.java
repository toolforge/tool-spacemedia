package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import static java.lang.Integer.parseInt;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.wikimedia.commons.donvip.spacemedia.service.wikimedia.WikidataItem.Q725252_SATELLITE_IMAGERY;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.newURL;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.replace;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.Media;
import org.wikimedia.commons.donvip.spacemedia.data.domain.stac.StacMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.stac.StacMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.exception.UploadException;
import org.wikimedia.commons.donvip.spacemedia.service.wikimedia.GlitchTip;
import org.wikimedia.commons.donvip.spacemedia.service.wikimedia.SdcStatements;
import org.wikimedia.commons.donvip.spacemedia.utils.Emojis;

@Service
public class CapellaStacService extends AbstractOrgStacService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CapellaStacService.class);

    private static final Map<String, String> SATS = Map.ofEntries(e("01", "Q124124084"), e("02", "Q124124108"),
            e("03", "Q124124120"), e("04", "Q124124133"), e("05", "Q124124202"), e("06", "Q124124533"),
            e("07", "Q124126674"), e("08", "Q124126687"), e("09", "Q124126690"), e("10", "Q124126999"),
            e("11", "Q124127134"), e("14", "Q125885260"));

    private static final Pattern SAT_PATTERN = Pattern.compile("CAPELLA_C(\\d{2})_.*");

    private static final Set<String> GEC_SLC = Set.of("GEC", "SLC");

    @Autowired
    public CapellaStacService(StacMediaRepository repository,
            @Value("${capella.stac.catalogs}") Set<String> catalogs) {
        super(repository, "capella", catalogs);
    }

    @Override
    public String getName() {
        return "Capella";
    }

    @Override
    protected String getSource(StacMedia media, FileMetadata metadata) {
        URL itemUrl = getSourceUrl(media, metadata, metadata.getExtension());
        return "{{en|1=" + wikiLink(metadata.getAssetUrl(), "Capella Space")
                + " via their AWS S3 Bucket. Further data via " + wikiLink(itemUrl, "STAC API") + ". View in "
                + wikiLink(newURL(itemUrl.toExternalForm().replace("://",
                        "://radiantearth.github.io/stac-browser/#/external/")), "STAC Browser")
                + ".}}";
    }

    @Override
    protected Optional<String> getPermission(StacMedia media) {
        return Optional.of("{{en|See "
                + wikiLink(newURL("https://www.capellaspace.com/products/product-documentation/data-licensing/"),
                        "Capella-Open-Data Image Collection data license")
                + ". Reverse geocoding in description and category: "
                + wikiLink(newURL("https://www.openstreetmap.org/copyright"),
                        "Data © OpenStreetMap contributors, ODbL 1.0")
                + ". }}");
    }

    @Override
    protected String hiddenUploadCategory() {
        return "Files from Capella Space uploaded by " + commonsService.getAccount();
    }

    private static boolean isGecOrSlc(StacMedia media) {
        return GEC_SLC.contains(media.getProductType());
    }

    @Override
    protected boolean shouldUploadAuto(StacMedia media, boolean isManual) {
        return super.shouldUploadAuto(media, isManual) && !isGecOrSlc(media);
    }

    @Override
    protected int processStacCatalog(URL catalogUrl, LocalDateTime start, LocalDate doNotFetchEarlierThan,
            String repoId, List<StacMedia> uploadedMedia, Set<String> processedItems, int startCount)
            throws IOException {
        int result = super.processStacCatalog(catalogUrl, start, doNotFetchEarlierThan, repoId, uploadedMedia,
                processedItems, startCount);
        try {
            if (catalogUrl.toURI().equals(catalogUrls.get(repoId).toURI())) {
                LOGGER.info("Post-processing of GEC/SLC files of repo {}", repoId);
                for (StacMedia media : listMissingMedia()) {
                    if (isGecOrSlc(media)) {
                        postProcessGecSlcItem(uploadedMedia, media);
                    }
                }
            }
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
        return result;
    }

    private void postProcessGecSlcItem(List<StacMedia> uploadedMedia, StacMedia media) {
        LOGGER.debug("Post-processing of {}", media);
        if (stacRepository.findByProductTypeAndCollectId("GEO", media.getCollectId()).map(Media::isIgnored)
                .orElse(true)) {
            LOGGER.debug("GEO ignored or missing, check to upload {}", media);
            if (super.shouldUploadAuto(media, false)) {
                try {
                    media = upload(media, true, false).getLeft();
                    uploadedMedia.add(media);
                } catch (UploadException e) {
                    LOGGER.error("Failed to upload {}: {}", media, e);
                    GlitchTip.capture(e);
                }
                saveMedia(media);
            }
        } else {
            LOGGER.debug("Ignore GEC/SLC over GEO version for {}", media);
            media.getMetadataStream()
                    .filter(x -> x.isIgnored() != Boolean.TRUE && ("tiff".equals(x.getExtension())
                            || ("png".equals(x.getExtension()) && !x.getAssetUrl().toExternalForm().contains("_GEO_"))))
                    .forEach(x -> mediaService.ignoreAndSaveMetadata(x, "Ignored GEC/SLC over GEO version"));
        }
    }

    @Override
    protected void enrichStacMedia(StacMedia media, StacItem item) {
        try {
            media.setCollectId(item.properties().capellaCollectId());
            media.setTitle(nominatim.reverse(media.getLatitude(), media.getLongitude(), 10).display_name());
            if (isBlank(media.getTitle())) {
                LOGGER.warn("Nominatim empty response for /reverse lat={}, lon={}", media.getLatitude(),
                        media.getLongitude());
                media.setTitle(media.getIdUsedInOrg());
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    protected boolean isStacItemBefore(String itemHref, LocalDate doNotFetchEarlierThan) {
        String s = itemHref.substring(itemHref.lastIndexOf('_') + 1);
        return LocalDate.of(
                parseInt(s.substring(0, 4)),
                parseInt(s.substring(4, 6)),
                parseInt(s.substring(6, 8))).isBefore(doNotFetchEarlierThan);
    }

    @Override
    protected boolean isStacItemIgnored(String itemHref) {
        return itemHref.contains("_SICD_") || itemHref.contains("_SIDD_") || itemHref.contains("_CPHD_");
    }

    @Override
    protected boolean categorizeGeolocalizedByName() {
        return true;
    }

    @Override
    public Set<String> findLicenceTemplates(StacMedia media, FileMetadata metadata) {
        Set<String> result = super.findLicenceTemplates(media, metadata);
        result.add("Cc-by-4.0");
        return result;
    }

    @Override
    public Set<String> findCategories(StacMedia media, FileMetadata metadata, boolean includeHidden) {
        Set<String> result = super.findCategories(media, metadata, includeHidden);
        result.addAll(findCategoriesFromTitleAndAffixes(media.getTitle(),
                new Affixes(List.of("Images of "), false),
                new Affixes(List.of(" by Capella"), false)));
        if (replace(result, media.getYear() + " satellite pictures", media.getYear() + " Capella images")) {
            result.remove("Images by Capella");
        }
        return result;
    }

    @Override
    protected String getAuthor(StacMedia media, FileMetadata metadata) {
        // https://registry.opendata.aws/capella_opendata/
        return "Capella Space";
    }

    @Override
    protected SdcStatements getStatements(StacMedia media, FileMetadata metadata) {
        return super.getStatements(media, metadata)
                .creator(media.getIdUsedInOrg(), SAT_PATTERN, SATS) // Created by Capella-XX
                .locationOfCreation("Q663611") // Created in low earth orbit
                .fabricationMethod(Q725252_SATELLITE_IMAGERY)
                .capturedWith("Q740686"); // Taken with SAR
    }

    @Override
    protected Set<String> getEmojis(StacMedia uploadedMedia) {
        Set<String> result = super.getEmojis(uploadedMedia);
        result.add(Emojis.EARTH_AMERICA);
        return result;
    }

    @Override
    protected Set<String> getTwitterAccounts(StacMedia uploadedMedia) {
        return Set.of("@capellaspace");
    }
}
