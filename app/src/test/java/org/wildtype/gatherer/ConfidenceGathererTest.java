package org.wildtype.gatherer;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class ConfidenceGathererTest {

    record Detection(String species, float confidence) {}

    @Test
    void filtersBelowThreshold() {
        List<Detection> input =
                List.of(new Detection("Bird", 0.91f), new Detection("Bird", 0.49f), new Detection("Cat", 0.75f));

        List<Detection> result = input.stream()
                .gather(ConfidenceGatherer.aboveThreshold(Detection::confidence, 0.5f))
                .toList();

        assertEquals(2, result.size());
        assertEquals("Bird", result.get(0).species());
        assertEquals("Cat", result.get(1).species());
    }

    @Test
    void emptyWhenAllBelowThreshold() {
        List<Detection> input = List.of(new Detection("Bird", 0.1f), new Detection("Cat", 0.2f));

        List<Detection> result = input.stream()
                .gather(ConfidenceGatherer.aboveThreshold(Detection::confidence, 0.5f))
                .toList();

        assertTrue(result.isEmpty());
    }
}
