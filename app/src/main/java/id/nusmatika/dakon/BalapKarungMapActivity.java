package id.nusmatika.dakon;

import android.app.Activity;

/** Three 3N stages using the same path artwork as Dakon. */
public final class BalapKarungMapActivity extends LearningPathMapActivity {
    private static final String[] TITLES = {"Niteni", "Nirokke", "Nambahi"};
    private static final String[] SUBTITLES = {
        "Segitiga JKW & balapan", "Contoh jarak, waktu & kecepatan", "Soal cerita & LKPD"
    };
    private static final String[] DESCRIPTIONS = {
        "Kenali segitiga JKW, lalu amati balapan.",
        "Ikuti tiga contoh menghitung lewat balapan.",
        "Selesaikan tiga soal cerita dengan caramu."
    };
    @Override protected LearningPathStore createLearningStore() { return new BalapLearningStore(this); }
    @Override protected String courseTitle() { return "Balap Karung"; }
    @Override protected int levelCount() { return TITLES.length; }
    @Override protected String levelTitle(int level) { return TITLES[level]; }
    @Override protected String levelSubtitle(int level) { return SUBTITLES[level]; }
    @Override protected String levelDescription(int level) { return DESCRIPTIONS[level]; }
    @Override protected Class<? extends Activity> lessonActivity() { return BalapKarungActivity.class; }
    @Override protected boolean sackRaceCourse() { return true; }
    @Override protected float nodeStep() { return 150f; }
}
