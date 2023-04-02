package org.wikimedia.commons.donvip.spacemedia.service.agencies;

import java.time.ZonedDateTime;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.data.domain.Metadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.dvids.DvidsMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.dvids.DvidsMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.dvids.DvidsMediaTypedId;

@Service
public class UsSpaceForceDvidsService extends AbstractAgencyDvidsService<DvidsMedia, DvidsMediaTypedId, ZonedDateTime> {

    @Autowired
    public UsSpaceForceDvidsService(DvidsMediaRepository<DvidsMedia> repository,
            @Value("${usspaceforce.dvids.units}") Set<String> dvidsUnits,
            @Value("${usspaceforce.dvids.min.year}") int minYear) {
        super(repository, "usspaceforce.dvids", dvidsUnits, minYear);
    }

    public final void checkDvidsCategories() {
        checkCommonsCategories(KEYWORDS_CATS);
    }

    @Override
    public void updateMedia() {
        updateDvidsMedia();
    }

    @Override
    public String getName() {
        return "U.S. Space Force/Command (DVIDS)";
    }

    @Override
    public Set<String> findCategories(DvidsMedia media, Metadata metadata, boolean includeHidden) {
        Set<String> result = super.findCategories(media, metadata, includeHidden);
        if (includeHidden) {
            switch (media.getUnit()) {
            case "AFSC":
                result.add("Photographs by the United States Air Force Space Command");
                break;
            case "SSC":
                result.add("Photographs by the Space Systems Command");
                break;
            case "USSPACECOM":
                result.add("Photographs by the United States Space Command");
                break;
            default:
                // Do nothing
            }
        }
        return result;
    }

    @Override
    protected Set<String> getTwitterAccounts(DvidsMedia uploadedMedia) {
        switch (uploadedMedia.getUnit()) {
        case "SBD1":
            return Set.of("SpaceBaseDelta1");
        case "SLD30":
            return Set.of("SLDelta30");
        case "45SW":
            return Set.of("SLDelta45");
        case "SSC":
            return Set.of("USSF_SSC");
        case "SpOC":
            return Set.of("ussfspoc");
        case "STARCOM":
            return Set.of("USSF_STARCOM");
        case "USSPACECOM":
            return Set.of("US_SpaceCom");
        default:
            return Set.of("SpaceForceDoD");
        }
    }
}
