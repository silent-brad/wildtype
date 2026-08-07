package org.wildtype.gatherer;

import java.time.Duration;
import java.time.Instant;
import java.util.stream.Gatherer;

/**
 * A stream gatherer that filters consecutive elements with the same key within a cooldown window.
 * Demonstrates JEP 485: Stream Gatherers.
 */
public class CooldownGatherer {

    private CooldownGatherer() {}

    public static <T> Gatherer<T, ?, T> cooldown(
            java.util.function.Function<T, String> keyExtractor, Duration window, double minConfidence) {
        return Gatherer.ofSequential(
                State::new,
                (state, element, downstream) -> {
                    String key = keyExtractor.apply(element);
                    if (state.allow(key, window)) {
                        return downstream.push(element);
                    }
                    return true; // drop this element
                },
                (state, downstream) -> {} // no finisher needed
                );
    }

    static class State {
        private final java.util.Map<String, Instant> lastSeen = new java.util.HashMap<>();

        boolean allow(String key, Duration window) {
            Instant now = Instant.now();
            Instant previous = lastSeen.get(key);
            if (previous == null || Duration.between(previous, now).compareTo(window) >= 0) {
                lastSeen.put(key, now);
                return true;
            }
            return false;
        }
    }
}
