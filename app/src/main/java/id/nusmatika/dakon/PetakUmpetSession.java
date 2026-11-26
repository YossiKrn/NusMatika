package id.nusmatika.dakon;

/** Three resumable lessons; only an explicit check evaluates the two counts. */
public final class PetakUmpetSession {
    public enum Feedback { NONE, FOUND, MISS, INVALID, INCORRECT, CORRECT }
    private final int level;
    private int foundMask, lastFound=-1;
    private boolean assembled, complete;
    private String balokAnswer="", kubusAnswer="";
    private Feedback feedback=Feedback.NONE;

    public PetakUmpetSession(int level) {
        if(level<0||level>2)throw new IllegalArgumentException("Unknown Petak Umpet level");
        this.level=level;
        // The store only creates this lesson after the Nirokke inventory is earned.
        foundMask=level==2?PetakUmpetCatalog.ALL_ITEMS:0;
    }
    public int getLevel(){return level;}
    public int getFoundMask(){return foundMask;}
    public boolean isComplete(){return complete;}
    public boolean isAssembled(){return assembled;}
    public boolean isSearch(){return level<2&&!complete;}
    public boolean showsMarkers(){return level==0&&!complete;}
    public boolean isFound(int item){return PetakUmpetCatalog.validItem(item)&&(foundMask&(1<<item))!=0;}
    public int foundCount(){return Integer.bitCount(foundMask);}
    public int foundCount(PetakUmpetCatalog.Kind kind){return PetakUmpetCatalog.count(kind,foundMask);}
    public Feedback getFeedback(){return feedback;}
    public String getBalokAnswer(){return balokAnswer;}
    public String getKubusAnswer(){return kubusAnswer;}
    public boolean find(int item){
        if(!isSearch()||!PetakUmpetCatalog.validItem(item)||isFound(item))return false;
        foundMask|=1<<item;lastFound=item;feedback=Feedback.FOUND;return true;
    }
    public void miss(){if(isSearch())feedback=Feedback.MISS;}
    public void setBalokAnswer(String value){if(canEdit()){balokAnswer=bounded(value);feedback=Feedback.NONE;}}
    public void setKubusAnswer(String value){if(canEdit()){kubusAnswer=bounded(value);feedback=Feedback.NONE;}}
    public boolean canEdit(){return level==2&&!assembled&&!complete;}
    public Feedback checkAnswers(){
        if(!canEdit())return feedback;
        int balok=number(balokAnswer),kubus=number(kubusAnswer);
        if(balok<0||kubus<0)feedback=Feedback.INVALID;
        else feedback=answersCorrect()?Feedback.CORRECT:Feedback.INCORRECT;
        return feedback;
    }
    public boolean assemble(){
        if(!canEdit()||feedback!=Feedback.CORRECT)return false;
        assembled=true;return true;
    }
    public boolean finish(){
        if(complete||level<2&&foundMask!=PetakUmpetCatalog.ALL_ITEMS||level==2&&!assembled)return false;
        complete=true;return true;
    }
    public boolean primaryEnabled(){
        return !complete&&(level<2?foundMask==PetakUmpetCatalog.ALL_ITEMS
                :assembled||feedback==Feedback.CORRECT||!balokAnswer.isEmpty()&&!kubusAnswer.isEmpty());
    }
    public String primaryLabel(){return complete?"Kembali ke jalur":level<2||assembled?"Selesai":feedback==Feedback.CORRECT?"Susun bangun":"Periksa";}
    public String feedbackText(){
        if(feedback==Feedback.FOUND&&lastFound>=0)return PetakUmpetCatalog.piece(lastFound).label()+" ditemukan! "
                +(PetakUmpetCatalog.piece(lastFound).kind==PetakUmpetCatalog.Kind.KUBUS?"Semua rusuk kubus sama panjang.":"Balok memiliki panjang, lebar, dan tinggi.");
        if(feedback==Feedback.MISS)return "Cari benda yang berbentuk kubus atau balok.";
        if(feedback==Feedback.INVALID)return "Isi kedua jawaban dengan bilangan bulat.";
        if(feedback==Feedback.INCORRECT)return "Coba lagi. Hitung tiap bangun dari garis sambungannya.";
        if(feedback==Feedback.CORRECT)return assembled?"Semua bangun temuanmu membentuk huruf L.":"Benar! Sekarang susun bangun temuanmu.";
        return "";
    }
    public String encode(){
        return "PU1|"+level+"|"+foundMask+"|"+hex(balokAnswer)+"|"+hex(kubusAnswer)+"|"+(assembled?1:0)+"|"+(complete?1:0);
    }
    public static PetakUmpetSession restore(int level,String encoded){
        PetakUmpetSession fresh=new PetakUmpetSession(level);
        if(encoded==null||encoded.length()>180)return fresh;
        String[] fields=encoded.split("\\|",-1);
        if(fields.length!=7||!"PU1".equals(fields[0]))return fresh;
        try{
            if(parse(fields[1],2)!=level)return fresh;
            PetakUmpetSession saved=new PetakUmpetSession(level);
            saved.foundMask=parse(fields[2],PetakUmpetCatalog.ALL_ITEMS);
            saved.balokAnswer=unhex(fields[3]);saved.kubusAnswer=unhex(fields[4]);
            saved.assembled=parse(fields[5],1)==1;saved.complete=parse(fields[6],1)==1;
            if(level<2){
                if(saved.assembled||!saved.balokAnswer.isEmpty()||!saved.kubusAnswer.isEmpty()
                        ||saved.complete&&saved.foundMask!=PetakUmpetCatalog.ALL_ITEMS)return fresh;
            }else{
                if(saved.foundMask!=PetakUmpetCatalog.ALL_ITEMS||saved.assembled&&!saved.answersCorrect()
                        ||saved.complete&&!saved.assembled)return fresh;
                if(saved.assembled)saved.feedback=Feedback.CORRECT;
            }
            return saved;
        }catch(IllegalArgumentException invalid){return fresh;}
    }
    private boolean answersCorrect(){
        return number(balokAnswer)==PetakUmpetCatalog.count(PetakUmpetCatalog.Kind.BALOK,foundMask)
                &&number(kubusAnswer)==PetakUmpetCatalog.count(PetakUmpetCatalog.Kind.KUBUS,foundMask);
    }
    private static String bounded(String value){String s=value==null?"":value;return s.length()>16?s.substring(0,16):s;}
    private static int number(String text){
        if(text.isEmpty())return -1;
        int value=0;for(int i=0;i<text.length();i++){char c=text.charAt(i);if(c<'0'||c>'9'||value>99)return -1;value=value*10+c-'0';}
        return value<=99?value:-1;
    }
    private static int parse(String text,int max){int n=number(text);if(n<0||n>max)throw new IllegalArgumentException("Invalid metadata");return n;}
    private static String hex(String value){
        String digits="0123456789abcdef";StringBuilder out=new StringBuilder();
        for(int i=0;i<value.length();i++)for(int shift=12;shift>=0;shift-=4)out.append(digits.charAt(value.charAt(i)>>shift&15));return out.toString();
    }
    private static String unhex(String value){
        if(value.length()%4!=0||value.length()>64)throw new IllegalArgumentException("Invalid saved answer");
        StringBuilder out=new StringBuilder();
        for(int i=0;i<value.length();i+=4){int n=0;for(int j=0;j<4;j++){char c=value.charAt(i+j);int d=c>='0'&&c<='9'?c-'0':c>='a'&&c<='f'?c-'a'+10:-1;if(d<0)throw new IllegalArgumentException("Invalid hex");n=n*16+d;}out.append((char)n);}return out.toString();
    }
}
