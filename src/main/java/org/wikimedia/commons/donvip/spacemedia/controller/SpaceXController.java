package org.wikimedia.commons.donvip.spacemedia.controller;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.wikimedia.commons.donvip.spacemedia.data.local.flickr.FlickrMedia;
import org.wikimedia.commons.donvip.spacemedia.service.agencies.SpaceXService;

@RestController()
@RequestMapping("/spacemedia/spacex")
public class SpaceXController {

    @Autowired
    private SpaceXService service;

    @GetMapping("/all")
    public Iterable<? extends FlickrMedia> listAll() throws IOException {
        return service.listAllMedia();
    }

    @GetMapping("/update")
    public List<FlickrMedia> update() throws IOException {
        return service.updateMedia();
    }

    @GetMapping("/missing")
    public List<FlickrMedia> listMissing() throws IOException {
        return service.listMissingMedia();
    }

    @GetMapping("/duplicates")
    public List<FlickrMedia> listDuplicate() throws IOException {
        return service.listDuplicateMedia();
    }
}
