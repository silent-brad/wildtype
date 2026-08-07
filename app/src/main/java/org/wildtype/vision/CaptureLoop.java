package org.wildtype.vision;

import nu.pattern.OpenCV;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

public class CaptureLoop implements AutoCloseable {

    static {
        OpenCV.loadLocally();
    }

    private final VideoCapture camera;
    private Mat background;

    public CaptureLoop() {
        this.camera = new VideoCapture(0);
        if (!camera.isOpened()) {
            throw new IllegalStateException("Cannot open webcam /dev/video0");
        }

        Mat first = new Mat();
        if (!camera.read(first)) {
            throw new IllegalStateException("Cannot read from webcam");
        }
        this.background = preprocess(first);
    }

    public Mat readFrame() {
        Mat frame = new Mat();
        return camera.read(frame) ? frame : null;
    }

    public Mat preprocess(Mat frame) {
        Mat gray = new Mat();
        Imgproc.cvtColor(frame, gray, Imgproc.COLOR_BGR2GRAY);

        Mat blurred = new Mat();
        Imgproc.GaussianBlur(gray, blurred, new Size(21, 21), 0);

        gray.release();
        return blurred;
    }

    public Mat getBackground() {
        return background;
    }

    public void updateBackground(Mat newBackground) {
        if (this.background != null) {
            this.background.release();
        }
        this.background = newBackground.clone();
    }

    @Override
    public void close() {
        camera.release();
        if (background != null) {
            background.release();
        }
    }
}
