package id.nusmatika.dakon;

/** Standalone worksheet assertions; no Android runtime or test library required. */
public final class WorksheetStateTest {
    private static int assertions;
    private static int tests;
    private static int failures;
    private static final String[] CORRECT = {"3", "6", "9", "12", "4", "8", "12", "16", "12", "12"};

    public static void main(String[] args) {
        run("blank work has no answers or feedback", WorksheetStateTest::blankWork);
        run("completion requires ten positive unsigned numbers", WorksheetStateTest::completionGate);
        run("all first multiples and both conclusions are checked", WorksheetStateTest::checkEveryStep);
        run("leading zeros preserve numeric meaning", WorksheetStateTest::leadingZeros);
        run("changed answers clear all submitted feedback", WorksheetStateTest::editFeedback);
        run("failed checks preserve every answer", WorksheetStateTest::wrongAnswersPreserved);
        run("reset clears answers and feedback", WorksheetStateTest::resetWork);
        run("snapshots preserve raw answers but clear feedback", WorksheetStateTest::snapshots);
        run("malformed snapshots return a clean worksheet", WorksheetStateTest::malformedSnapshots);
        run("invalid indexes and null values are defensive", WorksheetStateTest::defensiveAccess);
        if (failures > 0) throw new AssertionError(failures + " tests failed");
        System.out.println("PASS: " + tests + " tests, " + assertions + " assertions");
    }

    private static void blankWork() {
        WorksheetState state = new WorksheetState();
        assertBlank(state);
        check(!state.isComplete(), "blank work cannot enable checking");
        check(!state.isCorrect(), "no correct result before checking");
        for (int i = 0; i < WorksheetState.FIELD_COUNT; i++) {
            state.set(i, CORRECT[i]);
            check(!state.isChecked(), "typing never checks answers");
            check(!state.isCorrect(), "typing does not reveal correctness");
            check(!state.isIncorrect(i), "typing does not reveal errors");
        }
    }

    private static void completionGate() {
        WorksheetState state = correctWork();
        state.set(9, "");
        check(!state.isComplete(), "one empty answer blocks checking");
        String[] invalid = {"", "0", "0000", "-12", "+12", "12.0", "1e1", " 12", "12 ", "abc", "\u0661\u0662", "\n12"};
        for (String value : invalid) {
            state.set(9, value);
            check(!state.isComplete(), "invalid number blocks checking: " + value);
            check(!state.check(), "invalid number cannot pass: " + value);
            check(state.isChecked(), "explicit invalid check is recorded");
            check(state.isIncorrect(9), "invalid answer is highlighted after checking");
            check(!state.isIncorrect(0), "correct fields remain unmarked");
        }
        state.set(9, "9999999999999999");
        check(state.isComplete(), "sixteen positive digits are valid input");
        check(!state.check(), "large valid number remains mathematically wrong");
    }

    private static void checkEveryStep() {
        WorksheetState state = correctWork();
        check(state.isComplete(), "complete answers enable checking");
        check(state.check(), "first four multiples and both conclusions pass");
        check(state.isChecked(), "check is recorded");
        check(state.isCorrect(), "successful checked work is correct");
        for (int index = 0; index < WorksheetState.FIELD_COUNT; index++) {
            check(!state.isIncorrect(index), "correct field " + index + " is unmarked");
            state.set(index, "24");
            check(state.isComplete(), "wrong but valid value still enables checking");
            check(!state.check(), "wrong field " + index + " fails even with correct KPK elsewhere");
            check(state.isIncorrect(index), "wrong field " + index + " is identified");
            state.set(index, CORRECT[index]);
        }
        state.set(0, "6");
        state.set(1, "9");
        state.set(2, "12");
        state.set(3, "15");
        check(!state.check(), "later valid multiples do not satisfy first-four instruction");
    }

    private static void leadingZeros() {
        WorksheetState state = new WorksheetState();
        for (int i = 0; i < WorksheetState.FIELD_COUNT; i++) state.set(i, "00" + CORRECT[i]);
        check(state.isComplete(), "leading zeros are valid");
        check(state.check(), "leading zeros do not change numeric answer");
        eq("003", state.get(0), "original numeric text remains visible");
        state.set(9, "0000000000000012");
        check(state.check(), "sixteen-character zero prefix does not overflow");
    }

