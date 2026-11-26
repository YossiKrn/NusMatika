package id.nusmatika.dakon;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ScrollView;

/** Native game picker. Each game owns its own saved work. */
public final class GameChooserActivity extends Activity {
    private static final float DESIGN_WIDTH = 360f;
    private static final float DESIGN_HEIGHT = 1118f;
    private static final float CARD_LEFT = 16f;
    private static final float CARD_WIDTH = 328f;
    private static final float CARD_HEIGHT = 210f;
    private static final float[] CARD_TOP = {172f, 406f, 640f, 874f};
    private static final String[] GAME_NAMES = {"Dakon", "Balap Karung", "Petak Umpet", "Engklek"};
    private static final String[] GAME_ACTIVITIES = {".DakonMapActivity", ".BalapKarungMapActivity", ".PetakUmpetMapActivity", ".EngklekMapActivity"};

    private NaraRenderer nara;
    private ChooserLayout chooser;
    private boolean launching;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        Typeface font;
        try {
            font = Typeface.createFromAsset(getAssets(), "fonts/Nunito.ttf");
        } catch (RuntimeException error) {
            font = Typeface.create("sans-serif-rounded", Typeface.NORMAL);
        }
        nara = new NaraRenderer(this);
        chooser = new ChooserLayout(this, font);
        FrameLayout safeArea = new FrameLayout(this);
        safeArea.setBackgroundColor(Color.WHITE);
        ScrollView games = new ScrollView(this);
        games.setFillViewport(true);
        games.setVerticalScrollBarEnabled(false);
        games.addView(chooser, new ScrollView.LayoutParams(-1, -2));
        safeArea.addView(games, new FrameLayout.LayoutParams(-1, -1));
        if (Build.VERSION.SDK_INT >= 30) getWindow().setDecorFitsSystemWindows(false);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        safeArea.setOnApplyWindowInsetsListener((view, insets) -> {
            int left, top, right, bottom;
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets bars = insets.getInsets(
                        WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                left = bars.left;
                top = bars.top;
                right = bars.right;
                bottom = bars.bottom;
            } else {
                left = insets.getSystemWindowInsetLeft();
                top = insets.getSystemWindowInsetTop();
                right = insets.getSystemWindowInsetRight();
                bottom = insets.getSystemWindowInsetBottom();
            }
            view.setPadding(left, top, right, bottom);
            return insets;
        });
        setContentView(safeArea);
        safeArea.requestApplyInsets();
    }

    private void openGame(int index) {
        if (launching || index < 0 || index >= GAME_ACTIVITIES.length) return;
        launching = true;
        Intent intent = new Intent();
        intent.setClassName(this, getPackageName() + GAME_ACTIVITIES[index]);
        startActivity(intent);
    }

    private boolean isReducedMotion() {
        try {
            return Settings.Global.getFloat(getContentResolver(),
                    Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    @Override protected void onResume() {
        super.onResume();
        launching = false;
        if (nara == null) nara = new NaraRenderer(this);
        if (chooser != null) chooser.surface.setRunning(true);
    }

    @Override protected void onPause() {
        if (chooser != null) chooser.surface.setRunning(false);
        super.onPause();
    }

    @Override protected void onStop() {
        releaseNara();
        super.onStop();
    }

    @Override protected void onDestroy() {
        releaseNara();
        super.onDestroy();
    }

    private void releaseNara() {
        if (nara != null) {
            nara.release();
            nara = null;
        }
    }

    private final class ChooserLayout extends FrameLayout {
        final ChooserSurface surface;
        final CardButton[] cards = new CardButton[GAME_NAMES.length];
        float contentScale = 1f;
        float contentLeft;

        ChooserLayout(Context context, Typeface font) {
            super(context);
            setBackgroundColor(Color.WHITE);
            surface = new ChooserSurface(context, font);
            addView(surface);
            for (int index = 0; index < cards.length; index++) {
                final int game = index;
                CardButton button = new CardButton(context);
                cards[index] = button;
                button.setId(2001 + index);
                button.setText(GAME_NAMES[index]);
                button.setContentDescription(GAME_NAMES[index]);
                button.setTextColor(Color.TRANSPARENT);
                button.setBackgroundColor(Color.TRANSPARENT);
                button.setStateListAnimator(null);
                button.setElevation(0f);
                button.setAllCaps(false);
                button.setMinWidth(0);
                button.setMinHeight(0);
                button.setMinimumWidth(0);
                button.setMinimumHeight(0);
                button.setPadding(0, 0, 0, 0);
                button.setOnClickListener(view -> openGame(game));
                addView(button);
            }
            for (int index = 0; index < cards.length; index++) {
                if (index + 1 < cards.length) {
                    cards[index].setNextFocusDownId(cards[index + 1].getId());
                    cards[index].setNextFocusForwardId(cards[index + 1].getId());
                }
                if (index > 0) cards[index].setNextFocusUpId(cards[index - 1].getId());
            }
        }

        @Override protected void onMeasure(int widthSpec, int heightSpec) {
            int width = MeasureSpec.getSize(widthSpec);
            contentScale = width / DESIGN_WIDTH;
            int height = Math.max(MeasureSpec.getSize(heightSpec), Math.round(DESIGN_HEIGHT * contentScale));
            setMeasuredDimension(width, height);
            contentLeft = (width - DESIGN_WIDTH * contentScale) * .5f;
            surface.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
            for (CardButton card : cards) {
                card.measure(MeasureSpec.makeMeasureSpec(Math.round(CARD_WIDTH * contentScale),
                                MeasureSpec.EXACTLY),
                        MeasureSpec.makeMeasureSpec(Math.round(CARD_HEIGHT * contentScale),
                                MeasureSpec.EXACTLY));
            }
        }

        @Override protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
            surface.layout(0, 0, right - left, bottom - top);
            for (int index = 0; index < cards.length; index++) {
                int x = Math.round(contentLeft + CARD_LEFT * contentScale);
                int y = Math.round(CARD_TOP[index] * contentScale);
                cards[index].layout(x, y, x + cards[index].getMeasuredWidth(),
                        y + cards[index].getMeasuredHeight());
            }
        }

        private final class CardButton extends Button {
            CardButton(Context context) { super(context); }

            @Override protected void drawableStateChanged() {
                super.drawableStateChanged();
                if (surface != null) surface.invalidate();
            }
        }
    }

    private final class ChooserSurface extends View {
        private final DakonArtwork art;
        private final PetakUmpetArtwork hiddenShapes;
        private final RectF naraBounds = new RectF(240, 26, 344, 132);
        private final long animationEpoch = SystemClock.uptimeMillis();
        private boolean running;

        ChooserSurface(Context context, Typeface font) {
            super(context);
            art = new DakonArtwork(context, font);
            hiddenShapes = new PetakUmpetArtwork(context, font);
            setContentDescription("NusMatika. Pilih permainan.");
            setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
            if (Build.VERSION.SDK_INT >= 28) setAccessibilityHeading(true);
        }

        void setRunning(boolean value) {
            running = value;
            if (value) invalidate();
        }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (chooser == null || chooser.contentScale <= 0f) return;
            canvas.save();
            canvas.translate(chooser.contentLeft, 0f);
            canvas.scale(chooser.contentScale, chooser.contentScale);
            art.text(canvas, "NusMatika", 16, 76, 31, 900, DakonArtwork.INK, false);
            art.text(canvas, "Pilih permainan", 16, 130, 19, 750, 0xFF777777, false);
            boolean reduced = isReducedMotion();
            if (nara != null) nara.draw(canvas, naraBounds,
                    SystemClock.uptimeMillis() - animationEpoch, reduced);
            for (int index = 0; index < GAME_NAMES.length; index++) drawCard(canvas, index);
            canvas.restore();
            if (running && isShown() && !reduced) postInvalidateDelayed(83L);
        }

        private void drawCard(Canvas canvas, int index) {
            boolean dakon = index == 0;
            float top = CARD_TOP[index];
            float pressed = chooser.cards[index].isPressed() ? 3f : 0f;
            int base = dakon ? 0xFFE6B15E : index == 1 ? 0xFFD4AA73 : index == 2 ? 0xFFD4B9EE : 0xFF98B9E0;
            int face = dakon ? 0xFFFFF2D9 : index == 1 ? 0xFFF5D7B0 : index == 2 ? 0xFFF3EAFE : 0xFFEAF4FF;
            art.round(canvas, CARD_LEFT, top + 5, CARD_WIDTH, CARD_HEIGHT - 5, 24, base);
            art.round(canvas, CARD_LEFT, top + pressed, CARD_WIDTH, CARD_HEIGHT - 5, 24, face);
            canvas.save();
            canvas.translate(0, pressed);
            if (dakon) drawMiniBoard(canvas);
            else if (index == 1) drawRacers(canvas);
            else if (index == 2) hiddenShapes.icon(canvas, 150, top + 129, 1.4f);
            else drawEngklek(canvas, top);
            art.text(canvas, GAME_NAMES[index], 40, top + 178,
                    26, 900, DakonArtwork.INK, false);
            float arrowY = top + 169;
            art.stroke(DakonArtwork.INK, 3f);
            canvas.drawLine(300, arrowY - 6, 307, arrowY, art.paint);
            canvas.drawLine(307, arrowY, 300, arrowY + 6, art.paint);
            canvas.restore();
            if (chooser.cards[index].isFocused()) {
                art.stroke(DakonArtwork.BLUE, 3f);
                canvas.drawRoundRect(CARD_LEFT + 2, top + 2,
                        CARD_LEFT + CARD_WIDTH - 2, top + CARD_HEIGHT - 2,
                        23, 23, art.paint);
            }
        }

        /** Ikon papan dan gaco Engklek; tidak membutuhkan aset tambahan. */
        private void drawEngklek(Canvas canvas, float top) {
            int[][] cells = {{1, 0}, {0, 1}, {1, 1}, {2, 1}, {3, 1}, {1, 2}};
            for (int index = 0; index < cells.length; index++) {
                float x = 58 + cells[index][0] * 37f;
                float y = top + 22 + cells[index][1] * 37f;
                art.round(canvas, x, y, 33, 33, 5, EngklekUi.FACE_COLORS[index]);
            }
            art.circle(canvas, 113, top + 76, 8, 0xFFE86435);
        }

        private void drawMiniBoard(Canvas canvas) {
            art.round(canvas, 36, 204, 188, 98, 15, 0xFFC68A34);
            art.round(canvas, 36, 200, 188, 98, 15, 0xFFFBD18A);
            for (int hole = 0; hole < 24; hole++) {
                float x = 55 + (hole % 6) * 30f;
                float y = 216 + (hole / 6) * 21f;
                art.circle(canvas, x, y, 8.5f, 0xFFC68A34);
                art.circle(canvas, x, y + 1.3f, 6.8f, 0xFFFFF0D3);
                if (hole == 1 || hole == 4 || hole == 12 || hole == 19) {
                    art.seed(canvas, x, y + 1.1f, hole < 12 ? 0 : 1, 4.4f);
                }
            }
        }

        private void drawRacers(Canvas canvas) {
            art.stroke(0xFFE4BB85, 3f);
            canvas.drawLine(40, 530, 292, 530, art.paint);
            for (int racer = 0; racer < 4; racer++) {
                SackRacerArtwork.draw(canvas, racer, 70 + racer * 60f,
                        527, .65f, 0f, true);
            }
        }
    }
}
