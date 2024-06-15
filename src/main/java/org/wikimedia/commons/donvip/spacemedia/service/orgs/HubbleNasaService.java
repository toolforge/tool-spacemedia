package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import static java.util.stream.Collectors.toSet;
import static org.wikimedia.commons.donvip.spacemedia.utils.CsvHelper.loadCsvMapping;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.stsci.StsciMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.stsci.StsciMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.utils.Emojis;

/**
 * Service harvesting images from NASA Hubble website.
 */
@Service
public class HubbleNasaService extends AbstractOrgStsciService {

    private Map<String, String> hubbleCategories;

    @Lazy
    @Autowired
    private HubbleEsaService esaService;

    @Autowired
    public HubbleNasaService(StsciMediaRepository repository,
            @Value("${hubble.nasa.search.link}") String searchEndpoint,
            @Value("${hubble.nasa.detail.link}") String detailEndpoint) {
        super(repository, "hubble", searchEndpoint, detailEndpoint);
    }

    @Override
    protected List<AbstractOrgService<?>> getSimilarOrgServices() {
        return List.of(esaService);
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

    public void checkHubbleCategories() {
        checkCommonsCategories(hubbleCategories);
    }

    @Override
    public Set<String> findCategories(StsciMedia media, FileMetadata metadata, boolean includeHidden) {
        Set<String> result = super.findCategories(media, metadata, includeHidden);
        result.addAll(
                media.getKeywordStream().map(hubbleCategories::get).filter(StringUtils::isNotBlank).collect(toSet()));
        return result;
    }

    @Override
    protected Set<String> getEmojis(StsciMedia uploadedMedia) {
        Set<String> result = super.getEmojis(uploadedMedia);
        result.add(Emojis.STARS);
        return result;
    }

    @Override
    protected Set<String> getTwitterAccounts(StsciMedia uploadedMedia) {
        return Set.of("@NASAHubble", "@HubbleTelescope");
    }
}
