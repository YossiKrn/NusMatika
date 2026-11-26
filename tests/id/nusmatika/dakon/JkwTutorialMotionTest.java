package id.nusmatika.dakon;

public final class JkwTutorialMotionTest {
    private static int assertions;
    public static void main(String[] args){
        String explanation="Yang dicari: jarak (J).\nJ ditutup, jadi K dikali W.";
        JkwTutorialMotion.Frame start=JkwTutorialMotion.at(0,0,false,explanation);
        check(start.divideLine==0&&start.multiplyLine==0,"introduction begins before the colored lines appear");
        check(start.visibleChars==0&&!start.ready,"copy and continue wait for the demonstration");
        JkwTutorialMotion.Frame drawing=JkwTutorialMotion.at(0,550,false,explanation);
        check(drawing.divideLine>0&&drawing.divideLine<1&&drawing.divideArrow==0,"red line draws before arrow label");
        JkwTutorialMotion.Frame label=JkwTutorialMotion.at(0,1100,false,explanation);
        check(label.divideLine==1&&label.divideArrow>0&&label.multiplyArrow==0,"Dibagi appears after its line");
        check(label.multiplyLine==0,"multiplication line is reserved for next introduction");
        JkwTutorialMotion.Frame blue=JkwTutorialMotion.at(1,550,false,explanation);
        check(blue.divideLine==1&&blue.multiplyLine>0&&blue.multiplyLine<1,"next introduction retains red while drawing blue");
        for(int step=2;step<=4;step++){
            JkwTutorialMotion.Frame early=JkwTutorialMotion.at(step,300,false,explanation);
            check(early.divideLine==1&&early.multiplyLine==1,"examples start with complete colored separators");
            check(early.calculationAlpha==0&&!early.ready,"worked arithmetic waits for typed explanation");
            check(early.activeOperation==(step==2?JkwTutorialMotion.MULTIPLY:JkwTutorialMotion.DIVIDE),"animated arrow matches arithmetic");
            JkwTutorialMotion.Frame complete=JkwTutorialMotion.at(step,10000,false,explanation);
            check(complete.visibleChars==explanation.length()&&complete.calculationAlpha==1&&complete.ready,"complete explanation enables continue");
        }
        String unicode="A\ud83d\ude03B";
        for(long ms=-40;ms<4000;ms+=17){
            JkwTutorialMotion.Frame frame=JkwTutorialMotion.at(2,ms,false,unicode);
            String typed=unicode.substring(0,frame.visibleChars);
            check(typed.isEmpty()||!Character.isHighSurrogate(typed.charAt(typed.length()-1)),"typing never splits a Unicode character");
            check(frame.visibleChars>=0&&frame.visibleChars<=unicode.length(),"typing stays within text");
        }
        for(int step=0;step<5;step++){
            JkwTutorialMotion.Frame reduced=JkwTutorialMotion.at(step,0,true,explanation);
            check(reduced.ready&&reduced.visibleChars==explanation.length(),"reduced motion displays full text immediately");
            check(reduced.pulse==0,"reduced motion does not pulse");
        }
        JkwTutorialMotion.Frame huge=JkwTutorialMotion.at(4,Long.MAX_VALUE,false,explanation);
        check(huge.ready&&huge.calculationAlpha==1&&huge.pulse>=0&&huge.pulse<=1,"large clock values remain bounded");
        check(JkwTutorialMotion.at(2,0,false,explanation).visibleChars==0,"new page clock restarts typing");
        System.out.println("PASS: JKW motion, "+assertions+" assertions");
    }
    private static void check(boolean value,String message){assertions++;if(!value)throw new AssertionError(message);}
}
