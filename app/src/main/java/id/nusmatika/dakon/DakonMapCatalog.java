package id.nusmatika.dakon;

import android.content.Context;

/** Material labels and availability for the current offline Dakon prototype. */
public final class DakonMapCatalog {
    private static final String[] TITLES = {
            "Niteni",
            "Nirokke",
            "Nambahi",
            "Nambahi"
    };
    private static final String[] SUBTITLES = {
            "Kenali dakon",
            "Contoh dua & tiga bilangan",
            "Dua bilangan & LKPD",
            "Tiga bilangan & LKPD"
    };

    private DakonMapCatalog() { }

    public static int count() {
        return TITLES.length;
    }

    public static String title(int index) {
        return validIndex(index) ? TITLES[index] : "Dakon";
    }

    public static String subtitle(int index) {
        return validIndex(index) ? SUBTITLES[index] : "";
    }

    public static boolean isPlayable(int index) {
        return index >= 0 && index < DakonLearningStore.LEVEL_COUNT;
    }

    public static int playableIndex(Context context) {
        return new DakonLearningStore(context).currentLevel();
    }

    public static String progressTitle(Context context) {
        DakonLearningStore store = new DakonLearningStore(context);
        return store.progressTitle(store.currentLevel());
    }

    private static boolean validIndex(int index) {
        return index >= 0 && index < TITLES.length;
    }
}
