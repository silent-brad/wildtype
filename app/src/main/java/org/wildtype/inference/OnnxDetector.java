package org.wildtype.inference;

import ai.onnxruntime.NodeInfo;
import ai.onnxruntime.OnnxJavaType;
import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OnnxValue;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import ai.onnxruntime.TensorInfo;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class OnnxDetector implements AutoCloseable {

    private final OrtEnvironment env;
    private final OrtSession session;
    private final int inputWidth;
    private final int inputHeight;
    private final boolean nchw;
    private final boolean uint8Input;
    private final String inputName;

    public OnnxDetector(String modelPath) throws OrtException {
        this.env = OrtEnvironment.getEnvironment();
        this.session = env.createSession(modelPath, new OrtSession.SessionOptions());

        Map<String, NodeInfo> inputs = session.getInputInfo();
        Map.Entry<String, NodeInfo> firstInput = inputs.entrySet().iterator().next();
        this.inputName = firstInput.getKey();
        TensorInfo info = (TensorInfo) firstInput.getValue().getInfo();
        long[] shape = info.getShape();
        this.uint8Input = info.type == OnnxJavaType.UINT8;

        if (shape.length == 4) {
            if (shape[1] == 3 || shape[1] == -1) {
                if (shape[3] == 3) {
                    this.nchw = false;
                    this.inputHeight = shape[1] == -1 ? 300 : (int) shape[1];
                    this.inputWidth = shape[2] == -1 ? 300 : (int) shape[2];
                } else {
                    this.nchw = true;
                    this.inputHeight = shape[2] == -1 ? 300 : (int) shape[2];
                    this.inputWidth = shape[3] == -1 ? 300 : (int) shape[3];
                }
            } else {
                this.nchw = false;
                this.inputHeight = shape[1] == -1 ? 300 : (int) shape[1];
                this.inputWidth = shape[2] == -1 ? 300 : (int) shape[2];
            }
        } else {
            throw new IllegalStateException("Unexpected input shape: " + java.util.Arrays.toString(shape));
        }

        System.out.println("ONNX model loaded: " + inputWidth + "x" + inputHeight + " " + (nchw ? "NCHW" : "NHWC") + " "
                + (uint8Input ? "uint8" : "float"));
    }

    public List<Detection> detect(Mat bgrFrame) throws OrtException {
        Mat resized = new Mat();
        Imgproc.resize(bgrFrame, resized, new Size(inputWidth, inputHeight));

        Mat rgb = new Mat();
        Imgproc.cvtColor(resized, rgb, Imgproc.COLOR_BGR2RGB);

        byte[] buffer = new byte[inputWidth * inputHeight * 3];
        rgb.get(0, 0, buffer);
        resized.release();
        rgb.release();

        long[] inputShape =
                nchw ? new long[] {1, 3, inputHeight, inputWidth} : new long[] {1, inputHeight, inputWidth, 3};

        if (uint8Input) {
            byte[] tensorData = nchw ? toNchw(buffer) : buffer;
            try (OnnxTensor tensor =
                    OnnxTensor.createTensor(env, ByteBuffer.wrap(tensorData), inputShape, OnnxJavaType.UINT8)) {
                OrtSession.Result outputs = session.run(Collections.singletonMap(inputName, tensor));
                return parseOutputs(outputs);
            }
        } else {
            float[] pixels = new float[buffer.length];
            for (int i = 0; i < buffer.length; i++) {
                pixels[i] = (buffer[i] & 0xFF) / 255.0f;
            }
            float[] tensorData = nchw ? toNchwFloat(pixels) : pixels;
            try (OnnxTensor tensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(tensorData), inputShape)) {
                OrtSession.Result outputs = session.run(Collections.singletonMap(inputName, tensor));
                return parseOutputs(outputs);
            }
        }
    }

    private byte[] toNchw(byte[] nhwc) {
        byte[] nchw = new byte[nhwc.length];
        int planar = inputWidth * inputHeight;
        for (int h = 0; h < inputHeight; h++) {
            for (int w = 0; w < inputWidth; w++) {
                int src = (h * inputWidth + w) * 3;
                int dstR = h * inputWidth + w;
                int dstG = dstR + planar;
                int dstB = dstG + planar;
                nchw[dstR] = nhwc[src];
                nchw[dstG] = nhwc[src + 1];
                nchw[dstB] = nhwc[src + 2];
            }
        }
        return nchw;
    }

    private float[] toNchwFloat(float[] nhwc) {
        float[] nchw = new float[nhwc.length];
        int planar = inputWidth * inputHeight;
        for (int h = 0; h < inputHeight; h++) {
            for (int w = 0; w < inputWidth; w++) {
                int src = (h * inputWidth + w) * 3;
                int dstR = h * inputWidth + w;
                int dstG = dstR + planar;
                int dstB = dstG + planar;
                nchw[dstR] = nhwc[src];
                nchw[dstG] = nhwc[src + 1];
                nchw[dstB] = nhwc[src + 2];
            }
        }
        return nchw;
    }

    private List<Detection> parseOutputs(OrtSession.Result outputs) throws OrtException {
        // SSD MobileNet v1 (TensorFlow) format: 4 output tensors
        float[][][] boxes = null;
        float[][] classes = null;
        float[][] scores = null;
        float[] numDets = null;

        for (Map.Entry<String, OnnxValue> entry : outputs) {
            OnnxValue value = entry.getValue();
            if (value instanceof OnnxTensor t) {
                Object raw = t.getValue();
                String name = entry.getKey();
                if (name.contains("detection_boxes")) boxes = (float[][][]) raw;
                else if (name.contains("detection_classes")) classes = (float[][]) raw;
                else if (name.contains("detection_scores")) scores = (float[][]) raw;
                else if (name.contains("num_detections")) numDets = (float[]) raw;
            }
        }

        if (boxes == null || classes == null || scores == null || numDets == null) {
            return Collections.emptyList();
        }

        int count = (int) numDets[0];
        List<Detection> results = new java.util.ArrayList<>();
        for (int i = 0; i < count; i++) {
            float confidence = scores[0][i];
            if (confidence < 0.3f) continue;
            int classId = (int) classes[0][i];
            // boxes are [ymin, xmin, ymax, xmax]
            float ymin = boxes[0][i][0];
            float xmin = boxes[0][i][1];
            float ymax = boxes[0][i][2];
            float xmax = boxes[0][i][3];
            results.add(new Detection(classId, confidence, xmin, ymin, xmax, ymax));
        }
        return results;
    }

    @Override
    public void close() throws OrtException {
        session.close();
        env.close();
    }

    public record Detection(int classId, float confidence, float xmin, float ymin, float xmax, float ymax) {}
}
