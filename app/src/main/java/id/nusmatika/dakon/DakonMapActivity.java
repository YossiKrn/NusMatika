package id.nusmatika.dakon;

import android.app.Activity;

/** Dakon metadata over the shared approved learning path. */
public final class DakonMapActivity extends LearningPathMapActivity {
    private static final String[] DESCRIPTIONS = {
        "Coba menaruh, menghapus, dan mengurungkan biji.",
        "Ikuti contoh KPK dan FPB dua, lalu tiga bilangan.",
        "Jawab empat soal dua bilangan. Bantuan tersedia.",
        "Coba empat soal tiga bilangan dengan caramu."
    };
    @Override protected LearningPathStore createLearningStore() { return new DakonLearningStore(this); }
    @Override protected String courseTitle() { return "Dakon"; }
    @Override protected int levelCount() { return DakonMapCatalog.count(); }
    @Override protected String levelTitle(int level) { return DakonMapCatalog.title(level); }
    @Override protected String levelSubtitle(int level) { return DakonMapCatalog.subtitle(level); }
    @Override protected String levelDescription(int level) { return DESCRIPTIONS[level]; }
    @Override protected Class<? extends Activity> lessonActivity() { return DakonLearningActivity.class; }
}
