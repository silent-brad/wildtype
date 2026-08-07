package org.wildtype.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.wildtype.service.SightingService;

@Controller
public class DashboardController {

    private final SightingService sightingService;

    public DashboardController(SightingService sightingService) {
        this.sightingService = sightingService;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("sightings", sightingService.recent(50));
        model.addAttribute("counts", sightingService.counts());
        return "index";
    }

    @GetMapping("/sightings/{id}")
    public String detail(@PathVariable long id, Model model) {
        // For now redirect to index; detail page can be added later
        return "redirect:/";
    }
}
