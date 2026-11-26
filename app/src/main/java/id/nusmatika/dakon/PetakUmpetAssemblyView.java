package id.nusmatika.dakon;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.ScrollView;

import static id.nusmatika.dakon.PetakUmpetAssemblyRenderer.HEIGHT;
import static id.nusmatika.dakon.PetakUmpetAssemblyRenderer.LEFT;
import static id.nusmatika.dakon.PetakUmpetAssemblyRenderer.TOP;
import static id.nusmatika.dakon.PetakUmpetAssemblyRenderer.UNIT;
import static id.nusmatika.dakon.PetakUmpetAssemblyRenderer.WIDTH;

/** Menangani drag, magnet, pelepasan, gulir otomatis, keyboard dan aksesibilitas. */
public final class PetakUmpetAssemblyView extends View {
    interface Host {
        void onAssemblyChanged(String message);
        void onAssemblySelectionChanged();
        void onAssemblyRotateRequested();
        boolean isAssemblyComplete();
    }

    private static final int NEXT = 0x01000001, MOVE_LEFT = NEXT + 1,
            MOVE_RIGHT = NEXT + 2, MOVE_UP = NEXT + 3, MOVE_DOWN = NEXT + 4, ROTATE = NEXT + 5;
    private final PetakUmpetAssemblyModel model;
    private final PetakUmpetAssemblyRenderer renderer;
    private final Host host;
    private ScrollView scroll;
    private int active = -1, pointer = -1;
    private float downX, downY, grabX, grabY, rawX, rawY, screenX, screenY;
    private boolean moved, sticky;
    private final float touchSlop;
    private final Runnable autoScroll = new Runnable() {
        @Override public void run() {
            if (active < 0 || !moved || scroll == null) return;
            android.graphics.Rect visible = new android.graphics.Rect();
            if (!scroll.getGlobalVisibleRect(visible)) return;
            int step = screenY < visible.top + dp(64) ? -dp(8)
                    : screenY > visible.bottom - dp(64) ? dp(8) : 0;
            if (step == 0) return;
            int before = scroll.getScrollY();
            scroll.scrollBy(0, step);
            if (before != scroll.getScrollY()) {
                int[] location = new int[2];
                getLocationOnScreen(location);
                updateDrag((screenX - location[0]) / scale(), (screenY - location[1]) / scale());
                postDelayed(this, 16);
            }
        }
    };
    public PetakUmpetAssemblyView(Context context, PetakUmpetAssemblyModel model,
            Typeface font, Typeface headingFont, Host host) {
        super(context);
        this.model = model;
        this.host = host;
        renderer = new PetakUmpetAssemblyRenderer(model, font, headingFont);
        setFocusable(true); setClickable(true);
        setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }
    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        int width = MeasureSpec.getSize(widthSpec);
        setMeasuredDimension(width, Math.round(width * HEIGHT / WIDTH));
    }
    private float scale() { return Math.max(1, getWidth()) / WIDTH; }
    void setScrollContainer(ScrollView scroll) { this.scroll = scroll; }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        c.save();
        c.scale(scale(), scale());
        renderer.draw(c, active, moved, sticky, rawX, rawY);
        c.restore();
    }

    private void updateDrag(float px, float py) {
        rawX = (px - LEFT) / UNIT - grabX;
        rawY = (py - TOP) / UNIT - grabY;
        boolean magnet = model.magnet(active, rawX, rawY);
        if (moved && magnet && !sticky) performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
        sticky = magnet;
        invalidate();
    }
    @Override public boolean onTouchEvent(MotionEvent event) {
        if (host.isAssemblyComplete()) return false;
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            float px = event.getX() / scale(), py = event.getY() / scale();
            active = renderer.hit(px, py);
            if (active < 0) return false;
            model.selected = active; pointer = event.getPointerId(0);
            downX = px; downY = py; moved = false; sticky = false;
            grabX = model.placed[active] ? (px - renderer.face(active).left) / UNIT : model.w[active] / 2f;
            grabY = model.placed[active] ? (py - renderer.face(active).top) / UNIT : model.h[active] / 2f;
            getParent().requestDisallowInterceptTouchEvent(true);
            host.onAssemblySelectionChanged(); return true;
        }
        if (active < 0) return false;
        if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_POINTER_UP
                && event.getPointerId(event.getActionIndex()) == pointer) {
            cancelGesture(); host.onAssemblySelectionChanged(); return true;
        }
        int index = event.findPointerIndex(pointer);
        if (index < 0) { cancelGesture(); return true; }
        float px = event.getX(index) / scale(), py = event.getY(index) / scale();
        if (action == MotionEvent.ACTION_MOVE) {
            if (!moved && Math.hypot(px - downX, py - downY) * scale() >= touchSlop) {
                moved = true;
                host.onAssemblyChanged("");
            }
            screenX = event.getRawX(); screenY = event.getRawY();
            updateDrag(px, py);
            removeCallbacks(autoScroll);
            if (moved) postOnAnimation(autoScroll);
            return true;
        }
        if (action == MotionEvent.ACTION_UP) {
            if (moved) {
                updateDrag(px, py);
                String message;
                if (renderer.isInTray(px, py)) {
                    model.placed[active] = false;
                    message = "Bangun kembali ke tempat temuan.";
                } else {
                    int gx = Math.round(rawX), gy = Math.round(rawY);
                    boolean close = rawX >= -.5f && rawY >= -.5f
                            && rawX + model.w[active] <= PetakUmpetAssemblyModel.COLS + .5f
                            && rawY + model.h[active] <= PetakUmpetAssemblyModel.ROWS + .5f;
                    boolean placed = close && model.place(active, gx, gy);
                    message = !placed ? "Tempat itu penuh atau di luar papan. Coba geser ke ruang kosong."
                            : model.adjacent(active, gx, gy) ? "Menempel! Bangun tetap bisa ditarik lepas."
                            : "Bangun diletakkan. Dekatkan sisinya ke bangun lain.";
                }
                cancelGesture();
                host.onAssemblyChanged(message);
            } else {
                performClick();
                cancelGesture();
                host.onAssemblySelectionChanged();
            }
            return true;
        }
        return true;
    }
    @Override public boolean performClick() { super.performClick(); return true; }
    void cancelGesture() {
        removeCallbacks(autoScroll);
        active = -1; pointer = -1; moved = false; sticky = false;
        if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(false);
        invalidate();
    }
    private boolean moveSelected(int dx, int dy) {
        if (model.selected < 0) model.selected = 0;
        int i = model.selected;
        if (!model.placed[i]) {
            for (int y = 0; y < PetakUmpetAssemblyModel.ROWS; y++) for (int x = 0; x < PetakUmpetAssemblyModel.COLS; x++)
                if (model.place(i, x, y)) { host.onAssemblyChanged("Bangun masuk ke papan."); return true; }
        } else if (model.place(i, model.x[i] + dx, model.y[i] + dy)) {
            host.onAssemblyChanged("Bangun digeser."); return true;
        }
        return false;
    }
    @Override public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        String[] labels = {"Pilih bangun berikutnya", "Geser ke kiri", "Geser ke kanan",
                "Geser ke atas", "Geser ke bawah", "Putar bangun"};
        for (int i = 0; i < labels.length; i++)
            info.addAction(new AccessibilityNodeInfo.AccessibilityAction(NEXT + i, labels[i]));
    }
    @Override public boolean performAccessibilityAction(int action, Bundle arguments) {
        if (host.isAssemblyComplete()) return super.performAccessibilityAction(action, arguments);
        if (action == NEXT) {
            model.selected = (model.selected + 1) % PetakUmpetAssemblyModel.COUNT;
            host.onAssemblySelectionChanged(); announceForAccessibility("Bangun " + (model.selected + 1)
                    + (model.isBalok(model.selected) ? ", balok" : ", kubus")); return true;
        }
        if (action == ROTATE) { host.onAssemblyRotateRequested(); return true; }
        if (action >= MOVE_LEFT && action <= MOVE_DOWN)
            return moveSelected(action == MOVE_LEFT ? -1 : action == MOVE_RIGHT ? 1 : 0,
                    action == MOVE_UP ? -1 : action == MOVE_DOWN ? 1 : 0);
        return super.performAccessibilityAction(action, arguments);
    }
    @Override public boolean onKeyDown(int key, KeyEvent event) {
        if (key == KeyEvent.KEYCODE_DPAD_CENTER || key == KeyEvent.KEYCODE_ENTER)
            return performAccessibilityAction(NEXT, null);
        if (key == KeyEvent.KEYCODE_DPAD_LEFT) return moveSelected(-1, 0);
        if (key == KeyEvent.KEYCODE_DPAD_RIGHT) return moveSelected(1, 0);
        if (key == KeyEvent.KEYCODE_DPAD_UP) return moveSelected(0, -1);
        if (key == KeyEvent.KEYCODE_DPAD_DOWN) return moveSelected(0, 1);
        return super.onKeyDown(key, event);
    }
}