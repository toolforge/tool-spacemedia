package org.wikimedia.commons.donvip.spacemedia.controller;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.wikimedia.commons.donvip.spacemedia.data.local.esa.EsaFile;
import org.wikimedia.commons.donvip.spacemedia.data.local.esa.EsaImage;
import org.wikimedia.commons.donvip.spacemedia.service.agencies.EsaService;

@RestController
@RequestMapping("/esa")
public class EsaController extends SpaceAgencyController<EsaFile, String> {

    private final EsaService esaService;

    @Autowired
    public EsaController(EsaService service) {
        super(service);
        this.esaService = service;
    }

    @GetMapping("ignored")
    public List<EsaFile> listIgnoredFiles() {
        return esaService.listIgnoredMedia();
    }

    // ESA IMAGES

    @GetMapping("/images/all")
    public Iterable<EsaImage> listAllImages() {
        return esaService.listAllImages();
    }

    @GetMapping("/images/missing")
    public List<EsaImage> listMissingImages() {
        return esaService.listMissingImages();
    }

    @GetMapping("/images/ignored")
    public List<EsaImage> listIgnoredImages() {
        return esaService.listIgnoredImages();
    }

    @GetMapping("/images/duplicates")
    public List<EsaImage> listDuplicateImages() {
        return esaService.listDuplicateImages();
    }

    @GetMapping("/images/update")
    public List<EsaImage> updateImages() throws IOException {
        return esaService.updateImages();
    }

    @GetMapping("/upload/{sha1}")
    public EsaFile upload(@PathVariable String sha1) {
        return esaService.upload(sha1);
    }
}
