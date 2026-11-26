package id.nusmatika.dakon;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

/** Penghubung kegiatan, papan, lipatan dan progres. Aturan jawaban ada di Session. */
public final class EngklekActivity extends Activity
        implements EngklekBoardView.Host, EngklekFoldView.Host {
    private EngklekStore store;
    private EngklekSession session;
    private EngklekBoardView board;
    private EngklekFoldView preview;
    private TextView counter, feedback, previewTitle;
    private Button primary, fold, reset;
    private String message = "";

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        store = new EngklekStore(this);
        int level = getIntent().getIntExtra("level", 0);
        if (!store.isUnlocked(level)) { finish(); return; }
        session = store.load(level);
        render();
    }

    private void render() {
        if (preview != null) preview.stop();
        LinearLayout page = EngklekUi.column(this);
        EngklekUi.add(page, EngklekUi.button(this, "‹ Jalur Engklek", this::finish), 0);
        EngklekUi.add(page, EngklekUi.text(this,
                "Engklek · " + EngklekCatalog.STAGES[session.level()], 14, true), 16);
        if (session.complete()) { renderCompletion(page); return; }
        EngklekCatalog.Task task = session.task();
        ProgressBar progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(EngklekCatalog.count(session.level()));
        progress.setProgress(session.taskIndex());
        EngklekUi.add(page, progress, 10);
        EngklekUi.add(page, EngklekUi.text(this, "Kegiatan " + (session.taskIndex() + 1) + " dari "
                + EngklekCatalog.count(session.level()), 13, false), 6);
        EngklekUi.add(page, EngklekUi.text(this, task.title, 24, true), 12);
        EngklekUi.add(page, EngklekUi.text(this, task.instruction, 16, false), 8);
        board = new EngklekBoardView(this, this);
        EngklekUi.add(page, board, 10);
        counter = EngklekUi.text(this, "", 14, true);
        EngklekUi.add(page, counter, 4);
        if (task.editable()) addBoardTools(page);
        if (task.kind == EngklekCatalog.JUDGE || task.kind == EngklekCatalog.GIFT) addAnswers(page);
        previewTitle = EngklekUi.text(this, "", 16, true);
        EngklekUi.add(page, previewTitle, 16);
        preview = new EngklekFoldView(this, this);
        EngklekUi.add(page, preview, 8);
        fold = EngklekUi.button(this, "Lipat", this::foldAction);
        EngklekUi.add(page, fold, 10);
        if (task.kind == EngklekCatalog.OTHER_NET) {
            EngklekUi.add(page, EngklekUi.button(this, "Lihat contoh awal", () ->
                    new AlertDialog.Builder(this).setTitle("Contoh jaring-jaring kubus")
                            .setMessage("Pada papan yang sama, pilih B2, C1, C2, C3, C4, D2."
                                    + " Pola barumu harus berbeda, termasuk setelah diputar dan dicerminkan.")
                            .setPositiveButton("Mengerti", null).show()), 8);
        }
        feedback = EngklekUi.text(this, "", 15, false);
        feedback.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        EngklekUi.add(page, feedback, 14);
        EngklekUi.add(page, EngklekUi.button(this, "Petunjuk", () -> {
            message = task.hint;
            refresh();
        }), 8);
        primary = EngklekUi.button(this, "Periksa", this::primaryAction);
        EngklekUi.add(page, primary, 12);
        EngklekUi.install(this, page);
        refresh();
        save();
    }

    private void addBoardTools(LinearLayout page) {
        EngklekUi.add(page, EngklekUi.text(this,
                "Biru = sisi yang sudah tersedia. Gaco jingga melompat ke pilihanmu."
                        + (session.task().cuboid ? " Angka pada bidang menunjukkan ukuran dalam satuan." : ""),
                13, false), 4);
        EngklekUi.add(page, EngklekUi.button(this, "Pilih kotak dengan daftar", this::showCellList), 8);
        reset = EngklekUi.button(this, session.task().kind == EngklekCatalog.OTHER_NET
                ? "Bersihkan / cari pola lagi" : "Ulangi pilihan", () -> {
            session.resetTask();
            preview.stop();
            message = session.task().kind == EngklekCatalog.OTHER_NET
                    ? "Pilihan dikembalikan ke awal. Pola yang sudah ditemukan tetap disimpan."
                    : "Pilihan dikembalikan ke bidang-bidang awal.";
            save();
            refresh();
        });
        EngklekUi.add(page, reset, 8);
    }

    private void showCellList() {
        String[] labels = new String[EngklekNet.COLUMNS * EngklekNet.ROWS];
        for (int cell = 0; cell < labels.length; cell++) {
            labels[cell] = "" + (char) ('A' + EngklekNet.row(cell)) + (EngklekNet.column(cell) + 1)
                    + (session.task().cuboid ? " · " + EngklekNet.width(true, cell) + " × "
                    + EngklekNet.height(true, cell) + " satuan" : "")
                    + (EngklekNet.contains(session.task().initial, cell) ? " · sudah tersedia"
                    : EngklekNet.contains(session.selected(), cell) ? " · terpilih" : "");
        }
        new AlertDialog.Builder(this).setTitle("Pilih baris dan kolom")
                .setItems(labels, (dialog, cell) -> onCellSelected(cell))
                .setNegativeButton("Tutup", null).show();
    }

    private void addAnswers(LinearLayout page) {
        EngklekCatalog.Task task = session.task();
        if (task.kind == EngklekCatalog.GIFT) {
            for (int option = 0; option < 2; option++) {
                EngklekUi.add(page, EngklekUi.text(this, "Pola " + (option == 0 ? "A" : "B"), 17, true), 14);
                EngklekBoardView sample = new EngklekBoardView(this, cell -> { });
                sample.configure(task.cuboid, task.options[option], 0, false);
                EngklekUi.add(page, sample, 0);
            }
        }
        RadioGroup answers = new RadioGroup(this);
        for (int option = 0; option < 2; option++) {
            RadioButton radio = radio(task.kind == EngklekCatalog.GIFT
                    ? "Pola " + (option == 0 ? "A" : "B")
                    : option == 0 ? "Tidak dapat dilipat menjadi bangun tertutup" : "Dapat dilipat menjadi bangun tertutup");
            radio.setId(7100 + option);
            answers.addView(radio);
        }
        if (session.choice() >= 0) answers.check(7100 + session.choice());
        answers.setOnCheckedChangeListener((group, id) -> {
            session.choose(id - 7100);
            message = "Pilih juga alasanmu, lalu tekan Periksa jawaban.";
            save(); refresh();
        });
        EngklekUi.add(page, answers, 12);
        EngklekUi.add(page, EngklekUi.text(this, "Apa alasanmu?", 16, true), 8);
        RadioGroup reasons = new RadioGroup(this);
        for (int option = 0; option < EngklekCatalog.REASONS.length; option++) {
            RadioButton radio = radio(EngklekCatalog.REASONS[option]);
            radio.setId(7200 + option);
            reasons.addView(radio);
        }
        if (session.reason() >= 0) reasons.check(7200 + session.reason());
        reasons.setOnCheckedChangeListener((group, id) -> {
            session.giveReason(id - 7200);
            message = "Tekan Periksa jawaban untuk memeriksa pilihan dan alasanmu.";
            save(); refresh();
        });
        EngklekUi.add(page, reasons, 4);
    }

    private RadioButton radio(String label) {
        RadioButton radio = new RadioButton(this);
        radio.setText(label);
        radio.setTextSize(15);
        radio.setTextColor(EngklekUi.INK);
        radio.setTypeface(EngklekUi.font(this));
        radio.setMinHeight(EngklekUi.dp(this, 52));
        return radio;
    }

    @Override public void onCellSelected(int cell) {
        preview.stop();
        message = session.toggle(cell);
        save();
        refresh();
    }

    @Override public void onFoldFinished(boolean closed) {
        session.observed(closed);
        if (session.task().kind == EngklekCatalog.OBSERVE) {
            message = session.passed() ? "Pengamatan selesai. "
                    + EngklekNet.inspect(session.selected(), session.task().cuboid).message
                    : "Sekarang tekan Bentangkan untuk melihat kembali keenam sisi.";
        }
        save();
        refresh();
    }

    private void foldAction() {
        if (preview.moving()) return;
        preview.animateFold(!preview.closed());
        refresh();
    }

    private void primaryAction() {
        if (preview.moving()) return;
        if (session.passed()) {
            session.advance();
            message = "";
            save();
            render();
            return;
        }
        message = session.check();
        save();
        refresh();
        int kind = session.task().kind;
        if ((kind == EngklekCatalog.BUILD || kind == EngklekCatalog.OTHER_NET || session.passed())
                && EngklekNet.inspect(session.previewMask(), session.task().cuboid).faces.size() == 6) {
            preview.animateFold(true);
            refresh();
        }
    }

    private void refresh() {
        if (session.complete() || preview == null) return;
        EngklekCatalog.Task task = session.task();
        long mask = session.previewMask();
        boolean reference = task.editable() && Long.bitCount(mask) < 6 && task.initial == 0;
        boolean judgement = task.kind == EngklekCatalog.JUDGE || task.kind == EngklekCatalog.GIFT;
        long previewMask = reference ? task.reference : mask;
        board.configure(task.cuboid, task.kind == EngklekCatalog.GIFT ? 0 : mask,
                task.initial, task.editable());
        board.setVisibility(task.kind == EngklekCatalog.GIFT ? View.GONE : View.VISIBLE);
        counter.setText(task.kind == EngklekCatalog.OTHER_NET
                ? "Pola kubus dikenal: " + session.discoveredCount() + " / 11 (termasuk contoh). "
                    + Long.bitCount(mask) + " / 6 sisi dipilih."
                : task.editable() ? Long.bitCount(mask) + " / 6 sisi dipilih"
                : task.cuboid ? "Ukuran bidang ditampilkan dalam satuan." : "Enam bidang persegi sama besar.");
        previewTitle.setText(reference ? "Contoh " + task.solidName() + " yang diuraikan"
                : judgement ? "Periksa lipatan setelah menjawab" : "Pratinjau lipatan");
        preview.setPattern(previewMask, task.cuboid, reference && task.kind == EngklekCatalog.BUILD);
        preview.setVisibility(judgement && !session.passed() ? View.GONE : View.VISIBLE);
        fold.setVisibility(judgement && !session.passed() ? View.GONE : View.VISIBLE);
        fold.setText(preview.closed() ? "Bentangkan" : "Lipat");
        fold.setEnabled(!preview.moving() && EngklekNet.inspect(previewMask, task.cuboid).faces.size() == 6);
        primary.setEnabled(!preview.moving() && (task.kind != EngklekCatalog.OBSERVE || session.passed()));
        primary.setText(session.passed() ? session.taskIndex() == EngklekCatalog.count(session.level()) - 1
                ? "Selesaikan tahap" : "Kegiatan berikutnya" : "Periksa jawaban");
        feedback.setText(message.isEmpty() ? task.kind == EngklekCatalog.OBSERVE
                ? "Tekan Lipat, amati sampai selesai, lalu tekan Bentangkan." : "Pilih jawabanmu. Gunakan Petunjuk bila diperlukan." : message);
    }

    private void renderCompletion(LinearLayout page) {
        EngklekUi.add(page, EngklekUi.text(this, "Tahap selesai!", 28, true), 28);
        EngklekUi.add(page, EngklekUi.text(this, "Kamu telah menyelesaikan "
                + EngklekCatalog.STAGES[session.level()] + " Engklek.", 18, false), 12);
        if (session.level() < 2) EngklekUi.add(page, EngklekUi.button(this, "Lanjut ke "
                + EngklekCatalog.STAGES[session.level() + 1], () -> {
            startActivity(new Intent(this, EngklekActivity.class).putExtra("level", session.level() + 1));
            finish();
        }), 24);
        EngklekUi.add(page, EngklekUi.button(this, "Kembali ke jalur Engklek", this::finish), 12);
        EngklekUi.install(this, page);
    }
    private void save() { if (store != null && session != null) store.save(session); }
    @Override protected void onPause() {
        if (preview != null) preview.stop();
        save();
        super.onPause();
    }
}
