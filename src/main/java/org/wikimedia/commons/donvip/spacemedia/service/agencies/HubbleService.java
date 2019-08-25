package org.wikimedia.commons.donvip.spacemedia.service.agencies;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.data.domain.hubble.HubbleMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.hubble.HubbleMediaRepository;

@Service
public class HubbleService extends CommonEsoService<HubbleMedia> {

    private static final String HUB_BASE_PUBLIC_URL = "https://www.spacetelescope.org/public/";

    private static final String HUB_IMAGES_PATH = "images/";

    private static final Pattern PATTERN_LOCALIZED_URL = Pattern
            .compile(HUB_BASE_PUBLIC_URL + "([a-z]+/)" + HUB_IMAGES_PATH + ".*");

    @Autowired
    public HubbleService(HubbleMediaRepository repository, @Value("${hubble.search.link}") String searchLink) {
        super(repository, searchLink, HubbleMedia.class);
    }

    @Override
    @Scheduled(fixedRateString = "${hubble.update.rate}", initialDelayString = "${initial.delay}")
    public void updateMedia() throws IOException {
        doUpdateMedia();
    }

    @Override
    public String getName() {
        return "Hubble";
    }

    @Override
    protected List<String> findTemplates(HubbleMedia media) {
        List<String> result = super.findTemplates(media);
        result.add("Hubble");
        return result;
    }

    @Override
    protected String getSource(HubbleMedia media) throws MalformedURLException {
        return wikiLink(new URL(HUB_BASE_PUBLIC_URL + HUB_IMAGES_PATH + media.getId()), media.getTitle());
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
