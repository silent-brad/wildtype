package org.wildtype.inference;

import org.wildtype.inference.OnnxDetector.Detection;

public class DetectionDispatcher {

    public String speciesFromCoco(int classId) {
        return switch (classId) {
            case 1 -> "Person";
            case 15 -> "Bird";
            case 16 -> "Cat";
            case 17 -> "Dog";
            case 18 -> "Horse";
            case 19 -> "Sheep";
            case 20 -> "Cow";
            case 22 -> "Bear";
            default -> "Unknown";
        };
    }

    public boolean isInteresting(int classId) {
        return switch (classId) {
            case 1, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24 -> true;
            default -> false;
        };
    }

    public boolean worthStoring(float confidence, int classId) {
        float threshold =
                switch (classId) {
                    case 1 -> 0.70f;
                    case 15 -> 0.40f;
                    default -> 0.55f;
                };
        return confidence >= threshold;
    }

    public Sighting dispatch(Detection detection) {
        int classId = detection.classId();
        String species = speciesFromCoco(classId);
        boolean store = isInteresting(classId) && worthStoring(detection.confidence(), classId);
        return new Sighting(species, detection.confidence(), store);
    }

    public record Sighting(String species, float confidence, boolean store) {}
}
