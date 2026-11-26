package id.nusmatika.dakon;

import android.content.Context;
import android.content.SharedPreferences;

/** Per-node progress; the former BK1 lesson and its sound preference remain untouched. */
public final class BalapLearningStore implements LearningPathStore {
    public static final int LEVEL_COUNT = BalapLearningProgress.LEVEL_COUNT;
    private static final String COMPLETED = "completed-levels";
    private final SharedPreferences preferences;
    private final BalapLearningProgress progress;

    public BalapLearningStore(Context context) {
        preferences = context.getSharedPreferences("nusmatika-balap-learning", Context.MODE_PRIVATE);
        SharedPreferences legacy = context.getSharedPreferences("nusmatika-balap-karung", Context.MODE_PRIVATE);
        boolean migrated = preferences.getBoolean("legacy-imported", false);
        String[] snapshots = new String[LEVEL_COUNT];
        for (int level = 0; level < LEVEL_COUNT; level++) snapshots[level] = preferences.getString(sessionKey(level), null);
        progress = new BalapLearningProgress(snapshots, preferences.getInt(COMPLETED, 0));
        if (!migrated) {
            String encoded = legacy.getString("session", "");
            if (!encoded.isEmpty()) {
                for (int level = 0; level < LEVEL_COUNT; level++) {
                    if (!progress.hasSession(level)) progress.saveSession(BalapKarungSession.migrateLegacyLevel(level, encoded));
                }
            }
            if (!preferences.contains("sound")) preferences.edit().putBoolean("sound", legacy.getBoolean("sound", false)).apply();
            persist();
            preferences.edit().putBoolean("legacy-imported", true).commit();
        }
    }

    public BalapKarungSession loadSession(int level) { return progress.loadSession(level); }
    public void saveSession(BalapKarungSession session) { progress.saveSession(session); persist(); }
    @Override public boolean hasSession(int level) { return progress.hasSession(level); }
    @Override public boolean isUnlocked(int level) { return progress.isUnlocked(level); }
    @Override public boolean isCompleted(int level) { return progress.isCompleted(level); }
    @Override public boolean isSessionComplete(int level) { return progress.isSessionComplete(level); }
    @Override public int currentLevel() { return progress.currentLevel(); }
    @Override public String progressTitle(int level) { return progress.progressTitle(level); }
    @Override public void restartSession(int level) { progress.restartSession(level); persist(); }
    public boolean isSoundEnabled() { return preferences.getBoolean("sound", false); }
    public void setSoundEnabled(boolean sound) { preferences.edit().putBoolean("sound", sound).apply(); }

    private void persist() {
        SharedPreferences.Editor editor = preferences.edit().putInt(COMPLETED, progress.getCompletedMask());
        for (int level = 0; level < LEVEL_COUNT; level++) {
            String encoded = progress.encodedSession(level);
            if (encoded != null) editor.putString(sessionKey(level), encoded);
        }
        // Commit before returning to the map so its newly constructed store sees the unlock.
        editor.commit();
    }
    private static String sessionKey(int level) { return "level-" + level; }
}
