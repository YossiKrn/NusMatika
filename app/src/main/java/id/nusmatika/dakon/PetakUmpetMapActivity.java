package id.nusmatika.dakon;

import android.app.Activity;

/** User-defined 3N sequence: marked search, unmarked search, numeric assembly task. */
public final class PetakUmpetMapActivity extends LearningPathMapActivity {
    private static final String[] SUBTITLES={"Kenali bangun bertanda","Cari bangun yang bersembunyi","Hitung & susun bentuk L"};
    private static final String[] DESCRIPTIONS={
        "Ketuk bangun bertanda. Kenali kubus dan balok di halaman sekolah.",
        "Cari enam bangun tanpa penanda. Simpan semuanya untuk disusun.",
        "Gunakan semua bangun temuanmu. Isi jumlah balok dan kubus, lalu susun bentuk L."
    };
    @Override protected LearningPathStore createLearningStore(){return new PetakUmpetStore(this);}
    @Override protected String courseTitle(){return "Petak Umpet";}
    @Override protected int levelCount(){return PetakUmpetProgress.LEVEL_COUNT;}
    @Override protected String levelTitle(int level){return PetakUmpetCatalog.levelTitle(level);}
    @Override protected String levelSubtitle(int level){return SUBTITLES[level];}
    @Override protected String levelDescription(int level){return DESCRIPTIONS[level];}
    @Override protected Class<? extends Activity> lessonActivity(){return PetakUmpetActivity.class;}
    @Override protected boolean hiddenShapeCourse(){return true;}
    @Override protected float nodeStep(){return 150f;}
}
