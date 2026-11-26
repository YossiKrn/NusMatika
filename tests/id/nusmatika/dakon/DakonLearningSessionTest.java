package id.nusmatika.dakon;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** Plain JVM tests: these lessons must not depend on an Activity or Android. */
public final class DakonLearningSessionTest {
    private static int assertions;

    public static void main(String[] args) {
        check(new DakonLearningSession(2).progressText().contains("1 / 4"), "Nambahi now has four questions total");
        introRequiresEveryAction();
        guidedExamplesAndResume();
        independentBankAndCompleteFlow();
        independentWorkAndUndoSurviveRestore();
        freeBoardLimitsAndInvalidActions();
        invalidSavesResetAtomically();
        progressTracksCompletedActions();
        threeColorBoardAndHints();
        legacySavesMigrateWithoutLosingWork();
        everyBankQuestionFitsTheBoard();
        System.out.println("PASS: DakonLearningSession, " + assertions + " assertions");
    }

    private static void introRequiresEveryAction() {
        DakonLearningSession s = new DakonLearningSession(0);
        check(count(s) == 0, "intro begins without legacy opening seed");
        check(s.getBoard().getStage() == DakonSession.NITENI, "underlying board stays free");
        check(!s.primaryEnabled() && !s.primaryAction(), "cannot skip intro");
        check(!s.undo() && !s.clearBoard(), "future actions are gated");
        unchangedTap(s, 4);
        check(!s.selectTool(DakonSession.BLUE), "must inspect hole before choosing tool");
        check(s.tapHole(5), "inspect hole five");
        check(count(s) == 0, "inspection does not add a seed");
        unchangedTap(s, 5);
        check(!s.selectTool(DakonSession.CORAL), "wrong tool cannot skip blue task");
        check(s.selectTool(DakonSession.BLUE), "choose blue explicitly, even when default");
        unchangedTap(s, 6);
        check(s.tapHole(5), "place blue in five");
        check(!s.undo() && !s.clearBoard(), "cannot undo before removal task");
        unchangedTap(s, 5);
        check(s.selectTool(DakonSession.REMOVE), "choose remove");
        s = roundTrip(s);
        check(s.getTool() == DakonSession.REMOVE, "remove tool persists mid-task");
        check(s.tapHole(5), "remove seed");
        check(count(s) == 0 && s.canUndo(), "removal can be undone in the expected task");
        s = roundTrip(s);
        check(s.undo() && count(s) == 1, "persisted undo restores removed seed");
        check(!s.undo(), "cannot use undo to bypass clear task");
        check(s.clearBoard() && count(s) == 0, "clear finishes last hands-on task");
        check(s.primaryEnabled() && !s.isComplete(), "completion is explicit");
        check(!s.undo() && !s.tapHole(5), "completed tasks cannot be changed");
        s = roundTrip(s);
        check(s.primaryAction() && s.isComplete(), "Selesai completes intro");
        check(roundTrip(s).isComplete(), "intro completion persists");
    }

