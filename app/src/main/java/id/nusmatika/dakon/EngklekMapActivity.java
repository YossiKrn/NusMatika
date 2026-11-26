package id.nusmatika.dakon;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Pintu masuk Engklek: tiga tahap 3N dengan progres dan pengulangan mandiri. */
public final class EngklekMapActivity extends Activity {
    private EngklekStore store;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        store = new EngklekStore(this);
    }
    @Override protected void onResume() {
        super.onResume();
        render();
    }
    private void render() {
        LinearLayout page = EngklekUi.column(this);
        EngklekUi.add(page, EngklekUi.button(this, "‹ Pilih permainan", this::finish), 0);
        EngklekUi.add(page, EngklekUi.text(this, "Engklek", 30, true), 20);
        EngklekUi.add(page, EngklekUi.text(this, "Lompat di papan, temukan jaring-jaring!", 19, true), 6);
        EngklekUi.add(page, EngklekUi.text(this,
                "Pelajari cara menyusun dan menguraikan kubus serta balok melalui"
                + " bidang-bidang pada papan engklek.", 16, false), 8);
        for (int level = 0; level < 3; level++) {
            final int stage = level;
            LinearLayout card = EngklekUi.column(this);
            card.setBackground(EngklekUi.background(0xFFF5F9F0, 0xFFDBE8CF, this));
            boolean unlocked = store.isUnlocked(level);
            String status = !unlocked ? "Terkunci" : store.isCompleted(level) ? "Selesai ✓" : "Terbuka";
            TextView title = EngklekUi.text(this, (level + 1) + ". " + EngklekCatalog.STAGES[level], 22, true);
            EngklekUi.add(card, title, 0);
            EngklekUi.add(card, EngklekUi.text(this, EngklekCatalog.SUBTITLES[level], 16, false), 4);
            EngklekUi.add(card, EngklekUi.text(this, status + " · "
                    + (unlocked ? store.progressTitle(level) : "Selesaikan tahap sebelumnya"), 14, false), 8);
            String action = store.isSessionComplete(level) ? "Ulangi tahap"
                    : store.hasSession(level) ? "Lanjutkan" : "Mulai";
            Button start = EngklekUi.button(this, unlocked ? action : "Terkunci", () -> {
                if (store.isSessionComplete(stage)) store.restartSession(stage);
                startActivity(new Intent(this, EngklekActivity.class).putExtra("level", stage));
            });
            start.setEnabled(unlocked);
            EngklekUi.add(card, start, 12);
            EngklekUi.add(page, card, 18);
        }
        EngklekUi.install(this, page);
    }
}
