package org.wikimedia.commons.donvip.spacemedia.harvester.inpe.website;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.Optional;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.core.AbstractHarvesterService;
import org.wikimedia.commons.donvip.spacemedia.data.jpa.entity.Depot;
import org.wikimedia.commons.donvip.spacemedia.data.jpa.entity.FilePublication;
import org.wikimedia.commons.donvip.spacemedia.data.jpa.entity.Licence;
import org.wikimedia.commons.donvip.spacemedia.data.jpa.entity.Organization;
import org.wikimedia.commons.donvip.spacemedia.data.jpa.entity.PublicationKey;

@Service
public class InpeWebsiteHarvesterService extends AbstractHarvesterService {

    private static final Logger LOGGER = LoggerFactory.getLogger(InpeWebsiteHarvesterService.class);

    @Value("${inpe.gallery.link}")
    private String galleryLink;

    private static final String DEPOT_ID = "inpe-dpi-gallery";

    @Override
    public void harvestMedia() throws IOException {
        LocalDateTime start = startUpdateMedia(DEPOT_ID);
        Depot depot = depotRepository.findById(DEPOT_ID).orElseThrow();
        Organization org = organizationRepository.findById("INPE").orElseThrow();
        int count = 0;
        String baseUrl = galleryLink.substring(0, galleryLink.lastIndexOf('/'));
        LOGGER.debug("Fetching {} media: {}", DEPOT_ID, galleryLink);
        Document html = Jsoup.connect(galleryLink).timeout(60_000).get();
        for (Element element : html.getElementsByTag("a")) {
            String filename = element.text();
            FilePublication file;
            boolean save = false;
            PublicationKey key = new PublicationKey(depot.getId(), filename);
            Optional<FilePublication> fileInRepo = filePublicationRepository.findById(key);
            if (fileInRepo.isPresent()) {
                file = fileInRepo.get();
            } else {
                file = fetchFile(key, baseUrl + '/' + element.attr("href"), depot, org);
                save = true;
            }
            if (save) {
                filePublicationRepository.save(file);
            }
            count++;
        }
        endUpdateMedia(DEPOT_ID, count, start);
    }

    private FilePublication fetchFile(PublicationKey key, String imgUrlLink, Depot depot, Organization org)
            throws MalformedURLException {
        FilePublication file = new FilePublication(depot, key, new URL(imgUrlLink));
        // Licence / credit as per https://www.dpi.inpe.br/galeria/
        file.setLicence(Licence.CC_BY_SA_4_0);
        file.setCredit("INPE");
        file.addAuthor(org);
        LOGGER.info(imgUrlLink);
        return file;
    }
}
