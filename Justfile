# Wildtype — Java 26 Edge AI Wildlife Camera

_default:
    @just --list

# Format all Java source
fmt:
    cd app && ../gradlew spotlessApply

# Run all tests
test:
    cd app && ../gradlew test

# Build the project
build:
    cd app && ../gradlew build

# Run with webcam on port 8081
run:
    cd app && ../gradlew bootRun

# Run without vision pipeline (debug API only)
run-debug:
    cd app && ../gradlew bootRun --args='--wildtype.vision.enabled=false'

# Inject a test sighting
debug-sighting species="Bird" confidence="0.91":
    curl -X POST 'http://localhost:8081/api/debug/sighting?species={{species}}&confidence={{confidence}}'

# Download the ONNX model (needed after fresh clone)
model:
    mkdir -p app/src/main/resources/models
    curl -L -o app/src/main/resources/models/mobilenet-ssd.onnx \
        "https://media.githubusercontent.com/media/onnx/models/main/validated/vision/object_detection_segmentation/ssd-mobilenetv1/model/ssd_mobilenet_v1_12.onnx"

# Clean build artifacts
clean:
    cd app && ../gradlew clean
    rm -f wildtype.db
    rm -rf captures/

# Full check: format, build, test
check: fmt build test
    @echo "All checks passed"
