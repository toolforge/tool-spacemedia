package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import static java.util.stream.Collectors.toSet;
import static org.wikimedia.commons.donvip.spacemedia.utils.CsvHelper.loadCsvMapping;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.Media;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.WithInstruments;
import org.wikimedia.commons.donvip.spacemedia.data.domain.stsci.StsciMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.stsci.StsciMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.service.wikimedia.SdcStatements;
import org.wikimedia.commons.donvip.spacemedia.utils.Emojis;

/**
 * Service harvesting images from NASA JWST website.
 */
@Service
public class WebbNasaService extends AbstractOrgStsciService {

    private Map<String, String> webbCategories;

    @Lazy
    @Autowired
    private WebbEsaService esaService;

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
    public boolean updateOnProfiles(List<String> activeProfiles) {
        return super.updateOnProfiles(activeProfiles) || activeProfiles.contains("job-webb");
    }

    @Override
    protected List<AbstractOrgService<?>> getSimilarOrgServices() {
        return List.of(esaService);
    }

    @Override
    public String getName() {
        return "Webb (NASA)";
    }

    public void checkWebbCategories() {
        checkCommonsCategories(webbCategories);
    }

    @Override
    protected SdcStatements getStatements(StsciMedia media, FileMetadata metadata) {
        SdcStatements sdc = super.getStatements(media, metadata).creator("Q186447"); // Created by JWST
        // TODO multiple values for MIRI+NIRCam composite
        switchForInstruments(media, "Q1881516", "Q29598269", null, "Q16153509").ifPresent(sdc::capturedWith);
        return sdc.locationOfCreation("Q15725510"); // Created in L2-Earth-Sun
    }

    @Override
    public Set<String> findCategories(StsciMedia media, FileMetadata metadata, boolean includeHidden) {
        Set<String> result = super.findCategories(media, metadata, includeHidden);
        result.addAll(
                media.getKeywordStream().map(webbCategories::get).filter(StringUtils::isNotBlank).collect(toSet()));
        switchForInstruments(media, "Images by MIRI", "Images by NIRCam", "NIRCam and MIRI composite images",
                "Spectra by NIRSpec").ifPresent(result::add);
        return result;
    }

    <T extends Media & WithInstruments> Optional<String> switchForInstruments(T media, String miri, String nircam,
            String composite, String nirspec) {
        String title = media.getTitle().toLowerCase(Locale.ENGLISH);
        Set<String> instr = media.getInstruments().stream().map(s -> s.toLowerCase(Locale.ENGLISH)).collect(toSet());
        if (title.contains("nircam and miri composite") || title.contains("nircam + miri imag")
                || instr.containsAll(List.of("nircam", "miri"))) {
            return Optional.ofNullable(composite);
        } else if (title.contains("miri image") || title.contains("miri annotated image")
                || title.contains("miri compass image") || instr.contains("miri")) {
            return Optional.of(miri);
        } else if (title.contains("nircam image") || title.contains("nircam annotated image")
                || title.contains("nircam compass image") || instr.contains("nircam")) {
            return Optional.of(nircam);
        } else if (title.contains("nirspec") || instr.contains("nirspec")) {
            return Optional.of(nirspec);
        }
        return Optional.empty();
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
