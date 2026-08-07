package org.wildtype.inference;

import static org.junit.jupiter.api.Assertions.*;

import ai.onnxruntime.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import nu.pattern.OpenCV;
import org.junit.jupiter.api.Test;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

class OnnxDetectorTest {

    static {
        OpenCV.loadLocally();
    }

    @Test
    void inspectModelOutputs() throws OrtException {
        try (var env = OrtEnvironment.getEnvironment();
                var session = env.createSession(
                        "src/main/resources/models/mobilenet-ssd.onnx", new OrtSession.SessionOptions())) {

            System.out.println("=== OUTPUTS ===");
            for (Map.Entry<String, NodeInfo> e : session.getOutputInfo().entrySet()) {
                TensorInfo info = (TensorInfo) e.getValue().getInfo();
                System.out.println(e.getKey() + " -> " + info.type + " " + java.util.Arrays.toString(info.getShape()));
            }
        }
    }

    @Test
    void detectsOnStaticImage() throws Exception {
        Path capturesDir = Path.of("src/test/resources/captures");
        Path testImage = Files.list(capturesDir)
                .filter(p -> p.toString().endsWith(".jpg"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No captured image found. Run the app first."));

        Mat frame = Imgcodecs.imread(testImage.toString());
        assertFalse(frame.empty(), "Test image could not be loaded");

        try (var detector = new OnnxDetector("src/main/resources/models/mobilenet-ssd.onnx")) {
            List<OnnxDetector.Detection> detections = detector.detect(frame);
            System.out.println("Detections: " + detections.size());
            for (var d : detections) {
                System.out.println("  class=" + d.classId() + " conf=" + d.confidence());
            }
            assertNotNull(detections);
        }
    }
}
