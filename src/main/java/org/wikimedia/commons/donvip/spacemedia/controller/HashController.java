package org.wikimedia.commons.donvip.spacemedia.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.wikimedia.commons.donvip.spacemedia.data.domain.RuntimeDataRepository;

@RestController
public class HashController {

    @Autowired
    private RuntimeDataRepository runtime;

    @GetMapping("/hashLastTimestamp")
    public String hashLastTimestamp() {
        return runtime.findById("commons").orElseThrow().getLastTimestamp();
    }
}
