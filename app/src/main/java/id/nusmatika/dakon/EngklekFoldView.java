package id.nusmatika.dakon;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Pratinjau 3D: setiap bidang berputar pada engsel rusuknya, bukan gambar pengganti. */
public final class EngklekFoldView extends View {
    public interface Host { void onFoldFinished(boolean folded); }
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Host host;
    private long mask = -1;
    private boolean cuboid;
    private float fraction;
    private ValueAnimator animation;
    private EngklekNet.Result result;

    public EngklekFoldView(Context context) { this(context, closed -> { }); }

    public EngklekFoldView(Context context, Host host) {
        super(context);
        this.host = host;
        paint.setTypeface(EngklekUi.font(context));
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
    }
    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        setMeasuredDimension(MeasureSpec.getSize(widthSpec), EngklekUi.dp(getContext(), 240));
    }
    public void setPattern(long next, boolean box, boolean initiallyClosed) {
        if (mask == next && cuboid == box) return;
        stop();
        mask = next;
        cuboid = box;
        result = EngklekNet.inspect(mask, cuboid);
        fraction = initiallyClosed ? 1 : 0;
        describe();
        invalidate();
    }
    public boolean closed() { return fraction > .5f; }
    public boolean moving() { return animation != null; }
    public void animateFold(boolean closed) {
        stop();
        if (result == null || result.faces.size() != 6) return;
        ValueAnimator value = ValueAnimator.ofFloat(fraction, closed ? 1 : 0);
        animation = value;
        value.setDuration(1300);
        value.addUpdateListener(frame -> { fraction = (float) frame.getAnimatedValue(); invalidate(); });
        value.addListener(new AnimatorListenerAdapter() {
            private boolean cancelled;
            @Override public void onAnimationCancel(Animator animator) { cancelled = true; }
            @Override public void onAnimationEnd(Animator animator) {
                animation = null;
                describe();
                if (!cancelled) host.onFoldFinished(closed);
            }
        });
        value.start();
    }
    public void stop() {
        if (animation != null) animation.cancel();
        animation = null;
    }
    private void describe() {
        setContentDescription("Pratinjau " + (cuboid ? "balok" : "kubus")
                + (closed() ? " terlipat. " : " terbuka. ")
                + (result != null ? result.message : ""));
    }

    private static final class V {
        final double x, y, z;
        V(double x, double y, double z) { this.x = x; this.y = y; this.z = z; }
        V plus(V v) { return new V(x + v.x, y + v.y, z + v.z); }
        V times(double scalar) { return new V(x * scalar, y * scalar, z * scalar); }
        V cross(V v) { return new V(y * v.z - z * v.y, z * v.x - x * v.z, x * v.y - y * v.x); }
        double dot(V v) { return x * v.x + y * v.y + z * v.z; }
        V rotate(V axis, double angle) {
            return times(Math.cos(angle)).plus(axis.cross(this).times(Math.sin(angle)))
                    .plus(axis.times(axis.dot(this) * (1 - Math.cos(angle))));
        }
    }
    private static final class Panel {
        int cell;
        V origin, across, down;
        V[] vertices;
        double depth;
    }

    private List<Panel> panels() {
        List<Panel> panels = new ArrayList<>();
        Map<Integer, Panel> indexed = new HashMap<>();
        double angle = fraction * Math.PI / 2;
        for (EngklekNet.Face face : result.faces) {
            Panel panel = new Panel();
            panel.cell = face.cell;
            Panel parent = indexed.get(face.parent);
            if (parent == null) {
                panel.origin = new V(0, 0, 0);
                panel.across = new V(1, 0, 0);
                panel.down = new V(0, 1, 0);
            } else {
                int direction = face.direction;
                panel.across = direction < 2 ? parent.across.rotate(parent.down,
                        direction == 0 ? angle : -angle) : parent.across;
                panel.down = direction >= 2 ? parent.down.rotate(parent.across,
                        direction == 2 ? -angle : angle) : parent.down;
                if (direction == 0) panel.origin = parent.origin.plus(parent.across.times(
                        EngklekNet.width(cuboid, parent.cell)));
                else if (direction == 1) panel.origin = parent.origin.plus(panel.across.times(
                        -EngklekNet.width(cuboid, panel.cell)));
                else if (direction == 2) panel.origin = parent.origin.plus(parent.down.times(
                        EngklekNet.height(cuboid, parent.cell)));
                else panel.origin = parent.origin.plus(panel.down.times(-EngklekNet.height(cuboid, panel.cell)));
            }
            V width = panel.across.times(EngklekNet.width(cuboid, panel.cell));
            V height = panel.down.times(EngklekNet.height(cuboid, panel.cell));
            panel.vertices = new V[]{panel.origin, panel.origin.plus(width),
                    panel.origin.plus(width).plus(height), panel.origin.plus(height)};
            V center = panel.origin.plus(width.times(.5)).plus(height.times(.5));
            panel.depth = center.dot(new V(.45, -.463, .783));
            panels.add(panel);
            indexed.put(face.cell, panel);
        }
        return panels;
    }
    private double px(V vertex) { return vertex.x * .87 - vertex.z * .50; }
    private double py(V vertex) { return vertex.y * .90 + vertex.x * .23 + vertex.z * .40; }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        paint.setColor(0xFFF5F8FC);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(0, 0, getWidth(), getHeight(), 24, 24, paint);
        if (result == null || result.faces.size() != 6) {
            label(canvas, "Pilih enam sisi yang tersambung untuk melihat lipatan.", getWidth() / 2f,
                    getHeight() / 2f, 11, EngklekUi.MUTED);
            return;
        }
        List<Panel> panels = panels();
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        for (Panel panel : panels) for (V vertex : panel.vertices) {
            minX = Math.min(minX, px(vertex)); maxX = Math.max(maxX, px(vertex));
            minY = Math.min(minY, py(vertex)); maxY = Math.max(maxY, py(vertex));
        }
        float inset = EngklekUi.dp(getContext(), 24);
        double scale = Math.min((getWidth() - inset * 2) / Math.max(.1, maxX - minX),
                (getHeight() - inset * 3) / Math.max(.1, maxY - minY));
        double offsetX = getWidth() / 2f - (minX + maxX) * .5 * scale;
        double offsetY = (getHeight() - inset) / 2f - (minY + maxY) * .5 * scale;
        panels.sort(Comparator.comparingDouble(panel -> panel.depth));
        for (Panel panel : panels) {
            Path path = new Path();
            for (int corner = 0; corner < 4; corner++) {
                float x = (float) (offsetX + px(panel.vertices[corner]) * scale);
                float y = (float) (offsetY + py(panel.vertices[corner]) * scale);
                if (corner == 0) path.moveTo(x, y); else path.lineTo(x, y);
            }
            path.close();
            int order = Long.bitCount(mask & ((1L << panel.cell) - 1));
            paint.setColor(EngklekUi.FACE_COLORS[order % 6]);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawPath(path, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(EngklekUi.dp(getContext(), 1.5f));
            paint.setColor(!result.valid && fraction > .8f ? 0xFFCB374F : EngklekUi.INK);
            canvas.drawPath(path, paint);
        }
        String state = Math.round(fraction * 90) + "° · " + (fraction < .01f ? "jaring-jaring terbuka"
                : fraction < .99f ? "melipat pada rusuk" : result.valid ? "bangun tertutup" : "lipatan gagal");
        label(canvas, state, getWidth() / 2f, getHeight() - inset / 2, 13,
                fraction > .99f && !result.valid ? 0xFFCB374F : EngklekUi.INK);
    }
    private void label(Canvas canvas, String text, float x, float y, int size, int color) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(EngklekUi.dp(getContext(), size));
        canvas.drawText(text, x, y, paint);
    }
    @Override protected void onDetachedFromWindow() { stop(); super.onDetachedFromWindow(); }
}
