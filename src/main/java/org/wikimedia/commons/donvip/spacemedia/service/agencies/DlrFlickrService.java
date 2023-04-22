package org.wikimedia.commons.donvip.spacemedia.service.agencies;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.data.domain.Metadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrMediaRepository;

@Service
public class DlrFlickrService extends AbstractAgencyFlickrService<FlickrMedia, Long, LocalDateTime> {

    private static final List<String> STRINGS_TO_REMOVE = List.of("Über die Mission Mars Express:",
            "<a href=\"https://www.dlr.de/content/de/missionen/marsexpress\">www.dlr.de/content/de/missionen/marsexpress</a>");

    @Autowired
    public DlrFlickrService(FlickrMediaRepository repository, @Value("${dlr.flickr.accounts}") Set<String> flickrAccounts) {
        super(repository, "dlr", flickrAccounts);
    }

    @Override
    public void updateMedia() {
        updateFlickrMedia();
    }

    @Override
    public Set<String> findCategories(FlickrMedia media, Metadata metadata, boolean includeHidden) {
        Set<String> result = super.findCategories(media, metadata, includeHidden);
        if (includeHidden) {
            result.add("Files from Deutsches Zentrum für Luft- und Raumfahrt Flickr stream");
        }
        EsaService.enrichEsaCategories(result, media, "");
        return result;
    }

    @Override
    public Set<String> findTemplates(FlickrMedia media) {
        Set<String> result = super.findTemplates(media);
        result.add("DLR-License");
        if (media.getDescription() != null && media.getDescription().contains("ESA/DLR/FU Berlin")) {
            result.add("ESA|ESA/DLR/FU Berlin");
        }
        return result;
    }

    @Override
    public String getName() {
        return "DLR (Flickr)";
    }

    @Override
    protected Collection<String> getStringsToRemove(String pathAlias) {
        return STRINGS_TO_REMOVE;
    }

    @Override
    protected String getLanguage(FlickrMedia media) {
        return media.getDescription() != null && media.getDescription().contains("Photo Credit:") ? "en" : "de";
    }

    @Override
    protected Set<String> getEmojis(FlickrMedia uploadedMedia) {
        return Set.of("🇩🇪");
    }

    @Override
    protected Set<String> getMastodonAccounts(FlickrMedia uploadedMedia) {
        return Set.of("@DLR@social.bund.de");
    }

    @Override
    protected Set<String> getTwitterAccounts(FlickrMedia uploadedMedia) {
        return Set.of("@DLR_en");
    }
}
