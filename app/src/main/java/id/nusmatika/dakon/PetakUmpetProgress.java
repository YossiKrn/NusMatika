package id.nusmatika.dakon;

/** Earned unlocks and the Nirokke collection survive replay of earlier lessons. */
public final class PetakUmpetProgress {
    public static final int LEVEL_COUNT=3;
    private final String[] snapshots=new String[LEVEL_COUNT];
    private int completedMask;
    public PetakUmpetProgress(String[] encoded,int completed){
        for(int i=0;i<LEVEL_COUNT;i++){if((completed&(1<<i))==0)break;completedMask|=1<<i;}
        if(encoded!=null)for(int i=0;i<Math.min(encoded.length,LEVEL_COUNT);i++){
            if(encoded[i]!=null&&!encoded[i].isEmpty()&&isUnlocked(i))snapshots[i]=PetakUmpetSession.restore(i,encoded[i]).encode();
        }
    }
    public boolean isUnlocked(int level){return valid(level)&&(completedMask&((1<<level)-1))==((1<<level)-1);}
    public boolean isCompleted(int level){return valid(level)&&(completedMask&(1<<level))!=0;}
    public boolean hasSession(int level){return valid(level)&&snapshots[level]!=null;}
    public boolean isSessionComplete(int level){return hasSession(level)&&loadSession(level).isComplete();}
    public PetakUmpetSession loadSession(int level){require(level);return PetakUmpetSession.restore(level,snapshots[level]);}
    public void saveSession(PetakUmpetSession session){
        int level=session.getLevel();if(!isUnlocked(level))return;
        snapshots[level]=session.encode();if(session.isComplete())completedMask|=1<<level;
    }
    public void restartSession(int level){require(level);if(isUnlocked(level))snapshots[level]=new PetakUmpetSession(level).encode();}
    public int currentLevel(){for(int i=0;i<LEVEL_COUNT;i++)if(!isCompleted(i))return i;return LEVEL_COUNT-1;}
    public int inventoryMask(){return isCompleted(1)?PetakUmpetCatalog.ALL_ITEMS:0;}
    public int getCompletedMask(){return completedMask;}
    public String encodedSession(int level){require(level);return snapshots[level];}
    public String progressTitle(int level){
        require(level);if(!hasSession(level))return "Siap dimulai";
        PetakUmpetSession s=loadSession(level);
        return s.isComplete()?"Level selesai":level<2?s.foundCount()+" dari 6 bangun ditemukan":s.isAssembled()?"Susunan L sudah terbentuk":"Hitung penyusun bentuk L";
    }
    private static boolean valid(int level){return level>=0&&level<LEVEL_COUNT;}
    private static void require(int level){if(!valid(level))throw new IllegalArgumentException("Unknown Petak Umpet level");}
}
