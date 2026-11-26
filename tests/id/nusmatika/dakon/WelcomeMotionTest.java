package id.nusmatika.dakon;

/** Standalone tests for the welcome animation's frame-independent timing. */
public final class WelcomeMotionTest {
    private static int assertions;
    private static int failures;

    private WelcomeMotionTest() { }

    public static void main(String[] args) {
        openingHoldsThenFinishesOnce();
        itemsAppearInOrderWithoutOvershoot();
        easingMeetsItsBoundariesSmoothly();
        travelHasOnlyAGentlePositionOvershoot();
        idlePhaseWrapsSafelyEveryTwelveSeconds();
        bookGroundFollowsTheOpening();
        reducedMotionIsFullyRevealedAndStill();
        if (failures != 0) {
            throw new AssertionError(failures + " of " + assertions + " assertions failed");
        }
        System.out.println("WelcomeMotion: " + assertions + " assertions passed");
    }

    private static void openingHoldsThenFinishesOnce() {
        // A timer that starts early, restarts with the idle loop, or overflows fails here.
        near("negative opening", 0f, WelcomeMotion.openProgress(Long.MIN_VALUE, false));
        near("first frame closed", 0f, WelcomeMotion.openProgress(0L, false));
        near("opening hold", 0f, WelcomeMotion.openProgress(599L, false));
        near("opening begins closed", 0f, WelcomeMotion.openProgress(600L, false));
        near("opening midpoint", .5f, WelcomeMotion.openProgress(1450L, false));
        near("opening complete", 1f, WelcomeMotion.openProgress(2300L, false));
        near("opening stays complete", 1f, WelcomeMotion.openProgress(2301L, false));
        near("opening does not repeat", 1f, WelcomeMotion.openProgress(12000L, false));
        near("opening still does not repeat", 1f, WelcomeMotion.openProgress(24000L, false));
        near("largest opening time", 1f, WelcomeMotion.openProgress(Long.MAX_VALUE, false));
        float previous = 0f;
        for (long millis = 0L; millis <= 6000L; millis += 25L) {
            float progress = WelcomeMotion.openProgress(millis, false);
            check("opening remains bounded", progress >= 0f && progress <= 1f);
            check("opening never moves backwards", progress >= previous);
            previous = progress;
        }
        // Reading an older frame must not mutate a later frame's result.
        WelcomeMotion.openProgress(1450L, false);
        near("opening is independent of sampling order", 1f,
                WelcomeMotion.openProgress(24000L, false));
    }

    private static void itemsAppearInOrderWithoutOvershoot() {
        // Wrong staggering or reuse of positional back-easing breaks these alpha/scale values.
        near("first item hold", 0f, WelcomeMotion.itemProgress(1149L, 0, false));
        near("first item begins hidden", 0f, WelcomeMotion.itemProgress(1150L, 0, false));
        near("first item midpoint", .5f, WelcomeMotion.itemProgress(1650L, 0, false));
        near("first item complete", 1f, WelcomeMotion.itemProgress(2150L, 0, false));
        near("second item begins later", 0f, WelcomeMotion.itemProgress(1230L, 1, false));
        near("second item midpoint", .5f, WelcomeMotion.itemProgress(1730L, 1, false));
        near("last supported item begins", 0f, WelcomeMotion.itemProgress(3070L, 24, false));
        near("last supported item midpoint", .5f, WelcomeMotion.itemProgress(3570L, 24, false));
        near("last supported item complete", 1f, WelcomeMotion.itemProgress(4070L, 24, false));
        near("negative item index clamps", .5f,
                WelcomeMotion.itemProgress(1650L, Integer.MIN_VALUE, false));
        near("large item index clamps", .5f,
                WelcomeMotion.itemProgress(3570L, Integer.MAX_VALUE, false));
        near("negative item time", 0f, WelcomeMotion.itemProgress(Long.MIN_VALUE, 0, false));
        near("largest item time", 1f, WelcomeMotion.itemProgress(Long.MAX_VALUE, 24, false));
        for (int item = 0; item <= 24; item++) {
            float previous = 0f;
            for (long millis = 0L; millis <= 5000L; millis += 25L) {
                float progress = WelcomeMotion.itemProgress(millis, item, false);
                check("item remains bounded", progress >= 0f && progress <= 1f);
                check("item never moves backwards", progress >= previous);
                previous = progress;
            }
            near("every item finishes only once", 1f,
                    WelcomeMotion.itemProgress(24000L, item, false));
        }
        check("earlier item leads next item",
                WelcomeMotion.itemProgress(1650L, 0, false)
                        > WelcomeMotion.itemProgress(1650L, 1, false));
    }

    private static void easingMeetsItsBoundariesSmoothly() {
        // Linear interpolation, a step, or nonzero end slopes would create a visible snap.
        check("opening starts gently", WelcomeMotion.openProgress(601L, false) < .000001f);
        check("opening finishes gently", 1f - WelcomeMotion.openProgress(2299L, false) < .000001f);
        check("item starts gently", WelcomeMotion.itemProgress(1151L, 0, false) < .000001f);
        check("item finishes gently", 1f - WelcomeMotion.itemProgress(2149L, 0, false) < .000001f);
        near("opening early smootherstep", .103515625f, WelcomeMotion.openProgress(1025L, false));
        near("opening late smootherstep", .896484375f, WelcomeMotion.openProgress(1875L, false));
        near("item early smootherstep", .103515625f, WelcomeMotion.itemProgress(1400L, 0, false));
        near("item late smootherstep", .896484375f, WelcomeMotion.itemProgress(1900L, 0, false));
    }

