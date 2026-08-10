package org.wildtype;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.opencv.core.Mat;
import org.wildtype.inference.DetectionDispatcher;
import org.wildtype.inference.OnnxDetector;
import org.wildtype.vision.CaptureLoop;
import org.wildtype.vision.MotionDetector;

public class Pipeline implements AutoCloseable {

    private final CaptureLoop captureLoop;
    private final MotionDetector motionDetector;
    private final OnnxDetector onnxDetector;
    private final DetectionDispatcher dispatcher;
    private final Path captureDir;

    public Pipeline() throws Exception {
        this.captureLoop = new CaptureLoop();
        this.motionDetector = new MotionDetector(captureLoop, 25, 500);
        this.onnxDetector = new OnnxDetector("src/main/resources/models/mobilenet-ssd.onnx");
        this.dispatcher = new DetectionDispatcher();
        this.captureDir = Path.of("captures");
        Files.createDirectories(captureDir);
    }

    public void run() {
        while (!Thread.interrupted()) {
            Mat frame = motionDetector.tick();
            if (frame != null) {
                runInference(frame);
                frame.release();
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void runInference(Mat frame) {
        try {
            List<OnnxDetector.Detection> detections = onnxDetector.detect(frame);
            for (var det : detections) {
                if (!dispatcher.isInteresting(det.classId(), det.confidence())) continue;
                System.out.println("  → " + det.classId() + " @ " + String.format("%.2f", det.confidence()));
            }
        } catch (Exception e) {
            System.err.println("Inference error: " + e.getMessage());
        }
    }

    @Override
    public void close() throws Exception {
        motionDetector.close();
        onnxDetector.close();
    }
}
