# Wildtype

> A Raspberry Pi wildlife camera written in Java 26 that watches your yard and tells you who visited.

Edge AI inference, SQLite journaling, and a live HTML-over-the-wire dashboard — all running on a Pi, written entirely in Java 26.

[![Java 26](https://img.shields.io/badge/Java-26-orange)](https://openjdk.org/projects/jdk/26/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.16-brightgreen)](https://spring.io/projects/spring-boot)
[![ONNX Runtime](https://img.shields.io/badge/ONNX-Runtime-blue)](https://onnxruntime.ai/)

## What It Does

1. **Watches** — USB webcam captures frames on a Virtual Thread
2. **Detects motion** — OpenCV `absdiff` background subtraction
3. **Classifies** — ONNX MobileNet-SSD runs edge inference (90 ms on Pi 4)
4. **Records** — SQLite stores species, confidence, timestamp, image path
5. **Streams** — Server-Sent Events push live HTML cards to every open browser

## Quick Start

```bash
# 1. Clone and build
./gradlew build

# 2. Run (laptop with webcam, or Pi with USB camera)
./gradlew bootRun

# 3. Open http://localhost:8081
```

To disable the vision pipeline and use the debug API instead:

```bash
./gradlew bootRun --args='--wildtype.vision.enabled=false'
```

Inject a test sighting:

```bash
curl -X POST 'http://localhost:8081/api/debug/sighting?species=Bird&confidence=0.91'
```

## Hardware

| Item                | Qty | Est. Cost  | Notes                       |
| ------------------- | --- | ---------- | --------------------------- |
| Raspberry Pi 4/5    | 1   | BYOD       | 64-bit OS                   |
| MicroSD + PSU       | 1   | BYOD       | Already owned               |
| USB Webcam (720p)   | 1   | $15–25     | Logitech C270 or equivalent |
| 3D-printed tray     | 1   | $1         | Single-piece, 30-min print  |
| M2.5 screws         | 2   | $0         | Pi mounting                 |
| **Total purchased** | —   | **$15–25** | —                           |

Enclosure: [`enclosure/tray.scad`](enclosure/tray.scad) — print in PLA, 0.3mm layer, 20% infill.

## Architecture

```
[USB Webcam] → [OpenCV capture loop (Virtual Thread)]
                     ↓
          [absdiff motion check]
                     ↓
          [MotionEvent] → [ONNX MobileNet-SSD inference]
                     ↓
          [DetectionDispatcher (JEP 488 primitive patterns)]
                     ↓
          [SQLite INSERT + image save]
                     ↓
          [SseEmitter.emit(fragment)] → [Browser EventSource]
                     ↓
          [HTML-over-the-wire DOM merge]
```

## Java 26 JEP Showcase

### JEP 485 — Stream Gatherers

Custom gatherers for the detection pipeline:

```java
// Filters consecutive same-label detections within a cooldown window
public static <T> Gatherer<T, ?, T> cooldown(
        Function<T, String> keyExtractor, Duration window, double minConfidence) {
    return Gatherer.ofSequential(
            State::new,
            (state, element, downstream) -> {
                String key = keyExtractor.apply(element);
                return state.allow(key, window) ? downstream.push(element) : true;
            });
}
```

```java
// Filters detections below confidence threshold
public static <T> Gatherer<T, ?, T> aboveThreshold(
        Function<T, Float> confidenceExtractor, float threshold) {
    return Gatherer.ofSequential(
            () -> null,
            (state, element, downstream) -> {
                float confidence = confidenceExtractor.apply(element);
                return confidence >= threshold ? downstream.push(element) : true;
            });
}
```

### JEP 488 — Primitive Type Patterns

Routes COCO class IDs and confidence thresholds without `if-else` chains:

```java
public String speciesFromCoco(int classId) {
    return switch (classId) {
        case 1  -> "Person";
        case 16 -> "Cat";
        case 17 -> "Dog";
        case 18 -> "Horse";
        case 19 -> "Sheep";
        case 20 -> "Cow";
        case 21 -> "Elephant";
        case 22 -> "Bear";
        case 23 -> "Zebra";
        case 24 -> "Giraffe";
        case int id when id >= 25 && id <= 80 -> "Object";
        default -> "Unknown";
    };
}

public boolean worthStoring(float confidence) {
    return switch ((int) (confidence * 100)) {
        case int c when c >= 30 -> true;
        default -> false;
    };
}
```

### JEP 491 — Virtual Threads

No explicit `ThreadPoolExecutor` anywhere:

```properties
# Spring Boot Tomcat serves every request on a virtual thread
server.tomcat.threads.virtual.enabled=true
```

```java
// Vision capture runs on its own virtual thread
@PostConstruct
public void start() {
    captureThread = Thread.startVirtualThread(this::runLoop);
}
```

Concurrent workloads — capture, ONNX inference (blocking native call), SQLite write, and multiple open SSE connections — all run on virtual threads.

### JEP 487 — Scoped Values

Context flows from motion detection through inference to the repository layer without parameter drilling or `ThreadLocal`:

```java
public static final ScopedValue<SightingContext> CTX = ScopedValue.newInstance();

ScopedValue.where(CTX, new SightingContext("backyard", Instant.now()))
    .run(() -> pipeline.process(frame));

// Inside service layer:
String cam = SightingContextHolder.get().cameraId();
```

## Technology Stack

| Layer      | Tech                                     | Why                                               |
| ---------- | ---------------------------------------- | ------------------------------------------------- |
| Language   | Java 26                                  | Contest requirement; 4 JEPs showcased             |
| Build      | Gradle                                   | `sourceCompatibility = JavaVersion.VERSION_26`    |
| Camera     | `org.openpnp:opencv`                     | Pre-built aarch64 native libs                     |
| Vision AI  | ONNX Runtime Java + MobileNet SSD (COCO) | One model, one inference call                     |
| Database   | SQLite + `sqlite-jdbc` + JdbcTemplate    | Zero-config, file-based, survives reboots         |
| Web        | Spring Boot 3.5 + Tomcat virtual threads | One-line VT activation                            |
| Frontend   | Thymeleaf + raw SSE                      | Server pushes HTML fragments; browser merges them |
| Formatting | palantir-java-format                     | Consistent style across all source                |

## SQLite Schema

```sql
CREATE TABLE sightings (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    detected_at TEXT NOT NULL,    -- ISO-8601
    species TEXT NOT NULL,
    confidence REAL NOT NULL,
    image_path TEXT NOT NULL
);

CREATE INDEX idx_sightings_time ON sightings(detected_at DESC);
```

## HTML-over-the-Wire SSE

The dashboard uses server-sent HTML fragments instead of JSON APIs:

1. Initial page is server-rendered Thymeleaf
2. `EventSource` receives HTML fragments pushed from the Pi
3. A 5-line JS snippet merges them into the DOM

No JSON APIs. No React. No polling. Just server-sent HTML fragments via Java Virtual Threads.

## Development

```bash
# Format all Java
./gradlew spotlessApply

# Run tests
./gradlew test

# Run with webcam
./gradlew bootRun

# Run without vision (debug API only)
./gradlew bootRun --args='--wildtype.vision.enabled=false'

# Inject test data
curl -X POST 'http://localhost:8081/api/debug/sighting?species=Bird&confidence=0.91'
```

## Pi Deployment

See [`PI_TRANSFER.md`](PI_TRANSFER.md) for SD card setup, Java 26 install, project transfer, and systemd auto-start.

## Pages

- `/` — Dashboard with live SSE updates, species badges, and sighting cards
- `/sightings/{id}` — Detail page for a single sighting with full-size image

## License

- **Code**: MIT
- **MobileNet-SSD model**: Apache 2.0 (ONNX Model Zoo)
- **OpenCV**: Apache 2.0
- **ONNX Runtime**: MIT