    private static void guidedExamplesAndResume() {
        DakonLearningSession s = new DakonLearningSession(1);
        check(!s.isAnswerVisible(), "guide asks to select a board hole");
        check(!s.primaryAction(), "cannot skip guide");
        int[][] examples = {
            {3, 6, 9, 12, 15, 18, 21, 24, 4, 8, 12, 16, 20, 24},
            {1, 2, 3, 4, 6, 12, 1, 2, 3, 6, 9, 18},
            {2,4,6,8,10,12,3,6,9,12,4,8,12},
            {1,2,3,6,1,2,3,4,6,12,1,2,3,6,9,18}
        };
        for (int example = 0; example < examples.length; example++) {
            check(count(s) == 0, "each example starts blank");
            check(!s.selectTool(DakonSession.REMOVE) && !s.clearBoard() && !s.undo(), "guide protects placement order");
            for (int i = 0; i < examples[example].length; i++) {
                int expected = examples[example][i];
                check(s.highlightHole() == expected, "guide indicates next placement");
                int blueCount=example==0?8:example==1?6:example==2?6:4;
                int coralCount=example==0?6:example==1?6:example==2?4:6;
                check(s.getTool()==(i<blueCount?0:i<blueCount+coralCount?1:3), "automatic color follows example");
                check(s.instruction().length() <= 60, "arithmetic instruction fits teaching bubble");
                check(s.instruction().contains(example % 2 == 0 ? " \u00d7 " : " \u00f7 "), "guide shows the arithmetic reason");
                if (example % 2 == 1) check(s.instruction().contains("tanpa sisa"), "factors divide without remainder");
                unchangedTap(s, expected == 24 ? 23 : 24);
                check(s.tapHole(expected), "guided placement accepted");
                s = roundTrip(s);
                check(!s.primaryEnabled(), "all placements still require choosing an answer");
            }
            check(s.highlightHole() == -1, "answer hole is not disclosed");
            check(Arrays.equals(example==0?new int[]{12,24}:example==2?new int[]{12}:new int[]{1,2,3,6}, s.commonHoles()), "shared multiples or factors");
            String markedBoard = board(s);
            check(s.tapHole(example % 2 == 0 ? 24 : 3), "wrong shared choice produces feedback");
            check(s.feedback().contains(example % 2 == 0 ? "juga kelipatan bersama" : "faktor bersama"), "wrong shared choice explains smallest or greatest");
            check(markedBoard.equals(board(s)) && !s.primaryEnabled() && !s.primaryAction(), "wrong choice preserves board and keeps next gated");
            s = roundTrip(s);
            check(!s.isFeedbackCorrect() && !s.feedback().isEmpty(), "wrong guided feedback survives restore");
            check(!s.tapHole(example % 2 == 0 ? 24 : 3), "same wrong choice does not signal a new feedback change");
            check(s.tapHole(example % 2 == 0 ? 5 : 24), "nonshared choice receives friendly feedback");
            check(s.feedback().equals(example<2?"Pilih lubang yang dimiliki kedua bilangan.":"Pilih lubang yang dimiliki ketiga bilangan."), "nonshared explanation makes no false common claim");
            check(markedBoard.equals(board(s)) && !s.primaryEnabled(), "nonshared choice preserves board and gate");
            s = roundTrip(s);
            check(s.tapHole(example % 2 == 0 ? 12 : 6), "choose smallest multiple or greatest factor");
            check(s.isFeedbackCorrect() && s.primaryEnabled(), "correct selection enables next");
            s = roundTrip(s);
            check(s.isFeedbackCorrect(), "consistent guided feedback restores");
            check(s.primaryAction(), "advance after correct selection");
        }
        check(s.isComplete(), "four guided examples complete level");
        check(roundTrip(s).isComplete(), "guided completion persists");
    }

