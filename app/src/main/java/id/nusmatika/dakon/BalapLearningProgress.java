package id.nusmatika.dakon;

/** Pure lesson-path state. Completion history survives replay of any unlocked node. */
public final class BalapLearningProgress {
    public static final int LEVEL_COUNT = 3;
    private final String[] snapshots = new String[LEVEL_COUNT];
    private int completedMask;

    public BalapLearningProgress(String[] encodedSessions, int completed) {
        for (int level = 0; level < LEVEL_COUNT; level++) {
            if ((completed & (1 << level)) == 0) break;
            completedMask |= 1 << level;
        }
        if (encodedSessions != null) {
            for (int level = 0; level < Math.min(LEVEL_COUNT, encodedSessions.length); level++) {
                String encoded = encodedSessions[level];
                if (encoded != null && !encoded.isEmpty()) {
                    BalapKarungSession restored = BalapKarungSession.restoreForLevel(level, encoded);
                    // A damaged snapshot is treated as fresh, while earned unlocks remain.
                    snapshots[level] = restored.encode();
                }
            }
        }
    }

    public BalapKarungSession loadSession(int level) {
        requireLevel(level);
        return BalapKarungSession.restoreForLevel(level, snapshots[level]);
    }

    public void saveSession(BalapKarungSession session) {
        int level = session.getLevel(); requireLevel(level);
        if (!isUnlocked(level)) return;
        snapshots[level] = session.encode();
        if (session.isLevelComplete()) completedMask |= 1 << level;
    }

    public void restartSession(int level) {
        requireLevel(level);
        if (isUnlocked(level)) snapshots[level] = BalapKarungSession.forLevel(level).encode();
    }

    public boolean hasSession(int level) { return validLevel(level) && snapshots[level] != null; }
    public boolean isCompleted(int level) { return validLevel(level) && (completedMask & (1 << level)) != 0; }
    public boolean isSessionComplete(int level) { return hasSession(level) && loadSession(level).isLevelComplete(); }
    public boolean isUnlocked(int level) {
        if (!validLevel(level)) return false;
        int previous = (1 << level) - 1;
        return (completedMask & previous) == previous;
    }
    public int currentLevel() {
        for (int level = 0; level < LEVEL_COUNT; level++) if (!isCompleted(level)) return level;
        return LEVEL_COUNT - 1;
    }
    public String progressTitle(int level) {
        requireLevel(level);
        if (!hasSession(level)) return "Siap dimulai";
        BalapKarungSession session = loadSession(level);
        if (session.isLevelComplete()) return "Level selesai";
        return session.isTriangleTutorial() ? session.isTriangleLineIntro() ? "Kenali garis JKW" : "Contoh JKW · " + (session.getTriangleExampleIndex() + 1) + "/3"
                : level == BalapKarungSession.NITENI ? "Kenali balap karung" : "Soal " + (session.getIndex() + 1) + " dari 3";
    }
    public int getCompletedMask() { return completedMask; }
    public String encodedSession(int level) { requireLevel(level); return snapshots[level]; }

    private static boolean validLevel(int level) { return level >= 0 && level < LEVEL_COUNT; }
    private static void requireLevel(int level) {
        if (!validLevel(level)) throw new IllegalArgumentException("Unknown Balap Karung level");
    }
}
