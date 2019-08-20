package org.wikimedia.commons.donvip.spacemedia.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.wikimedia.commons.donvip.spacemedia.data.domain.eso.EsoMedia;
import org.wikimedia.commons.donvip.spacemedia.service.agencies.EsoService;

@RestController
@RequestMapping("/eso")
public class EsoController extends AbstractSpaceAgencyController<EsoMedia, String> {

    @Autowired
    public EsoController(EsoService service) {
        super(service);
    }
}
