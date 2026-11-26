package id.nusmatika.dakon;

/** Executable, dependency-free checks for the sack-race lesson. */
public final class BalapKarungSessionTest {
    private static int assertions;
    private static int tests;
    private static int failures;

    public static void main(String[] args) {
        run("four racers share a finish line and retain distinct speeds", BalapKarungSessionTest::raceMotion);
        run("race positions clamp invalid time and racer input", BalapKarungSessionTest::raceBounds);
        run("three guided quantities use their own units and calculations", BalapKarungSessionTest::guidedQuestions);
        run("each guided and independent page requires an explicit correct check", BalapKarungSessionTest::progressionGates);
        run("independent stories start blank and accept arbitrary working", BalapKarungSessionTest::independentWork);
        run("wrong and invalid answers preserve the learner's work", BalapKarungSessionTest::wrongAnswers);
        run("numeric validation rejects non-integers and overflow", BalapKarungSessionTest::numericValidation);
        run("changed answers and working invalidate previous feedback", BalapKarungSessionTest::feedbackEdits);
        run("working is bounded without splitting a Unicode character", BalapKarungSessionTest::workingLimit);
        run("snapshots preserve Unicode, stage, current page, and previous work", BalapKarungSessionTest::roundTrips);
        run("restored correctness requires another explicit check", BalapKarungSessionTest::restoreIsNeutral);
        run("malformed and inconsistent snapshots reset safely", BalapKarungSessionTest::corruptSnapshots);
        run("completion and reset begin a fresh lesson", BalapKarungSessionTest::completionAndReset);
        run("map levels stop at their own completion boundary", BalapKarungSessionTest::levelBoundaries);
        run("unfinished level snapshots preserve attempts and require rechecking", BalapKarungSessionTest::levelResume);
        run("legacy progress migrates each stage without changing the old snapshot", BalapKarungSessionTest::legacyLevels);
        run("level completion unlocks in order and replay retains later progress", BalapKarungSessionTest::learningPath);
        if (failures != 0) throw new AssertionError(failures + " tests failed");
        System.out.println("PASS: " + tests + " tests, " + assertions + " assertions");
    }

    private static void raceMotion() {
        double[] speeds = {1.0, 1.5, 2.0, 3.0};
        double[] finishes = {12.0, 8.0, 6.0, 4.0};
        double[] afterThree = {3.0, 4.5, 6.0, 9.0};
        String[] names = {"Bima", "Raka", "Nara", "Tari"};
        for (int racer = 0; racer < 4; racer++) {
            eq(names[racer], BalapKarungSession.racerName(racer), "racer name");
            near(speeds[racer], BalapKarungSession.raceSpeed(racer), "constant racer speed");
            near(finishes[racer], BalapKarungSession.raceFinishSeconds(racer), "finish duration");
            near(0.0, BalapKarungSession.racePosition(racer, 0), "starting line");
            near(afterThree[racer], BalapKarungSession.racePosition(racer, 3), "position after three seconds");
            near(12.0, BalapKarungSession.racePosition(racer, finishes[racer]), "exact finish");
            near(12.0, BalapKarungSession.racePosition(racer, finishes[racer] + 100), "stays at finish");
        }
        near(12.0, BalapKarungSession.RACE_DISTANCE, "common race distance");
    }

    private static void raceBounds() {
        for (int racer = 0; racer < 4; racer++) {
            near(0.0, BalapKarungSession.racePosition(racer, -1), "negative elapsed time");
            near(0.0, BalapKarungSession.racePosition(racer, Double.NaN), "NaN elapsed time");
            near(0.0, BalapKarungSession.racePosition(racer, Double.NEGATIVE_INFINITY), "negative infinity");
            near(12.0, BalapKarungSession.racePosition(racer, Double.POSITIVE_INFINITY), "positive infinity finishes");
        }
        for (int racer : new int[] {-1, 4, Integer.MAX_VALUE}) {
            near(0.0, BalapKarungSession.raceSpeed(racer), "invalid racer speed");
            near(0.0, BalapKarungSession.raceFinishSeconds(racer), "invalid racer finish");
            near(0.0, BalapKarungSession.racePosition(racer, 2), "invalid racer position");
            eq("", BalapKarungSession.racerName(racer), "invalid racer name");
        }
    }

