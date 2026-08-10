package org.wildtype.inference;

public class DetectionDispatcher {

    public boolean isInteresting(int classId, float confidence) {
        return switch (classId) {
            case int id when id == 15 && confidence >= 0.40f -> true;
            default -> false;
        };
    }
}
