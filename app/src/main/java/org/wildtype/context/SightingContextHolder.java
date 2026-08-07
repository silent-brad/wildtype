package org.wildtype.context;

import java.time.Instant;

public class SightingContextHolder {

    public static final ScopedValue<SightingContext> CTX = ScopedValue.newInstance();

    private SightingContextHolder() {}

    public static void runWith(String cameraId, Runnable task) {
        ScopedValue.where(CTX, new SightingContext(cameraId, Instant.now())).run(task);
    }

    public static <T, X extends Throwable> T callWith(String cameraId, ScopedValue.CallableOp<T, X> task) throws X {
        return ScopedValue.where(CTX, new SightingContext(cameraId, Instant.now()))
                .call(task);
    }

    public static SightingContext get() {
        return CTX.get();
    }
}
