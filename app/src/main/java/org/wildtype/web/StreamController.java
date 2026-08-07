package org.wildtype.web;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
public class StreamController {

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    @GetMapping(value = "/api/sightings/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(0L);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));
        return emitter;
    }

    public void broadcast(String htmlFragment) {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().data(htmlFragment));
            } catch (IOException e) {
                emitters.remove(emitter);
            }
        }
    }
}