    private static void travelHasOnlyAGentlePositionOvershoot() {
        // Unclamped/invalid input or a stronger back curve would throw artwork off its path.
        near("travel starts exactly", 0f, WelcomeMotion.travel(0f));
        near("travel ends exactly", 1f, WelcomeMotion.travel(1f));
        near("travel clamps negative", 0f, WelcomeMotion.travel(-1f));
        near("travel clamps large", 1f, WelcomeMotion.travel(2f));
        near("travel handles NaN", 0f, WelcomeMotion.travel(Float.NaN));
        near("travel handles negative infinity", 0f, WelcomeMotion.travel(Float.NEGATIVE_INFINITY));
        near("travel handles positive infinity", 1f, WelcomeMotion.travel(Float.POSITIVE_INFINITY));
        near("travel early back curve", .7328125f, WelcomeMotion.travel(.25f));
        near("travel middle back curve", 1.0125f, WelcomeMotion.travel(.5f));
        near("travel late back curve", 1.0359375f, WelcomeMotion.travel(.75f));
        float maximum = 0f;
        for (int sample = 0; sample <= 1000; sample++) {
            float value = WelcomeMotion.travel(sample / 1000f);
            check("travel remains finite and bounded", value >= 0f && value <= 1.06f);
            maximum = Math.max(maximum, value);
        }
        check("travel has a visible gentle overshoot", maximum > 1.04f && maximum < 1.05f);
        check("travel starts continuously", WelcomeMotion.travel(.001f) < .005f);
        check("travel finishes continuously", Math.abs(WelcomeMotion.travel(.999f) - 1f) < .00001f);
    }

    private static void idlePhaseWrapsSafelyEveryTwelveSeconds() {
        // Casting huge times to float before modulo would lose phase or yield invalid values.
        near("negative idle phase", 0f, WelcomeMotion.loopSeconds(Long.MIN_VALUE, false));
        near("zero idle phase", 0f, WelcomeMotion.loopSeconds(0L, false));
        near("one millisecond idle phase", .001f, WelcomeMotion.loopSeconds(1L, false));
        near("last idle frame before wrap", 11.999f, WelcomeMotion.loopSeconds(11999L, false));
        near("idle wraps exactly", 0f, WelcomeMotion.loopSeconds(12000L, false));
        near("idle repeats", 1.234f, WelcomeMotion.loopSeconds(13234L, false));
        near("idle repeats again", 1.234f, WelcomeMotion.loopSeconds(25234L, false));
        near("largest idle time remains precise", 7.807f,
                WelcomeMotion.loopSeconds(Long.MAX_VALUE, false));
        for (long millis = 0L; millis <= 48000L; millis += 37L) {
            float phase = WelcomeMotion.loopSeconds(millis, false);
            check("idle phase remains in its cycle", phase >= 0f && phase < 12f);
            near("idle is periodic", phase, WelcomeMotion.loopSeconds(millis + 12000L, false));
        }
        double angleBeforeWrap = WelcomeMotion.loopSeconds(11999L, false) * Math.PI / 6d;
        check("idle sine crosses wrap continuously", Math.abs(Math.sin(angleBeforeWrap)) < .001d);
        check("idle cosine crosses wrap continuously", Math.abs(Math.cos(angleBeforeWrap) - 1d) < .000001d);
    }

    private static void bookGroundFollowsTheOpening() {
        // The closed cover and the open book must stay on their corresponding ground lines.
        near("negative book ground", 470f, WelcomeMotion.bookGround(Long.MIN_VALUE, false));
        near("closed book ground", 470f, WelcomeMotion.bookGround(600L, false));
        near("opening book ground midpoint", 511f, WelcomeMotion.bookGround(1450L, false));
        near("open book ground", 552f, WelcomeMotion.bookGround(2300L, false));
        near("open book stays grounded", 552f, WelcomeMotion.bookGround(Long.MAX_VALUE, false));
        float previous = 470f;
        for (long millis = 0L; millis <= 6000L; millis += 25L) {
            float ground = WelcomeMotion.bookGround(millis, false);
            check("book ground remains bounded", ground >= 470f && ground <= 552f);
            check("book ground never moves backwards", ground >= previous);
            previous = ground;
        }
    }

    private static void reducedMotionIsFullyRevealedAndStill() {
        // Reduced motion must never wait for, open, or loop through the animated intro.
        long[] times = {Long.MIN_VALUE, -1L, 0L, 600L, 1450L, 2300L, 12000L, Long.MAX_VALUE};
        int[] items = {Integer.MIN_VALUE, 0, 1, 24, Integer.MAX_VALUE};
        for (long millis : times) {
            near("reduced motion open", 1f, WelcomeMotion.openProgress(millis, true));
            near("reduced motion grounded", 552f, WelcomeMotion.bookGround(millis, true));
            near("reduced motion idle stopped", 0f, WelcomeMotion.loopSeconds(millis, true));
            for (int item : items) {
                near("reduced motion item visible", 1f, WelcomeMotion.itemProgress(millis, item, true));
            }
        }
    }

    private static void near(String description, float expected, float actual) {
        check(description + ": expected " + expected + ", got " + actual,
                Math.abs(expected - actual) <= .00001f);
    }

    private static void check(String description, boolean condition) {
        assertions++;
        if (!condition) {
            failures++;
            if (failures <= 20) {
                System.err.println("FAIL: " + description);
            }
        }
    }
}
