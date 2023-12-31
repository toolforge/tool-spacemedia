package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import static java.util.stream.Collectors.toSet;
import static org.wikimedia.commons.donvip.spacemedia.utils.CsvHelper.loadCsvMapping;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.stsci.StsciMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.stsci.StsciMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.utils.Emojis;

/**
 * Service harvesting images from NASA JWST website.
 */
@Service
public class WebbNasaService extends AbstractOrgStsciService {

    private Map<String, String> webbCategories;

    @Autowired
    public WebbNasaService(StsciMediaRepository repository,
            @Value("${webb.nasa.search.link}") String searchEndpoint,
            @Value("${webb.nasa.detail.link}") String detailEndpoint) {
        super(repository, "webb", searchEndpoint, detailEndpoint);
    }

    @Override
    @PostConstruct
    void init() throws IOException {
        super.init();
        webbCategories = loadCsvMapping("webbnasa.categories.csv");
    }

    @Override
    public String getName() {
        return "Webb (NASA)";
    }

    public void checkWebbCategories() {
        checkCommonsCategories(webbCategories);
    }

    @Override
    public Set<String> findCategories(StsciMedia media, FileMetadata metadata, boolean includeHidden) {
        Set<String> result = super.findCategories(media, metadata, includeHidden);
        result.addAll(
                media.getKeywordStream().map(webbCategories::get).filter(StringUtils::isNotBlank).collect(toSet()));
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
        return Set.of("@NASAWebb");
    }
}
