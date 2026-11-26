package id.nusmatika.dakon;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Shared approved path drawing and navigation for native learning courses. */
public abstract class LearningPathMapActivity extends Activity {
    protected abstract LearningPathStore createLearningStore();
    protected abstract String courseTitle();
    protected abstract int levelCount();
    protected abstract String levelTitle(int level);
    protected abstract String levelSubtitle(int level);
    protected abstract String levelDescription(int level);
    protected abstract Class<? extends Activity> lessonActivity();
    protected boolean sackRaceCourse() { return false; }
    protected boolean hiddenShapeCourse() { return false; }
    protected float nodeStep() { return 114f; }
    private float contentHeight() { return MAP_TOP + 100f + nodeStep() * levelCount(); }
    private boolean playableLevel(int level) { return level >= 0 && level < levelCount(); }

    private static final float DESIGN_WIDTH = 402f;
    private static final float HEADER_HEIGHT = 196f;
    private static final float MAP_TOP = 0f;
    private static final float NODE_WIDTH = 76f;
    private static final float NODE_HEIGHT = 64f;
    private static final float[] NODE_X = {.50f, .34f, .24f, .34f};
    private static final int PAPER = Color.WHITE;
    private static final int LINE = 0xFFE5E5E5;
    private static final int MUTED = 0xFF777777;
    private static final int GREEN_EDGE = 0xFF46A302;
    private static final int GREEN_INK = 0xFF4A9500;
    private static final int NODE_FACE = 0xFFE5E5E5;
    private static final int NODE_EDGE = 0xFFCECECE;
    private static final int LOCK_INK = 0xFFB7B7B7;
    private static final String SCROLL_STATE = "learningPathScroll";

