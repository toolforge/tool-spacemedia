package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import static org.wikimedia.commons.donvip.spacemedia.service.wikimedia.WikidataItem.Q725252_SATELLITE_IMAGERY;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrMediaRepository;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrPhotoSet;
import org.wikimedia.commons.donvip.spacemedia.service.wikimedia.SdcStatements;
import org.wikimedia.commons.donvip.spacemedia.utils.Emojis;

@Service
public class InpeFlickrService extends AbstractOrgFlickrService {

    private static final Map<String, Pair<String, String>> INPE_SATELLITES = Map.of(
            "CBERS-1", Pair.of("Q15292930", "Photos by CBERS-1"),
            "CBERS-4", Pair.of("Q15300663", "Satellite pictures by CBERS-4"),
            "CBERS-4A", Pair.of("Q20050234", "Photos by CBERS-4A"),
            "AMAZONIA-1", Pair.of("Q4740919", "Photos by Amazônia 1"));

    @Autowired
    public InpeFlickrService(FlickrMediaRepository repository,
            @Value("${inpe.flickr.accounts}") Set<String> flickrAccounts) {
        super(repository, "inpe.flickr", flickrAccounts);
    }

    static void doForSatellites(Supplier<Stream<String>> stream, Consumer<Pair<String, String>> consumer) {
        for (Map.Entry<String, Pair<String, String>> e : INPE_SATELLITES.entrySet()) {
            if (stream.get().anyMatch(
                    x -> x.toUpperCase(Locale.forLanguageTag("pt_BR")).replace("_", "-").contains(e.getKey()))) {
                consumer.accept(e.getValue());
                break;
            }
        }
    }

    @Override
    public Set<String> findCategories(FlickrMedia media, FileMetadata metadata, boolean includeHidden) {
        Set<String> result = super.findCategories(media, metadata, includeHidden);
        if (includeHidden) {
            result.add("Files from INPE Coordenação-Geral de Observação da Terra Flickr stream");
        }
        doForSatellites(() -> media.getPhotosets().stream().map(FlickrPhotoSet::getTitle),
                p -> result.add(p.getValue()));
        return result;
    }

    @Override
    protected SdcStatements getStatements(FlickrMedia media, FileMetadata metadata) {
        SdcStatements result = super.getStatements(media, metadata);
        doForSatellites(() -> media.getPhotosets().stream().map(FlickrPhotoSet::getTitle),
                p -> result.creator(p.getKey()));
        return result.locationOfCreation("Q663611") // Created in low earth orbit
                .fabricationMethod(Q725252_SATELLITE_IMAGERY);
    }

    @Override
    protected boolean isSatellitePicture(FlickrMedia media, FileMetadata metadata) {
        return true;
    }

    @Override
    protected boolean checkBlocklist() {
        return false;
    }

    @Override
    public String getName() {
        return "INPE (Flickr)";
    }

    @Override
    protected String getLanguage(FlickrMedia media) {
        return "pt-br";
    }

    @Override
    protected Set<String> getEmojis(FlickrMedia uploadedMedia) {
        return Set.of(Emojis.FLAG_BRA);
    }

    @Override
    protected Set<String> getTwitterAccounts(FlickrMedia uploadedMedia) {
        return Set.of("@inpe_mcti");
    }
}
