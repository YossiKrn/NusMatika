package id.nusmatika.dakon;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

/** Four resumable lessons sharing the approved 24 numbered holes. */
public final class DakonLearningSession {
    public static final int PURPLE = 3;
    public static final int UNDO = 100, CLEAR = 101;
    private static final int NONE = 0, WRONG = 1, CORRECT = 2, INVALID = 3;
    private static final int MAX_HISTORY = 100;
    private static final Question[] QUESTIONS = {
        new Question(true,2,3,0), new Question(true,3,4,0),
        new Question(true,4,6,0), new Question(true,3,8,0),
        new Question(true,6,8,0), new Question(true,4,5,0),
        new Question(false,8,12,0), new Question(false,12,18,0),
        new Question(false,16,24,0), new Question(false,10,15,0),
        new Question(false,9,12,0), new Question(false,14,21,0),
        new Question(true,3,4,1), new Question(false,12,18,2),
        new Question(true,2,3,4,0), new Question(true,2,4,6,0),
        new Question(true,3,4,6,0), new Question(true,2,3,8,0),
        new Question(true,3,6,8,0), new Question(true,4,6,8,0),
        new Question(false,6,12,18,0), new Question(false,8,16,24,0),
        new Question(false,6,10,14,0), new Question(false,9,12,18,0),
        new Question(false,4,8,12,0), new Question(false,12,18,24,0),
        new Question(true,2,3,4,3), new Question(false,6,12,18,4)
    };
    private static final Question[] GUIDES = {QUESTIONS[1],QUESTIONS[7],QUESTIONS[14],QUESTIONS[20]};
    private static final int[][] GUIDE_HOLES = new int[GUIDES.length][];
    private static final int[][] GUIDE_TOOLS = new int[GUIDES.length][];
    static {
        for(int example=0;example<GUIDES.length;example++){
            List<Integer> holes=new ArrayList<Integer>(),tools=new ArrayList<Integer>();
            Question q=GUIDES[example];
            for(int operand=0;operand<q.count();operand++){
                int value=q.operand(operand),tool=operand==2?PURPLE:operand;
                int limit=example==2?12:24;
                for(int hole=1;hole<=limit;hole++)if(q.kpk?hole%value==0:value%hole==0){
                    holes.add(hole);tools.add(tool);
                }
            }
            GUIDE_HOLES[example]=ints(holes);GUIDE_TOOLS[example]=ints(tools);
        }
    }
    private final int level;
    private Board board=new Board();
    private final Deque<String> history=new ArrayDeque<String>();
    private int introStep,guideExample,guideStep,guideChoice,questionIndex,result;
    private int[] questionOrder;
    private String answer="",working="";
    private boolean complete;

