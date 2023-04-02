package org.wikimedia.commons.donvip.spacemedia.service.agencies;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.data.domain.eso.EsoMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.eso.EsoMediaRepository;

@Service
public class EsoService extends CommonEsoService<EsoMedia> {

    private static final String ESO_BASE_PUBLIC_URL = "https://www.eso.org/public/";

    private static final String ESO_IMAGES_PATH = "images/";

    private static final Pattern PATTERN_LOCALIZED_URL = Pattern
            .compile(ESO_BASE_PUBLIC_URL + "([a-z]+/)" + ESO_IMAGES_PATH + ".*");

    @Autowired
    public EsoService(EsoMediaRepository repository, @Value("${eso.search.link}") String searchLink) {
        super(repository, "eso", searchLink, EsoMedia.class);
    }

    @Override
    public void updateMedia() throws IOException {
        doUpdateMedia();
    }

    @Override
    protected Class<EsoMedia> getMediaClass() {
        return EsoMedia.class;
    }

    @Override
    public String getName() {
        return "ESO";
    }

    @Override
    public Set<String> findTemplates(EsoMedia media) {
        Set<String> result = super.findTemplates(media);
        result.add("ESO");
        return result;
    }

    @Override
    public URL getSourceUrl(EsoMedia media) throws MalformedURLException {
        return new URL(ESO_BASE_PUBLIC_URL + ESO_IMAGES_PATH + media.getId());
    }

    @Override
    public final String getSource(EsoMedia media) throws MalformedURLException {
        return "{{ESO-source|" + media.getId() + "|" + media.getId() + "}}";
    }

    @Override
    protected Matcher getLocalizedUrlMatcher(String imgUrlLink) {
        return PATTERN_LOCALIZED_URL.matcher(imgUrlLink);
    }

    @Override
    protected String getCopyrightLink() {
        return "/public/outreach/copyright/";
    }

    @Override
    protected Set<String> getTwitterAccounts(EsoMedia uploadedMedia) {
        return Set.of("ESO");
    }
}
