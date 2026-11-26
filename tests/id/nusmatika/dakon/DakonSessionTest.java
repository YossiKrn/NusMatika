package id.nusmatika.dakon;

import java.util.Arrays;

/** Standalone assertions: javac the two sources, then run this main class. */
public final class DakonSessionTest {
    private static int assertions;
    private static int tests;
    private static int failures;

    public static void main(String[] args) {
        run("opening board is editable and defensive", DakonSessionTest::openingBoard);
        run("free tools preserve insertion order and undo moves atomically", DakonSessionTest::freeTools);
        run("guide enforces order and completion gate", DakonSessionTest::guidedOrder);
        run("guide can undo and restart", DakonSessionTest::guidedUndo);
        run("independent work starts blank without hints", DakonSessionTest::independentStartsBlank);
        run("answer checking is independent of board arrangement", DakonSessionTest::answersDoNotRequireBoardPattern);
        run("wrong answers keep work and distinguish common multiples", DakonSessionTest::wrongAnswersKeepWork);
        run("answers reject invalid numbers and respect the numeric bound", DakonSessionTest::answerValidation);
        run("edits invalidate submitted feedback", DakonSessionTest::feedbackInvalidation);
        run("hole seed and history limits are enforced", DakonSessionTest::limits);
        run("invalid inputs do not mutate session", DakonSessionTest::invalidInputs);
        run("save and restore preserve stages and board order", DakonSessionTest::roundTrips);
        run("malformed and inconsistent saves fail safely", DakonSessionTest::corruptSaves);
        if (failures > 0) throw new AssertionError(failures + " tests failed");
        System.out.println("PASS: " + tests + " tests, " + assertions + " assertions");
    }

    private static void openingBoard() {
        DakonSession s = new DakonSession();
        eq(DakonSession.NITENI, s.getStage(), "opening stage");
        eq(DakonSession.BLUE, s.getTool(), "opening tool");
        seeds(s, 5, 0);
        eq(1, count(s), "only one opening seed");
        eq(0, s.getExpectedHole(), "exploration has no guide hint");
        check(!s.canUndo(), "opening seed is not an edit");
        int[] copy = s.getSeeds(5);
        copy[0] = 1;
        seeds(s, 5, 0);
        check(s.clearBoard(), "can clear opening board");
        eq(0, count(s), "clear empties board");
        check(s.undo(), "clear can be undone");
        seeds(s, 5, 0);
    }

    private static void freeTools() {
        DakonSession s = new DakonSession();
        check(s.tapHole(1), "add blue");
        check(s.setTool(DakonSession.CORAL), "choose coral");
        check(s.tapHole(1), "add coral");
        seeds(s, 1, 0, 1);
        s.setTool(DakonSession.MOVE);
        check(!s.tapHole(2), "empty source cannot be selected");
        check(s.tapHole(1), "select occupied source");
        eq(1, s.getMoveSource(), "selected source is exposed");
        check(s.tapHole(1), "same hole cancels selection");
        eq(0, s.getMoveSource(), "same-hole cancellation");
        seeds(s, 1, 0, 1);
        s.tapHole(1);
        check(s.tapHole(2), "move last seed");
        seeds(s, 1, 0);
        seeds(s, 2, 1);
        check(s.undo(), "undo whole move");
        seeds(s, 1, 0, 1);
        seeds(s, 2);
        eq(0, s.getMoveSource(), "undo clears move selection");
        s.setTool(DakonSession.REMOVE);
        check(s.tapHole(1), "remove last seed");
        seeds(s, 1, 0);
        check(!s.tapHole(2), "remove from empty hole is ignored");
        check(s.undo(), "undo removal");
        seeds(s, 1, 0, 1);
    }

    private static void guidedOrder() {
        DakonSession s = new DakonSession();
        check(s.advance(), "exploration can advance");
        eq(DakonSession.NIROKKE, s.getStage(), "guided stage");
        eq(0, count(s), "guide starts empty");
        eq(14, s.getGuideLength(), "complete demonstration has fourteen placements");
        eq(3, s.getExpectedHole(), "first blue mark");
        eq(0, s.getExpectedKind(), "blue demonstration first");
        check(!s.advance(), "guide cannot be skipped");
        check(!s.setTool(DakonSession.CORAL), "cannot override guided color");
        check(!s.setTool(DakonSession.REMOVE), "cannot remove through guided tool");
        String before = s.encode();
        check(!s.tapHole(4), "wrong guided hole is rejected");
        eq(before, s.encode(), "wrong hole leaves all state unchanged");
        int[] holes = {3, 6, 9, 12, 15, 18, 21, 24, 4, 8, 12, 16, 20, 24};
        for (int i = 0; i < holes.length; i++) {
            eq(holes[i], s.getExpectedHole(), "guide placement " + i);
            eq(i < 8 ? 0 : 1, s.getExpectedKind(), "guide color " + i);
            eq(s.getExpectedKind(), s.getTool(), "guided tool follows expected color");
            check(s.tapHole(holes[i]), "accepted guided placement " + i);
            eq(i + 1, s.getGuideStep(), "guide progress");
        }
        check(s.isGuideComplete(), "guide reports completion");
        eq(0, s.getExpectedHole(), "complete guide no longer points to a hole");
        seeds(s, 12, 0, 1);
        seeds(s, 24, 0, 1);
        eq(14, count(s), "guide contains exactly fourteen seeds");
        check(!s.tapHole(24), "completed guide cannot add extra seeds");
        check(s.advance(), "complete guide can advance");
    }

