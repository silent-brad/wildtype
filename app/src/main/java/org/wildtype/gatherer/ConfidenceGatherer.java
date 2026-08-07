package org.wildtype.gatherer;

import java.util.function.Function;
import java.util.stream.Gatherer;

/**
 * A stream gatherer that filters elements below a confidence threshold.
 * Demonstrates JEP 485: Stream Gatherers.
 */
public class ConfidenceGatherer {

    private ConfidenceGatherer() {}

    public static <T> Gatherer<T, ?, T> aboveThreshold(Function<T, Float> confidenceExtractor, float threshold) {
        return Gatherer.ofSequential(() -> null, (state, element, downstream) -> {
            float confidence = confidenceExtractor.apply(element);
            if (confidence >= threshold) {
                return downstream.push(element);
            }
            return true;
        });
    }
}
