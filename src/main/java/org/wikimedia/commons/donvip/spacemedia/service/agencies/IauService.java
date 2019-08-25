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
import org.wikimedia.commons.donvip.spacemedia.data.domain.iau.IauMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.iau.IauMediaRepository;

@Service
public class IauService extends CommonEsoService<IauMedia> {

    private static final String IAU_BASE_PUBLIC_URL = "https://www.iau.org/public/";

    private static final String IAU_IMAGES_PATH = "images/";

    private static final Pattern PATTERN_LOCALIZED_URL = Pattern
            .compile(IAU_BASE_PUBLIC_URL + "([a-z]+/)" + IAU_IMAGES_PATH + ".*");

    @Autowired
    public IauService(IauMediaRepository repository, @Value("${iau.search.link}") String searchLink) {
        super(repository, searchLink, IauMedia.class);
    }

    @Override
    @Scheduled(fixedRateString = "${iau.update.rate}", initialDelayString = "${initial.delay}")
    public void updateMedia() throws IOException {
        doUpdateMedia();
    }

    @Override
    public String getName() {
        return "IAU";
    }

    @Override
    protected List<String> findTemplates(IauMedia media) {
        List<String> result = super.findTemplates(media);
        result.add("IAU");
        return result;
    }

    @Override
    protected String getSource(IauMedia media) throws MalformedURLException {
        return wikiLink(new URL(IAU_BASE_PUBLIC_URL + IAU_IMAGES_PATH + media.getId()), media.getTitle());
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
