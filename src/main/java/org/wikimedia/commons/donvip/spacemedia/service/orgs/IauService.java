package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.newURL;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.FileMetadata;
import org.wikimedia.commons.donvip.spacemedia.data.domain.djangoplicity.DjangoplicityFrontPageItem;
import org.wikimedia.commons.donvip.spacemedia.data.domain.djangoplicity.DjangoplicityMedia;
import org.wikimedia.commons.donvip.spacemedia.data.domain.djangoplicity.DjangoplicityMediaRepository;

@Service
public class IauService extends AbstractOrgDjangoplicityService {

    private static final String IAU_BASE_URL = "https://www.iau.org";

    private static final String IAU_PUBLIC_PATH = "/public/";

    private static final String IAU_BASE_PUBLIC_URL = IAU_BASE_URL + IAU_PUBLIC_PATH;

    private static final String IAU_IMAGES_PATH = "images/detail/";
    private static final String IAU_VIDEOS_PATH = "videos/detail/";

    private static final Pattern PATTERN_LOCALIZED_URL = Pattern
            .compile(IAU_BASE_PUBLIC_URL + "([a-z]+/)" + IAU_IMAGES_PATH + ".*");

    @Autowired
    public IauService(DjangoplicityMediaRepository repository, @Value("${iau.search.link}") String searchLink) {
        super(repository, "iau", searchLink);
    }

    @Override
    protected Iterator<DjangoplicityFrontPageItem> findFrontPageItems(Document document) throws IOException {
        return document.getElementById("main-page-grey").getElementsByClass("thumbnail").stream()
                .flatMap(e -> e.getElementsByTag("a").stream()).map(a -> {
                    Element img = a.getElementsByTag("img").get(0);
                    DjangoplicityFrontPageItem item = new DjangoplicityFrontPageItem();
                    item.setId(a.attr("href").replace(IAU_PUBLIC_PATH + IAU_IMAGES_PATH, "").replace("/", ""));
                    item.setUrl(a.attr("href"));
                    item.setTitle(img.attr("alt"));
                    item.setSrc(IAU_BASE_URL + img.attr("src"));
                    return item;
                }).iterator();
    }

    @Override
    protected StringBuilder findDescription(Element div) {
        StringBuilder description = new StringBuilder();
        for (Element p : div.getElementById("main-page-grey").getElementsByTag("p")) {
            if (p.text().startsWith("Credit:")) {
                break;
            }
            description.append(p.html());
        }
        return description;
    }

    @Override
    protected String getObjectInfoClass() {
        return "col-md-3";
    }

    @Override
    protected String getObjectInfoTitleClass() {
        return "info-list-title";
    }

    @Override
    protected Collection<String> getForbiddenCategories() {
        // General permission does not extend to use of the IAU's logo,
        // which shall remain protected and may not be used or reproduced without prior
        // and individual written consent of the IAU.
        return Arrays.asList("IAU Logos");
    }

    @Override
    protected Collection<String> getForbiddenWordsInTitleOrDescription() {
        return List.of("Cosmology Prize");
    }

    @Override
    public String getName() {
        return "IAU";
    }

    @Override
    protected String hiddenUploadCategory(String repoId) {
        return "Spacemedia IAU files uploaded by " + commonsService.getAccount();
    }

    @Override
    protected boolean checkBlocklist(DjangoplicityMedia media) {
        return true;
    }

    @Override
    public Set<String> findLicenceTemplates(DjangoplicityMedia media, FileMetadata metadata) {
        Set<String> result = super.findLicenceTemplates(media, metadata);
        result.add("IAU");
        return result;
    }

    @Override
    public URL getSourceUrl(DjangoplicityMedia media, FileMetadata metadata, String ext) {
        return newURL(
                IAU_BASE_PUBLIC_URL + imageOrVideo(ext, IAU_IMAGES_PATH, IAU_VIDEOS_PATH) + media.getIdUsedInOrg());
    }

    @Override
    public final String getSource(DjangoplicityMedia media, FileMetadata metadata) {
        return "{{IAU-source|" + media.getIdUsedInOrg() + '|' + media.getIdUsedInOrg() + "}}";
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
