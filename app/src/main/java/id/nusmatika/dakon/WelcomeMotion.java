package id.nusmatika.dakon;

/** Frame-independent motion values for the native welcome artwork. */
public final class WelcomeMotion {
    private WelcomeMotion() { }

    /** Opens once after a 600 ms hold, settling fully open at 2300 ms. */
    public static float openProgress(long elapsedMillis, boolean reducedMotion) {
        return reducedMotion ? 1f : reveal(elapsedMillis, 600L, 1700L);
    }

    /** Reveals item 0..24 over one second, with 80 ms between item starts. */
    public static float itemProgress(long elapsedMillis, int itemIndex, boolean reducedMotion) {
        if (reducedMotion) {
            return 1f;
        }
        int index = Math.max(0, Math.min(24, itemIndex));
        return reveal(elapsedMillis, 1150L + index * 80L, 1000L);
    }

    /** Gentle ease-out-back for position only; reveal values remain bounded at one. */
    public static float travel(float progress) {
        float bounded = clampFinite(progress);
        if (bounded == 0f || bounded == 1f) {
            return bounded;
        }
        double shifted = bounded - 1d;
        return (float) (1d + 2.1d * shifted * shifted * shifted
                + 1.1d * shifted * shifted);
    }

    /** Twelve-second idle phase; modulo is applied before conversion to float. */
    public static float loopSeconds(long elapsedMillis, boolean reducedMotion) {
        if (reducedMotion || elapsedMillis <= 0L) {
            return 0f;
        }
        return (elapsedMillis % 12000L) / 1000f;
    }

    /** Moves the book's ground from the closed cover to the open spread. */
    public static float bookGround(long elapsedMillis, boolean reducedMotion) {
        return 470f + 82f * openProgress(elapsedMillis, reducedMotion);
    }

    private static float reveal(long elapsedMillis, long startMillis, long durationMillis) {
        // Check the limits before subtraction so extreme elapsed values cannot overflow.
        if (elapsedMillis <= startMillis) {
            return 0f;
        }
        if (elapsedMillis >= startMillis + durationMillis) {
            return 1f;
        }
        double progress = (double) (elapsedMillis - startMillis) / durationMillis;
        // Smootherstep has zero first and second derivatives at both ends.
        return (float) (progress * progress * progress
                * (progress * (progress * 6d - 15d) + 10d));
    }

    private static float clampFinite(float progress) {
        if (Float.isNaN(progress) || progress <= 0f) {
            return 0f;
        }
        return progress >= 1f ? 1f : progress;
    }
}
