package id.nusmatika.dakon;

import java.util.Arrays;

/** Offline lesson state. Race movement depends only on the supplied elapsed time. */
public final class BalapKarungSession {
    public static final int NITENI = 0, NIROKKE = 1, NAMBAHI = 2, COMPLETE = 3;
    public static final int RACE_DISTANCE = 12;
    public enum Feedback { NONE, INVALID, INCORRECT, CORRECT }
    public enum Quantity { DISTANCE, SPEED, TIME }

    /** Immutable task data; the view chooses when to reveal the guided explanation. */
    public static final class Question {
        public final Quantity unknown;
        public final int distance;
        public final int speed;
        public final int seconds;
        public final String story;

        private Question(Quantity unknown, int distance, int speed, int seconds, String story) {
            this.unknown = unknown;
            this.distance = distance;
            this.speed = speed;
            this.seconds = seconds;
            this.story = story;
        }

        public int expectedAnswer() {
            if (unknown == Quantity.DISTANCE) return speed * seconds;
            if (unknown == Quantity.SPEED) return distance / seconds;
            return distance / speed;
        }

        public String unit() {
            if (unknown == Quantity.DISTANCE) return "m";
            if (unknown == Quantity.SPEED) return "m/detik";
            return "detik";
        }

        public String formula() {
            if (unknown == Quantity.DISTANCE) return "jarak = kecepatan × waktu";
            if (unknown == Quantity.SPEED) return "kecepatan = jarak ÷ waktu";
            return "waktu = jarak ÷ kecepatan";
        }

        public String calculation() {
            if (unknown == Quantity.DISTANCE) return speed + " × " + seconds;
            if (unknown == Quantity.SPEED) return distance + " ÷ " + seconds;
            return distance + " ÷ " + speed;
        }
    }

    private static final int QUESTION_COUNT = 3;
    private static final int MAX_WORKING = 300;
    private static final String[] RACER_NAMES = {"Bima", "Raka", "Nara", "Tari"};
    private static final double[] RACER_SPEEDS = {1.0, 1.5, 2.0, 3.0};
    private static final Question[] QUESTIONS = {
        new Question(Quantity.DISTANCE, 12, 2, 6,
                "Nara melompat dengan kecepatan 2 m/detik selama 6 detik. Berapa jaraknya?"),
        new Question(Quantity.SPEED, 20, 2, 10,
                "Seorang peserta menempuh 20 m dalam 10 detik. Berapa kecepatan rata-ratanya?"),
        new Question(Quantity.TIME, 18, 3, 6,
                "Seorang peserta menempuh 18 m dengan kecepatan 3 m/detik. Berapa waktu tempuhnya?"),
        new Question(Quantity.DISTANCE, 16, 2, 8,
                "Nara bergerak 2 m tiap detik. Ia melompat selama 8 detik. Berapa jaraknya?"),
        new Question(Quantity.SPEED, 18, 2, 9,
                "Bima menempuh 18 m dalam 9 detik. Berapa kecepatan rata-ratanya?"),
        new Question(Quantity.TIME, 24, 3, 8,
                "Tari menempuh 24 m dengan kecepatan 3 m/detik. Berapa waktu tempuhnya?")
    };

    private int level = -1;
    private int stage;
    private int index;
    public static final int TRIANGLE_RACE_STEP = 5;
    private int triangleStep = TRIANGLE_RACE_STEP;
    private Feedback feedback;
    private final String[] answers = new String[QUESTIONS.length];
    private final String[] working = new String[QUESTIONS.length];

    public BalapKarungSession() { reset(); }

    /** A map visit owns one stage; its completion never enters the next stage. */
    public static BalapKarungSession forLevel(int level) {
        requireLevel(level);
        BalapKarungSession session = new BalapKarungSession();
        session.level = level;
        session.reset();
        return session;
    }

    public int getLevel() { return level; }
    public boolean isLevelComplete() { return level >= 0 && stage == COMPLETE; }
    public int getStage() { return stage; }
    public int getIndex() { return index; }
    public int getQuestionCount() { return QUESTION_COUNT; }
    public Question getQuestion() { return isTriangleTutorial() ? getTriangleExample() : QUESTIONS[currentSlot()]; }
    public String getAnswer() { return answers[currentSlot()]; }
    public String getWorking() { return working[currentSlot()]; }
    public Feedback getFeedback() { return feedback; }