    public DakonLearningSession(int level){
        if(level<0||level>3)throw new IllegalArgumentException("Unknown Dakon level");
        this.level=level;questionOrder=level>=2?shuffledQuestions(level):new int[0];
    }
    public int getLevel(){return level;}
    public int[] getSeeds(int hole){return hole>=1&&hole<=24?board.seeds[hole-1].clone():new int[0];}
    /** Compatibility projection for consumers of the legacy two-color artwork. */
    public DakonSession getBoard(){
        DakonSession projected=new DakonSession();projected.clearBoard();
        for(int hole=1;hole<=24;hole++)for(int kind:board.seeds[hole-1])if(kind!=PURPLE){projected.setTool(kind);projected.tapHole(hole);}
        projected.setTool(board.tool==PURPLE?DakonSession.BLUE:board.tool);return DakonSession.restore(projected.encode());
    }
    public String title(){return level==0?"Niteni":level==1?"Nirokke":"Nambahi";}
    public int questionNumberA(){return level==0?0:currentQuestion().a;}
    public int questionNumberB(){return level==0?0:currentQuestion().b;}
    public int questionNumberC(){return level==0?0:currentQuestion().c;}
    public int operandCount(){return level==0?2:currentQuestion().count();}
    public boolean isKpkQuestion(){return level==0||currentQuestion().kpk;}
    public boolean isWorksheet(){return level>=2&&questionIndex>=2;}
    public boolean isAnswerVisible(){return level>=2;}
    public boolean isComplete(){return complete;}
    public int getTool(){return board.tool;}
    public String seedLabel(int kind){
        if(kind!=0&&kind!=1&&kind!=PURPLE)return "";
        if(level==0)return kind==0?"Biji biru":kind==1?"Biji merah":"Biji ungu";
        int number=kind==0?questionNumberA():kind==1?questionNumberB():questionNumberC();
        return number==0?"":(isKpkQuestion()?"Kelipatan ":"Faktor ")+number;
    }
    public String blueLabel(){return seedLabel(DakonSession.BLUE);}
    public String coralLabel(){return seedLabel(DakonSession.CORAL);}
    public boolean canShowHint(){return level>=2&&!complete;}
    public String hintText(){
        if(!canShowHint())return "";
        if(isWorksheet())return isKpkQuestion()?"Cari kelipatan setiap bilangan. Pertemuan berikutnya memakai kelipatan bersama yang paling kecil.":"Cari bilangan yang habis membagi semua jumlah biji. Pilih yang paling besar agar kelompoknya paling banyak.";
        return isKpkQuestion()?"Tandai kelipatan tiap bilangan dengan warna berbeda. Cari lubang yang memiliki semua warna, lalu pilih yang paling kecil.":"Tandai faktor tiap bilangan dengan warna berbeda. Faktor membagi bilangan tanpa sisa. Cari lubang yang memiliki semua warna, lalu pilih yang paling besar.";
    }
    public String instruction(){
        if(complete)return "Latihan selesai. Kerja bagus!";
        if(level==0){switch(introStep){
            case 0:return "Ketuk lubang 5 untuk mengenal papan.";
            case 1:return "Pilih alat Biji biru.";
            case 2:return "Letakkan satu biji biru di lubang 5.";
            case 3:return getTool()==DakonSession.REMOVE?"Ketuk lubang 5 untuk menghapus bijinya.":"Pilih Hapus, lalu ketuk lubang 5.";
            case 4:return "Ketuk Urungkan untuk mengembalikan biji.";
            case 5:return "Ketuk Kosongkan untuk mengosongkan papan.";
            default:return "Kamu sudah mencoba alat dakon. Ketuk Selesai.";
        }}
        if(level==1){
            if(result==CORRECT)return isKpkQuestion()?"KPK memilih kelipatan bersama paling kecil.":"FPB memilih faktor bersama paling besar.";
            if(guideStep<GUIDE_HOLES[guideExample].length){
                int tool=expectedGuideTool(),number=tool==0?questionNumberA():tool==1?questionNumberB():questionNumberC(),hole=highlightHole();
                if(isKpkQuestion())return number+" × "+hole/number+" = "+hole+". Taruh biji "+(tool==0?"biru":tool==1?"merah":"ungu")+" di "+hole+".";
                return number+" ÷ "+hole+" = "+number/hole+", tanpa sisa. Ketuk "+hole+".";
            }
            return isKpkQuestion()?"Cari lubang dengan semua warna. Pilih yang terkecil.":"Cari lubang dengan semua warna. Pilih yang terbesar.";
        }
        return isWorksheet()?"Baca cerita, tulis cara dan jawabanmu.":"Tentukan "+(isKpkQuestion()?"KPK ":"FPB ")+currentQuestion().numbers()+". Coba caramu.";
    }
    public String prompt(){
        if(level==0)return "Kenali papan dan alat dakon.";Question q=currentQuestion();
        if(level==1)return(q.kpk?"KPK ":"FPB ")+q.numbers();
        switch(q.story){
            case 1:return "Nara berlatih dakon setiap 3 hari. Bima berlatih setiap 4 hari. Hari ini mereka berlatih bersama. Berapa hari lagi mereka akan berlatih bersama? Tuliskan caramu.";
            case 2:return "Ada 12 biji biru dan 18 biji merah. Semua biji dibagi menjadi kelompok yang sama: setiap kelompok memiliki jumlah biji biru yang sama dan jumlah biji merah yang sama. Berapa kelompok paling banyak yang dapat dibuat? Tuliskan caramu.";
            case 3:return "Nara berlatih setiap 2 hari, Bima setiap 3 hari, dan Tari setiap 4 hari. Hari ini mereka berlatih bersama. Berapa hari lagi ketiganya berlatih bersama? Tuliskan caramu.";
            case 4:return "Ada 6 biji biru, 12 biji merah, dan 18 biji ungu. Tiap warna dibagi rata ke beberapa kelompok tanpa sisa. Berapa kelompok paling banyak yang dapat dibuat? Tuliskan caramu.";
            default:return "Berapakah "+(q.kpk?"KPK ":"FPB ")+q.numbers()+"?";
        }
    }
    public String progressText(){
        if(level==0)return "Alat "+Math.min(introStep+1,6)+" / 6";
        if(level==1)return "Contoh "+(guideExample+1)+" / 4";
        return "Soal "+(questionIndex+1)+" / 4"+(isWorksheet()?" · LKPD":"");
    }
    public float progressFraction(){
        if(complete)return 1f;if(level==0)return introStep/6f;
        if(level==1){int total=0,finished=guideStep+(result==CORRECT?1:0);for(int i=0;i<GUIDE_HOLES.length;i++){int actions=GUIDE_HOLES[i].length+1;total+=actions;if(i<guideExample)finished+=actions;}return finished/(float)total;}
        return(questionIndex+(result==CORRECT?1:0))/4f;
    }
    public int highlightHole(){
        if(complete)return -1;
        if(level==0&&(introStep==0||introStep==2||introStep==3&&getTool()==DakonSession.REMOVE))return 5;
        return level==1&&guideStep<GUIDE_HOLES[guideExample].length?GUIDE_HOLES[guideExample][guideStep]:-1;
    }
    public int highlightTool(){
        if(complete||level!=0)return -1;if(introStep==1)return DakonSession.BLUE;
        if(introStep==3&&getTool()!=DakonSession.REMOVE)return DakonSession.REMOVE;
        return introStep==4?UNDO:introStep==5?CLEAR:-1;
    }
    public int[] commonHoles(){
        if(level!=1||guideStep<GUIDE_HOLES[guideExample].length)return new int[0];
        List<Integer> shared=new ArrayList<Integer>();
        for(int hole=1;hole<=24;hole++){
            boolean blue=false,coral=false,purple=false;
            for(int kind:board.seeds[hole-1]){if(kind==0)blue=true;if(kind==1)coral=true;if(kind==PURPLE)purple=true;}
            if(blue&&coral&&(operandCount()==2||purple))shared.add(hole);
        }
        return ints(shared);
    }
    public boolean selectTool(int tool){
        if(complete||!validTool(tool)||tool==PURPLE&&operandCount()!=3)return false;
        if(level==0){if(introStep==1&&tool==DakonSession.BLUE){board.tool=tool;introStep=2;return true;}return introStep==3&&tool==DakonSession.REMOVE&&board.select(tool);}
        return level>=2&&!isWorksheet()&&board.select(tool);
    }
    public boolean tapHole(int hole){
        if(complete||hole<1||hole>24)return false;
        if(level==0){
            if(hole!=5)return false;if(introStep==0){introStep=1;return true;}
            if(introStep==2&&getTool()==DakonSession.BLUE||introStep==3&&getTool()==DakonSession.REMOVE){if(!editHole(hole))return false;introStep++;return true;}return false;
        }
        if(level==1){
            if(result==CORRECT)return false;
            if(guideStep<GUIDE_HOLES[guideExample].length){if(hole!=highlightHole())return false;board.tap(hole);guideStep++;board.tool=expectedGuideTool();return true;}
            if(hole!=currentQuestion().solution()){boolean changed=result!=WRONG||guideChoice!=hole;guideChoice=hole;result=WRONG;return changed;}
            guideChoice=0;result=CORRECT;return true;
        }
        return !isWorksheet()&&editHole(hole);
    }
    public boolean canUndo(){return !complete&&!history.isEmpty()&&(level==0&&introStep==4||level>=2&&!isWorksheet());}
    public boolean canClear(){return !complete&&board.hasSeeds()&&(level==0&&introStep==5||level>=2&&!isWorksheet());}
    public boolean canRemove(){return !complete&&(level==0&&introStep==3||level>=2&&!isWorksheet());}
    public boolean undo(){if(!canUndo())return false;int tool=board.tool;board=Board.decode(history.removeLast());board.tool=tool;result=NONE;if(level==0)introStep=5;return true;}
    public boolean clearBoard(){if(!canClear())return false;remember(board.encode());board.clear();result=NONE;if(level==0)introStep=6;return true;}
    public String getAnswer(){return answer;}
    public void setAnswer(String value){if(level>=2&&!complete){answer=value==null?"":value;result=NONE;}}
    public String getWorking(){return working;}
    public void setWorking(String value){if(level<2||complete)return;String updated=value==null?"":value;if(!working.equals(updated)){working=updated;result=NONE;}}
    public String answerUnit(){return level>=2&&currentQuestion().story!=0?(isKpkQuestion()?"hari":"kelompok"):"";}
    public String feedback(){
        if(result==NONE)return "";if(result==INVALID)return "Tulis jawaban dengan bilangan bulat positif.";
        if(result==WRONG&&level==1){if(currentQuestion().shares(guideChoice))return isKpkQuestion()?guideChoice+" juga kelipatan bersama. Cari yang paling kecil.":"Itu faktor bersama. Cari yang paling besar.";return operandCount()==2?"Pilih lubang yang dimiliki kedua bilangan.":"Pilih lubang yang dimiliki ketiga bilangan.";}
        if(result==WRONG)return isKpkQuestion()?"Coba lagi. Cari kelipatan bersama yang paling kecil.":"Coba lagi. Cari faktor bersama yang paling besar.";
        if(level==1)return "Benar! "+(isKpkQuestion()?"KPK ":"FPB ")+currentQuestion().numbers()+" adalah "+currentQuestion().solution()+".";
        return "Benar! "+(isKpkQuestion()?"KPK":"FPB")+" sudah ditemukan.";
    }
    public boolean isFeedbackCorrect(){return result==CORRECT;}
    public String primaryLabel(){if(complete||level==0)return "Selesai";if(level==1)return guideExample==GUIDES.length-1?"Selesai":"Lanjut";return result==CORRECT?(questionIndex==3?"Selesai":"Lanjut"):"Periksa";}
    public boolean primaryEnabled(){return !complete&&(level==0?introStep==6:level==1?result==CORRECT:result==CORRECT||!answer.isEmpty());}
    public boolean primaryAction(){
        if(!primaryEnabled())return false;if(level==0){complete=true;return true;}
        if(level==1){if(guideExample==GUIDES.length-1){complete=true;return true;}guideExample++;guideStep=0;resetQuestionWork();return true;}
        if(result!=CORRECT){int parsed=positiveNumber(answer);result=parsed<1?INVALID:parsed==currentQuestion().solution()?CORRECT:WRONG;return true;}
        if(questionIndex==3){complete=true;return true;}questionIndex++;resetQuestionWork();return true;
    }
    /** DL2 stores all three colors; DL1 is validated and migrated below. */
    public String encode(){
        StringBuilder saved=new StringBuilder("DL2|");saved.append(level).append('|').append(complete?1:0).append('|').append(introStep).append('|').append(guideExample).append('|').append(guideStep).append('|').append(questionIndex).append('|');
        for(int i=0;i<questionOrder.length;i++){if(i>0)saved.append(',');saved.append(questionOrder[i]);}
        saved.append('|').append(hex(answer)).append('|').append(hex(working)).append('|').append(result).append('|').append(hex(board.encode())).append('|');
        boolean first=true;for(String previous:history){if(!first)saved.append(',');first=false;saved.append(hex(previous));}return saved.append('|').append(guideChoice).toString();
    }
    public static DakonLearningSession restore(int level,String encoded){
        DakonLearningSession fresh=new DakonLearningSession(level);if(encoded==null||encoded.length()>4000000)return fresh;
        String[] fields=encoded.split("\\|",-1);boolean legacy=fields.length>=1&&"DL1".equals(fields[0]);
        if((fields.length!=14&&!(legacy&&fields.length==13))||!legacy&&!"DL2".equals(fields[0]))return fresh;
        try{
            if(number(fields[1],legacy?2:3)!=level)return fresh;DakonLearningSession candidate=new DakonLearningSession(level);
            candidate.complete=number(fields[2],1)==1;candidate.introStep=number(fields[3],6);candidate.guideExample=number(fields[4],legacy?1:3);candidate.guideStep=number(fields[5],26);candidate.questionIndex=number(fields[6],legacy?5:3);
            candidate.questionOrder=decodeOrder(fields[7],legacy?6:4);candidate.answer=unhex(fields[8]);candidate.working=unhex(fields[9]);candidate.result=number(fields[10],INVALID);candidate.guideChoice=fields.length==14?number(fields[13],24):0;
            candidate.board=legacy?Board.fromLegacy(unhex(fields[11])):Board.decode(unhex(fields[11]));
            if(!fields[12].isEmpty()){String[] entries=fields[12].split(",",-1);if(entries.length>MAX_HISTORY)return fresh;for(String entry:entries){String raw=unhex(entry);candidate.history.addLast((legacy?Board.fromLegacy(raw):Board.decode(raw)).encode());}}
            if(legacy){if(!candidate.consistentLegacy())return fresh;candidate.migrateLegacy();}return candidate.consistent()?candidate:fresh;
        }catch(IllegalArgumentException invalid){return fresh;}
    }
    private boolean consistent(){
        if(level==0)return consistentIntro();if(introStep!=0)return false;if(level==1)return consistentGuide(false);
        if(guideExample!=0||guideStep!=0||guideChoice!=0||!validOrder(questionOrder,level)||complete&&(questionIndex!=3||result!=CORRECT)||!colorsAllowed(board))return false;
        for(String previous:history)if(!colorsAllowed(Board.decode(previous)))return false;
        if(isWorksheet()&&(board.hasSeeds()||!history.isEmpty()||getTool()!=DakonSession.BLUE))return false;return consistentAnswer();
    }
    private boolean consistentAnswer(){int parsed=positiveNumber(answer);if(result==CORRECT&&parsed!=currentQuestion().solution())return false;if(result==WRONG&&(parsed<1||parsed==currentQuestion().solution()))return false;return result!=INVALID||parsed<1;}
    private boolean consistentIntro(){
        if(guideExample!=0||guideStep!=0||guideChoice!=0||questionIndex!=0||questionOrder.length!=0||result!=NONE||!answer.isEmpty()||!working.isEmpty()||complete&&introStep!=6)return false;
        DakonLearningSession expected=new DakonLearningSession(0);if(introStep>=1)expected.tapHole(5);if(introStep>=2)expected.selectTool(DakonSession.BLUE);if(introStep>=3)expected.tapHole(5);if(introStep>=4||introStep==3&&getTool()==DakonSession.REMOVE)expected.selectTool(DakonSession.REMOVE);if(introStep>=4)expected.tapHole(5);if(introStep>=5)expected.undo();if(introStep>=6)expected.clearBoard();
        return board.encode().equals(expected.board.encode())&&new ArrayList<String>(history).equals(new ArrayList<String>(expected.history));
    }
    private boolean consistentGuide(boolean legacy){
        if(questionIndex!=0||questionOrder.length!=0||!answer.isEmpty()||!working.isEmpty()||!history.isEmpty()||guideStep>GUIDE_HOLES[guideExample].length||result==INVALID||result!=NONE&&guideStep!=GUIDE_HOLES[guideExample].length||result==WRONG&&(guideChoice<1||guideChoice==currentQuestion().solution())||result!=WRONG&&guideChoice!=0||complete&&(guideExample!=(legacy?1:3)||result!=CORRECT))return false;
        Board expected=new Board();for(int i=0;i<guideStep;i++){expected.tool=GUIDE_TOOLS[guideExample][i];expected.tap(GUIDE_HOLES[guideExample][i]);}expected.tool=expectedGuideTool();return expected.encode().equals(board.encode());
    }
    private boolean consistentLegacy(){
        if(level==0)return consistentIntro();if(introStep!=0)return false;if(level==1)return consistentGuide(true);
        if(guideExample!=0||guideStep!=0||guideChoice!=0||!validLegacyOrder(questionOrder)||complete&&(questionIndex!=5||result!=CORRECT))return false;
        if(questionIndex>=4&&(board.hasSeeds()||!history.isEmpty()||getTool()!=0))return false;return consistentAnswer();
    }
    private void migrateLegacy(){
        if(level==1&&complete){complete=false;guideExample=2;guideStep=0;resetQuestionWork();return;}if(level!=2)return;int[] old=questionOrder;
        if(questionIndex>=4){int multiple=-1,factor=-1;for(int i=0;i<4;i++){if(QUESTIONS[old[i]].kpk&&multiple<0)multiple=old[i];if(!QUESTIONS[old[i]].kpk&&factor<0)factor=old[i];}questionOrder=new int[]{multiple,factor,12,13};questionIndex-=2;return;}
        int current=old[questionIndex],opposite=-1;boolean previouslySeenOpposite=false;
        for(int i=0;i<4;i++)if(QUESTIONS[old[i]].kpk!=QUESTIONS[current].kpk){if(opposite<0)opposite=old[i];if(i<questionIndex){opposite=old[i];previouslySeenOpposite=true;break;}}
        questionOrder=previouslySeenOpposite?new int[]{opposite,current,12,13}:new int[]{current,opposite,12,13};questionIndex=previouslySeenOpposite?1:0;
    }
    private boolean colorsAllowed(Board candidate){if(level==3)return true;if(candidate.tool==PURPLE)return false;for(int[] hole:candidate.seeds)for(int kind:hole)if(kind==PURPLE)return false;return true;}
    private boolean editHole(int hole){String previous=board.encode();if(!board.tap(hole))return false;remember(previous);result=NONE;guideChoice=0;return true;}
    private void remember(String previous){if(history.size()==MAX_HISTORY)history.removeFirst();history.addLast(previous);}
    private void resetQuestionWork(){board=new Board();history.clear();answer="";working="";result=NONE;guideChoice=0;}
    private int expectedGuideTool(){return guideStep<GUIDE_TOOLS[guideExample].length?GUIDE_TOOLS[guideExample][guideStep]:DakonSession.BLUE;}
    private Question currentQuestion(){return level==1?GUIDES[guideExample]:QUESTIONS[questionOrder[questionIndex]];}
    private static int[] shuffledQuestions(int level){
        int offset=level==3?14:0;List<Integer> multiples=new ArrayList<Integer>(),factors=new ArrayList<Integer>();for(int i=0;i<6;i++){multiples.add(offset+i);factors.add(offset+6+i);}Collections.shuffle(multiples);Collections.shuffle(factors);
        List<Integer> mixed=new ArrayList<Integer>(Arrays.asList(multiples.get(0),factors.get(0)));Collections.shuffle(mixed);return new int[]{mixed.get(0),mixed.get(1),offset+12,offset+13};
    }
    private static int[] decodeOrder(String saved,int length){if(saved.isEmpty())return new int[0];String[] ids=saved.split(",",-1);if(ids.length!=length)throw new IllegalArgumentException("Invalid question order");int[] order=new int[length];for(int i=0;i<length;i++)order[i]=number(ids[i],QUESTIONS.length-1);return order;}
    private static boolean validOrder(int[] order,int level){int offset=level==3?14:0;return order.length==4&&order[2]==offset+12&&order[3]==offset+13&&order[0]>=offset&&order[0]<offset+12&&order[1]>=offset&&order[1]<offset+12&&QUESTIONS[order[0]].kpk!=QUESTIONS[order[1]].kpk;}
    private static boolean validLegacyOrder(int[] order){if(order.length!=6||order[4]!=12||order[5]!=13)return false;boolean[] seen=new boolean[12];int multiples=0;for(int i=0;i<4;i++){int id=order[i];if(id>=12||seen[id])return false;seen[id]=true;if(QUESTIONS[id].kpk)multiples++;}return multiples==2;}
    private static int[] ints(List<Integer> list){int[] values=new int[list.size()];for(int i=0;i<values.length;i++)values[i]=list.get(i);return values;}
    private static boolean validTool(int tool){return tool==0||tool==1||tool==DakonSession.REMOVE||tool==PURPLE;}
    private static int positiveNumber(String text){if(text.isEmpty())return -1;int parsed=0;for(int i=0;i<text.length();i++){char digit=text.charAt(i);if(digit<'0'||digit>'9')return -1;parsed=parsed*10+digit-'0';if(parsed>999999)return -1;}return parsed>0?parsed:-1;}
    private static int number(String text,int maximum){if(text.isEmpty())throw new IllegalArgumentException("Missing number");int parsed=0;for(int i=0;i<text.length();i++){char digit=text.charAt(i);if(digit<'0'||digit>'9')throw new IllegalArgumentException("Invalid number");if(parsed>(maximum-(digit-'0'))/10)throw new IllegalArgumentException("Number outside range");parsed=parsed*10+digit-'0';if(parsed>maximum)throw new IllegalArgumentException("Number outside range");}return parsed;}
    private static String hex(String text){String digits="0123456789abcdef";StringBuilder encoded=new StringBuilder(text.length()*4);for(int i=0;i<text.length();i++){int value=text.charAt(i);for(int shift=12;shift>=0;shift-=4)encoded.append(digits.charAt(value>>shift&15));}return encoded.toString();}
    private static String unhex(String encoded){if(encoded.length()%4!=0)throw new IllegalArgumentException("Invalid text");StringBuilder decoded=new StringBuilder(encoded.length()/4);for(int offset=0;offset<encoded.length();offset+=4){int value=0;for(int digit=0;digit<4;digit++){char ch=encoded.charAt(offset+digit);int part=ch>='0'&&ch<='9'?ch-'0':ch>='a'&&ch<='f'?ch-'a'+10:-1;if(part<0)throw new IllegalArgumentException("Invalid text");value=value*16+part;}decoded.append((char)value);}return decoded.toString();}
    private static int gcd(int a,int b){while(b!=0){int next=a%b;a=b;b=next;}return a;}
    private static final class Question {
        final boolean kpk;final int a,b,c,story;
        Question(boolean kpk,int a,int b,int story){this(kpk,a,b,0,story);}
        Question(boolean kpk,int a,int b,int c,int story){this.kpk=kpk;this.a=a;this.b=b;this.c=c;this.story=story;}
        int count(){return c==0?2:3;}
        int operand(int index){return index==0?a:index==1?b:c;}
        String numbers(){return c==0?a+" dan "+b:a+", "+b+", dan "+c;}
        int solution(){int result=kpk?a/gcd(a,b)*b:gcd(a,b);return c==0?result:kpk?result/gcd(result,c)*c:gcd(result,c);}
        boolean shares(int hole){if(hole<1)return false;for(int i=0;i<count();i++)if(kpk?hole%operand(i)!=0:operand(i)%hole!=0)return false;return true;}
    }
    private static final class Board {
        int tool;int[][] seeds=new int[24][];
        Board(){clear();}
        void clear(){for(int i=0;i<24;i++)seeds[i]=new int[0];}
        boolean hasSeeds(){for(int[] hole:seeds)if(hole.length>0)return true;return false;}
        boolean select(int next){if(next==tool)return false;tool=next;return true;}
        boolean tap(int hole){int[] values=seeds[hole-1];int length=values.length;if(tool==DakonSession.REMOVE){if(length==0)return false;seeds[hole-1]=Arrays.copyOf(values,length-1);return true;}if(length>=99)return false;seeds[hole-1]=Arrays.copyOf(values,length+1);seeds[hole-1][length]=tool;return true;}
        String encode(){StringBuilder saved=new StringBuilder("B2:").append(tool).append(':');for(int i=0;i<24;i++){if(i>0)saved.append(';');for(int kind:seeds[i])saved.append(kind);}return saved.toString();}
        static Board decode(String saved){
            String[] fields=saved.split(":",-1);if(fields.length!=3||!"B2".equals(fields[0]))throw new IllegalArgumentException("Invalid board");Board board=new Board();board.tool=number(fields[1],PURPLE);String[] holes=fields[2].split(";",-1);if(holes.length!=24)throw new IllegalArgumentException("Invalid holes");
            for(int i=0;i<24;i++){if(holes[i].length()>99)throw new IllegalArgumentException("Too many seeds");board.seeds[i]=new int[holes[i].length()];for(int j=0;j<holes[i].length();j++){int kind=holes[i].charAt(j)-'0';if(kind!=0&&kind!=1&&kind!=PURPLE)throw new IllegalArgumentException("Invalid seed");board.seeds[i][j]=kind;}}return board;
        }
        static Board fromLegacy(String saved){DakonSession legacy=DakonSession.restore(saved);if(!saved.equals(legacy.encode())||legacy.getStage()!=DakonSession.NITENI||legacy.getTool()>DakonSession.REMOVE||!legacy.getAnswer().isEmpty())throw new IllegalArgumentException("Invalid legacy board");Board board=new Board();board.tool=legacy.getTool();for(int hole=1;hole<=24;hole++)board.seeds[hole-1]=legacy.getSeeds(hole);return board;}
    }
}