    private static void guidedQuestions() {
        BalapKarungSession s = guided();
        question(s.getQuestion(), BalapKarungSession.Quantity.DISTANCE, 12, 2, 6, 12,
                "m", "jarak = kecepatan × waktu", "2 × 6");
        answerAndAdvance(s, "12");
        question(s.getQuestion(), BalapKarungSession.Quantity.SPEED, 20, 2, 10, 2,
                "m/detik", "kecepatan = jarak ÷ waktu", "20 ÷ 10");
        answerAndAdvance(s, "2");
        question(s.getQuestion(), BalapKarungSession.Quantity.TIME, 18, 3, 6, 6,
                "detik", "waktu = jarak ÷ kecepatan", "18 ÷ 3");
    }

    private static void progressionGates() {
        BalapKarungSession s = new BalapKarungSession();
        eq(BalapKarungSession.NITENI, s.getStage(), "start at observation");
        eq(0, s.getIndex(), "observation index");
        eq(3, s.getQuestionCount(), "three pages per exercise stage");
        s.setAnswer("12");
        check(!s.canSubmit(), "observation cannot score");
        eq(BalapKarungSession.Feedback.NONE, s.submitAnswer(), "observation has no feedback");
        check(s.advance(), "observation advances freely");
        int[][] answers = {{12, 2, 6}, {16, 2, 8}};
        for (int section = 0; section < 2; section++) {
            eq(section + 1, s.getStage(), "exercise stage");
            for (int page = 0; page < 3; page++) {
                eq(page, s.getIndex(), "page index");
                eq("", s.getAnswer(), "each page starts blank");
                eq("", s.getWorking(), "each page starts with blank working");
                check(!s.advance(), "blank page cannot advance");
                s.setAnswer(Integer.toString(answers[section][page]));
                check(s.canSubmit(), "valid answer may be checked");
                eq(BalapKarungSession.Feedback.NONE, s.getFeedback(), "typing does not auto-check");
                check(!s.advance(), "correct unsubmitted answer cannot advance");
                eq(BalapKarungSession.Feedback.CORRECT, s.submitAnswer(), "explicit correct check");
                check(s.advance(), "checked correct answer advances");
            }
        }
        eq(BalapKarungSession.COMPLETE, s.getStage(), "six checked pages complete lesson");
        eq(0, s.getIndex(), "completion index is zero");
        eq("8", s.getAnswer(), "completion preserves final answer");
        check(!s.canSubmit(), "completion cannot score");
        eq(BalapKarungSession.Feedback.NONE, s.submitAnswer(), "completion submit has no new feedback");
    }

    private static void independentWork() {
        BalapKarungSession s = independent();
        String[] stories = {
            "Nara bergerak 2 m tiap detik. Ia melompat selama 8 detik. Berapa jaraknya?",
            "Bima menempuh 18 m dalam 9 detik. Berapa kecepatan rata-ratanya?",
            "Tari menempuh 24 m dengan kecepatan 3 m/detik. Berapa waktu tempuhnya?"
        };
        int[] answers = {16, 2, 8};
        for (int page = 0; page < 3; page++) {
            eq(stories[page], s.getQuestion().story, "independent story");
            eq(answers[page], s.getQuestion().expectedAnswer(), "independent quantity answer");
            eq("", s.getAnswer(), "no prefilled independent answer");
            eq("", s.getWorking(), "no prefilled independent working");
            if (page != 0) s.setWorking("Cara saya: gambar lompatan; boleh berbeda | \n🧮");
            s.setAnswer(Integer.toString(answers[page]));
            eq(BalapKarungSession.Feedback.NONE, s.getFeedback(), "working and answer are not auto-scored");
            eq(BalapKarungSession.Feedback.CORRECT, s.submitAnswer(), "working need not match any pattern");
            check(s.advance(), "independent correct answer proceeds");
        }
        eq("Cara saya: gambar lompatan; boleh berbeda | \n🧮", s.getWorking(), "completion keeps final working");
    }

    private static void wrongAnswers() {
        BalapKarungSession s = independent();
        s.setWorking("2 + 2 = 4; coba delapan lompatan.");
        s.setAnswer("15");
        eq(BalapKarungSession.Feedback.INCORRECT, s.submitAnswer(), "wrong numeric answer");
        eq("15", s.getAnswer(), "wrong answer retained");
        eq("2 + 2 = 4; coba delapan lompatan.", s.getWorking(), "working retained");
        check(!s.advance(), "wrong answer is gated");
        s.setAnswer("16.0");
        eq(BalapKarungSession.Feedback.INVALID, s.submitAnswer(), "decimal answer is invalid");
        eq("16.0", s.getAnswer(), "invalid answer retained");
        eq("2 + 2 = 4; coba delapan lompatan.", s.getWorking(), "invalid answer keeps working");
    }

