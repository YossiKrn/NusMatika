package id.nusmatika.dakon;

import android.content.Context;
import android.content.SharedPreferences;

/** Saves the four-level sequence without replacing legacy game work. */
public final class DakonLearningStore implements LearningPathStore {
    public static final int LEVEL_COUNT = 4;
    private static final String COMPLETED = "completed-levels";
    private final SharedPreferences preferences;

    public DakonLearningStore(Context context) {
        preferences = context.getSharedPreferences("nusmatika-dakon-learning", Context.MODE_PRIVATE);
    }

    public DakonLearningSession loadSession(int level) {
        requireLevel(level);
        return DakonLearningSession.restore(level, preferences.getString(sessionKey(level), ""));
    }

    public void saveSession(DakonLearningSession session) {
        int level = session.getLevel();
        requireLevel(level);
        preferences.edit().putString(sessionKey(level), session.encode()).apply();
    }

    public boolean hasSession(int level) {
        return validLevel(level) && preferences.contains(sessionKey(level));
    }

    @Override public boolean isSessionComplete(int level) {
        return validLevel(level) && loadSession(level).isComplete();
    }

    public boolean isUnlocked(int level) {
        if (!validLevel(level)) return false;
        int previous = (1 << level) - 1;
        return (completedMask() & previous) == previous;
    }

    public boolean isCompleted(int level) {
        return validLevel(level) && (completedMask() & (1 << level)) != 0;
    }

    public void markComplete(int level) {
        requireLevel(level);
        if (!isUnlocked(level) || !loadSession(level).isComplete()) return;
        preferences.edit().putInt(COMPLETED, completedMask() | (1 << level)).commit();
    }

    public void restartSession(int level) {
        requireLevel(level);
        if (isUnlocked(level)) saveSession(new DakonLearningSession(level));
    }

    public int currentLevel() {
        for (int level = 0; level < LEVEL_COUNT; level++) {
            if (!isCompleted(level)) return level;
        }
        return LEVEL_COUNT - 1;
    }

    public String progressTitle(int level) {
        if (!hasSession(level)) return "Siap dimulai";
        DakonLearningSession session = loadSession(level);
        return session.isComplete() ? "Level selesai" : session.progressText();
    }

    private int completedMask() {
        int mask = preferences.getInt(COMPLETED, 0);
        // A later completion cannot unlock a sequence whose earlier flag is absent.
        int prefix = 0;
        for (int level = 0; level < LEVEL_COUNT; level++) {
            if ((mask & (1 << level)) == 0) break;
            prefix |= 1 << level;
        }
        return prefix;
    }

    private static String sessionKey(int level) {
        return "level-" + level;
    }

    private static boolean validLevel(int level) {
        return level >= 0 && level < LEVEL_COUNT;
    }

    private static void requireLevel(int level) {
        if (!validLevel(level)) throw new IllegalArgumentException("Unknown Dakon level");
    }
}
