package org.wildtype;

import java.time.Instant;

public record Sighting(long id, Instant detectedAt, String species, float confidence, String imagePath) {}
