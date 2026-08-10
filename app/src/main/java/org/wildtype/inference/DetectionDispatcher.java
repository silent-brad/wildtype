package org.wildtype.inference;

public class DetectionDispatcher {

    public boolean isInteresting(int classId) {
        return switch (classId) {
            case 15 -> true;
            default -> false;
        };
    }
}
