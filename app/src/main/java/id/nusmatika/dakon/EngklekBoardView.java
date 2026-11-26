package id.nusmatika.dakon;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

/** Papan engklek: bidang berskala, ketukan pilihan, serta animasi lompatan gaco. */
public final class EngklekBoardView extends View {
    public interface Host { void onCellSelected(int cell); }
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF[] bounds = new RectF[EngklekNet.COLUMNS * EngklekNet.ROWS];
    private final Host host;
    private final Typeface font;
    private long selected, fixed;
    private boolean cuboid, editable;
    private int gaco = -1;
    private float jump, touchX, touchY;
    private ValueAnimator animation;

    public EngklekBoardView(Context context) { this(context, cell -> { }); }

    public EngklekBoardView(Context context, Host host) {
        super(context);
        this.host = host;
        font = EngklekUi.font(context);
        for (int cell = 0; cell < bounds.length; cell++) bounds[cell] = new RectF();
        setClickable(true);
        setFocusable(true);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
    }

    public void configure(boolean box, long mask, long locked, boolean canEdit) {
        boolean geometryChanged = cuboid != box;
        cuboid = box;
        selected = mask;
        fixed = locked;
        editable = canEdit;
        setContentDescription("Papan engklek, " + Long.bitCount(mask) + " dari 6 sisi dipilih. "
                + (box ? "Ukuran bidang dalam satuan. " : "Setiap kotak adalah persegi satu satuan. ")
                + (canEdit ? "Ketuk kotak, atau gunakan tombol Pilih kotak dengan daftar." : "Pola pengamatan."));
        if (geometryChanged) requestLayout();
        invalidate();
    }

    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        int width = MeasureSpec.getSize(widthSpec);
        float unit = (width - EngklekUi.dp(getContext(), 44)) / (cuboid ? 12f : 5f);
        int height = Math.round(unit * (cuboid ? 9f : 6f)) + EngklekUi.dp(getContext(), 48);
        setMeasuredDimension(width, height);
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float left = EngklekUi.dp(getContext(), 30), top = EngklekUi.dp(getContext(), 28);
        float unit = (getWidth() - EngklekUi.dp(getContext(), 44)) / (cuboid ? 12f : 5f);
        float y = top;
        int colorIndex = 0;
        for (int row = 0; row < EngklekNet.ROWS; row++) {
            float x = left;
            int first = EngklekNet.index(0, row);
            float height = unit * EngklekNet.height(cuboid, first);
            text(canvas, String.valueOf((char) ('A' + row)), left / 2f, y + height / 2f, 12, EngklekUi.MUTED);
            for (int column = 0; column < EngklekNet.COLUMNS; column++) {
                int cell = EngklekNet.index(column, row);
                float width = unit * EngklekNet.width(cuboid, cell);
                bounds[cell].set(x, y, x + width, y + height);
                if (row == 0) text(canvas, String.valueOf(column + 1), x + width / 2, top / 2, 12, EngklekUi.MUTED);
                boolean chosen = EngklekNet.contains(selected, cell);
                boolean locked = EngklekNet.contains(fixed, cell) && editable;
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(locked ? 0xFFCCE6FF : chosen
                        ? EngklekUi.FACE_COLORS[colorIndex++ % 6] : 0xFFF5F7FA);
                canvas.drawRect(bounds[cell], paint);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(EngklekUi.dp(getContext(), locked ? 2 : 1));
                paint.setColor(locked ? EngklekUi.BLUE : 0xFFB7C6D7);
                canvas.drawRect(bounds[cell], paint);
                String label = cuboid ? EngklekNet.width(true, cell) + "×" + EngklekNet.height(true, cell)
                        : chosen ? "✓" : "";
                text(canvas, label, x + width / 2, y + height / 2, 12, EngklekUi.INK);
                x += width;
            }
            y += height;
        }
        if (gaco >= 0 && EngklekNet.contains(selected, gaco)) {
            RectF square = bounds[gaco];
            float radius = Math.min(unit * .18f, EngklekUi.dp(getContext(), 9));
            float centerY = square.top + radius + 2 - (float) Math.sin(jump * Math.PI) * unit * .45f;
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(0xFFE86435);
            canvas.drawCircle(square.right - radius - 3, centerY, radius, paint);
        }
    }

    private void text(Canvas canvas, String text, float x, float y, int size, int color) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        paint.setTypeface(font);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(EngklekUi.dp(getContext(), size));
        canvas.drawText(text, x, y - (paint.ascent() + paint.descent()) / 2, paint);
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        if (!editable) return super.onTouchEvent(event);
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            touchX = event.getX();
            touchY = event.getY();
            return true;
        }
        if (event.getActionMasked() == MotionEvent.ACTION_UP) {
            float slop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
            if (Math.hypot(event.getX() - touchX, event.getY() - touchY) > slop) return true;
            for (int cell = 0; cell < bounds.length; cell++) {
                if (!bounds[cell].contains(event.getX(), event.getY())) continue;
                performClick();
                host.onCellSelected(cell);
                gaco = cell;
                hop();
                break;
            }
            return true;
        }
        return true;
    }

    @Override public boolean performClick() { super.performClick(); return true; }

    private void hop() {
        if (animation != null) animation.cancel();
        animation = ValueAnimator.ofFloat(0, 1);
        animation.setDuration(320);
        animation.addUpdateListener(value -> { jump = (float) value.getAnimatedValue(); invalidate(); });
        animation.start();
    }
    @Override protected void onDetachedFromWindow() {
        if (animation != null) animation.cancel();
        super.onDetachedFromWindow();
    }
}