    private Typeface font;
    private NaraRenderer nara;
    private ScrollView scroll;
    private MapLayout map;
    private HeaderLayout header;
    private Dialog detailDialog;
    private boolean launching;
    private boolean motionReduced;
    private long animationEpoch;
    private LearningPathStore learning;
    private String mapAction = "MULAI";

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        learning = createLearningStore();
        try {
            font = Typeface.createFromAsset(getAssets(), "fonts/Nunito.ttf");
        } catch (RuntimeException error) {
            font = Typeface.create("sans-serif-rounded", Typeface.NORMAL);
        }
        animationEpoch = SystemClock.uptimeMillis();
        nara = new NaraRenderer(this);
        motionReduced = reducedMotion();

        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setBackgroundColor(PAPER);
        header = new HeaderLayout(this);
        column.addView(header, new LinearLayout.LayoutParams(-1, -2));
        scroll = new ScrollView(this);
        scroll.setId(R.id.learning_path_scroll);
        scroll.setFillViewport(false);
        scroll.setClipToPadding(false);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
        map = new MapLayout(this);
        scroll.addView(map, new ScrollView.LayoutParams(-1, -2));
        column.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));

        FrameLayout safeArea = new FrameLayout(this);
        safeArea.setBackgroundColor(PAPER);
        safeArea.addView(column, new FrameLayout.LayoutParams(-1, -1));
        if (Build.VERSION.SDK_INT >= 30) getWindow().setDecorFitsSystemWindows(false);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        getWindow().setStatusBarColor(PAPER);
        getWindow().setNavigationBarColor(PAPER);
        safeArea.setOnApplyWindowInsetsListener((view, insets) -> {
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets bars = insets.getInsets(
                        WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            } else {
                view.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                        insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            }
            return insets;
        });
        setContentView(safeArea);
        safeArea.requestApplyInsets();
        positionInitialMap(state);
    }

    private void positionInitialMap(Bundle state) {
        final int savedScroll = state == null ? -1 : state.getInt(SCROLL_STATE, -1);
        scroll.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override public void onGlobalLayout() {
                        if (map.getWidth() == 0 || scroll.getHeight() == 0) return;
                        scroll.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        if (savedScroll >= 0) {
                            scroll.scrollTo(0, savedScroll);
                            return;
                        }
                        // Keep the current node and earlier route visible on short screens.
                        float activeBottom = (nodeY(learning.currentLevel()) + 58f)
                                * map.scale;
                        if (activeBottom > scroll.getHeight()) {
                            int offset = Math.round(nodeY(learning.currentLevel())
                                    * map.scale - scroll.getHeight() * .73f);
                            scroll.scrollTo(0, Math.max(0, offset));
                        }
                    }
                });
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        if (scroll != null) state.putInt(SCROLL_STATE, scroll.getScrollY());
        super.onSaveInstanceState(state);
    }

    @Override protected void onResume() {
        super.onResume();
        launching = false;
        learning = createLearningStore();
        int active = learning.currentLevel();
        mapAction = !learning.hasSession(active) ? "MULAI"
                : learning.isSessionComplete(active) ? "ULANGI" : "LANJUT";
        motionReduced = reducedMotion();
        if (nara == null) nara = new NaraRenderer(this);
        if (map != null) map.surface.setRunning(true);
        if (map != null) map.refreshDescriptions();
        if (header != null) header.surface.invalidate();
    }

    @Override protected void onPause() {
        if (map != null) map.surface.setRunning(false);
        super.onPause();
    }

    @Override protected void onStop() {
        releaseNara();
        super.onStop();
    }

    @Override protected void onDestroy() {
        if (map != null) map.surface.setRunning(false);
        if (detailDialog != null) detailDialog.dismiss();
        releaseNara();
        super.onDestroy();
    }

    private void releaseNara() {
        if (nara != null) {
            nara.release();
            nara = null;
        }
    }

    private boolean reducedMotion() {
        try {
            return Settings.Global.getFloat(getContentResolver(),
                    Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private float nodeX(int index) {
        return 16f + (DESIGN_WIDTH - 32f) * NODE_X[index];
    }

    private float nodeY(int index) {
        return MAP_TOP + 110f + nodeStep() * (levelCount() - 1 - index);
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void launchLesson(int level) {
        if (launching || isFinishing() || !learning.isUnlocked(level)) return;
        launching = true;
        if (detailDialog != null) detailDialog.dismiss();
        startActivity(new Intent(this, lessonActivity()).putExtra("level", level));
    }

    private void showMaterial(int index) {
        if (launching || isFinishing() || detailDialog != null) return;
        final boolean available = playableLevel(index);
        final boolean playable = available && learning.isUnlocked(index);
        final boolean completed = available && learning.isCompleted(index);
        final boolean resumeReplay = completed && learning.hasSession(index)
                && !learning.isSessionComplete(index);
        final Button trigger = map.nodes[index];
        Dialog dialog = new Dialog(this);
        detailDialog = dialog;
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCanceledOnTouchOutside(true);
        dialog.setOnDismissListener(ignored -> {
            detailDialog = null;
            if (!launching && !isFinishing()) trigger.requestFocus();
        });

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(22), dp(22), dp(22), dp(22));
        card.setBackground(rounded(PAPER, LINE, 20));
        TextView status = label(playable ? "LEVEL " + (index + 1) + " DARI "
                + levelCount() : available ? "TERKUNCI" : "BELUM TERSEDIA", 12,
                playable ? GREEN_INK : MUTED, true);
        card.addView(status);
        TextView title = label(levelTitle(index),
                23, DakonArtwork.INK, true);
        if (Build.VERSION.SDK_INT >= 28) title.setAccessibilityHeading(true);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(-1, -2);
        titleParams.topMargin = dp(6);
        card.addView(title, titleParams);
        TextView subtitle = label(levelSubtitle(index), 14, MUTED, false);
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(-1, -2);
        subtitleParams.topMargin = dp(5);
        card.addView(subtitle, subtitleParams);

        if (playable) {
            TextView progress = label(learning.progressTitle(index), 14, GREEN_INK, true);
            LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(-1, -2);
            progressParams.topMargin = dp(18);
            card.addView(progress, progressParams);
            TextView description = label(levelDescription(index), 15, MUTED, false);
            LinearLayout.LayoutParams descriptionParams = new LinearLayout.LayoutParams(-1, -2);
            descriptionParams.topMargin = dp(16);
            card.addView(description, descriptionParams);
            Button continueButton = dialogButton(completed && !resumeReplay ? "Ulangi level"
                    : learning.hasSession(index) ? "Lanjutkan" : "Mulai", true);
            continueButton.setOnClickListener(view -> {
                if (completed && !resumeReplay) learning.restartSession(index);
                launchLesson(index);
            });
            LinearLayout.LayoutParams continueParams = new LinearLayout.LayoutParams(-1, -2);
            continueParams.topMargin = dp(22);
            card.addView(continueButton, continueParams);
        } else {
            TextView message = label(available ? "Selesaikan level sebelumnya lebih dulu."
                    : "Level ini akan ditambahkan nanti.", 16, DakonArtwork.INK, false);
            LinearLayout.LayoutParams messageParams = new LinearLayout.LayoutParams(-1, -2);
            messageParams.topMargin = dp(22);
            messageParams.bottomMargin = dp(14);
            card.addView(message, messageParams);
        }
        Button close = dialogButton("Tutup", false);
        close.setOnClickListener(view -> dialog.dismiss());
        LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(-1, -2);
        closeParams.topMargin = dp(10);
        card.addView(close, closeParams);

        ScrollView dialogScroll = new ScrollView(this);
        dialogScroll.setFillViewport(false);
        dialogScroll.setVerticalScrollBarEnabled(false);
        dialogScroll.addView(card, new ScrollView.LayoutParams(-1, -2));
        dialog.setContentView(dialogScroll);
        dialog.setTitle(levelTitle(index));
        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            WindowManager.LayoutParams params = window.getAttributes();
            params.width = Math.min(getResources().getDisplayMetrics().widthPixels - dp(32), dp(370));
            params.height = WindowManager.LayoutParams.WRAP_CONTENT;
            params.dimAmount = .25f;
            params.gravity = Gravity.CENTER;
            window.setAttributes(params);
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        }
        title.sendAccessibilityEvent(android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
    }

    private TextView label(String value, int size, int color, boolean bold) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextSize(size);
        text.setTextColor(color);
        text.setTypeface(font);
        text.setFontVariationSettings("'wght' " + (bold ? 850 : 650));
        text.setIncludeFontPadding(false);
        text.setLineSpacing(dp(2), 1f);
        return text;
    }

    private GradientDrawable rounded(int fill, int stroke, float radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(dp(radius));
        if (stroke != Color.TRANSPARENT) drawable.setStroke(dp(2), stroke);
        return drawable;
    }

    private Button dialogButton(String value, boolean primary) {
        Button button = new Button(this);
        button.setText(value);
        button.setTextSize(16);
        button.setTypeface(font);
        button.setFontVariationSettings("'wght' 850");
        button.setAllCaps(false);
        button.setTextColor(primary ? Color.WHITE : DakonArtwork.INK);
        button.setMinHeight(dp(50));
        button.setMinimumHeight(dp(50));
        button.setPadding(dp(12), dp(12), dp(12), dp(12));
        android.content.res.ColorStateList colors = new android.content.res.ColorStateList(
                new int[][]{new int[]{android.R.attr.state_pressed}, new int[]{}},
                new int[]{primary ? GREEN_EDGE : LINE, primary ? DakonArtwork.GREEN : PAPER});
        GradientDrawable background = rounded(primary ? DakonArtwork.GREEN : PAPER,
                primary ? GREEN_EDGE : LINE, 13);
        background.setColor(colors);
        button.setBackground(new android.graphics.drawable.RippleDrawable(
                android.content.res.ColorStateList.valueOf(0x22000000), background, null));
        button.setStateListAnimator(null);
        return button;
    }

    /** Real Buttons provide touch, keyboard, and screen-reader semantics over the Canvas. */
    private final class MapButton extends Button {
        private final View drawing;

        MapButton(Context context, View drawing, String text, String description) {
            super(context);
            this.drawing = drawing;
            setText(text);
            setContentDescription(description);
            setTextColor(Color.TRANSPARENT);
            setBackgroundColor(Color.TRANSPARENT);
            setStateListAnimator(null);
            setElevation(0f);
            setAllCaps(false);
            setMinWidth(0);
            setMinHeight(0);
            setMinimumWidth(0);
            setMinimumHeight(0);
            setPadding(0, 0, 0, 0);
        }

        @Override protected void drawableStateChanged() {
            super.drawableStateChanged();
            if (drawing != null) drawing.invalidate();
        }
    }

    private final class HeaderLayout extends FrameLayout {
        final HeaderSurface surface;
        final MapButton back;
        float scale;

        HeaderLayout(Context context) {
            super(context);
            surface = new HeaderSurface(context);
            addView(surface);
            back = new MapButton(context, surface, "Kembali", "Kembali ke pilihan permainan");
            back.setId(R.id.learning_path_back);
            back.setOnClickListener(view -> {
                if (!launching) {
                    launching = true;
                    finish();
                }
            });
            addView(back);
        }

        @Override protected void onMeasure(int widthSpec, int heightSpec) {
            int width = MeasureSpec.getSize(widthSpec);
            scale = width / DESIGN_WIDTH;
            int height = Math.max(dp(56), Math.round(HEADER_HEIGHT * scale));
            setMeasuredDimension(width, height);
            surface.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
            int touch = Math.max(dp(48), Math.round(48f * scale));
            back.measure(MeasureSpec.makeMeasureSpec(touch, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(touch, MeasureSpec.EXACTLY));
        }

        @Override protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
            surface.layout(0, 0, getMeasuredWidth(), getMeasuredHeight());
            int x = Math.round(38f * scale - back.getMeasuredWidth() * .5f);
            int y = Math.round(39f * scale - back.getMeasuredHeight() * .5f);
            back.layout(x, y, x + back.getMeasuredWidth(), y + back.getMeasuredHeight());
        }
    }

    private final class HeaderSurface extends View {
        private final DakonArtwork art;
        private final WelcomeIcons icons;
        private final PetakUmpetArtwork hiddenShapes;

        HeaderSurface(Context context) {
            super(context);
            art = new DakonArtwork(context, font);
            icons = new WelcomeIcons(art);
            hiddenShapes = new PetakUmpetArtwork(context, font);
            setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (header == null || header.scale <= 0f) return;
            canvas.save();
            canvas.translate(0, (getHeight() - HEADER_HEIGHT * header.scale) * .5f);
            canvas.scale(header.scale, header.scale);
            if (header.back.isPressed()) art.round(canvas, 14, 15, 48, 48, 13, 0xFFF1F1F1);
            art.stroke(DakonArtwork.INK, 3f);
            canvas.drawLine(42, 31, 34, 39, art.paint);
            canvas.drawLine(34, 39, 42, 47, art.paint);
            art.text(canvas, courseTitle(), 76, 48, 23, 900, DakonArtwork.INK, false);
            drawMiniBoard(canvas);
            drawBanner(canvas);
            if (header.back.isFocused()) {
                art.stroke(DakonArtwork.BLUE, 2.5f);
                canvas.drawRoundRect(14, 15, 62, 63, 13, 13, art.paint);
            }
            canvas.restore();
            art.stroke(LINE, Math.max(1f, header.scale * 1.5f));
            float divider = 78f * header.scale;
            canvas.drawLine(0, divider, getWidth(), divider, art.paint);
        }

        private void drawBanner(Canvas canvas) {
            int active = learning.currentLevel();
            art.round(canvas, 16, 98, 370, 98, 15, GREEN_EDGE);
            art.round(canvas, 16, 94, 370, 98, 15, DakonArtwork.GREEN);
            art.text(canvas, "LEVEL " + (active + 1) + " DARI " + levelCount(),
                    34, 121, 12, 900, 0xFFE2FFC7, false);
            art.text(canvas, levelTitle(active), 34, 150, 22, 900, Color.WHITE, false);
            art.text(canvas, levelSubtitle(active), 34, 174, 15, 800, Color.WHITE, false);
        }

        private void drawMiniBoard(Canvas canvas) {
            if (hiddenShapeCourse()) {
                hiddenShapes.icon(canvas, 357f, 61f, .48f);
                return;
            }
            if (sackRaceCourse()) {
                SackRacerArtwork.draw(canvas, 2, 359f, 64f, .45f, 0f, true);
                return;
            }
            canvas.save();
            canvas.translate(359f, 39f);
            canvas.rotate(-9f);
            canvas.scale(.48f, .48f);
            icons.dakon(canvas);
            canvas.restore();
        }
    }

    private final class MapLayout extends FrameLayout {
        final MapSurface surface;
        final MapButton[] nodes = new MapButton[levelCount()];
        float scale;

        MapLayout(Context context) {
            super(context);
            setBackgroundColor(PAPER);
            surface = new MapSurface(context);
            addView(surface);
            for (int index = 0; index < nodes.length; index++) {
                final int material = index;
                MapButton node = new MapButton(context, surface,
                        levelTitle(index), nodeDescription(index));
                node.setId(4110 + index);
                node.setOnClickListener(view -> showMaterial(material));
                nodes[index] = node;
                addView(node);
            }
            for (int index = 0; index < nodes.length; index++) {
                if (index > 0) nodes[index].setNextFocusDownId(nodes[index - 1].getId());
                if (index + 1 < nodes.length) {
                    nodes[index].setNextFocusUpId(nodes[index + 1].getId());
                    nodes[index].setNextFocusForwardId(nodes[index + 1].getId());
                }
            }
            nodes[nodes.length - 1].setNextFocusUpId(R.id.learning_path_back);
            header.back.setNextFocusDownId(nodes[nodes.length - 1].getId());
            header.back.setNextFocusForwardId(nodes[0].getId());
        }

        private String nodeDescription(int index) {
            String status = !playableLevel(index) ? "Belum tersedia."
                    : learning.isCompleted(index) ? "Selesai. Ulangi level."
                    : learning.isUnlocked(index) ? "Buka level." : "Terkunci.";
            return "Level " + (index + 1) + ". " + levelTitle(index)
                    + ". " + levelSubtitle(index) + ". " + status;
        }

        void refreshDescriptions() {
            for (int index = 0; index < nodes.length; index++) {
                nodes[index].setContentDescription(nodeDescription(index));
            }
            surface.setContentDescription("Jalur " + courseTitle() + ". Level " + (learning.currentLevel() + 1)
                    + ". " + levelTitle(learning.currentLevel()));
            surface.invalidate();
        }

        @Override protected void onMeasure(int widthSpec, int heightSpec) {
            int width = MeasureSpec.getSize(widthSpec);
            scale = width / DESIGN_WIDTH;
            int height = Math.round(contentHeight() * scale);
            setMeasuredDimension(width, height);
            surface.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
            int touchWidth = Math.max(dp(48), Math.round(NODE_WIDTH * scale));
            int touchHeight = Math.max(dp(48), Math.round(NODE_HEIGHT * scale));
            for (MapButton node : nodes) {
                node.measure(MeasureSpec.makeMeasureSpec(touchWidth, MeasureSpec.EXACTLY),
                        MeasureSpec.makeMeasureSpec(touchHeight, MeasureSpec.EXACTLY));
            }
        }

        @Override protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
            surface.layout(0, 0, getMeasuredWidth(), getMeasuredHeight());
            for (int index = 0; index < nodes.length; index++) {
                MapButton node = nodes[index];
                int x = Math.round(nodeX(index) * scale - node.getMeasuredWidth() * .5f);
                int y = Math.round(nodeY(index) * scale - node.getMeasuredHeight() * .5f);
                node.layout(x, y, x + node.getMeasuredWidth(), y + node.getMeasuredHeight());
            }
        }
    }

    private final class MapSurface extends View {
        private final DakonArtwork art;
        private final Path route = new Path();
        private final Path star = new Path();
        private final Path bubble = new Path();
        private final RectF naraBounds = new RectF(243.5f, MAP_TOP + 193f,
                343.5f, MAP_TOP + 305f);
        private boolean running;

        MapSurface(Context context) {
            super(context);
            art = new DakonArtwork(context, font);
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            int active = learning.currentLevel();
            setContentDescription("Peta level " + courseTitle() + ". Level " + (active + 1) + " dari "
                    + levelCount() + ". " + levelTitle(active)
                    + ".");
            setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
            if (Build.VERSION.SDK_INT >= 28) setAccessibilityHeading(true);
            route.moveTo(nodeX(0), nodeY(0));
            for (int index = 1; index < levelCount(); index++) {
                float middle = (nodeY(index - 1) + nodeY(index)) * .5f;
                route.cubicTo(nodeX(index - 1), middle, nodeX(index), middle,
                        nodeX(index), nodeY(index));
            }
            for (int point = 0; point < 10; point++) {
                double angle = -Math.PI * .5 + point * Math.PI / 5;
                float radius = point % 2 == 0 ? 15.5f : 6.8f;
                float x = (float) Math.cos(angle) * radius;
                float y = (float) Math.sin(angle) * radius;
                if (point == 0) star.moveTo(x, y);
                else star.lineTo(x, y);
            }
            star.close();
        }

        void setRunning(boolean value) {
            running = value;
            if (value) invalidate();
        }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (map == null || map.scale <= 0f) return;
            long elapsed = SystemClock.uptimeMillis() - animationEpoch;
            canvas.save();
            canvas.scale(map.scale, map.scale);
            canvas.save();
            canvas.translate(0, 2f);
            art.stroke(0xFFB8D3DF, 13f);
            canvas.drawPath(route, art.paint);
            canvas.restore();
            art.stroke(0xFFD4EAF4, 11f);
            canvas.drawPath(route, art.paint);
            for (int index = 0; index < levelCount(); index++) drawNode(canvas, index);
            if (sackRaceCourse()) {
                SackRacerArtwork.draw(canvas, 2, 293.5f, 260f, .95f,
                        elapsed % 1600L / 1600f, motionReduced);
            } else if (nara != null) nara.draw(canvas, naraBounds, elapsed, motionReduced);
            canvas.save();
            canvas.translate(303f, nodeY(0) - 59f);
            canvas.scale(-1f, 1f);
            WelcomeCharacterArtwork.draw(canvas, 3, WelcomeCharacterArtwork.POINT,
                    0, 0, .73f, 0f, elapsed / 1000f, motionReduced);
            canvas.restore();
            canvas.restore();
            if (running && isShown() && !motionReduced) postInvalidateOnAnimation();
        }

        private void drawNode(Canvas canvas, int index) {
            float x = nodeX(index);
            float y = nodeY(index);
            boolean current = index == learning.currentLevel();
            boolean completed = learning.isCompleted(index);
            boolean unlocked = learning.isUnlocked(index);
            float pressed = map.nodes[index].isPressed() ? 6f : 0f;
            if (current) {
                art.stroke(LINE, 6f);
                canvas.drawOval(x - 47, y - 41, x + 47, y + 47, art.paint);
                drawStartBubble(canvas, x, y - 73f);
            }
            art.fill(unlocked ? GREEN_EDGE : NODE_EDGE);
            canvas.drawOval(x - NODE_WIDTH / 2, y - NODE_HEIGHT / 2 + 8,
                    x + NODE_WIDTH / 2, y + NODE_HEIGHT / 2 + 8, art.paint);
            art.fill(unlocked ? DakonArtwork.GREEN : NODE_FACE);
            canvas.drawOval(x - NODE_WIDTH / 2, y - NODE_HEIGHT / 2 + pressed,
                    x + NODE_WIDTH / 2, y + NODE_HEIGHT / 2 + pressed, art.paint);
            canvas.save();
            canvas.translate(x, y + pressed);
            if (completed) {
                art.stroke(Color.WHITE, 4f);
                canvas.drawLine(-10, 0, -3, 7, art.paint);
                canvas.drawLine(-3, 7, 12, -8, art.paint);
            } else if (unlocked) {
                art.fill(Color.WHITE);
                canvas.drawPath(star, art.paint);
            } else MapPadlockArtwork.draw(canvas, 0, 0, 32f, LOCK_INK, NODE_FACE);
            canvas.restore();
            if (map.nodes[index].isFocused()) {
                art.stroke(DakonArtwork.BLUE, 3f);
                canvas.drawOval(x - 43, y - 37, x + 43, y + 45, art.paint);
            }
        }

        private void drawStartBubble(Canvas canvas, float x, float top) {
            bubble.reset();
            bubble.addRoundRect(x - 32, top, x + 32, top + 24, 8, 8, Path.Direction.CW);
            Path tail = new Path();
            tail.moveTo(x - 6, top + 23);
            tail.lineTo(x, top + 29);
            tail.lineTo(x + 6, top + 23);
            tail.close();
            bubble.op(tail, Path.Op.UNION);
            art.fill(PAPER);
            canvas.drawPath(bubble, art.paint);
            art.stroke(LINE, 2f);
            canvas.drawPath(bubble, art.paint);
            art.text(canvas, mapAction, x, top + 17, 12, 900, GREEN_INK, true);
        }
    }
}
