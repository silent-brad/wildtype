package org.wildtype.web;

import java.time.Instant;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.wildtype.Sighting;
import org.wildtype.service.FragmentRenderer;
import org.wildtype.service.SightingService;

@RestController
public class DebugController {

    private final SightingService sightingService;
    private final FragmentRenderer fragmentRenderer;
    private final StreamController streamController;

    public DebugController(
            SightingService sightingService, FragmentRenderer fragmentRenderer, StreamController streamController) {
        this.sightingService = sightingService;
        this.fragmentRenderer = fragmentRenderer;
        this.streamController = streamController;
    }

    @PostMapping("/api/debug/sighting")
    public ResponseEntity<Long> injectSighting(
            @RequestParam(defaultValue = "Bird") String species,
            @RequestParam(defaultValue = "0.91") float confidence) {

        String imagePath = "/captures/demo-" + Instant.now().toEpochMilli() + ".jpg";
        long id = sightingService.record(new Sighting(0, Instant.now(), species, confidence, imagePath));

        Sighting s = new Sighting(id, Instant.now(), species, confidence, imagePath);
        String html = fragmentRenderer.renderSightingCard(s);
        streamController.broadcast(html);

        return ResponseEntity.ok(id);
    }
}