    private static void independentBankAndCompleteFlow() {
        Set<String> orders = new HashSet<String>();
        for (int sample = 0; sample < 160; sample++) {
            int level=sample<80?2:3;
            DakonLearningSession s = new DakonLearningSession(level);
            StringBuilder order = new StringBuilder();
            int kpk = 0;
            int fpb = 0;
            for (int question = 0; question < 4; question++) {
                check(s.isWorksheet() == (question >= 2), "last two questions are LKPD");
                check(s.isAnswerVisible(), "independent response is typed");
                check(count(s) == 0 && !s.canUndo(), "each question starts with a blank board and history");
                int a = s.questionNumberA();
                int b = s.questionNumberB();
                check(a > 0 && a <= 24 && b > 0 && b <= 24, "all numbers fit original board");
                int expected = s.isKpkQuestion() ? lcm(a, b) : gcd(a, b);
                int c=s.questionNumberC();
                check(s.operandCount()==(level==2?2:3)&&(level==2?c==0:c>=1&&c<=24),"level operand count and third operand fit the board");
                if(c>0)expected=s.isKpkQuestion()?lcm(expected,c):gcd(expected,c);
                check(expected > 0 && expected <= 24, "answer fits original board");
                if (question < 2) {
                    if (s.isKpkQuestion()) kpk++; else fpb++;
                    order.append(s.prompt()).append('/');
                    check(s.highlightHole() == -1 && s.commonHoles().length == 0, "free board provides no answer hints");
                    check(s.tapHole(23), "arbitrary independent arrangement is accepted");
                } else {
                    check(!s.tapHole(5) && !s.clearBoard() && !s.undo() && !s.selectTool(DakonSession.REMOVE), "worksheet has no board actions");
                    check(!s.answerUnit().isEmpty(), "story supplies meaningful unit");
                }
                s.setWorking("cara saya |\n\u2603 " + question);
                s.setAnswer("000" + expected);
                check(!s.isFeedbackCorrect() && s.feedback().isEmpty(), "correct typed text reveals nothing before checking");
                s = roundTrip(s);
                check(s.primaryAction() && s.isFeedbackCorrect(), "check accepts correct answer without prescribed arrangement");
                check(s.getWorking().contains("cara saya"), "check keeps reasoning");
                s = roundTrip(s);
                check(s.isFeedbackCorrect(), "correct independent feedback restores");
                check(s.primaryAction(), "advance checked question");
                check(s.isComplete() == (question == 3), "only all four responses finish level");
            }
            check(kpk == 1 && fpb == 1, "two board questions are balanced");
            check(roundTrip(s).isComplete(), "independent completion survives restore");
            orders.add(order.toString());
        }
        check(orders.size() > 1, "new sessions vary question selection and order");
    }

    private static void independentWorkAndUndoSurviveRestore() {
        DakonLearningSession s = new DakonLearningSession(2);
        check(s.tapHole(7), "place free blue");
        check(s.selectTool(DakonSession.CORAL) && s.tapHole(7), "place free red");
        check(s.selectTool(DakonSession.REMOVE) && s.tapHole(7), "remove last seed");
        String board = board(s);
        s.setAnswer("999");
        s.setWorking("belum selesai\n|;\u2603");
        check(s.primaryAction() && !s.isFeedbackCorrect(), "wrong answer receives feedback after check");
        check(!s.feedback().isEmpty() && "999".equals(s.getAnswer()), "wrong answer retained");
        check(board.equals(board(s)), "wrong check keeps all board work");
        s = roundTrip(s);
        check(s.getWorking().equals("belum selesai\n|;\u2603"), "raw working restores");
        check(s.undo(), "undo survives process restore");
        check(Arrays.equals(new int[] {0, 1}, s.getSeeds(7)), "undo restores insertion order");
        s = roundTrip(s);
        check(s.undo(), "multiple persisted undo entries survive");
        check(Arrays.equals(new int[] {0}, s.getSeeds(7)), "second undo removes only last addition");
        check(s.clearBoard(), "free clear accepted");
        s = roundTrip(s);
        check(s.undo() && count(s) == 1, "clear undo survives restore");
        int expected = s.isKpkQuestion() ? lcm(s.questionNumberA(), s.questionNumberB()) : gcd(s.questionNumberA(), s.questionNumberB());
        s.setAnswer(Integer.toString(expected));
        check(s.primaryAction() && s.isFeedbackCorrect(), "correct after wrong answer");
        s.setAnswer(Integer.toString(expected));
        check(s.feedback().isEmpty() && !s.isFeedbackCorrect(), "answer editing invalidates prior feedback even with same text");
        s.setAnswer("12.0");
        check(s.primaryAction() && !s.isFeedbackCorrect(), "malformed raw answer can be checked safely");
        check(s.getAnswer().equals("12.0"), "malformed response is preserved");
    }

