package org.wikimedia.commons.donvip.spacemedia.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.wikimedia.commons.donvip.spacemedia.data.domain.esa.EsaMedia;
import org.wikimedia.commons.donvip.spacemedia.service.agencies.EsaService;

@RestController
@RequestMapping("/esa")
public class EsaController extends AbstractSpaceAgencyController<EsaMedia, Integer> {

    @Autowired
    public EsaController(EsaService service) {
        super(service);
    }
}
