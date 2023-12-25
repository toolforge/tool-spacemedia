package org.wikimedia.commons.donvip.spacemedia.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.RuntimeDataRepository;
import org.wikimedia.commons.donvip.spacemedia.data.hashes.HashAssociation;
import org.wikimedia.commons.donvip.spacemedia.data.hashes.HashAssociationRepository;

@RestController
public class HashController {

    @Autowired
    private RuntimeDataRepository runtimeRepo;

    @Autowired
    private HashAssociationRepository hashRepo;

    @GetMapping("/hashLastTimestamp")
    public String hashLastTimestamp() {
        return runtimeRepo.findById("commons").orElseThrow().getLastTimestamp();
    }

    @PutMapping("/hashAssociation")
    public HashAssociation putHashAssociation(@RequestBody HashAssociation association) {
        return hashRepo.save(association);
    }
}
