package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.box.BoxMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.box.BoxMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.utils.Emojis;

@Service
public class NasaBoxService extends AbstractOrgBoxService {

    private static final String ARTEMIS_SHARE = "onrtmdvofqluv5ei5kfu5u1pf8v4xqtl";

    @Autowired
    public NasaBoxService(BoxMediaRepository repository, @Value("${nasa.box.app-shares}") Set<String> appShares) {
        super(repository, "nasa.box", appShares);
    }

    @Override
    public String getName() {
        return "NASA (Box)";
    }

    @Override
    public Set<String> findLicenceTemplates(BoxMedia media, FileMetadata metadata) {
        Set<String> result = super.findLicenceTemplates(media, metadata);
        result.add("PD-USGov-NASA");
        return result;
    }

    @Override
    public Set<String> findCategories(BoxMedia media, FileMetadata metadata, boolean includeHidden) {
        Set<String> result = super.findCategories(media, metadata, includeHidden);
        if (isArtemis(media)) {
            result.add("Artemis program");
        }
        return result;
    }

    @Override
    protected Set<String> getEmojis(BoxMedia uploadedMedia) {
        Set<String> result = super.getEmojis(uploadedMedia);
        if (isArtemis(uploadedMedia)) {
            result.add(Emojis.ASTRONAUT);
            result.add(Emojis.MOON);
        }
        return result;
    }

    @Override
    protected Set<String> getTwitterAccounts(BoxMedia uploadedMedia) {
        Set<String> result = super.getTwitterAccounts(uploadedMedia);
        if (isArtemis(uploadedMedia)) {
            result.add("@NASAArtemis");
        }
        return result;
    }

    private static final boolean isArtemis(BoxMedia media) {
        return ARTEMIS_SHARE.equals(getShare(media.getId()));
    }
}
