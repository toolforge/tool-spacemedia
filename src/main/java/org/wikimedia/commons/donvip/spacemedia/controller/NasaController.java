package org.wikimedia.commons.donvip.spacemedia.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.wikimedia.commons.donvip.spacemedia.data.local.nasa.NasaMedia;
import org.wikimedia.commons.donvip.spacemedia.service.agencies.NasaService;

@RestController()
@RequestMapping("/spacemedia/nasa")
public class NasaController extends SpaceAgencyController<NasaMedia, String> {

    @Autowired
    public NasaController(NasaService service) {
        super(service);
    }
}