    public boolean isTriangleTutorial() { return level == NITENI && stage == NITENI && triangleStep < TRIANGLE_RACE_STEP; }
    public boolean isTriangleLineIntro() { return isTriangleTutorial() && triangleStep < 2; }
    public int getTriangleStep() { return triangleStep; }
    public int getTriangleExampleIndex() { return Math.max(0, Math.min(2, triangleStep - 2)); }
    public Question getTriangleExample() { return QUESTIONS[getTriangleExampleIndex()]; }
    public boolean isTriangleCovered() { return isTriangleTutorial() && !isTriangleLineIntro(); }
    public Quantity triangleQuestionQuantity() {
        if (isTriangleTutorial()) return isTriangleLineIntro() ? null : getTriangleExample().unknown;
        return isQuestionStage() ? getQuestion().unknown : null;
    }
    public String triangleTitle() {
        if (isTriangleLineIntro()) return triangleStep == 0 ? "Garis pembagian" : "Garis perkalian";
        Quantity quantity = getTriangleExample().unknown;
        return quantity == Quantity.DISTANCE ? "Cari jarak" : quantity == Quantity.SPEED ? "Cari kecepatan" : "Cari waktu";
    }
    public String triangleInstruction() {
        if (isTriangleLineIntro()) return triangleStep == 0
                ? "Garis merah mendatar\nmenunjukkan pembagian."
                : "Garis biru tegak\nmenunjukkan perkalian.";
        Quantity quantity = getTriangleExample().unknown;
        return quantity == Quantity.DISTANCE ? "Yang dicari: jarak (J).\nJ dicari, jadi K dikali W."
                : quantity == Quantity.SPEED ? "Yang dicari: kecepatan (K).\nK dicari, jadi J dibagi W."
                : "Yang dicari: waktu (W).\nW dicari, jadi J dibagi K.";
    }
    public String triangleCalculation() {
        if (!isTriangleCovered()) return "";
        Question q = getTriangleExample();
        String formula = q.unknown == Quantity.DISTANCE ? "J = K × W" : q.unknown == Quantity.SPEED ? "K = J ÷ W" : "W = J ÷ K";
        return formula + "\n" + triangleLetter(q.unknown) + " = " + q.calculation() + " = " + q.expectedAnswer() + " " + q.unit();
    }
    public static String triangleLetter(Quantity quantity) {
        return quantity == Quantity.DISTANCE ? "J" : quantity == Quantity.SPEED ? "K" : quantity == Quantity.TIME ? "W" : "";
    }

    public void setAnswer(String answer) {
        if (stage == COMPLETE || level == NITENI) return;
        String next = answer == null ? "" : answer;
        int slot = currentSlot();
        if (!next.equals(answers[slot])) {
            answers[slot] = next;
            feedback = Feedback.NONE;
        }
    }

    public void setWorking(String text) {
        if (stage == COMPLETE || level == NITENI) return;
        String next = text == null ? "" : text;
        if (next.length() > MAX_WORKING) {
            int end = MAX_WORKING;
            if (Character.isHighSurrogate(next.charAt(end - 1))
                    && Character.isLowSurrogate(next.charAt(end))) end--;
            next = next.substring(0, end);
        }
        int slot = currentSlot();
        if (!next.equals(working[slot])) {
            working[slot] = next;
            feedback = Feedback.NONE;
        }
    }

    public boolean canSubmit() {
        return isQuestionStage() && parseAnswer(getAnswer()) > 0;
    }

    public Feedback submitAnswer() {
        if (!isQuestionStage()) return Feedback.NONE;
        int value = parseAnswer(getAnswer());
        if (value < 1) feedback = Feedback.INVALID;
        else feedback = value == getQuestion().expectedAnswer() ? Feedback.CORRECT : Feedback.INCORRECT;
        return feedback;
    }

    public boolean advance() {
        if (stage == COMPLETE) {
            if (level >= 0) return false;
            reset();
            return true;
        }
        if (stage == NITENI) {
            if (isTriangleTutorial()) {
                triangleStep++;
                return true;
            }
            stage = level >= 0 ? COMPLETE : NIROKKE;
        } else {
            if (feedback != Feedback.CORRECT) return false;
            if (index + 1 < QUESTION_COUNT) {
                index++;
            } else {
                stage = level >= 0 ? COMPLETE : stage + 1;
                index = 0;
            }
        }
        feedback = Feedback.NONE;
        if (stage != COMPLETE) {
            answers[currentSlot()] = "";
            working[currentSlot()] = "";
        }
        return true;
    }

    public void reset() {
        stage = level >= 0 ? level : NITENI;
        index = 0;
        triangleStep = level == NITENI ? 0 : TRIANGLE_RACE_STEP;
        feedback = Feedback.NONE;
        Arrays.fill(answers, "");
        Arrays.fill(working, "");
    }

