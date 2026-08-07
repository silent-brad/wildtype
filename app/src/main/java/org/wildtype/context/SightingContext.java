package org.wildtype.context;

import java.time.Instant;

public record SightingContext(String cameraId, Instant detectionStart) {}
