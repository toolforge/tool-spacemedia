package org.wikimedia.commons.donvip.spacemedia.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.wikimedia.commons.donvip.spacemedia.data.domain.flickr.FlickrMedia;
import org.wikimedia.commons.donvip.spacemedia.service.agencies.PotentialFlickrService;

@RestController()
@RequestMapping("/potential")
public class PotentialController extends AbstractSpaceAgencyController<FlickrMedia, Long> {

    @Autowired
    public PotentialController(PotentialFlickrService service) {
        super(service);
    }
}
