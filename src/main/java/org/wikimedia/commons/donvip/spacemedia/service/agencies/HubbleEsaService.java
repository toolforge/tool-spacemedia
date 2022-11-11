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
import org.wikimedia.commons.donvip.spacemedia.data.domain.esa.hubble.HubbleEsaMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.esa.hubble.HubbleEsaMediaRepository;

@Service
public class HubbleEsaService extends CommonEsoService<HubbleEsaMedia> {

    private static final String HUB_BASE_PUBLIC_URL = "https://www.spacetelescope.org/public/";

    private static final String HUB_IMAGES_PATH = "images/";

    private static final Pattern PATTERN_LOCALIZED_URL = Pattern
            .compile(HUB_BASE_PUBLIC_URL + "([a-z]+/)" + HUB_IMAGES_PATH + ".*");

    @Autowired
    public HubbleEsaService(HubbleEsaMediaRepository repository,
            @Value("${hubble.esa.search.link}") String searchLink) {
        super(repository, "hubble.esa", searchLink, HubbleEsaMedia.class);
    }

    @Override
    protected Class<HubbleEsaMedia> getMediaClass() {
        return HubbleEsaMedia.class;
    }

    @Override
    public void updateMedia() throws IOException {
        doUpdateMedia();
    }

    @Override
    public String getName() {
        return "Hubble (ESA)";
    }

    @Override
    public Set<String> findTemplates(HubbleEsaMedia media) {
        Set<String> result = super.findTemplates(media);
        if (media.getDate().getYear() < 2009) {
            result.add("PD-Hubble");
        } else {
            result.add("ESA-Hubble");
        }
        return result;
    }

    @Override
    public URL getSourceUrl(HubbleEsaMedia media) throws MalformedURLException {
        return new URL(HUB_BASE_PUBLIC_URL + HUB_IMAGES_PATH + media.getId());
    }

    @Override
    protected Matcher getLocalizedUrlMatcher(String imgUrlLink) {
        return PATTERN_LOCALIZED_URL.matcher(imgUrlLink);
    }

    @Override
    protected String getCopyrightLink() {
        return "/copyright/";
    }
}
