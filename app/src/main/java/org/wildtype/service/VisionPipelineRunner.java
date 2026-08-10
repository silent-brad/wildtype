package org.wildtype.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.imgcodecs.Imgcodecs;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.wildtype.Sighting;
import org.wildtype.inference.DetectionDispatcher;
import org.wildtype.inference.OnnxDetector;
import org.wildtype.inference.SpeciesClassifier;
import org.wildtype.vision.CaptureLoop;
import org.wildtype.vision.MotionDetector;
import org.wildtype.web.StreamController;

@Component
public class VisionPipelineRunner implements AutoCloseable {

    private final SightingService sightingService;
    private final FragmentRenderer fragmentRenderer;
    private final StreamController streamController;
    private final DetectionDispatcher dispatcher;
    private Thread captureThread;
    private volatile boolean running = true;

    private final boolean enabled;

    public VisionPipelineRunner(
            @Value("${wildtype.vision.enabled:true}") boolean enabled,
            SightingService sightingService,
            FragmentRenderer fragmentRenderer,
            StreamController streamController) {
        this.enabled = enabled;
        this.sightingService = sightingService;
        this.fragmentRenderer = fragmentRenderer;
        this.streamController = streamController;
        this.dispatcher = new DetectionDispatcher();
    }

    @PostConstruct
    public void start() {
        if (enabled) {
            captureThread = Thread.startVirtualThread(this::runLoop);
        }
    }

    @PreDestroy
    public void shutdown() {
        running = false;
        if (captureThread != null) {
            captureThread.interrupt();
        }
    }

    @Override
    public void close() {
        shutdown();
    }

    private void runLoop() {
        try (var captureLoop = new CaptureLoop();
                var motionDetector = new MotionDetector(captureLoop, 25, 500);
                var onnxDetector = new OnnxDetector("src/main/resources/models/mobilenet-ssd.onnx");
                var speciesClassifier = new SpeciesClassifier(
                        "src/main/resources/models/animal_species_cls.onnx", "models/animal_species_labels.txt")) {

            Path captureDir = Path.of("captures");
            Files.createDirectories(captureDir);

            while (running && !Thread.interrupted()) {
                var frame = motionDetector.tick();
                if (frame != null) {
                    runInference(onnxDetector, speciesClassifier, frame, captureDir);
                    frame.release();
                }
                Thread.sleep(100);
            }
        } catch (Exception e) {
            System.err.println("Vision pipeline error: " + e.getMessage());
        }
    }

    private void runInference(
            OnnxDetector onnxDetector, SpeciesClassifier speciesClassifier, Mat frame, Path captureDir) {
        try {
            var detections = onnxDetector.detect(frame);
            for (var det : detections) {
                if (!dispatcher.isInteresting(det.classId(), det.confidence())) continue;

                SpeciesClassifier.Result species = classifyCrop(speciesClassifier, frame, det);

                String filename = "capture-" + Instant.now().toEpochMilli() + ".jpg";
                String fsPath = captureDir.resolve(filename).toString();
                String webPath = "/captures/" + filename;
                Imgcodecs.imwrite(fsPath, frame);

                long id = sightingService.record(
                        new Sighting(0, Instant.now(), species.species(), species.confidence(), webPath));

                var sighting = new Sighting(id, Instant.now(), species.species(), species.confidence(), webPath);
                String html = fragmentRenderer.renderSightingCard(sighting);
                streamController.broadcast(html);
            }
        } catch (Exception e) {
            System.err.println("Inference error: " + e.getMessage());
        }
    }

    private SpeciesClassifier.Result classifyCrop(
            SpeciesClassifier speciesClassifier, Mat frame, OnnxDetector.Detection det) {
        int x1 = Math.max(0, (int) (det.xmin() * frame.width()));
        int y1 = Math.max(0, (int) (det.ymin() * frame.height()));
        int x2 = Math.min(frame.width(), (int) (det.xmax() * frame.width()));
        int y2 = Math.min(frame.height(), (int) (det.ymax() * frame.height()));

        if (x2 <= x1 || y2 <= y1) {
            return new SpeciesClassifier.Result("unknown", 0f);
        }

        Mat crop = new Mat(frame, new Rect(x1, y1, x2 - x1, y2 - y1));
        try {
            return speciesClassifier.classify(crop);
        } catch (Exception e) {
            System.err.println("Classifier error: " + e.getMessage());
            return new SpeciesClassifier.Result("unknown", 0f);
        } finally {
            crop.release();
        }
    }
}
