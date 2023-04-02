package org.wikimedia.commons.donvip.spacemedia.service.agencies;

import static java.util.stream.Collectors.toSet;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.data.domain.Metadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.stsci.StsciMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.stsci.StsciMediaRepository;

/**
 * Service harvesting images from NASA Hubble website.
 */
@Service
public class HubbleNasaService extends AbstractStsciService {

    private Map<String, String> hubbleCategories;

    @Autowired
    public HubbleNasaService(StsciMediaRepository repository,
            @Value("${hubble.nasa.search.link}") String searchEndpoint,
            @Value("${hubble.nasa.detail.link}") String detailEndpoint) {
        super(repository, "hubble", searchEndpoint, detailEndpoint);
    }

    @Override
    @PostConstruct
    void init() throws IOException {
        super.init();
        hubbleCategories = loadCsvMapping("hubblenasa.categories.csv");
    }

    @Override
    public String getName() {
        return "Hubble (NASA)";
    }

    @Override
    public void updateMedia() throws IOException {
        super.updateMedia();
    }

    public void checkHubbleCategories() {
        checkCommonsCategories(hubbleCategories);
    }

    @Override
    public Set<String> findCategories(StsciMedia media, Metadata metadata, boolean includeHidden) {
        Set<String> result = super.findCategories(media, metadata, includeHidden);
        if (media.getKeywords() != null) {
            result.addAll(media.getKeywords().stream().map(hubbleCategories::get).filter(StringUtils::isNotBlank)
                    .collect(toSet()));
        }
        return result;
    }

    @Override
    protected Set<String> getTwitterAccounts(StsciMedia uploadedMedia) {
        return Set.of("NASAHubble", "HubbleTelescope");
    }
}