    private static void numericValidation() {
        BalapKarungSession s = guided();
        String[] invalid = {"", "0", "0000", "-12", "+12", "12.0", "1e1", " 12", "12 ", "1\n2", "abc", "10000", "999999999999999999999999", "١٢"};
        for (String answer : invalid) {
            s.setAnswer(answer);
            check(!s.canSubmit(), "invalid number cannot submit: " + answer);
            eq(BalapKarungSession.Feedback.INVALID, s.submitAnswer(), "invalid feedback: " + answer);
        }
        s.setAnswer(null);
        eq("", s.getAnswer(), "null answer becomes blank");
        s.setAnswer("9999");
        check(s.canSubmit(), "upper numeric bound is inclusive");
        eq(BalapKarungSession.Feedback.INCORRECT, s.submitAnswer(), "upper bound is a number");
        s.setAnswer("00000000000000000012");
        check(s.canSubmit(), "leading zeros do not overflow");
        eq(BalapKarungSession.Feedback.CORRECT, s.submitAnswer(), "leading zeros preserve value");
    }

    private static void feedbackEdits() {
        BalapKarungSession s = guided();
        s.setAnswer("12");
        s.submitAnswer();
        s.setAnswer("11");
        eq(BalapKarungSession.Feedback.NONE, s.getFeedback(), "changed answer invalidates result");
        check(!s.advance(), "edited answer requires checking");
        s.setAnswer("12");
        s.submitAnswer();
        s.setWorking("2 × 6");
        eq(BalapKarungSession.Feedback.NONE, s.getFeedback(), "changed working invalidates result");
        check(!s.advance(), "edited working requires checking");
        s.submitAnswer();
        s.setAnswer("12");
        s.setWorking("2 × 6");
        eq(BalapKarungSession.Feedback.CORRECT, s.getFeedback(), "unchanged values keep checked result");
        s.setWorking(null);
        eq("", s.getWorking(), "null working becomes blank");
        eq(BalapKarungSession.Feedback.NONE, s.getFeedback(), "cleared working invalidates result");
    }

    private static void workingLimit() {
        BalapKarungSession s = independent();
        s.setWorking(repeat('x', 350));
        eq(repeat('x', 300), s.getWorking(), "working has a 300-character bound");
        s.setWorking(repeat('x', 299) + "🧮tail");
        eq(repeat('x', 299), s.getWorking(), "bound avoids a split surrogate pair");
        s.setWorking("🧮 | ; \n雪 → 16 m");
        eq("🧮 | ; \n雪 → 16 m", BalapKarungSession.restore(s.encode()).getWorking(), "arbitrary Unicode survives snapshot");
    }

    private static void roundTrips() {
        BalapKarungSession s = new BalapKarungSession();
        eq(s.encode(), BalapKarungSession.restore(s.encode()).encode(), "observation round trip");
        s.advance();
        s.setWorking("Dua × enam = dua belas 🧮");
        answerAndAdvance(s, "12");
        s.setAnswer("2|;\n雪");
        s.setWorking("20 ÷ 10; percobaan | lain\n☀");
        BalapKarungSession r = BalapKarungSession.restore(s.encode());
        eq(s.encode(), r.encode(), "partial guide including earlier page survives");
        eq(BalapKarungSession.NIROKKE, r.getStage(), "guided stage preserved");
        eq(1, r.getIndex(), "guided index preserved");
        eq("2|;\n雪", r.getAnswer(), "arbitrary answer snapshot is lossless");
        answerAndAdvance(r, "2");
        answerAndAdvance(r, "6");
        r.setWorking("Nara: delapan kali 2");
        answerAndAdvance(r, "16");
        r.setWorking("18 ÷ 9");
        r.setAnswer("3");
        r.submitAnswer();
        s = BalapKarungSession.restore(r.encode());
        eq(r.encode(), s.encode(), "independent pages and guided work persist");
        eq(BalapKarungSession.NAMBAHI, s.getStage(), "independent stage preserved");
        eq(1, s.getIndex(), "independent page preserved");
        eq("3", s.getAnswer(), "wrong answer persists");
        eq("18 ÷ 9", s.getWorking(), "working persists");
        check(s.encode().startsWith("BK1|"), "snapshot is versioned");
        check(s.encode().contains(hex("Nara: delapan kali 2")), "completed independent working persists");
        check(s.encode().contains(hex("Dua × enam = dua belas 🧮")), "completed guided working persists");
    }

