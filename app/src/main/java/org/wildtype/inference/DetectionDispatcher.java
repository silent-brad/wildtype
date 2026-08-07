package org.wildtype.inference;

import org.wildtype.inference.OnnxDetector.Detection;

public class DetectionDispatcher {

    public String speciesFromCoco(int classId) {
        return switch (classId) {
            case 1 -> "Person";
            case 2 -> "Bicycle";
            case 3 -> "Car";
            case 4 -> "Motorcycle";
            case 5 -> "Airplane";
            case 6 -> "Bus";
            case 7 -> "Train";
            case 8 -> "Truck";
            case 9 -> "Boat";
            case 10 -> "Traffic Light";
            case 11 -> "Fire Hydrant";
            case 12 -> "Stop Sign";
            case 13 -> "Parking Meter";
            case 14 -> "Bench";
            case 15 -> "Bird";
            case 16 -> "Cat";
            case 17 -> "Dog";
            case 18 -> "Horse";
            case 19 -> "Sheep";
            case 20 -> "Cow";
            case 21 -> "Elephant";
            case 22 -> "Bear";
            case 23 -> "Zebra";
            case 24 -> "Giraffe";
            case int id when id >= 25 && id <= 80 -> "Object";
            default -> "Unknown";
        };
    }

    public boolean worthStoring(float confidence) {
        return switch ((int) (confidence * 100)) {
            case int c when c >= 30 -> true;
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
