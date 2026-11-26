package id.nusmatika.dakon;

/** Shared map progress contract; each game retains its own lesson data. */
public interface LearningPathStore {
    int currentLevel();
    boolean isUnlocked(int level);
    boolean isCompleted(int level);
    boolean hasSession(int level);
    boolean isSessionComplete(int level);
    String progressTitle(int level);
    void restartSession(int level);
}