    /** Versioned snapshot retains every page; transient feedback is intentionally omitted. */
    public String encode() {
        if (level >= 0) return encodeLevel();
        StringBuilder encoded = new StringBuilder("BK1|");
        encoded.append(stage).append('|').append(index);
        for (int slot = 0; slot < QUESTIONS.length; slot++) {
            encoded.append('|');
            appendHex(encoded, answers[slot]);
            encoded.append('|');
            appendHex(encoded, working[slot]);
        }
        return encoded.toString();
    }

    private String encodeLevel() {
        StringBuilder encoded = new StringBuilder(level == NITENI ? "BK4|" : "BK2|");
        encoded.append(level).append('|').append(isLevelComplete() ? 1 : 0).append('|').append(index);
        if (level == NITENI) encoded.append('|').append(triangleStep);
        for (int page = 0; page < QUESTION_COUNT; page++) {
            int slot = level == NAMBAHI ? QUESTION_COUNT + page : page;
            encoded.append('|'); appendHex(encoded, answers[slot]);
            encoded.append('|'); appendHex(encoded, working[slot]);
        }
        return encoded.toString();
    }

    /** Restores only this map node and rejects forged completion or another node's work. */
    public static BalapKarungSession restoreForLevel(int level, String encoded) {
        BalapKarungSession fresh = forLevel(level);
        if (encoded == null) return fresh;
        String[] fields = encoded.split("\\|", -1);
        boolean oldTutorial = fields.length > 0 && "BK3".equals(fields[0]);
        boolean tutorialFormat = oldTutorial || fields.length > 0 && "BK4".equals(fields[0]);
        int offset = tutorialFormat ? 5 : 4;
        if (fields.length != offset + QUESTION_COUNT * 2 || !(tutorialFormat || "BK2".equals(fields[0]))
                || tutorialFormat && level != NITENI
                || metadata(fields[1], NAMBAHI) != level) return fresh;
        int completed = metadata(fields[2], 1);
        int savedIndex = metadata(fields[3], QUESTION_COUNT - 1);
        if (completed < 0 || savedIndex < 0 || (level == NITENI || completed == 1) && savedIndex != 0) return fresh;
        BalapKarungSession saved = forLevel(level);
        saved.stage = completed == 1 ? COMPLETE : level;
        saved.index = savedIndex;
        if (level == NITENI) {
            int step = tutorialFormat ? metadata(fields[4], oldTutorial ? 6 : TRIANGLE_RACE_STEP)
                    : completed == 1 ? TRIANGLE_RACE_STEP : 0;
            if (step < 0 || completed == 1 && step != (oldTutorial ? 6 : TRIANGLE_RACE_STEP)) return fresh;
            saved.triangleStep = oldTutorial ? step < 2 ? 0 : step < 4 ? 3 : step < 6 ? 4 : TRIANGLE_RACE_STEP : step;
        }
        for (int page = 0; page < QUESTION_COUNT; page++) {
            int slot = level == NAMBAHI ? QUESTION_COUNT + page : page;
            String answer = decodeHex(fields[offset + page * 2], Integer.MAX_VALUE);
            String work = decodeHex(fields[offset + 1 + page * 2], MAX_WORKING);
            if (answer == null || work == null) return fresh;
            if (level == NITENI) {
                if (!answer.isEmpty() || !work.isEmpty()) return fresh;
            } else if (completed == 1 || page < savedIndex) {
                if (parseAnswer(answer) != QUESTIONS[slot].expectedAnswer()) return fresh;
            } else if (page > savedIndex && (!answer.isEmpty() || !work.isEmpty())) return fresh;
            saved.answers[slot] = answer;
            saved.working[slot] = work;
        }
        return saved;
    }

    /** Copies the appropriate part of a BK1 lesson; the original encoding remains untouched. */
    public static BalapKarungSession migrateLegacyLevel(int level, String encoded) {
        BalapKarungSession session = forLevel(level);
        BalapKarungSession legacy = restore(encoded);
        if (legacy.stage < level) return session;
        session.stage = legacy.stage > level ? COMPLETE : level;
        if (level == NITENI && session.stage == COMPLETE) session.triangleStep = TRIANGLE_RACE_STEP;
        session.index = session.stage == COMPLETE ? 0 : legacy.index;
        if (level != NITENI) {
            int first = level == NAMBAHI ? QUESTION_COUNT : 0;
            for (int page = 0; page < QUESTION_COUNT; page++) {
                session.answers[first + page] = legacy.answers[first + page];
                session.working[first + page] = legacy.working[first + page];
            }
        }
        return session;
    }

