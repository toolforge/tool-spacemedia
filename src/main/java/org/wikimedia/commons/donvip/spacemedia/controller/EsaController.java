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
@RequestMapping("/spacemedia/esa")
public class EsaController {

    @Autowired
    private EsaService service;

    // IMAGE GETTERS

    @GetMapping("/images/all")
    public Iterable<EsaImage> listAllImages() {
        return service.listAllImages();
    }

    @GetMapping("/images/missing")
    public List<EsaImage> listMissingImages() {
        return service.listMissingImages();
    }

    @GetMapping("/images/ignored")
    public List<EsaImage> listIgnoredImages() {
        return service.listIgnoredImages();
    }

    @GetMapping("/images/duplicates")
    public List<EsaImage> listDuplicateImages() {
        return service.listDuplicateImages();
    }

    // FILE GETTERS

    @GetMapping("/files/all")
    public Iterable<EsaFile> listAllFiles() {
        return service.listAllMedia();
    }

    @GetMapping("/files/missing")
    public List<EsaFile> listMissingFiles() {
        return service.listMissingMedia();
    }

    @GetMapping("/files/ignored")
    public List<EsaFile> listIgnoredFiles() {
        return service.listIgnoredMedia();
    }

    @GetMapping("/files/duplicates")
    public List<EsaFile> listDuplicateFiles() {
        return service.listDuplicateMedia();
    }

    // ACTIONS

    @GetMapping("/action/update")
    public List<EsaImage> update() throws IOException {
        return service.updateImages();
    }

    @GetMapping("/action/upload/{sha1}")
    public EsaFile upload(@PathVariable String sha1) {
        return service.upload(sha1);
    }
}
