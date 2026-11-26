package id.nusmatika.dakon;

import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;

/**
 * Original, resolution-independent opening storybook for the welcome scene.
 *
 * Logical pixel bounds relative to (centerX, groundY): closed about
 * [-86,-175 .. 75,8] including the eight-degree tilt; fully open
 * [-130,-141 .. 130,27]. The unrotated closed cover is 132 x 156 pixels.
 * groundY is the book's scene anchor. The final diagonal page gutter runs
 * from (-20,-91) to (20,-20); contents can emerge around (0,-60).
 * The caller owns timing/easing and may animate groundY during the reveal.
 *
 * Both covers and the page leaves share one moving hinge. Their continuously
 * projected geometry turns through an edge-on pose rather than cross-fading
 * between a closed-book picture and an unrelated open-book picture.
 */
public final class WelcomeBookArtwork {
    private WelcomeBookArtwork() { }

    private static final int BLUE = 0xFF1CB0F6;
    private static final int BLUE_EDGE = 0xFF008CCB;
    private static final int BLUE_DARK = 0xFF0074AE;
    private static final int PAGE_EDGE = 0xFFE5CA8F;
    private static final int PAGE_MIDDLE = 0xFFFFE8B6;
    private static final int PAGE_LIGHT = 0xFFFFF8E4;
    private static final int PAGE_WARM = 0xFFFFF1CE;

    private static final ThreadLocal<Brushes> BRUSHES = new ThreadLocal<Brushes>() {
        @Override protected Brushes initialValue() { return new Brushes(); }
    };

    /**
     * Draws the complete book behind the scene's character and game pieces.
     * openProgress is clamped to 0..1. Reduced motion suppresses only the
     * subtle open-page flutter; the caller can pass 1 for an immediate reveal.
     * This method performs no per-frame object or bitmap allocation.
     */
    public static void draw(Canvas canvas, float centerX, float groundY,
                            float openProgress, float seconds, boolean reducedMotion) {
        if (canvas == null || !finite(centerX) || !finite(groundY)) return;
        float p = finite(openProgress) ? clamp(openProgress) : 0f;
        float time = finite(seconds) ? seconds : 0f;
        // A six-second period also joins seamlessly when the scene's
        // twelve-second idle clock wraps back to zero.
        float flutter = reducedMotion ? 0f
                : (float) Math.sin(time * 1.0471975512f) * .8f * p * p * p * p * p * p;
        Brushes b = BRUSHES.get();
        b.alpha = 255;
        b.depth = p;
        int save = canvas.save();
        canvas.translate(centerX, groundY);
        canvas.rotate(-8f * (1f - p), 0f, -6f);

        float hingeX = lerp(-64f, -20f, p);
        float top = lerp(-165f, -91f, p);
        float height = lerp(156f, 71f, p);
        float angle = (float) Math.PI * p;
        float turn = (float) Math.cos(angle);
        float lift = (float) Math.sin(angle);

        // The stationary back cover and the right-hand paper block.
        leafMatrix(b, hingeX + 3f * (1f - p), top + 7f,
                1f, -44f * p, height);
        cover(canvas, b, BLUE_DARK, false);
        leafMatrix(b, hingeX + 3f * (1f - p), top + 3f,
                1f, -44f * p, height);
        cover(canvas, b, BLUE, false);
        paperBlock(canvas, b, hingeX + 4f * (1f - p), top + 1f,
                1f, -44f * p + flutter, height, p, PAGE_WARM);

        // A cover rotates around the same gutter as the paper. The extra
        // sine term raises the fore-edge during the turn, giving real depth.
        // One shared oblique plane: right leaves recede up/right and the
        // turning left leaves settle down/left. This is a volume projection,
        // not a rotation of the previous frontal V-shaped spread.
        float coverDrop = -44f * p * turn - 48f * lift;
        if (turn <= 0f) {
            turningCover(canvas, b, hingeX, top, turn, coverDrop, height, false);
        }

        // Three leaves follow the front cover with a small stagger. These
        // are actual projected sheets, not particles or replacement sprites.
        for (int i = 0; i < 3; i++) {
            float delay = .035f + i * .035f;
            float leafP = clamp((p - delay) / (1f - delay));
            float leafAngle = (float) Math.PI * leafP;
            float leafTurn = (float) Math.cos(leafAngle);
            float leafLift = (float) Math.sin(leafAngle);
            float leafDrop = -44f * p * leafTurn - 33f * leafLift;
            int color = i == 2 ? PAGE_LIGHT : (i == 1 ? PAGE_MIDDLE : PAGE_EDGE);
            leafMatrix(b, hingeX, top + 1.5f + (2 - i) * 1.5f,
                    leafTurn * .965f, leafDrop - flutter * .7f,
                    height - 3f);
            page(canvas, b, p, color);
        }

        if (turn > 0f) {
            turningCover(canvas, b, hingeX, top, turn, coverDrop, height, true);
        }

        // The gutter follows the same projected depth axis as both pages.
        float reveal = clamp((p - .55f) / .45f);
        if (reveal > 0f) {
            b.alpha = Math.round(255f * reveal);
            float nearX = hingeX + 40f * p;
            float nearY = top + height;
            float middleX = (hingeX + nearX) * .5f;
            float middleY = (top + nearY) * .5f;
            b.path.reset();
            b.path.moveTo(hingeX - 1.5f, top + 1f);
            b.path.cubicTo(middleX - 9f, middleY - 15f,
                    nearX - 8f, nearY - 10f, nearX - 1f, nearY);
            b.path.quadTo(nearX + 2.5f, nearY + 3f, nearX + 5f, nearY - 2f);
            b.path.cubicTo(nearX, nearY - 15f, middleX + 4f, middleY - 13f,
                    hingeX + 2f, top + 1f);
            b.path.close();
            fill(canvas, b, 0xFFE7CD98);
            b.path.reset();
            b.path.moveTo(hingeX - 2f, top + 5f);
            b.path.cubicTo(middleX - 6f, middleY - 8f,
                    nearX - 6f, nearY - 10f, nearX - 1f, nearY - 5f);
            stroke(canvas, b, 0xFFFFFDF4, 2f);

            // A coral ribbon lays over the front edge, with a cut tail.
            b.path.reset();
            b.path.moveTo(nearX + 10f, nearY - 11f);
            b.path.cubicTo(nearX + 13f, nearY - 3f, nearX + 17f, nearY + 6f,
                    nearX + 17f, nearY + 15f);
            b.path.lineTo(nearX + 23f, nearY + 10f);
            b.path.lineTo(nearX + 29f, nearY + 13f);
            b.path.cubicTo(nearX + 28f, nearY + 3f, nearX + 24f, nearY - 6f,
                    nearX + 22f, nearY - 15f);
            b.path.close();
            fill(canvas, b, 0xFFFF6C70);
            b.path.reset();
            b.path.moveTo(nearX + 15f, nearY - 9f);
            b.path.quadTo(nearX + 20f, nearY + 1f, nearX + 21f, nearY + 9f);
            stroke(canvas, b, 0xFFFF9A94, 2f);
            b.alpha = 255;
        }

        canvas.restoreToCount(save);
    }