    private static void invalidSavesResetAtomically() {
        for (int level = 0; level < 4; level++) {
            for (String invalid : new String[] {null, "", "garbage", "DL1|2", "DL1|-1|0", "DL1|0|999999999999999999999999"}) assertFresh(level, invalid);
            DakonLearningSession s = new DakonLearningSession(level);
            String encoded = s.encode();
            assertFresh(level, encoded + "|extra");
            assertFresh(level, encoded.replaceFirst("DL2", "DL3"));
            assertFresh(level, new DakonLearningSession((level + 1) % 4).encode());
        }
        DakonLearningSession s = new DakonLearningSession(1);
        s.tapHole(3);
        String[] fields = s.encode().split("\\|", -1);
        fields[5] = "14";
        assertFresh(1, join(fields));
        s = new DakonLearningSession(2);
        s.setAnswer("999");
        fields = s.encode().split("\\|", -1);
        fields[10] = "2";
        assertFresh(2, join(fields));
        fields = s.encode().split("\\|", -1);
        fields[7] = "0,0,12,13";
        assertFresh(2, join(fields));
        fields = s.encode().split("\\|", -1);
        fields[8] = "zzzz";
        assertFresh(2, join(fields));
        fields = s.encode().split("\\|", -1);
        fields[12] = "zzzz";
        assertFresh(2, join(fields));
        fields = new DakonLearningSession(1).encode().split("\\|", -1);
        fields[10] = "1";
        fields[13] = "24";
        assertFresh(1, join(fields));
    }

    private static void freeBoardLimitsAndInvalidActions() {
        DakonLearningSession s = new DakonLearningSession(2);
        String initial = s.encode();
        for (int hole : new int[] {-1, 0, 25, Integer.MAX_VALUE}) check(!s.tapHole(hole), "invalid hole is ignored");
        for (int tool : new int[] {-1, DakonSession.MOVE, 99, 100}) check(!s.selectTool(tool), "unsupported tool is ignored");
        check(initial.equals(s.encode()), "invalid actions create no mutations or history");
        for (int i = 0; i < 99; i++) check(s.tapHole(1), "hole accepts up to 99 seeds");
        String full = s.encode();
        check(!s.tapHole(1) && full.equals(s.encode()), "full hole rejects an extra seed without history");
        for (int i = 0; i < 11; i++) check(s.tapHole(2), "later edits fill history");
        s = roundTrip(s);
        int undoCount = 0;
        while (s.undo()) undoCount++;
        check(undoCount == 100 && count(s) == 10, "wrapper retains last 100 edits across restore");
        int expected = s.isKpkQuestion() ? lcm(s.questionNumberA(), s.questionNumberB()) : gcd(s.questionNumberA(), s.questionNumberB());
        s.setAnswer(Integer.toString(expected));
        check(s.primaryAction() && s.isFeedbackCorrect(), "check before board edit");
        check(s.tapHole(3) && s.feedback().isEmpty(), "editing board invalidates checked result");
        check(s.primaryAction() && s.isFeedbackCorrect(), "recheck unchanged correct response");
        check(s.clearBoard() && s.feedback().isEmpty(), "clear invalidates checked result");
    }

    private static void assertFresh(int level, String invalid) {
        DakonLearningSession restored = DakonLearningSession.restore(level, invalid);
        check(restored.getLevel() == level && !restored.isComplete(), "invalid save resets requested level");
        check(count(restored) == 0 && !restored.canUndo(), "invalid save cannot partly restore board or history");
        check(restored.getAnswer().isEmpty() && restored.getWorking().isEmpty() && restored.feedback().isEmpty(), "invalid save cannot partly restore response or result");
        if (level == 0) check(restored.highlightHole() == 5, "intro invalid save returns first task");
        if (level == 1) check(restored.highlightHole() == 3, "guide invalid save returns first placement");
        if (level >= 2) check(restored.progressText().contains("1 / 4"), "independent invalid save starts first question");
    }

