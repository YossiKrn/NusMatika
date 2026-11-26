package id.nusmatika.dakon;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

/** Mengatur halaman, alur belajar, pemeriksaan, dan tombol Petak Umpet. */
public final class PetakUmpetActivity extends Activity implements PetakUmpetSceneView.Host,
        PetakUmpetAssemblyView.Host {
    private static final String MANUAL = "susun-v1";
    private PetakUmpetStore store;
    private PetakUmpetSession session;
    private PetakUmpetSceneView scene;
    private PetakUmpetAssemblyView board;
    private PetakUmpetAssemblyModel model;
    private PetakUmpetAssemblyStore assemblyStore;
    private NaraRenderer nara;
    private Typeface font, headingFont;
    private ScrollView scroll;
    private TextView title, instruction, inventory, feedback;
    private ProgressBar progress;
    private Button primary, rotate, takeBack;
    private LinearLayout tools;
    private boolean checked;
    private String assemblyFeedback = "";

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        store = new PetakUmpetStore(this);
        int level = getIntent().getIntExtra("level", -1);
        if (!store.isUnlocked(level)) { openMap(); return; }
        session = store.loadSession(level);
        if (level == 2) prepareAssembly();
        try {
            font = new Typeface.Builder(getAssets(), "fonts/Nunito.ttf")
                    .setFontVariationSettings("'wght' 650").build();
            headingFont = new Typeface.Builder(getAssets(), "fonts/Nunito.ttf")
                    .setFontVariationSettings("'wght' 850").build();
        } catch (RuntimeException error) {
            font = Typeface.create("sans-serif-rounded", Typeface.NORMAL);
            headingFont = Typeface.create(font, Typeface.BOLD);
        }
        nara = new NaraRenderer(this);
        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setPadding(dp(16), dp(6), dp(16), dp(20));
        column.setBackgroundColor(Color.WHITE);
        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        Button exit = button("×", () -> exit());
        exit.setTextSize(26);
        exit.setContentDescription("Kembali ke jalur Petak Umpet");
        exit.setBackgroundColor(Color.TRANSPARENT);
        header.addView(exit, new LinearLayout.LayoutParams(dp(48), dp(48)));
        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(PetakUmpetCatalog.ITEM_COUNT);
        progress.setProgressTintList(android.content.res.ColorStateList.valueOf(DakonArtwork.GREEN));
        progress.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFE5E5E5));
        header.addView(progress, new LinearLayout.LayoutParams(0, dp(12), 1));
        column.addView(header);
        add(column, label(PetakUmpetCatalog.levelTitle(level) + " · Petak Umpet",
                13, 0xFF777777, true), 4, 8);
        title = label("", 25, DakonArtwork.INK, true);
        instruction = label("", 16, DakonArtwork.INK, false);
        add(column, title, 0, 8);
        add(column, instruction, 0, 12);
        scene = new PetakUmpetSceneView(this, session, font, nara, this);
        add(column, scene, 0, 8);
        if (level == 2) {
            board = new PetakUmpetAssemblyView(this, model, font, headingFont, this);
            add(column, board, 0, 8);
            tools = new LinearLayout(this);
            rotate = button("Putar", () -> rotateSelected());
            takeBack = button("Ambil lagi", () -> returnSelected());
            tools.addView(rotate, new LinearLayout.LayoutParams(0, dp(50), 1));
            tools.addView(takeBack, new LinearLayout.LayoutParams(0, dp(50), 1));
            tools.addView(button("Ulangi", () -> {
                board.cancelGesture();
                model.reset();
                onAssemblyChanged("Semua bangun kembali ke tempat temuan.");
            }), new LinearLayout.LayoutParams(0, dp(50), 1));
            add(column, tools, 0, 10);
        }
        inventory = label("", 14, 0xFF52773E, true);
        inventory.setGravity(Gravity.CENTER);
        add(column, inventory, 0, 8);
        feedback = label("", 15, DakonArtwork.INK, false);
        feedback.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        add(column, feedback, 4, 10);
        primary = button("", () -> primaryAction());
        primary.setTextSize(17);
        primary.setMinHeight(dp(56));
        add(column, primary, 4, 0);
        scroll = new ScrollView(this);
        if (board != null) board.setScrollContainer(scroll);
        scroll.setFillViewport(true);
        scroll.addView(column, new ScrollView.LayoutParams(-1, -2));
        FrameLayout safe = new FrameLayout(this);
        safe.setBackgroundColor(Color.WHITE);
        safe.addView(scroll, new FrameLayout.LayoutParams(-1, -1));
        if (Build.VERSION.SDK_INT >= 30) getWindow().setDecorFitsSystemWindows(false);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        getWindow().setStatusBarColor(Color.WHITE);
        getWindow().setNavigationBarColor(Color.WHITE);
        safe.setOnApplyWindowInsetsListener((view, insets) -> {
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets bars = insets.getInsets(
                        WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            } else view.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                    insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            return insets;
        });
        setContentView(safe);
        safe.requestApplyInsets();
        save();
        refresh();
    }

    private void prepareAssembly() {
        assemblyStore = new PetakUmpetAssemblyStore(this);
        boolean resume = MANUAL.equals(session.getBalokAnswer()) || session.isAssembled();
        model = resume ? assemblyStore.load() : null;
        if (model == null) model = new PetakUmpetAssemblyModel();
        if (!session.isComplete()) {
            checked = session.isAssembled() && model.isSolved();
            newManualSession();
            if (checked) markCorrect();
        }
    }

    private void newManualSession() {
        session = new PetakUmpetSession(2);
        session.setBalokAnswer(MANUAL);
        if (scene != null) {
            LinearLayout parent = (LinearLayout) scene.getParent();
            int index = parent.indexOfChild(scene);
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) scene.getLayoutParams();
            scene.setRunning(false);
            parent.removeView(scene);
            scene = new PetakUmpetSceneView(this, session, font, nara, this);
            parent.addView(scene, index, params);
            scene.setRunning(true);
        }
    }

    private void markCorrect() {
        session.setBalokAnswer(String.valueOf(PetakUmpetCatalog.count(
                PetakUmpetCatalog.Kind.BALOK, store.inventoryMask())));
        session.setKubusAnswer(String.valueOf(PetakUmpetCatalog.count(
                PetakUmpetCatalog.Kind.KUBUS, store.inventoryMask())));
        session.checkAnswers();
        session.assemble();
        checked = true;
        assemblyFeedback = "Benar! Semua bangun membentuk huruf L. Tekan Selesai.";
    }

    @Override public void onAssemblyChanged(String message) {
        if (checked) newManualSession();
        checked = false;
        assemblyFeedback = message;
        save();
        refresh();
    }

    private void rotateSelected() {
        if (model.selected < 0) return;
        if (model.rotate(model.selected)) onAssemblyChanged("Bangun diputar. Jenisnya tetap sama.");
        else {
            assemblyFeedback = "Belum ada ruang untuk memutar. Geser atau ambil bangun itu dahulu.";
            refresh();
        }
    }

    private void returnSelected() {
        if (model.selected < 0) return;
        model.placed[model.selected] = false;
        onAssemblyChanged("Bangun kembali ke tempat temuan dan bisa disusun lagi.");
    }

    private void primaryAction() {
        if (session.isComplete()) { exit(); return; }
        if (session.getLevel() < 2) session.finish();
        else if (checked && model.isSolved()) session.finish();
        else if (model.placedCount() < PetakUmpetCatalog.ITEM_COUNT)
            assemblyFeedback = "Masih ada " + (PetakUmpetCatalog.ITEM_COUNT - model.placedCount())
                    + " bangun di tempat temuan. Gunakan semuanya.";
        else if (!model.isSolved())
            assemblyFeedback = "Belum membentuk huruf L. Perhatikan contoh, lalu geser atau putar bangunnya.";
        else markCorrect();
        save();
        refresh();
        if (session.isComplete()) scroll.post(() -> scroll.smoothScrollTo(0, 0));
    }

    private void refresh() {
        boolean complete = session.isComplete(), manual = session.getLevel() == 2;
        title.setText(complete ? "Level selesai!" : manual ? "Susun bentuk L"
                : session.showsMarkers() ? "Kenali bangun bertanda" : "Cari bangun tersembunyi");
        instruction.setText(complete ? "Kerja bagus! Kamu bisa kembali ke jalur."
                : manual ? "Tarik semua bangun temuanmu ke papan kosong. Susun seperti contoh. "
                        + "Ketuk balok lalu tekan Putar untuk mengubah arahnya."
                : session.showsMarkers() ? "Ketuk bangun yang diberi penanda. Kenali bentuknya."
                        : "Cari semua kubus dan balok di halaman sekolah.");
        scene.setVisibility(complete || !manual ? View.VISIBLE : View.GONE);
        scene.refresh();
        if (manual) {
            board.setVisibility(complete ? View.GONE : View.VISIBLE);
            tools.setVisibility(complete ? View.GONE : View.VISIBLE);
            rotate.setEnabled(model.selected >= 0 && model.isBalok(model.selected));
            takeBack.setEnabled(model.selected >= 0 && model.placed[model.selected]);
            board.setContentDescription("Papan susun. " + model.placedCount() + " dari 6 bangun di papan."
                    + (model.selected >= 0 ? " Terpilih: bangun " + (model.selected + 1) + "." : "")
                    + " Gunakan tindakan aksesibilitas untuk memilih dan menggeser bangun.");
            board.invalidate();
        }
        inventory.setVisibility(complete ? View.GONE : View.VISIBLE);
        inventory.setText(manual ? "Bangun di papan: " + model.placedCount() + "/6 · Temuan: 4 balok, 2 kubus"
                : "Balok " + session.foundCount(PetakUmpetCatalog.Kind.BALOK) + "/4 · Kubus "
                        + session.foundCount(PetakUmpetCatalog.Kind.KUBUS) + "/2");
        String message = manual ? assemblyFeedback : session.feedbackText();
        feedback.setText(message);
        feedback.setVisibility(complete || message.isEmpty() ? View.GONE : View.VISIBLE);
        feedback.setTextColor(manual && checked ? 0xFF448D00 : DakonArtwork.INK);
        progress.setProgress(complete ? 6 : manual ? model.placedCount() : session.foundCount());
        primary.setText(complete ? "Kembali ke jalur" : manual ? checked ? "Selesai" : "Periksa susunan" : "Selesai");
        boolean enabled = complete || manual || session.primaryEnabled();
        primary.setEnabled(enabled);
        primary.setTextColor(enabled ? DakonArtwork.INK : 0xFFAFAFAF);
        primary.setBackground(background(enabled ? DakonArtwork.GREEN : 0xFFE5E5E5,
                enabled ? 0xFF46A302 : 0xFFD7D7D7));
    }

    private TextView label(String text, int size, int color, boolean bold) {
        TextView v = new TextView(this);
        v.setText(text); v.setTextSize(size); v.setTextColor(color);
        v.setTypeface(bold ? headingFont : font); v.setIncludeFontPadding(false);
        v.setLineSpacing(dp(3), 1);
        return v;
    }
    private Button button(String text, Runnable action) {
        Button v = new Button(this);
        v.setText(text); v.setTypeface(headingFont); v.setAllCaps(false);
        v.setTextSize(14); v.setMinHeight(dp(48)); v.setStateListAnimator(null); v.setElevation(0);
        v.setOnClickListener(view -> action.run());
        return v;
    }
    private void add(LinearLayout parent, View child, int top, int bottom) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.topMargin = dp(top); p.bottomMargin = dp(bottom);
        parent.addView(child, p);
    }
    private GradientDrawable background(int color, int border) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color); d.setCornerRadius(dp(15)); d.setStroke(dp(2), border);
        return d;
    }
    private int dp(float value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private void save() {
        if (session == null || store == null) return;
        if (model != null) assemblyStore.save(model);
        store.saveSession(session);
    }
    private void exit() { if (board != null) board.cancelGesture(); save(); finish(); }
    private void openMap() {
        startActivity(new Intent(this, PetakUmpetMapActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
        finish();
    }
    @Override public void onAssemblySelectionChanged() { save(); refresh(); }
    @Override public void onAssemblyRotateRequested() { rotateSelected(); }
    @Override public boolean isAssemblyComplete() { return session.isComplete(); }
    @Override public void onFind(int item) {
        if (session.find(item)) {
            save(); refresh(); scene.announceForAccessibility(session.feedbackText());
        }
    }
    @Override public void onMiss() { session.miss(); refresh(); }
    @Override public void onAssemblyReady() { if (!isFinishing() && !isDestroyed()) refresh(); }
    @Override public boolean reducedMotion() {
        try {
            return Settings.Global.getFloat(getContentResolver(),
                    Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0;
        } catch (RuntimeException ignored) { return false; }
    }
    @Override protected void onResume() { super.onResume(); if (scene != null) scene.setRunning(true); }
    @Override protected void onPause() {
        if (board != null) board.cancelGesture();
        save(); if (scene != null) scene.setRunning(false); super.onPause();
    }
    @Override protected void onSaveInstanceState(Bundle state) { save(); super.onSaveInstanceState(state); }
    @Override protected void onDestroy() {
        if (scene != null) scene.setRunning(false);
        if (nara != null) nara.release();
        super.onDestroy();
    }
}