    private static void editFeedback() {
        WorksheetState state = correctWork();
        state.set(0, "5");
        state.check();
        state.set(0, "5");
        check(state.isChecked(), "same text preserves existing feedback");
        check(state.isIncorrect(0), "same text preserves error marker");
        state.set(9, "24");
        check(!state.isChecked(), "editing any field clears submitted state");
        check(!state.isCorrect(), "editing clears success");
        for (int i = 0; i < WorksheetState.FIELD_COUNT; i++) check(!state.isIncorrect(i), "editing clears all old markers");
        state = correctWork();
        state.check();
        state.set(0, "03");
        check(!state.isCorrect(), "changed text requires checking again even if numerically equivalent");
        check(state.check(), "equivalent edited answer passes recheck");
    }

    private static void wrongAnswersPreserved() {
        WorksheetState state = correctWork();
        state.set(0, "17");
        state.set(8, "24");
        state.set(9, "24");
        String before = state.encode();
        check(!state.check(), "wrong complete worksheet fails");
        eq(before, state.encode(), "checking never rewrites any answer");
        eq("17", state.get(0), "wrong multiple stays available for correction");
        eq("24", state.get(8), "wrong common multiple stays visible");
        eq("24", state.get(9), "wrong KPK stays visible");
    }

    private static void resetWork() {
        WorksheetState state = correctWork();
        state.check();
        state.reset();
        assertBlank(state);
        check(!state.isComplete(), "reset disables checking until filled");
        check(!state.isCorrect(), "reset clears correct result");
    }

    private static void snapshots() {
        WorksheetState state = correctWork();
        state.check();
        WorksheetState restored = WorksheetState.restore(state.encode());
        eq(state.encode(), restored.encode(), "complete worksheet round trip");
        check(restored.isComplete(), "all written answers survive relaunch");
        check(!restored.isChecked(), "relaunch begins with neutral feedback");
        check(!restored.isCorrect(), "relaunch does not show success before checking");
        check(restored.check(), "restored correct answers can be checked");
        String[] raw = {"12|;:\n\u2603", "-4", "", "0012", "\u0000", "\ud83d\ude42", "abc", "0", "1234567890123456", "12"};
        for (int i = 0; i < raw.length; i++) state.set(i, raw[i]);
        restored = WorksheetState.restore(state.encode());
        for (int i = 0; i < raw.length; i++) eq(raw[i], restored.get(i), "raw answer " + i + " survives safely");
        eq(new WorksheetState().encode(), WorksheetState.restore(new WorksheetState().encode()).encode(), "empty fields survive snapshot");
        state.set(0, "12345678901234567890");
        eq("1234567890123456", state.get(0), "field storage is bounded to sixteen characters");
    }

    private static void malformedSnapshots() {
        String valid = correctWork().encode();
        String[] malformed = {null, "", "garbage", "W2" + valid.substring(2), valid + "|extra", valid.substring(0, valid.length() - 1), repeat('x', 2000)};
        for (String encoded : malformed) assertBlank(WorksheetState.restore(encoded));
        String[] fields = valid.split("\\|", -1);
        fields[1] = "zzzz";
        assertBlank(WorksheetState.restore(join(fields)));
        fields[1] = repeat('0', 68);
        assertBlank(WorksheetState.restore(join(fields)));
        fields[1] = "0";
        assertBlank(WorksheetState.restore(join(fields)));
    }

    private static void defensiveAccess() {
        WorksheetState state = correctWork();
        state.check();
        String before = state.encode();
        for (int index : new int[] {-1, 10, Integer.MIN_VALUE, Integer.MAX_VALUE}) {
            eq("", state.get(index), "invalid read is empty");
            check(!state.isIncorrect(index), "invalid index has no marker");
            state.set(index, "999");
        }
        eq(before, state.encode(), "invalid writes leave answers untouched");
        check(state.isCorrect(), "invalid writes do not clear correct result");
        state.set(0, null);
        eq("", state.get(0), "null becomes empty");
        check(!state.isChecked(), "clearing with null invalidates feedback");
        check(!state.isComplete(), "clearing with null blocks checking");
    }

    private static WorksheetState correctWork() {
        WorksheetState state = new WorksheetState();
        for (int i = 0; i < CORRECT.length; i++) state.set(i, CORRECT[i]);
        return state;
    }

    private static void assertBlank(WorksheetState state) {
        for (int i = 0; i < WorksheetState.FIELD_COUNT; i++) {
            eq("", state.get(i), "blank field " + i);
            check(!state.isIncorrect(i), "blank field has no pre-check hint");
        }
        check(!state.isChecked(), "blank state is unchecked");
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
        check(expected.equals(actual), message + "; expected=" + expected + " actual=" + actual);
    }

    private static String repeat(char value, int count) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < count; i++) result.append(value);
        return result.toString();
    }

    private static String join(String[] values) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) result.append('|');
            result.append(values[i]);
        }
        return result.toString();
    }
}
