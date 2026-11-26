package id.nusmatika.dakon;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

/** The same centered padlock artwork is used for every unavailable map level. */
public final class MapPadlockArtwork {
    private static final Path SHACKLE = createShackle();
    private static final ThreadLocal<Paint> BRUSH = new ThreadLocal<Paint>() {
        @Override protected Paint initialValue() {
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeJoin(Paint.Join.ROUND);
            return paint;
        }
    };

    private MapPadlockArtwork() { }

    public static void draw(Canvas canvas, float centerX, float centerY, float size,
                            int color, int cutoutColor) {
        if (canvas == null || !(size > 0) || Float.isInfinite(size)) return;
        int saved = canvas.save();
        canvas.translate(centerX, centerY);
        canvas.scale(size / 32f, size / 32f);
        canvas.translate(-16, -16);

        Paint paint = BRUSH.get();
        paint.setColor(color);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3.5f);
        canvas.drawPath(SHACKLE, paint);

        paint.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(5, 12, 27, 29, 4.5f, 4.5f, paint);
        paint.setColor(cutoutColor);
        canvas.drawCircle(16, 19.5f, 2.2f, paint);
        canvas.drawRoundRect(14.8f, 20.4f, 17.2f, 25, 1.2f, 1.2f, paint);
        canvas.restoreToCount(saved);
    }

    private static Path createShackle() {
        Path path = new Path();
        path.moveTo(9.5f, 14);
        path.lineTo(9.5f, 10.5f);
        path.cubicTo(9.5f, 6.9f, 12.4f, 4, 16, 4);
        path.cubicTo(19.6f, 4, 22.5f, 6.9f, 22.5f, 10.5f);
        path.lineTo(22.5f, 14);
        return path;
    }
}