    private static void restoreIsNeutral() {
        BalapKarungSession s = independent();
        s.setAnswer("16");
        s.submitAnswer();
        BalapKarungSession r = BalapKarungSession.restore(s.encode());
        eq(BalapKarungSession.Feedback.NONE, r.getFeedback(), "restored result is neutral");
        check(!r.advance(), "restored answer must be checked again");
        eq(BalapKarungSession.Feedback.CORRECT, r.submitAnswer(), "explicit recheck restores correctness");
        check(r.advance(), "explicit recheck unlocks advance");
    }

    private static void corruptSnapshots() {
        String[] invalid = {null, "", "garbage", "BK0|0|0", "BK1|0|0", "BK1|999999999999999999999|0"};
        for (String encoded : invalid) assertFresh(BalapKarungSession.restore(encoded));
        BalapKarungSession s = independent();
        s.setWorking("test");
        String[] parts = s.encode().split("\\|", -1);
        for (String stage : new String[] {"-1", "4", "999999999999999999999", "x", ""}) {
            String[] broken = parts.clone();
            broken[1] = stage;
            assertFresh(BalapKarungSession.restore(join(broken)));
        }
        for (String index : new String[] {"-1", "3", "999999999999999999999", "x", ""}) {
            String[] broken = parts.clone();
            broken[2] = index;
            assertFresh(BalapKarungSession.restore(join(broken)));
        }
        String[] broken = parts.clone();
        broken[3] = "zzzz";
        assertFresh(BalapKarungSession.restore(join(broken)));
        broken = parts.clone();
        broken[4] = "000";
        assertFresh(BalapKarungSession.restore(join(broken)));
        broken = parts.clone();
        broken[4] = hex(repeat('x', 301));
        assertFresh(BalapKarungSession.restore(join(broken)));
        broken = parts.clone();
        broken[3] = hex("11");
        assertFresh(BalapKarungSession.restore(join(broken)));
        assertFresh(BalapKarungSession.restore(s.encode() + "|extra"));
        broken = new BalapKarungSession().encode().split("\\|", -1);
        broken[2] = "1";
        assertFresh(BalapKarungSession.restore(join(broken)));
    }

    private static void completionAndReset() {
        BalapKarungSession s = independent();
        answerAndAdvance(s, "16");
        answerAndAdvance(s, "2");
        s.setWorking("24 ÷ 3");
        answerAndAdvance(s, "8");
        BalapKarungSession r = BalapKarungSession.restore(s.encode());
        eq(BalapKarungSession.COMPLETE, r.getStage(), "completion survives restart");
        eq("8", r.getAnswer(), "last answer preserved on completion");
        eq("24 ÷ 3", r.getWorking(), "last working preserved on completion");
        check(r.advance(), "completed lesson can restart");
        assertFresh(r);
        s.reset();
        assertFresh(s);
        check(s.advance(), "reset observation can advance");
        eq("", s.getAnswer(), "reset clears every page");
        eq("", s.getWorking(), "reset clears working");
    }

    private static void levelBoundaries() {
        BalapKarungSession observation = BalapKarungSession.forLevel(0);
        finishTriangleTutorial(observation);
        check(observation.advance(), "observation can finish");
        eq(BalapKarungSession.COMPLETE, observation.getStage(), "observation stops before guided work");
        check(observation.isLevelComplete(), "observation completion recognized");
        check(!observation.advance(), "completed scoped level cannot advance or restart itself");
        observation.reset();
        eq(BalapKarungSession.NITENI, observation.getStage(), "replay starts the same level");
        for (int level = 1; level <= 2; level++) {
            BalapKarungSession session = BalapKarungSession.forLevel(level);
            eq(level, session.getStage(), "direct map node opens its stage");
            int[] answers = level == 1 ? new int[] {12, 2, 6} : new int[] {16, 2, 8};
            for (int answer : answers) {
                check(!session.advance(), "pending question cannot finish");
                session.setAnswer("999"); session.submitAnswer();
                check(!session.advance(), "wrong question cannot finish");
                answerAndAdvance(session, Integer.toString(answer));
            }
            check(session.isLevelComplete(), "three correct questions complete only this level");
            eq(level, session.getLevel(), "completion retains originating level");
            eq(BalapKarungSession.COMPLETE, session.getStage(), "does not jump to another stage");
            check(!session.advance(), "completed question stage remains complete");
            session.reset();
            eq(level, session.getStage(), "restart remains scoped");
            eq("", session.getAnswer(), "replay clears previous answers");
        }
    }

