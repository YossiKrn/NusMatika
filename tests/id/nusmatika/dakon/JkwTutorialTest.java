package id.nusmatika.dakon;

/** Exercise aid selection must be read-only for every question and saved attempt. */
public final class JkwTutorialTest {
    private static int assertions;
    public static void main(String[] args) {
        for(int level=1;level<=2;level++){
            BalapKarungSession exercise=BalapKarungSession.forLevel(level);
            int[] results=level==1?new int[]{12,2,6}:new int[]{16,2,8};
            for(int i=0;i<3;i++){
                exercise.setWorking("cara saya");exercise.setAnswer(Integer.toString(results[i]));
                String before=exercise.encode();
                check(exercise.triangleQuestionQuantity()==BalapKarungSession.Quantity.values()[i],"inline triangle hides current unknown");
                check(before.equals(exercise.encode()),"rendering aid selection does not mutate work");
                check(exercise.encode().startsWith("BK2|"),"higher-level encoding is unchanged");
                exercise=BalapKarungSession.restoreForLevel(level,before);
                check(exercise.getWorking().equals("cara saya")&&exercise.getAnswer().equals(Integer.toString(results[i])),"answers and reasoning survive restoration");
                exercise.submitAnswer();exercise.advance();
            }
            check(exercise.triangleQuestionQuantity()==null,"completed levels have no inline unknown");
        }
        System.out.println("PASS: JKW exercise aid, "+assertions+" assertions");
    }
    private static void check(boolean value,String message){assertions++;if(!value)throw new AssertionError(message);}
}
