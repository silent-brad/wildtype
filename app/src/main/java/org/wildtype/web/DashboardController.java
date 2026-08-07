package org.wildtype.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.wildtype.db.SightingRepository;
import org.wildtype.service.SightingService;

@Controller
public class DashboardController {

    private final SightingService sightingService;
    private final SightingRepository sightingRepository;

    public DashboardController(SightingService sightingService, SightingRepository sightingRepository) {
        this.sightingService = sightingService;
        this.sightingRepository = sightingRepository;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("sightings", sightingService.recent(50));
        model.addAttribute("counts", sightingService.counts());
        return "index";
    }

    @GetMapping("/sightings/{id}")
    public String detail(@PathVariable long id, Model model) {
        var sighting = sightingRepository.findById(id);
        if (sighting.isEmpty()) {
            return "redirect:/";
        }
        model.addAttribute("sighting", sighting.get());
        return "detail";
    }
}
