package org.wikimedia.commons.donvip.spacemedia.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.wikimedia.commons.donvip.spacemedia.data.local.kari.KariMedia;
import org.wikimedia.commons.donvip.spacemedia.service.agencies.KariService;

@RestController()
@RequestMapping("/kari")
public class KariController extends SpaceAgencyController<KariMedia, Integer> {

    @Autowired
    public KariController(KariService service) {
        super(service);
    }
}
