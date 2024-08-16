package org.wikimedia.commons.donvip.spacemedia.service.orgs;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.wikimedia.commons.donvip.spacemedia.data.domain.webmil.WebMilMediaRepository;

/**
 * https://www.web.dma.mil/Our-Customers/#space-force-websites
 */
@Service
public class UsSpaceForceWebMilService extends AbstractOrgWebMilService {

    @Autowired
    public UsSpaceForceWebMilService(WebMilMediaRepository repository,
            @Value("${usspaceforce.webmil.repoIds}") Set<String> websites) {
        super(repository, "usspaceforce.webmil", websites);
    }

    @Override
    public String getName() {
        return "U.S. Space Force/Command (WEB.mil)";
    }
}
