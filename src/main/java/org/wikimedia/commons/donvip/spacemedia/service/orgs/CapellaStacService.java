package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import static java.lang.Integer.parseInt;
import static org.apache.commons.lang3.StringUtils.isBlank;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.stac.StacMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.stac.StacMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.service.osm.NominatimService;
import org.wikimedia.commons.donvip.spacemedia.utils.Emojis;

@Service
public class CapellaStacService extends AbstractOrgStacService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CapellaStacService.class);

    @Autowired
    private NominatimService nominatim;

    @Autowired
    public CapellaStacService(StacMediaRepository repository,
            @Value("${capella.stac.catalogs}") Set<String> catalogs) {
        super(repository, "capella", catalogs);
    }

    @Override
    public String getName() {
        return "Capella Space";
    }

    @Override
    protected void enrichStacMedia(StacMedia media) {
        try {
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
    public Set<String> findCategories(StacMedia media, FileMetadata metadata, boolean includeHidden) {
        Set<String> result = super.findCategories(media, metadata, includeHidden);
        result.add("Images by Capella");
        return result;
    }

    @Override
    public Set<String> findLicenceTemplates(StacMedia media, FileMetadata metadata) {
        Set<String> result = super.findLicenceTemplates(media, metadata);
        result.add("Cc-by-4.0");
        return result;
    }

    @Override
    protected String getAuthor(StacMedia media) {
        // https://registry.opendata.aws/capella_opendata/
        return "Capella Space";
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
