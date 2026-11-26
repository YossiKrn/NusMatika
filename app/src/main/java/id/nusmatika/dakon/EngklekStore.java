package id.nusmatika.dakon;

import android.content.Context;
import android.content.SharedPreferences;

/** Sesi Engklek disimpan terpisah; mengulangi tahap tidak menghapus tahap terbuka. */
public final class EngklekStore implements LearningPathStore {
    private final SharedPreferences preferences;

    public EngklekStore(Context context) {
        preferences = context.getSharedPreferences("nusmatika-engklek-v1", Context.MODE_PRIVATE);
    }
    public EngklekSession load(int level) {
        return EngklekSession.restore(level, preferences.getString("level-" + level, null));
    }
    public void save(EngklekSession session) {
        if (!isUnlocked(session.level())) return;
        int earned = completedMask();
        if (session.complete()) earned |= 1 << session.level();
        preferences.edit().putString("level-" + session.level(), session.encode())
                .putInt("completed", earned).apply();
    }
    private int completedMask() {
        int raw = preferences.getInt("completed", 0), result = 0;
        for (int level = 0; level < 3; level++) {
            if ((raw & (1 << level)) == 0) break;
            result |= 1 << level;
        }
        return result;
    }
    @Override public int currentLevel() {
        for (int level = 0; level < 3; level++) if (!isCompleted(level)) return level;
        return 2;
    }
    @Override public boolean isUnlocked(int level) {
        return level >= 0 && level < 3
                && (completedMask() & ((1 << level) - 1)) == ((1 << level) - 1);
    }
    @Override public boolean isCompleted(int level) {
        return level >= 0 && level < 3 && (completedMask() & (1 << level)) != 0;
    }
    @Override public boolean hasSession(int level) {
        return isUnlocked(level) && preferences.contains("level-" + level);
    }
    @Override public boolean isSessionComplete(int level) {
        return hasSession(level) && load(level).complete();
    }
    @Override public String progressTitle(int level) {
        if (!hasSession(level)) return "Siap dimulai";
        EngklekSession session = load(level);
        return session.complete() ? "Tahap selesai"
                : session.taskIndex() + " dari " + EngklekCatalog.count(level) + " kegiatan selesai";
    }
    @Override public void restartSession(int level) {
        if (isUnlocked(level)) save(new EngklekSession(level));
    }
}
