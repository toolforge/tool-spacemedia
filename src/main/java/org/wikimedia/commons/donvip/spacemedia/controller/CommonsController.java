package org.wikimedia.commons.donvip.spacemedia.controller;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.wikimedia.commons.donvip.spacemedia.service.wikimedia.CommonsService;
import org.wikimedia.commons.donvip.spacemedia.service.wikimedia.Video2CommonsService;

@RestController
public class CommonsController {

    @Lazy
    @Autowired
    private CommonsService commons;

    @Lazy
    @Autowired
    private Video2CommonsService v2c;

    @GetMapping("/checkDuplicates")
    public void checkDuplicates() throws IOException {
        commons.checkExactDuplicateFiles();
    }

    @GetMapping("/checkVideos2Commons")
    public void checkVideos2Commons() throws IOException {
        v2c.checkTasks();
    }

    @GetMapping("/computeHashesAsc")
    public void computeHashesAsc() {
        commons.computeHashesOfAllFilesAsc();
    }

    @GetMapping("/computeHashesDesc")
    public void computeHashesDesc() {
        commons.computeHashesOfAllFilesDesc();
    }
}
