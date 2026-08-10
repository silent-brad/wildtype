package org.wildtype.inference;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class SpeciesClassifier implements AutoCloseable {

    private final OrtEnvironment env;
    private final OrtSession session;
    private final List<String> labels;
    private final int inputSize = 224;

    private static final float[] MEAN = {0.485f, 0.456f, 0.406f};
    private static final float[] STD = {0.229f, 0.224f, 0.225f};

    public SpeciesClassifier(String modelPath, String labelsResource) throws Exception {
        this.env = OrtEnvironment.getEnvironment();
        this.session = env.createSession(modelPath, new OrtSession.SessionOptions());
        this.labels = loadLabels(labelsResource);
    }

    private List<String> loadLabels(String resource) throws Exception {
        List<String> list = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(getClass().getClassLoader().getResourceAsStream(resource)))) {
            String line;
            while ((line = reader.readLine()) != null) {
                list.add(line.trim());
            }
        }
        return list;
    }

    public Result classify(Mat bgrCrop) throws OrtException {
        Mat rgb = new Mat();
        Imgproc.cvtColor(bgrCrop, rgb, Imgproc.COLOR_BGR2RGB);
        Mat resized = new Mat();
        Imgproc.resize(rgb, resized, new Size(inputSize, inputSize));

        float[] pixels = new float[inputSize * inputSize * 3];
        byte[] buffer = new byte[inputSize * inputSize * 3];
        resized.get(0, 0, buffer);

        // Normalize: (pixel/255 - mean) / std, NCHW layout
        int idx = 0;
        for (int c = 0; c < 3; c++) {
            for (int h = 0; h < inputSize; h++) {
                for (int w = 0; w < inputSize; w++) {
                    int srcIdx = (h * inputSize + w) * 3 + c;
                    float val = (buffer[srcIdx] & 0xFF) / 255.0f;
                    pixels[idx++] = (val - MEAN[c]) / STD[c];
                }
            }
        }

        rgb.release();
        resized.release();

        long[] shape = {1, 3, inputSize, inputSize};
        try (OnnxTensor tensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(pixels), shape)) {
            OrtSession.Result outputs = session.run(
                    Collections.singletonMap(session.getInputNames().iterator().next(), tensor));
            return parseOutput(outputs);
        }
    }

    private Result parseOutput(OrtSession.Result outputs) throws OrtException {
        float[][] probs = (float[][]) outputs.get(0).getValue();
        float[] p = probs[0];

        int bestIdx = 0;
        float bestScore = p[0];
        for (int i = 1; i < p.length; i++) {
            if (p[i] > bestScore) {
                bestScore = p[i];
                bestIdx = i;
            }
        }
        return new Result(labels.get(bestIdx), bestScore);
    }

    @Override
    public void close() throws OrtException {
        session.close();
        env.close();
    }

    public record Result(String species, float confidence) {}
}