    private static void levelResume() {
        BalapKarungSession session = BalapKarungSession.forLevel(2);
        answerAndAdvance(session, "16");
        session.setWorking("18 ÷ 9 | cara saya 🧮"); session.setAnswer("3"); session.submitAnswer();
        BalapKarungSession restored = BalapKarungSession.restoreForLevel(2, session.encode());
        eq(1, restored.getIndex(), "resume unfinished question");
        eq("3", restored.getAnswer(), "wrong attempt survives");
        eq("18 ÷ 9 | cara saya 🧮", restored.getWorking(), "working survives");
        check(!restored.advance(), "restored wrong attempt cannot advance");
        restored.setAnswer("2"); restored.submitAnswer();
        restored = BalapKarungSession.restoreForLevel(2, restored.encode());
        check(!restored.advance(), "restored right answer still needs an explicit check");
        answerAndAdvance(restored, "2"); answerAndAdvance(restored, "8");
        check(BalapKarungSession.restoreForLevel(2, restored.encode()).isLevelComplete(), "completed boundary survives restart");
        BalapKarungSession wrongLevel = BalapKarungSession.restoreForLevel(1, session.encode());
        eq(1, wrongLevel.getStage(), "other level snapshot resets to requested stage");
        eq("", wrongLevel.getAnswer(), "other level work cannot leak");
        String[] fields = session.encode().split("\\|", -1);
        fields[2] = "1";
        fields[3] = "0";
        check(!BalapKarungSession.restoreForLevel(2, join(fields)).isLevelComplete(), "incomplete answer history rejects forged completion");
    }

    private static void legacyLevels() {
        BalapKarungSession legacy = independent();
        legacy.setWorking("warisan 🧮"); legacy.setAnswer("15");
        String original = legacy.encode();
        check(BalapKarungSession.migrateLegacyLevel(0, original).isLevelComplete(), "legacy observation recognized");
        check(BalapKarungSession.migrateLegacyLevel(1, original).isLevelComplete(), "legacy guide recognized");
        BalapKarungSession migrated = BalapKarungSession.migrateLegacyLevel(2, original);
        eq(2, migrated.getStage(), "legacy unfinished independent stage resumed");
        eq("15", migrated.getAnswer(), "legacy attempt retained");
        eq("warisan 🧮", migrated.getWorking(), "legacy working retained");
        eq(original, legacy.encode(), "migration never mutates legacy session");
        eq(0, BalapKarungSession.migrateLegacyLevel(0, "broken").getStage(), "invalid legacy starts fresh");
        eq("", BalapKarungSession.migrateLegacyLevel(1, new BalapKarungSession().encode()).getAnswer(), "future legacy levels start blank");
        answerAndAdvance(legacy, "16"); answerAndAdvance(legacy, "2"); answerAndAdvance(legacy, "8");
        for (int level = 0; level < 3; level++) {
            migrated = BalapKarungSession.migrateLegacyLevel(level, legacy.encode());
            check(BalapKarungSession.restoreForLevel(level, migrated.encode()).isLevelComplete(), "completed legacy stage has a valid scoped snapshot");
        }
    }