    private static DakonLearningSession roundTrip(DakonLearningSession s) {
        String encoded = s.encode();
        DakonLearningSession restored = DakonLearningSession.restore(s.getLevel(), encoded);
        check(encoded.equals(restored.encode()), "all saved state round trips exactly");
        return restored;
    }

    private static void progressTracksCompletedActions() {
        for(int level=0;level<4;level++)
            check(new DakonLearningSession(level).progressFraction()==0f,"new lessons start with empty progress");
        DakonLearningSession s=new DakonLearningSession(1);
        int[] multiples={3,6,9,12,15,18,21,24,4,8,12,16,20,24};
        for(int hole:multiples)s.tapHole(hole);
        check(s.progressFraction()<1f,"guide is not complete before its goal");
        s.tapHole(12);s.primaryAction();
        check(s.progressFraction()>0f&&s.progressFraction()<1f,"second example retains partial progress");
        int[] factors={1,2,3,4,6,12,1,2,3,6,9,18};
        for(int hole:factors)s.tapHole(hole);
        check(s.progressFraction()<1f,"FPB still requires its final choice");
        s.tapHole(6);
        check(s.progressFraction()<1f,"guide still requires its three-number examples");
        s.primaryAction();
        for(int example=2;example<4;example++){while(s.highlightHole()>0)s.tapHole(s.highlightHole());s.tapHole(example==2?12:6);
            if(example==2)s.primaryAction();}
        check(s.progressFraction()==1f,"guide reaches full progress after all four goals");
    }

    private static void threeColorBoardAndHints() {
        DakonLearningSession s=new DakonLearningSession(3);
        check(s.operandCount()==3&&s.questionNumberC()>0,"fourth level uses three operands");
        check(s.seedLabel(3).contains(Integer.toString(s.questionNumberC())),"purple label identifies third operand");
        check(s.tapHole(12)&&s.selectTool(1)&&s.tapHole(12)&&s.selectTool(3)&&s.tapHole(12),"three seed colors can share a hole");
        check(Arrays.equals(s.getSeeds(12),new int[]{0,1,3}),"third seed preserves all insertion order");
        int[] returned=s.getSeeds(12);returned[0]=3;
        check(s.getSeeds(12)[0]==0,"seed reads are defensive");
        check(s.canShowHint()&&!s.hintText().isEmpty(),"independent work offers a teaching hint");
        String before=s.encode();s.hintText();s.canShowHint();
        check(before.equals(s.encode()),"opening a hint does not change work, result, or question order");
        check(!s.hintText().matches(".*[0-9].*"),"scaffold does not state any numeric answer");
        s=roundTrip(s);
        check(s.getTool()==3&&Arrays.equals(s.getSeeds(12),new int[]{0,1,3}),"purple seed and selection survive restore");
        check(s.selectTool(2)&&s.tapHole(12),"remove deletes last purple seed");
        check(Arrays.equals(s.getSeeds(12),new int[]{0,1}),"last-seed removal keeps older colors");
        s=roundTrip(s);check(s.undo()&&Arrays.equals(s.getSeeds(12),new int[]{0,1,3}),"undo restores purple after process restart");
        check(s.clearBoard(),"three-color board clears");s=roundTrip(s);
        check(s.undo()&&Arrays.equals(s.getSeeds(12),new int[]{0,1,3}),"undo restores three-color clear");
        check(!new DakonLearningSession(2).selectTool(3),"two-number level does not expose purple");
        check(!new DakonLearningSession(0).canShowHint()&&!new DakonLearningSession(1).canShowHint(),"intro and guide do not show independent hints");
        for(int i=0;i<4;i++){
            check(s.canShowHint(),"hint remains available on board and worksheets");
            int solution=solution(s);s.setAnswer(Integer.toString(solution));check(s.primaryAction()&&s.isFeedbackCorrect(),"triple arithmetic accepts all three operands");
            s.primaryAction();
        }
        check(s.isComplete()&&!s.canShowHint(),"completed level no longer offers lesson hint");
    }

