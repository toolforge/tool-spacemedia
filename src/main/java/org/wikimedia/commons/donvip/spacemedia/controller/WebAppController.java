package org.wikimedia.commons.donvip.spacemedia.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.wikimedia.commons.donvip.spacemedia.data.domain.base.Media;
import org.wikimedia.commons.donvip.spacemedia.service.orgs.AbstractOrgService;

@Controller
public class WebAppController {

    @Autowired
    private List<AbstractOrgService<? extends Media>> orgs;

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("orgs", orgs.stream().sorted().toList());
        return "index";
    }
}
