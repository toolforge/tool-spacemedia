package org.wikimedia.commons.donvip.spacemedia.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.wikimedia.commons.donvip.spacemedia.data.local.flickr.FlickrMedia;
import org.wikimedia.commons.donvip.spacemedia.service.agencies.SpaceXService;

@RestController()
@RequestMapping("/dlr")
public class DlrController extends SpaceAgencyController<FlickrMedia, Long> {

    @Autowired
    public DlrController(SpaceXService service) {
        super(service);
    }
}
