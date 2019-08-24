package org.wikimedia.commons.donvip.spacemedia.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.wikimedia.commons.donvip.spacemedia.data.domain.Media;
import org.wikimedia.commons.donvip.spacemedia.service.agencies.AbstractSpaceAgencyService;

@Controller
public class WebAppController {

    @Autowired
    private List<AbstractSpaceAgencyService<? extends Media, ?>> agencies;

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("agencies", agencies.stream().sorted().collect(Collectors.toList()));
        return "index";
    }
}
