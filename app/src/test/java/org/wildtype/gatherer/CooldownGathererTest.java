package org.wildtype.gatherer;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class CooldownGathererTest {

    record Detection(String species, float confidence) {}

    @Test
    void filtersWithinCooldown() {
        List<Detection> input = List.of(
                new Detection("Bird", 0.9f),
                new Detection("Bird", 0.8f),
                new Detection("Cat", 0.7f),
                new Detection("Bird", 0.6f));

        List<Detection> result = input.stream()
                .gather(CooldownGatherer.cooldown(Detection::species, Duration.ofMillis(100), 0.5))
                .toList();

        assertEquals(2, result.size());
        assertEquals("Bird", result.get(0).species());
        assertEquals("Cat", result.get(1).species());
    }

    @Test
    void allowsAfterCooldownExpires() throws InterruptedException {
        List<Detection> input = List.of(new Detection("Bird", 0.9f));

        List<Detection> first = input.stream()
                .gather(CooldownGatherer.cooldown(Detection::species, Duration.ofMillis(50), 0.5))
                .toList();
        assertEquals(1, first.size());

        Thread.sleep(60);

        List<Detection> second = input.stream()
                .gather(CooldownGatherer.cooldown(Detection::species, Duration.ofMillis(50), 0.5))
                .toList();
        assertEquals(1, second.size());
    }
}
