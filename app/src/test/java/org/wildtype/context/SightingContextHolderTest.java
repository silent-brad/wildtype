package org.wildtype.context;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class SightingContextHolderTest {

    @Test
    void contextFlowsThroughScopedValue() {
        SightingContextHolder.runWith("backyard-cam-01", () -> {
            SightingContext ctx = SightingContextHolder.get();
            assertEquals("backyard-cam-01", ctx.cameraId());
            assertNotNull(ctx.detectionStart());
        });
    }

    @Test
    void callWithReturnsValue() throws Exception {
        String result = SightingContextHolder.callWith("garden-cam", () -> {
            SightingContext ctx = SightingContextHolder.get();
            return ctx.cameraId() + "-" + ctx.detectionStart().toEpochMilli();
        });

        assertNotNull(result);
        assertTrue(result.startsWith("garden-cam-"));
    }
}