    /** Reject the whole snapshot if metadata, text, or completed-page history is inconsistent. */
    public static BalapKarungSession restore(String encoded) {
        BalapKarungSession fresh = new BalapKarungSession();
        if (encoded == null) return fresh;
        String[] fields = encoded.split("\\|", -1);
        if (fields.length != 3 + QUESTIONS.length * 2 || !"BK1".equals(fields[0])) return fresh;
        int savedStage = metadata(fields[1], COMPLETE);
        int savedIndex = metadata(fields[2], QUESTION_COUNT - 1);
        if (savedStage < 0 || savedIndex < 0) return fresh;
        if ((savedStage == NITENI || savedStage == COMPLETE) && savedIndex != 0) return fresh;
        BalapKarungSession saved = new BalapKarungSession();
        saved.stage = savedStage;
        saved.index = savedIndex;
        for (int slot = 0; slot < QUESTIONS.length; slot++) {
            String answer = decodeHex(fields[3 + slot * 2], Integer.MAX_VALUE);
            String work = decodeHex(fields[4 + slot * 2], MAX_WORKING);
            if (answer == null || work == null) return fresh;
            saved.answers[slot] = answer;
            saved.working[slot] = work;
        }
        int current = saved.currentSlot();
        for (int slot = 0; slot < QUESTIONS.length; slot++) {
            if (slot < current || savedStage == COMPLETE) {
                if (parseAnswer(saved.answers[slot]) != QUESTIONS[slot].expectedAnswer()) return fresh;
            } else if (slot > current && (!saved.answers[slot].isEmpty() || !saved.working[slot].isEmpty())) {
                return fresh;
            }
        }
        return saved;
    }

    public static String racerName(int racer) {
        return validRacer(racer) ? RACER_NAMES[racer] : "";
    }

    public static double raceSpeed(int racer) {
        return validRacer(racer) ? RACER_SPEEDS[racer] : 0.0;
    }

    public static double raceFinishSeconds(int racer) {
        return validRacer(racer) ? RACE_DISTANCE / RACER_SPEEDS[racer] : 0.0;
    }

    public static double racePosition(int racer, double elapsedSeconds) {
        if (!validRacer(racer) || Double.isNaN(elapsedSeconds) || elapsedSeconds <= 0) return 0.0;
        return Math.min(RACE_DISTANCE, RACER_SPEEDS[racer] * elapsedSeconds);
    }

    private int currentSlot() {
        if (stage == COMPLETE) return level == NITENI ? 0 : level == NIROKKE ? QUESTION_COUNT - 1 : QUESTIONS.length - 1;
        return stage == NAMBAHI ? QUESTION_COUNT + index : index;
    }

    private boolean isQuestionStage() { return stage == NIROKKE || stage == NAMBAHI; }

    private static boolean validRacer(int racer) { return racer >= 0 && racer < RACER_SPEEDS.length; }

    private static void requireLevel(int level) {
        if (level < NITENI || level > NAMBAHI) throw new IllegalArgumentException("Unknown Balap Karung level");
    }

    private static int parseAnswer(String text) {
        if (text.isEmpty()) return -1;
        int value = 0;
        for (int i = 0; i < text.length(); i++) {
            char digit = text.charAt(i);
            if (digit < '0' || digit > '9') return -1;
            value = value * 10 + digit - '0';
            if (value > 9999) return -1;
        }
        return value;
    }

    private static int metadata(String text, int maximum) {
        if (text.length() != 1) return -1;
        int value = text.charAt(0) - '0';
        return value >= 0 && value <= maximum ? value : -1;
    }

    private static void appendHex(StringBuilder encoded, String text) {
        final String hex = "0123456789abcdef";
        for (int i = 0; i < text.length(); i++) {
            int character = text.charAt(i);
            for (int shift = 12; shift >= 0; shift -= 4) encoded.append(hex.charAt((character >> shift) & 15));
        }
    }

    private static String decodeHex(String encoded, int maximumChars) {
        if (encoded.length() % 4 != 0 || encoded.length() / 4 > maximumChars) return null;
        StringBuilder decoded = new StringBuilder(encoded.length() / 4);
        for (int i = 0; i < encoded.length(); i += 4) {
            int character = 0;
            for (int offset = 0; offset < 4; offset++) {
                char digit = encoded.charAt(i + offset);
                int value;
                if (digit >= '0' && digit <= '9') value = digit - '0';
                else if (digit >= 'a' && digit <= 'f') value = digit - 'a' + 10;
                else if (digit >= 'A' && digit <= 'F') value = digit - 'A' + 10;
                else return null;
                character = character * 16 + value;
            }
            decoded.append((char) character);
        }
        return decoded.toString();
    }
}