    private static void learningPath() {
        BalapLearningProgress path = new BalapLearningProgress(new String[3], 0);
        check(path.isUnlocked(0), "first level unlocked");
        check(!path.isUnlocked(1) && !path.isUnlocked(2), "later levels locked");
        BalapKarungSession locked = BalapKarungSession.forLevel(2);
        answerAndAdvance(locked, "16"); answerAndAdvance(locked, "2"); answerAndAdvance(locked, "8");
        path.saveSession(locked);
        check(!path.isCompleted(2), "locked completion cannot skip prerequisites");
        BalapKarungSession first = path.loadSession(0); finishTriangleTutorial(first); first.advance(); path.saveSession(first);
        check(path.isCompleted(0) && path.isUnlocked(1), "finishing first unlocks second");
        BalapKarungSession second = path.loadSession(1);
        answerAndAdvance(second, "12"); answerAndAdvance(second, "2"); answerAndAdvance(second, "6");
        path.saveSession(second);
        check(path.isUnlocked(2), "finishing guide unlocks independent work");
        BalapKarungSession third = path.loadSession(2); third.setWorking("lanjut nanti"); third.setAnswer("15"); path.saveSession(third);
        path.restartSession(0);
        check(path.isCompleted(0) && path.isCompleted(1) && path.isUnlocked(2), "replay does not relock path");
        check(!path.isSessionComplete(0), "replay starts an unfinished session");
        eq("15", path.loadSession(2).getAnswer(), "replay retains later attempt");
        String[] snapshots = {path.encodedSession(0), path.encodedSession(1), path.encodedSession(2)};
        path = new BalapLearningProgress(snapshots, path.getCompletedMask());
        check(path.isCompleted(0) && path.isUnlocked(2), "replay unlocks survive app restart");
        eq("lanjut nanti", path.loadSession(2).getWorking(), "later working survives app restart");
        BalapLearningProgress invalid = new BalapLearningProgress(new String[3], 6);
        check(!invalid.isUnlocked(1) && !invalid.isCompleted(2), "non-prefix completion flags cannot skip levels");
    }

    private static void question(BalapKarungSession.Question q, BalapKarungSession.Quantity quantity,
            int distance, int speed, int seconds, int answer, String unit, String formula, String calculation) {
        eq(quantity, q.unknown, "unknown quantity");
        eq(distance, q.distance, "question distance");
        eq(speed, q.speed, "question speed");
        eq(seconds, q.seconds, "question time");
        eq(answer, q.expectedAnswer(), "derived answer");
        eq(unit, q.unit(), "quantity unit");
        eq(formula, q.formula(), "quantity formula");
        eq(calculation, q.calculation(), "calculation excludes final answer");
        check(q.story != null && !q.story.isEmpty(), "story is present");
    }

    private static BalapKarungSession guided() {
        BalapKarungSession s = new BalapKarungSession();
        check(s.advance(), "setup begins guide");
        return s;
    }

    private static void finishTriangleTutorial(BalapKarungSession session) {
        while (session.isTriangleTutorial()) {
            check(session.advance(), "continue the animated line or example");
        }
    }

    private static BalapKarungSession independent() {
        BalapKarungSession s = guided();
        answerAndAdvance(s, "12");
        answerAndAdvance(s, "2");
        answerAndAdvance(s, "6");
        return s;
    }

    private static void answerAndAdvance(BalapKarungSession s, String answer) {
        s.setAnswer(answer);
        eq(BalapKarungSession.Feedback.CORRECT, s.submitAnswer(), "correct setup answer");
        check(s.advance(), "correct setup advances");
    }

    private static void assertFresh(BalapKarungSession s) {
        eq(BalapKarungSession.NITENI, s.getStage(), "safe fallback stage");
        eq(0, s.getIndex(), "safe fallback page");
        eq("", s.getAnswer(), "safe fallback blank answer");
        eq("", s.getWorking(), "safe fallback blank working");
        eq(BalapKarungSession.Feedback.NONE, s.getFeedback(), "safe fallback neutral feedback");
        eq(new BalapKarungSession().encode(), s.encode(), "safe fallback clears all previous pages");
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

    private static void near(double expected, double actual, String message) {
        check(!Double.isNaN(actual) && Math.abs(expected - actual) < 0.000001,
                message + "; expected=" + expected + " actual=" + actual);
    }

    private static void eq(Object expected, Object actual, String message) {
        check(expected == null ? actual == null : expected.equals(actual),
                message + "; expected=" + expected + " actual=" + actual);
    }

    private static void check(boolean condition, String message) {
        assertions++;
        if (!condition) throw new AssertionError(message);
    }

    private static String repeat(char character, int count) {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < count; i++) text.append(character);
        return text.toString();
    }

    private static String hex(String text) {
        StringBuilder encoded = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            String digits = Integer.toHexString(text.charAt(i));
            for (int zeros = digits.length(); zeros < 4; zeros++) encoded.append('0');
            encoded.append(digits);
        }
        return encoded.toString();
    }

    private static String join(String[] parts) {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i != 0) text.append('|');
            text.append(parts[i]);
        }
        return text.toString();
    }
}