    private static void guidedUndo() {
        DakonSession s = new DakonSession();
        s.advance();
        check(!s.undo(), "empty guide cannot undo");
        int[] first = {3, 6, 9, 12, 15, 18, 21, 24, 4};
        for (int hole : first) s.tapHole(hole);
        check(s.undo(), "guided step can undo");
        eq(8, s.getGuideStep(), "guided undo decrements progress");
        eq(4, s.getExpectedHole(), "undone mark becomes expected");
        seeds(s, 4);
        s.undo();
        eq(DakonSession.BLUE, s.getTool(), "undo across color boundary changes tool");
        eq(24, s.getExpectedHole(), "last blue mark can be replayed");
        check(s.clearBoard(), "clear guided board");
        eq(0, count(s), "guided clear empties every hole");
        eq(0, s.getGuideStep(), "guided clear resets progress");
        eq(3, s.getExpectedHole(), "guided clear starts at first mark");
        check(!s.canUndo(), "clear starts a fresh guide");
    }

    private static void independentStartsBlank() {
        DakonSession s = independent();
        eq(DakonSession.NAMBAHI, s.getStage(), "independent stage");
        eq(0, count(s), "no demonstration seeds carried into independent work");
        eq("", s.getAnswer(), "independent answer is blank");
        eq(DakonSession.BLUE, s.getTool(), "independent tool starts blue");
        eq(0, s.getMoveSource(), "no selected move source");
        eq(0, s.getExpectedHole(), "independent placement offers no target hint");
        eq(DakonSession.Feedback.NONE, s.getFeedback(), "independent has no feedback before submission");
        check(!s.canUndo(), "guide history is not carried forward");
        check(!s.canSubmit(), "blank answer cannot submit");
        check(!s.advance(), "unsubmitted work cannot advance");
        s.tapHole(7);
        eq(DakonSession.Feedback.NONE, s.getFeedback(), "placement does not reveal correctness");
    }

    private static void answersDoNotRequireBoardPattern() {
        DakonSession s = independent();
        s.tapHole(7);
        s.setTool(DakonSession.CORAL);
        s.tapHole(2);
        s.tapHole(2);
        String board = board(s);
        s.setAnswer("0012");
        check(s.canSubmit(), "leading zeros are valid");
        eq(DakonSession.Feedback.CORRECT, s.submitAnswer(), "twelve accepted with an alternate board");
        eq(board, board(s), "checking answer never rearranges work");
        check(s.advance(), "correct submission unlocks completion");
        eq(DakonSession.COMPLETE, s.getStage(), "completion stage");
        check(!s.tapHole(3), "completed board is not editable");
        check(s.advance(), "completion restarts lesson");
        eq(DakonSession.NITENI, s.getStage(), "restart stage");
        eq("", s.getAnswer(), "restart resets answer");
        eq(1, count(s), "restart restores only opening seed");
        seeds(s, 5, 0);
        check(!s.canUndo(), "restart discards history");
        DakonSession empty = independent();
        empty.setAnswer("12");
        eq(DakonSession.Feedback.CORRECT, empty.submitAnswer(), "numeric answer does not require seed placement");
    }

    private static void wrongAnswersKeepWork() {
        DakonSession s = independent();
        s.tapHole(6);
        s.setTool(DakonSession.CORAL);
        s.tapHole(6);
        String board = board(s);
        s.setAnswer("24");
        eq(DakonSession.Feedback.COMMON_NOT_SMALLEST, s.submitAnswer(), "twenty-four is common but not smallest");
        eq(board, board(s), "wrong common multiple keeps work");
        check(!s.advance(), "common multiple does not complete lesson");
        s.setAnswer("6");
        eq(DakonSession.Feedback.NOT_COMMON, s.submitAnswer(), "six is not common to three and four");
        eq(board, board(s), "not-common answer keeps work");
    }