    private static void legacySavesMigrateWithoutLosingWork() {
        DakonLearningSession intro=new DakonLearningSession(0);
        intro.tapHole(5);intro.selectTool(0);intro.tapHole(5);intro.selectTool(2);
        DakonLearningSession restored=DakonLearningSession.restore(0,asLegacy(intro));
        check(restored.getTool()==2&&Arrays.equals(restored.getSeeds(5),new int[]{0}),"DL1 intro retains Hapus and its seed");
        check(restored.tapHole(5)&&restored.undo()&&restored.getSeeds(5).length==1,"DL1 intro history still works");
        DakonLearningSession guide=new DakonLearningSession(1);
        for(int example=0;example<2;example++){
            while(guide.highlightHole()>0)guide.tapHole(guide.highlightHole());guide.tapHole(example==0?12:6);
            if(example==0)guide.primaryAction();
        }
        String[] fields=asLegacy(guide).split("\\|",-1);fields[2]="1";
        restored=DakonLearningSession.restore(1,join(fields));
        check(restored.progressText().equals("Contoh 3 / 4")&&!restored.isComplete(),"old completed pair guide continues with the new triple examples");
        check(restored.questionNumberC()==4&&restored.highlightHole()==2&&count(restored)==0,"new triple guide starts with a clear board");
        DakonLearningSession independent=new DakonLearningSession(2);
        fields=independent.encode().split("\\|",-1);fields[7]="7,5,12,13";
        independent=DakonLearningSession.restore(2,join(fields));
        independent.tapHole(12);independent.selectTool(1);independent.tapHole(12);
        independent.setAnswer("999");independent.setWorking("cara lama |\n\u2603");independent.primaryAction();
        fields=asLegacy(independent).split("\\|",-1);fields[7]="7,5,8,1,12,13";
        restored=DakonLearningSession.restore(2,join(fields));
        check(restored.prompt().contains("FPB 12 dan 18")&&restored.progressText().equals("Soal 1 / 4"),"DL1 current question retained in reduced four-question lesson");
        check(restored.getAnswer().equals("999")&&restored.getWorking().equals("cara lama |\n\u2603"),"migration preserves raw answer and reasoning");
        check(Arrays.equals(restored.getSeeds(12),new int[]{0,1})&&!restored.feedback().isEmpty(),"migration preserves mixed seeds and wrong feedback");
        check(restored.undo()&&Arrays.equals(restored.getSeeds(12),new int[]{0}),"DL1 independent undo survives migration");
        for(int oldIndex=1;oldIndex<4;oldIndex++){
            fields[6]=Integer.toString(oldIndex);restored=DakonLearningSession.restore(2,join(fields));
            check(restored.progressText().equals("Soal 2 / 4"),"later old board question keeps current work as second board question");
            check(restored.getAnswer().equals("999")&&Arrays.equals(restored.getSeeds(12),new int[]{0,1}),"later old board migration retains raw work");
        }
        independent=new DakonLearningSession(2);
        for(int i=0;i<2;i++){independent.setAnswer(Integer.toString(solution(independent)));independent.primaryAction();independent.primaryAction();}
        independent.setWorking("LKPD lama");independent.setAnswer("12");
        fields=asLegacy(independent).split("\\|",-1);fields[7]="7,5,8,1,12,13";fields[6]="4";
        restored=DakonLearningSession.restore(2,join(fields));
        check(restored.isWorksheet()&&restored.progressText().contains("3 / 4")&&restored.getWorking().equals("LKPD lama"),"old fifth LKPD maps to new third question");
        check(restored.getAnswer().equals("12"),"LKPD migration keeps unfinished answer");
        restored.primaryAction();restored.primaryAction();restored.setAnswer("6");restored.primaryAction();restored.primaryAction();
        check(restored.isComplete(),"migrated LKPD can finish normally");
        fields=asLegacy(restored).split("\\|",-1);fields[7]="7,5,8,1,12,13";fields[6]="5";
        restored=DakonLearningSession.restore(2,join(fields));
        check(restored.isComplete()&&restored.progressText().contains("4 / 4"),"old six-question completion stays complete");
        check(roundTrip(restored).isComplete(),"migrated completion is valid DL2");
    }

