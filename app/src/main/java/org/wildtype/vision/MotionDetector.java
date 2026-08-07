package org.wildtype.vision;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

public class MotionDetector implements AutoCloseable {

    private final CaptureLoop captureLoop;
    private final int threshold;
    private final int minContourArea;
    private final Path captureDir;
    private Instant lastMotionTime = Instant.MIN;
    private final Duration cooldown;

    public MotionDetector(CaptureLoop captureLoop, int threshold, int minContourArea) throws Exception {
        this(captureLoop, threshold, minContourArea, Duration.ofSeconds(1));
    }

    public MotionDetector(CaptureLoop captureLoop, int threshold, int minContourArea, Duration cooldown)
            throws Exception {
        this.captureLoop = captureLoop;
        this.threshold = threshold;
        this.minContourArea = minContourArea;
        this.captureDir = Path.of("src/test/resources/captures");
        this.cooldown = cooldown;
        Files.createDirectories(captureDir);
    }

    /**
     * Run one detection cycle.
     *
     * @return the captured frame if motion was detected (caller must release it), otherwise null.
     */
    public Mat tick() {
        Mat frame = captureLoop.readFrame();
        if (frame == null) return null;

        Mat blurred = captureLoop.preprocess(frame);
        Mat diff = new Mat();
        Core.absdiff(captureLoop.getBackground(), blurred, diff);

        Mat binary = new Mat();
        Imgproc.threshold(diff, binary, threshold, 255, Imgproc.THRESH_BINARY);

        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(binary, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        boolean motion = false;
        for (MatOfPoint contour : contours) {
            double area = Imgproc.contourArea(contour);
            if (area > minContourArea) {
                motion = true;
                break;
            }
        }

        captureLoop.updateBackground(blurred);
        diff.release();
        binary.release();
        hierarchy.release();
        for (MatOfPoint c : contours) c.release();

        if (motion) {
            Instant now = Instant.now();
            if (Duration.between(lastMotionTime, now).compareTo(cooldown) >= 0) {
                lastMotionTime = now;
                System.out.println("MOTION at " + now);
                saveFrame(frame);
                return frame; // caller releases
            }
        }

        frame.release();
        return null;
    }

    private void saveFrame(Mat frame) {
        String filename = captureDir
                .resolve("capture-" + Instant.now().toEpochMilli() + ".jpg")
                .toString();
        Imgcodecs.imwrite(filename, frame);
        System.out.println("Saved: " + filename);
    }

    public void runLoop() {
        while (!Thread.interrupted()) {
            tick();
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    @Override
    public void close() {
        captureLoop.close();
    }
}