    private static void answerValidation() {
        DakonSession s = independent();
        String[] invalid = {"", "0", "0000", "-12", "+12", "12.0", "1e1", " 12", "12 ", "abc", "1000000", "999999999999999999999", "\u0661\u0662"};
        for (String answer : invalid) {
            s.setAnswer(answer);
            check(!s.canSubmit(), "invalid answer rejected: " + answer);
            eq(DakonSession.Feedback.INVALID, s.submitAnswer(), "invalid submission feedback: " + answer);
        }
        s.setAnswer(null);
        eq("", s.getAnswer(), "null answer becomes blank");
        check(!s.canSubmit(), "null cannot submit");
        s.setAnswer("999999");
        check(s.canSubmit(), "upper numeric bound is inclusive");
        eq(DakonSession.Feedback.NOT_COMMON, s.submitAnswer(), "upper bound remains a valid number");
        s.setAnswer("00000000000000000000000000000012");
        check(s.canSubmit(), "leading zeros do not cause overflow");
        eq(DakonSession.Feedback.CORRECT, s.submitAnswer(), "long zero prefix still means twelve");
        DakonSession opening = new DakonSession();
        opening.setAnswer("12");
        check(!opening.canSubmit(), "only independent stage permits submission");
        eq(DakonSession.Feedback.NONE, opening.submitAnswer(), "other stages do not score answers");
    }

    private static void feedbackInvalidation() {
        DakonSession s = independent();
        s.setAnswer("12");
        s.submitAnswer();
        s.setAnswer("12");
        eq(DakonSession.Feedback.NONE, s.getFeedback(), "editing even same answer clears old result");
        s.submitAnswer();
        s.tapHole(1);
        eq(DakonSession.Feedback.NONE, s.getFeedback(), "placement clears old result");
        check(!s.advance(), "board edit requires a new submission");
        s.submitAnswer();
        s.undo();
        eq(DakonSession.Feedback.NONE, s.getFeedback(), "undo clears old result");
        s.tapHole(2);
        s.submitAnswer();
        s.clearBoard();
        eq(DakonSession.Feedback.NONE, s.getFeedback(), "clear clears old result");
        s.tapHole(2);
        s.submitAnswer();
        s.setTool(DakonSession.MOVE);
        s.tapHole(2);
        eq(DakonSession.Feedback.CORRECT, s.getFeedback(), "selecting source does not change board or feedback");
        s.tapHole(3);
        eq(DakonSession.Feedback.NONE, s.getFeedback(), "moving a seed clears old result");
        s.submitAnswer();
        s.setTool(DakonSession.REMOVE);
        s.tapHole(3);
        eq(DakonSession.Feedback.NONE, s.getFeedback(), "removing a seed clears old result");
    }

    private static void limits() {
        DakonSession s = new DakonSession();
        for (int i = 0; i < 99; i++) check(s.tapHole(1), "seed up to hole cap");
        String full = s.encode();
        check(!s.tapHole(1), "hundredth seed is rejected");
        eq(full, s.encode(), "rejected seed does not mutate board");
        s.setTool(DakonSession.MOVE);
        s.tapHole(5);
        check(!s.tapHole(1), "move into full hole is rejected");
        seeds(s, 5, 0);
        eq(99, s.getSeeds(1).length, "move preserves full target");
        DakonSession h = new DakonSession();
        for (int i = 0; i < 110; i++) h.tapHole(i < 99 ? 1 : 2);
        int undos = 0;
        while (h.undo()) undos++;
        eq(100, undos, "history retains at most one hundred edits");
        eq(11, count(h), "ten oldest additions plus initial seed remain");
    }

    private static void invalidInputs() {
        DakonSession s = new DakonSession();
        String original = s.encode();
        for (int hole : new int[] {-1, 0, 25, Integer.MAX_VALUE}) {
            check(!s.tapHole(hole), "invalid hole cannot mutate");
            eq(0, s.getSeeds(hole).length, "invalid read returns empty defensively");
        }
        for (int tool : new int[] {-1, 4, 99}) check(!s.setTool(tool), "invalid tool cannot mutate");
        eq(original, s.encode(), "all invalid inputs leave session unchanged");
        check(!s.undo(), "invalid operations do not create undo entries");
    }