    private static void paperBlock(Canvas c, Brushes b, float x, float top,
                                   float turn, float drop, float height,
                                   float p, int faceColor) {
        leafMatrix(b, x, top + 6f, turn * .965f, drop, height - 3f);
        page(c, b, p, PAGE_EDGE);
        leafMatrix(b, x, top + 3.5f, turn * .965f, drop, height - 3f);
        page(c, b, p, PAGE_MIDDLE);
        leafMatrix(b, x, top, turn * .965f, drop, height - 3f);
        page(c, b, p, faceColor);
    }

    private static void turningCover(Canvas c, Brushes b, float x, float top,
                                      float turn, float drop, float height,
                                      boolean front) {
        leafMatrix(b, x, top + 6f, turn, drop, height);
        cover(c, b, BLUE_DARK, false);
        leafMatrix(b, x, top, turn, drop, height);
        cover(c, b, front ? BLUE : BLUE_EDGE, front);
    }

    // This affine projection uses full-size path coordinates, preserving
    // reliable path precision on Android hardware renderers.
    private static void leafMatrix(Brushes b, float x, float y, float turn,
                                   float drop, float height) {
        float[] v = b.values;
        // As the book lies down, x compresses while the common page-depth
        // axis projects down/right. Both leaves remain on the same plane.
        v[0] = turn * (1f - .2f * b.depth);
        v[1] = 40f * b.depth / 156f;
        v[2] = x;
        v[3] = drop / 132f; v[4] = height / 156f; v[5] = y;
        v[6] = 0f; v[7] = 0f; v[8] = 1f;
        b.matrix.setValues(v);
    }

