package id.nusmatika.dakon;

public final class JkwGuidedLessonTest {
    private static int assertions;
    public static void main(String[] args) {
        BalapKarungSession first=BalapKarungSession.forLevel(0);
        check(first.advance(),"line introduction advances without tapping a letter");
        check(first.isTriangleLineIntro()&&first.getTriangleStep()==1,"multiplication line follows division");
        check(first.triangleQuestionQuantity()==null,"line introduction has no question unknown");
        check(first.triangleInstruction().contains("biru")&&first.triangleInstruction().contains("perkalian"),"second line explains blue multiplication");
        first.advance();
        int[] answers={12,2,6};
        String[] operations={"K dikali W","J dibagi W","J dibagi K"};
        for(int i=0;i<3;i++){
            check(first.isTriangleTutorial()&&!first.isTriangleLineIntro(),"examples follow both line introductions");
            check(first.getTriangleExampleIndex()==i,"J then K then W");
            check(first.isTriangleCovered(),"unknown is hidden from first frame without a tap");
            check(first.triangleQuestionQuantity()==BalapKarungSession.Quantity.values()[i],"correct quantity is hidden");
            check(first.triangleInstruction().contains(operations[i])&&first.triangleInstruction().contains("Yang dicari:"),"explanation names unknown and correct operation");
            check(!first.triangleInstruction().contains("Ketuk"),"example does not ask for obsolete cover interaction");
            String[] steps=first.triangleCalculation().split("\\n",-1);
            check(steps.length==2,"formula and numeric substitution occupy separate lines");
            check(!first.triangleInstruction().contains("ditutup"),"explanation uses dicari instead of ditutup");
            check(first.triangleCalculation().endsWith(answers[i]+" "+first.getQuestion().unit()),"worked calculation has correct result and unit");
            check(first.encode().startsWith("BK4|"),"new tutorial uses unambiguous BK4 positions");
            first=BalapKarungSession.restoreForLevel(0,first.encode());
            check(first.isTriangleCovered()&&first.getTriangleExampleIndex()==i,"automatic hidden state survives process restart");
            first.advance();
        }
        check(!first.isTriangleTutorial()&&!first.isLevelComplete(),"original race observation follows examples");
        check(first.triangleQuestionQuantity()==null,"observation has no hidden quantity");
        check(first.advance()&&first.isLevelComplete(),"observation finishes Niteni");
        check(BalapKarungSession.restoreForLevel(0,first.encode()).isLevelComplete(),"BK4 completed state restores");
        first.reset();
        check(first.isTriangleLineIntro()&&first.getTriangleStep()==0,"replay starts with division line");
        check(first.triangleCalculation().isEmpty(),"line introduction does not show worked arithmetic");
        check(first.triangleInstruction().contains("merah")&&first.triangleInstruction().contains("pembagian"),"first line explains red division");
        for(int step=0;step<=6;step++){
            BalapKarungSession old=BalapKarungSession.restoreForLevel(0,"BK3|0|0|0|"+step+"||||||");
            int expected=step<2?0:step<4?3:step<6?4:5;
            check(old.getTriangleStep()==expected,"BK3 pending position migrates without changing quantity");
            check(!old.isLevelComplete(),"pending legacy position never forges completion");
        }
        check(BalapKarungSession.restoreForLevel(0,"BK3|0|1|0|6||||||").isLevelComplete(),"v15 completed Niteni remains complete");
        check(BalapKarungSession.restoreForLevel(0,"BK2|0|1|0||||||").isLevelComplete(),"v14 completed Niteni remains complete");
        check(BalapKarungSession.restoreForLevel(0,"BK2|0|0|0||||||").isTriangleLineIntro(),"old pending observation starts new line introduction");
        check(BalapKarungSession.restoreForLevel(0,"BK4|0|1|0|4||||||").isTriangleLineIntro(),"forged early completion resets safely");
        check(BalapKarungSession.restoreForLevel(0,"BK4|0|0|0|6||||||").isTriangleLineIntro(),"invalid new position resets safely");
        BalapLearningProgress progress=new BalapLearningProgress(new String[]{"BK3|0|0|0|3||||||","BK2|1|0|0||||||","BK2|2|0|0|00310036|0063006100720061||||"},7);
        check(progress.isUnlocked(2)&&progress.getCompletedMask()==7,"migration preserves earned unlocks");
        check(progress.loadSession(0).getTriangleExampleIndex()==1,"saved K demonstration keeps K");
        progress.restartSession(0);
        check(progress.loadSession(0).isTriangleLineIntro()&&progress.isUnlocked(2),"replay includes new introductions without relocking");
        check(progress.loadSession(2).getAnswer().equals("16")&&progress.loadSession(2).getWorking().equals("cara"),"intro replay preserves LKPD work");
        System.out.println("PASS: JKW guided lesson, "+assertions+" assertions");
    }
    private static void check(boolean value,String message){assertions++;if(!value)throw new AssertionError(message);}
}
