package org.wikimedia.commons.donvip.spacemedia.controller;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.wikimedia.commons.donvip.spacemedia.data.local.nasa.NasaMedia;
import org.wikimedia.commons.donvip.spacemedia.service.agencies.NasaService;

@RestController()
@RequestMapping("/spacemedia/nasa")
public class NasaController {

    @Autowired
    private NasaService service;

    @GetMapping("/all")
    public Iterable<? extends NasaMedia> listAll() throws IOException {
        return service.listAllMedia();
    }

    @GetMapping("/update")
    public List<NasaMedia> update() throws IOException {
        return service.updateMedia();
    }

    @GetMapping("/missing")
    public List<NasaMedia> listMissing() throws IOException {
        return service.listMissingMedia();
    }

    @GetMapping("/duplicates")
    public List<NasaMedia> listDuplicate() throws IOException {
        return service.listDuplicateMedia();
    }
}