    private static void cover(Canvas c, Brushes b, int color, boolean badge) {
        int save = c.save();
        c.concat(b.matrix);
        b.path.reset();
        b.path.moveTo(7f, 0f);
        b.path.lineTo(122f, 0f);
        b.path.quadTo(132f, 0f, 132f, 10f);
        b.path.lineTo(132f, 146f);
        b.path.quadTo(132f, 156f, 122f, 156f);
        b.path.lineTo(7f, 156f);
        b.path.quadTo(0f, 156f, 0f, 149f);
        b.path.lineTo(0f, 7f);
        b.path.quadTo(0f, 0f, 7f, 0f);
        b.path.close();
        fill(c, b, color);
        if (badge) {
            // A bound spine, a broad flat highlight and an original math seal.
            b.path.reset();
            b.path.moveTo(7f, 0f);
            b.path.lineTo(14f, 0f);
            b.path.lineTo(14f, 156f);
            b.path.lineTo(7f, 156f);
            b.path.quadTo(0f, 156f, 0f, 149f);
            b.path.lineTo(0f, 7f);
            b.path.quadTo(0f, 0f, 7f, 0f);
            b.path.close();
            fill(c, b, BLUE_EDGE);
            b.path.reset();
            b.path.moveTo(25f, 13f);
            b.path.lineTo(114f, 13f);
            stroke(c, b, 0xFF67CDFF, 4f);
            b.path.reset();
            b.path.moveTo(22f, 143f);
            b.path.lineTo(113f, 143f);
            stroke(c, b, 0xFF079FE6, 4f);
            b.paint.setColor(0xFF008CCB);
            c.drawCircle(75f, 79f, 35f, b.paint);
            b.paint.setColor(0xFFFFF9DF);
            c.drawCircle(75f, 74f, 35f, b.paint);
            // Coral plus with large, friendly arms.
            b.path.reset();
            b.path.moveTo(58f, 59f);
            b.path.lineTo(69f, 59f);
            b.path.quadTo(72f, 59f, 72f, 62f);
            b.path.lineTo(72f, 69f);
            b.path.lineTo(79f, 69f);
            b.path.quadTo(82f, 69f, 82f, 72f);
            b.path.lineTo(82f, 80f);
            b.path.quadTo(82f, 83f, 79f, 83f);
            b.path.lineTo(72f, 83f);
            b.path.lineTo(72f, 90f);
            b.path.quadTo(72f, 93f, 69f, 93f);
            b.path.lineTo(61f, 93f);
            b.path.quadTo(58f, 93f, 58f, 90f);
            b.path.lineTo(58f, 83f);
            b.path.lineTo(51f, 83f);
            b.path.quadTo(48f, 83f, 48f, 80f);
            b.path.lineTo(48f, 72f);
            b.path.quadTo(48f, 69f, 51f, 69f);
            b.path.lineTo(58f, 69f);
            b.path.lineTo(58f, 59f);
            b.path.close();
            fill(c, b, 0xFFFF7474);
            b.path.reset();
            b.path.moveTo(87f, 54f);
            b.path.quadTo(89f, 50f, 91f, 54f);
            b.path.lineTo(100f, 69f);
            b.path.quadTo(102f, 73f, 98f, 73f);
            b.path.lineTo(80f, 73f);
            b.path.quadTo(76f, 73f, 78f, 69f);
            b.path.close();
            fill(c, b, 0xFF71CE70);
            b.paint.setColor(0xFFFFC63C);
            c.drawCircle(92f, 88f, 8f, b.paint);
            // A short ribbon peeks out of the closed page block.
            b.path.reset();
            b.path.moveTo(91f, 151f);
            b.path.lineTo(103f, 151f);
            b.path.lineTo(103f, 170f);
            b.path.lineTo(97f, 166f);
            b.path.lineTo(91f, 170f);
            b.path.close();
            fill(c, b, 0xFFFF6C70);
        }
        c.restoreToCount(save);
    }

    private static void page(Canvas c, Brushes b, float p, int color) {
        int save = c.save();
        c.concat(b.matrix);
        b.path.reset();
        b.path.moveTo(4f, 3f);
        b.path.cubicTo(35f, 3f - 18f * p, 89f, -20f * p, 123f, 3f);
        b.path.quadTo(128f, 4f, 128f, 11f);
        b.path.lineTo(124f, 145f);
        b.path.quadTo(124f, 151f, 117f, 151f);
        b.path.cubicTo(81f, 151f - 13f * p, 38f, 151f - 17f * p, 4f, 151f);
        b.path.quadTo(1f, 151f, 1f, 147f);
        b.path.lineTo(1f, 9f);
        b.path.quadTo(1f, 4f, 4f, 3f);
        b.path.close();
        fill(c, b, color);
        // Quiet paper contours have enough weight to survive the final
        // perspective compression without reading as tiny text.
        b.path.reset();
        b.path.moveTo(17f, 137f);
        b.path.cubicTo(47f, 137f - 14f * p, 82f, 137f - 10f * p, 111f, 137f);
        stroke(c, b, 0xFFEAD8AE, 2f);
        b.path.reset();
        b.path.moveTo(119f, 19f);
        b.path.lineTo(117f, 119f);
        stroke(c, b, 0xFFFFFDF3, 3f);
        c.restoreToCount(save);
    }

    private static void fill(Canvas c, Brushes b, int color) {
        b.paint.setStyle(Paint.Style.FILL);
        b.paint.setColor(color);
        b.paint.setAlpha(b.alpha);
        c.drawPath(b.path, b.paint);
    }

    private static void stroke(Canvas c, Brushes b, int color, float width) {
        b.paint.setStyle(Paint.Style.STROKE);
        b.paint.setStrokeWidth(width);
        b.paint.setColor(color);
        b.paint.setAlpha(b.alpha);
        c.drawPath(b.path, b.paint);
        b.paint.setStyle(Paint.Style.FILL);
    }

    private static float clamp(float v) { return Math.max(0f, Math.min(1f, v)); }
    private static float lerp(float a, float b, float p) { return a + (b - a) * p; }
    private static boolean finite(float v) { return !Float.isNaN(v) && !Float.isInfinite(v); }

    private static final class Brushes {
        final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        final Path path = new Path();
        final Matrix matrix = new Matrix();
        final float[] values = new float[9];
        int alpha = 255;
        float depth;

        Brushes() {
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeJoin(Paint.Join.ROUND);
        }
    }
}
