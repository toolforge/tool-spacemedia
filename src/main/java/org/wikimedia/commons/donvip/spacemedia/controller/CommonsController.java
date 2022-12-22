package org.wikimedia.commons.donvip.spacemedia.controller;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.wikimedia.commons.donvip.spacemedia.service.wikimedia.CommonsService;

@RestController
public class CommonsController {

    @Autowired
    private CommonsService commons;

    @GetMapping("/checkDuplicates")
    public void checkDuplicates() throws IOException {
        commons.checkExactDuplicateFiles();
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
