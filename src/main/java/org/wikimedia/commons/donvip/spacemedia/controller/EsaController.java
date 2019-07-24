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
import org.wikimedia.commons.donvip.spacemedia.service.EsaService;

@RestController
@RequestMapping("/esa")
public class EsaController {

    @Autowired
    private EsaService service;

    // IMAGE GETTERS

    @GetMapping("/images/all")
    public Iterable<EsaImage> listAllImages() throws IOException {
        return service.listAllImages();
    }

    @GetMapping("/images/missing")
    public List<EsaImage> listMissingImages() throws IOException {
        return service.listMissingImages();
    }

    @GetMapping("/images/ignored")
    public List<EsaImage> listIgnoredImages() throws IOException {
        return service.listIgnoredImages();
    }

    @GetMapping("/images/duplicates")
    public List<EsaImage> listDuplicateImages() throws IOException {
        return service.listDuplicateImages();
    }

    // FILE GETTERS

    @GetMapping("/files/all")
    public Iterable<EsaFile> listAllFiles() throws IOException {
        return service.listAllFiles();
    }

    @GetMapping("/files/missing")
    public List<EsaFile> listMissingFiles() throws IOException {
        return service.listMissingFiles();
    }

    @GetMapping("/files/ignored")
    public List<EsaFile> listIgnoredFiles() throws IOException {
        return service.listIgnoredFiles();
    }

    @GetMapping("/files/duplicates")
    public List<EsaFile> listDuplicateFiles() throws IOException {
        return service.listDuplicateFiles();
    }

    // ACTIONS

    @GetMapping("/action/update")
    public List<EsaImage> update() throws IOException {
        return service.updateImages();
    }

    @GetMapping("/action/upload/{sha1}")
    public EsaFile upload(@PathVariable String sha1) throws IOException {
        return service.upload(sha1);
    }
}
