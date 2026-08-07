package org.wildtype.inference;

import org.wildtype.inference.OnnxDetector.Detection;

public class DetectionDispatcher {

    public String speciesFromCoco(int classId) {
        return switch (classId) {
            case 14, 15 -> "Bird";
            case 16 -> "Dog";
            case 17 -> "Cat";
            case int id when id >= 1 && id <= 13 -> "Animal (other)";
            default -> "Unknown";
        };
    }

    public boolean worthStoring(float confidence) {
        return switch ((int) (confidence * 100)) {
            case int c when c >= 50 -> true;
            default -> false;
        };
    }

    public Sighting dispatch(Detection detection) {
        String species = speciesFromCoco(detection.classId());
        boolean store = worthStoring(detection.confidence());
        return new Sighting(species, detection.confidence(), store);
    }

    public record Sighting(String species, float confidence, boolean store) {}
}
