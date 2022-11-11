package org.wikimedia.commons.donvip.spacemedia.service.agencies;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.data.domain.esa.EsaMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.esa.EsaMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.utils.Utils;

@Service
public class EsaFlickrService extends AbstractAgencyFlickrService<EsaMedia, Integer, LocalDateTime> {

    @Autowired
    private EsaMediaRepository esaRepository;

    private Map<String, String> esaMissions;
    private Map<String, String> esaPeople;

    @Autowired
    public EsaFlickrService(FlickrMediaRepository repository,
            @Value("${esa.flickr.accounts}") Set<String> flickrAccounts) {
        super(repository, "esa.flickr", flickrAccounts);
    }

    @Override
    @PostConstruct
    void init() throws IOException {
        super.init();
        esaMissions = loadCsvMapping("esa.missions.csv");
        esaPeople = loadCsvMapping("esa.people.csv");
    }

    @Override
    public void updateMedia() {
        updateFlickrMedia();
    }

    @Override
    public String getName() {
        return "ESA (Flickr)";
    }

    @Override
    protected EsaMediaRepository getOriginalRepository() {
        return esaRepository;
    }

    @Override
    protected Integer getOriginalId(String id) {
        return Integer.valueOf(id);
    }

    @Override
    public Set<String> findCategories(FlickrMedia media, boolean includeHidden) {
        Set<String> result = super.findCategories(media, includeHidden);
        if (includeHidden) {
            result.add("ESA images (review needed)");
        }
        // Try to find any ESA mission or people in the description and title.
        // Filters in the description search to minimize false positives such as Herschel, Galileo...
        String titleLc = media.getTitle().toLowerCase(Locale.ENGLISH);
        String descriptionLc = media.getDescription().toLowerCase(Locale.ENGLISH);
        for (Map<String, String> mapping : Arrays.asList(esaMissions, esaPeople)) {
            for (Entry<String, String> entry : mapping.entrySet()) {
                String key = entry.getKey();
                String keyLc = entry.getKey().toLowerCase(Locale.ENGLISH);
                if (Utils.isTextFound(titleLc, keyLc)
                        || ((key.contains("-") || key.contains(" ")) && Utils.isTextFound(descriptionLc, keyLc))) {
                    result.add(entry.getValue());
                }
            }
        }
        EsaService.enrichEsaCategories(result, media, "");
        return result;
    }
}
