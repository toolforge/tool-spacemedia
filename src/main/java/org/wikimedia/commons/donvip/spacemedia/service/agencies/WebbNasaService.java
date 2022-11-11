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
import org.wikimedia.commons.donvip.spacemedia.data.domain.stsci.StsciMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.stsci.StsciMediaRepository;

/**
 * Service harvesting images from NASA JWST website.
 */
@Service
public class WebbNasaService extends AbstractStsciService {

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
    public Set<String> findCategories(StsciMedia media, boolean includeHidden) {
        Set<String> result = super.findCategories(media, includeHidden);
        if (media.getKeywords() != null) {
            result.addAll(media.getKeywords().stream().map(webbCategories::get).filter(StringUtils::isNotBlank)
                    .collect(toSet()));
        }
        return result;
    }
}
