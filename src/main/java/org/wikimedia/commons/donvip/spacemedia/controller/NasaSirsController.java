package org.wikimedia.commons.donvip.spacemedia.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.wikimedia.commons.donvip.spacemedia.data.domain.nasa.sirs.NasaSirsImage;
import org.wikimedia.commons.donvip.spacemedia.service.agencies.NasaSirsService;

@RestController
@RequestMapping("/nasa/sirs")
public class NasaSirsController extends AbstractSpaceAgencyController<NasaSirsImage, String> {

    @Autowired
    public NasaSirsController(NasaSirsService service) {
        super(service);
    }
}