    private static void everyBankQuestionFitsTheBoard() {
        for(int level=2;level<=3;level++){
            int offset=level==3?14:0;
            for(int id=offset;id<offset+12;id++){
                DakonLearningSession s=new DakonLearningSession(level);
                String[] fields=s.encode().split("\\|",-1);
                String order=id+","+(id<offset+6?offset+6:offset)+","+(offset+12)+","+(offset+13);
                fields[7]=order;s=DakonLearningSession.restore(level,join(fields));
                check(s.encode().split("\\|",-1)[7].equals(order),"every bank ID is accepted deterministically");
                check(s.questionNumberA()>=1&&s.questionNumberA()<=24&&s.questionNumberB()>=1&&s.questionNumberB()<=24
                        &&(level==2?s.questionNumberC()==0:s.questionNumberC()>=1&&s.questionNumberC()<=24),"each bank operand fits 24 holes");
                int value=solution(s);check(value>=1&&value<=24,"each bank result fits 24 holes");
                s.setAnswer(Integer.toString(value));check(s.primaryAction()&&s.isFeedbackCorrect(),"each bank result uses every operand correctly");
            }
        }
    }

    private static String asLegacy(DakonLearningSession s) {
        String[] fields=s.encode().split("\\|",-1);fields[0]="DL1";
        fields[11]=hex(legacyBoard(unhex(fields[11])));
        if(!fields[12].isEmpty()){
            String[] entries=fields[12].split(",",-1);
            for(int i=0;i<entries.length;i++)entries[i]=hex(legacyBoard(unhex(entries[i])));
            fields[12]=String.join(",",entries);
        }
        return join(fields);
    }
    private static String legacyBoard(String saved){String[] fields=saved.split(":",-1);return "D1|0|"+fields[1]+"|0||"+fields[2];}
    private static String hex(String text){StringBuilder value=new StringBuilder();for(char c:text.toCharArray())value.append(String.format("%04x",(int)c));return value.toString();}
    private static String unhex(String text){StringBuilder value=new StringBuilder();for(int i=0;i<text.length();i+=4)value.append((char)Integer.parseInt(text.substring(i,i+4),16));return value.toString();}
    private static int solution(DakonLearningSession s){int value=s.isKpkQuestion()?lcm(s.questionNumberA(),s.questionNumberB()):gcd(s.questionNumberA(),s.questionNumberB());return s.questionNumberC()==0?value:s.isKpkQuestion()?lcm(value,s.questionNumberC()):gcd(value,s.questionNumberC());}

    private static void unchangedTap(DakonLearningSession s, int hole) {
        String before = s.encode();
        check(!s.tapHole(hole), "wrong action rejected");
        check(before.equals(s.encode()), "wrong action preserves every state field");
    }

    private static int count(DakonLearningSession s) {
        int count = 0;
        for (int i = 1; i <= 24; i++) count += s.getSeeds(i).length;
        return count;
    }

    private static String board(DakonLearningSession s) {
        StringBuilder value = new StringBuilder();
        for (int i = 1; i <= 24; i++) value.append(Arrays.toString(s.getSeeds(i)));
        return value.toString();
    }

    private static int gcd(int a, int b) {
        while (b != 0) { int next = a % b; a = b; b = next; }
        return a;
    }

    private static int lcm(int a, int b) { return a / gcd(a, b) * b; }

    private static String join(String[] fields) {
        StringBuilder value = new StringBuilder();
        for (String field : fields) { if (value.length() > 0) value.append('|'); value.append(field); }
        return value.toString();
    }

    private static void check(boolean success, String message) {
        assertions++;
        if (!success) throw new AssertionError(message);
    }
}
