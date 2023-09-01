package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.dvids.DvidsMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.dvids.DvidsMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.utils.Emojis;

@Service
public class UsSpaceForceDvidsService extends AbstractOrgDvidsService {

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
    public String getName() {
        return "U.S. Space Force/Command (DVIDS)";
    }

    @Override
    public Set<String> findCategories(DvidsMedia media, FileMetadata metadata, boolean includeHidden) {
        Set<String> result = super.findCategories(media, metadata, includeHidden);
        if (includeHidden) {
            switch (media.getId().getRepoId()) {
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
    protected Set<String> getEmojis(DvidsMedia uploadedMedia) {
        Set<String> result = super.getEmojis(uploadedMedia);
        switch (uploadedMedia.getId().getRepoId()) {
        case "SLD30", "45SW":
            result.add(Emojis.ROCKET);
            break;
        case "SSC", "SpOC", "STARCOM", "USSPACECOM", "SBD1":
        default:
            result.add(Emojis.FLAG_USA);
        }
        return result;
    }

    @Override
    protected Set<String> getTwitterAccounts(DvidsMedia uploadedMedia) {
        switch (uploadedMedia.getId().getRepoId()) {
        case "SBD1":
            return Set.of("@PeteSchriever");
        case "SLD30":
            return Set.of("@SLDelta30");
        case "45SW":
            return Set.of("@SLDelta45");
        case "SSC":
            return Set.of("@USSF_SSC");
        case "SpOC":
            return Set.of("@ussfspoc");
        case "STARCOM":
            return Set.of("@USSF_STARCOM");
        case "USSPACECOM":
            return Set.of("@US_SpaceCom");
        default:
            return Set.of("@SpaceForceDoD");
        }
    }
}
