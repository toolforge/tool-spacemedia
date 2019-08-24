package org.wikimedia.commons.donvip.spacemedia.controller;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.wikimedia.commons.donvip.spacemedia.service.agencies.AbstractSpaceAgencyService;

@Controller
public class WebAppController {

    @Autowired
    private List<AbstractSpaceAgencyService<?, ?>> agencies;

    @GetMapping("/")
    public String index(Model model) {
        Collections.sort(agencies);
        model.addAttribute("agencies", agencies);
        return "index";
    }
}