    private static void roundTrips() {
        DakonSession s = new DakonSession();
        s.tapHole(1);
        s.setTool(DakonSession.CORAL);
        s.tapHole(1);
        s.setTool(DakonSession.MOVE);
        DakonSession restored = DakonSession.restore(s.encode());
        eq(s.encode(), restored.encode(), "free session round trip");
        seeds(restored, 1, 0, 1);
        eq(DakonSession.MOVE, restored.getTool(), "chosen tool survives relaunch");
        check(!restored.canUndo(), "undo history need not survive relaunch");
        s.advance();
        int[] first = {3, 6, 9, 12, 15, 18, 21, 24, 4};
        for (int hole : first) s.tapHole(hole);
        restored = DakonSession.restore(s.encode());
        eq(9, restored.getGuideStep(), "guided progress survives relaunch");
        eq(8, restored.getExpectedHole(), "restored guide continues at next mark");
        eq(DakonSession.CORAL, restored.getTool(), "restored guide color");
        check(!restored.advance(), "partial restored guide is gated");
        while (!restored.isGuideComplete()) restored.tapHole(restored.getExpectedHole());
        restored.advance();
        restored.setTool(DakonSession.CORAL);
        restored.tapHole(23);
        restored.setAnswer("12|;\n\u2603");
        DakonSession independent = DakonSession.restore(restored.encode());
        eq(restored.encode(), independent.encode(), "independent round trip preserves arbitrary answer characters");
        eq(0, independent.getExpectedHole(), "relaunch does not reveal an independent hint");
        seeds(independent, 23, 1);
        independent.setAnswer("12");
        independent.submitAnswer();
        independent.advance();
        DakonSession complete = DakonSession.restore(independent.encode());
        eq(DakonSession.COMPLETE, complete.getStage(), "completion survives relaunch");
        eq("12", complete.getAnswer(), "completion preserves answer");
        check(complete.advance(), "restored completion can restart");
    }

    private static void corruptSaves() {
        for (String encoded : new String[] {null, "", "garbage", "D1|0", "D1|4|0|0||", "D1|0|9|0||"}) assertDefault(DakonSession.restore(encoded));
        DakonSession s = new DakonSession();
        s.advance();
        s.tapHole(3);
        String[] fields = s.encode().split("\\|", -1);
        fields[1] = "99";
        assertDefault(DakonSession.restore(join(fields, "|")));
        fields = s.encode().split("\\|", -1);
        fields[3] = "15";
        assertDefault(DakonSession.restore(join(fields, "|")));
        fields = s.encode().split("\\|", -1);
        fields[4] = "zzzz";
        assertDefault(DakonSession.restore(join(fields, "|")));
        fields = s.encode().split("\\|", -1);
        String[] holes = fields[5].split(";", -1);
        holes[2] = "1";
        fields[5] = join(holes, ";");
        assertDefault(DakonSession.restore(join(fields, "|")));
        holes[2] = "2";
        fields[5] = join(holes, ";");
        assertDefault(DakonSession.restore(join(fields, "|")));
        holes[2] = repeat('0', 100);
        fields[5] = join(holes, ";");
        assertDefault(DakonSession.restore(join(fields, "|")));
        assertDefault(DakonSession.restore(s.encode() + "|extra"));
    }

    private static DakonSession independent() {
        DakonSession s = new DakonSession();
        s.advance();
        int[] guide = {3, 6, 9, 12, 15, 18, 21, 24, 4, 8, 12, 16, 20, 24};
        for (int hole : guide) s.tapHole(hole);
        check(s.advance(), "test setup completes guide");
        return s;
    }

    private static void assertDefault(DakonSession s) {
        eq(DakonSession.NITENI, s.getStage(), "invalid save falls back to opening");
        eq(1, count(s), "invalid save restores only opening seed");
        seeds(s, 5, 0);
        eq(DakonSession.BLUE, s.getTool(), "invalid save restores default tool");
    }

    private static int count(DakonSession s) {
        int count = 0;
        for (int hole = 1; hole <= 24; hole++) count += s.getSeeds(hole).length;
        return count;
    }

    private static String board(DakonSession s) {
        StringBuilder b = new StringBuilder();
        for (int hole = 1; hole <= 24; hole++) b.append(Arrays.toString(s.getSeeds(hole)));
        return b.toString();
    }

    private static void seeds(DakonSession s, int hole, int... expected) {
        check(Arrays.equals(expected, s.getSeeds(hole)), "hole " + hole + " expected " + Arrays.toString(expected) + " got " + Arrays.toString(s.getSeeds(hole)));
    }

    private static void run(String name, Runnable test) {
        try {
            test.run();
            tests++;
            System.out.println("PASS " + name);
        } catch (AssertionError failure) {
            failures++;
            System.err.println("FAIL " + name + ": " + failure.getMessage());
        }
    }

    private static void check(boolean condition, String message) {
        assertions++;
        if (!condition) throw new AssertionError(message);
    }

    private static void eq(Object expected, Object actual, String message) {
        check(expected == null ? actual == null : expected.equals(actual), message + "; expected=" + expected + " actual=" + actual);
    }

    private static String join(String[] values, String delimiter) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) b.append(delimiter);
            b.append(values[i]);
        }
        return b.toString();
    }

    private static String repeat(char character, int count) {
        char[] chars = new char[count];
        Arrays.fill(chars, character);
        return new String(chars);
    }
}
