package id.nusmatika.dakon;

import android.content.Context;
import android.content.SharedPreferences;

/** No preferences belonging to Dakon or Balap are read or changed. */
public final class PetakUmpetStore implements LearningPathStore {
    private final SharedPreferences preferences;
    private final PetakUmpetProgress progress;
    public PetakUmpetStore(Context context){
        preferences=context.getSharedPreferences("nusmatika-petak-umpet-learning",Context.MODE_PRIVATE);
        String[] saved=new String[PetakUmpetProgress.LEVEL_COUNT];
        for(int i=0;i<saved.length;i++)saved[i]=preferences.getString("level-"+i,null);
        progress=new PetakUmpetProgress(saved,preferences.getInt("completed-levels",0));
    }
    public PetakUmpetSession loadSession(int level){return progress.loadSession(level);}
    public void saveSession(PetakUmpetSession session){progress.saveSession(session);persist();}
    @Override public boolean isUnlocked(int level){return progress.isUnlocked(level);}
    @Override public boolean isCompleted(int level){return progress.isCompleted(level);}
    @Override public boolean hasSession(int level){return progress.hasSession(level);}
    @Override public boolean isSessionComplete(int level){return progress.isSessionComplete(level);}
    @Override public int currentLevel(){return progress.currentLevel();}
    @Override public String progressTitle(int level){return progress.progressTitle(level);}
    @Override public void restartSession(int level){progress.restartSession(level);persist();}
    public int inventoryMask(){return progress.inventoryMask();}
    private void persist(){
        SharedPreferences.Editor edit=preferences.edit().putInt("completed-levels",progress.getCompletedMask());
        for(int i=0;i<PetakUmpetProgress.LEVEL_COUNT;i++){String s=progress.encodedSession(i);if(s!=null)edit.putString("level-"+i,s);}
        edit.commit();
    }
}
