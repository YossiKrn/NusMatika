package id.nusmatika.dakon;

/** Independent worksheet answers and explicit checking state. */
public final class WorksheetState {
    public static final int FIELD_COUNT = 10;
    private static final int MAX_FIELD_LENGTH = 16;
    private static final int[] EXPECTED = {3, 6, 9, 12, 4, 8, 12, 16, 12, 12};
    private static final String SNAPSHOT_VERSION = "W1";
    private static final char[] HEX = "0123456789abcdef".toCharArray();
    private final String[] answers = new String[FIELD_COUNT];
    private boolean checked;

    public WorksheetState() {
        reset();
    }

    public String get(int index) {
        return validIndex(index) ? answers[index] : "";
    }

    /** Keep the written text, including invalid input, so a check never erases work. */
    public void set(int index, String value) {
        if (!validIndex(index)) return;
        String bounded = value == null ? "" : value;
        if (bounded.length() > MAX_FIELD_LENGTH) bounded = bounded.substring(0, MAX_FIELD_LENGTH);
        if (!answers[index].equals(bounded)) {
            answers[index] = bounded;
            checked = false;
        }
    }

    /** Completion gates checking without revealing whether any answer is correct. */
    public boolean isComplete() {
        for (String answer : answers) if (positiveNumber(answer) < 1) return false;
        return true;
    }

    public boolean check() {
        checked = true;
        return allCorrect();
    }

    public boolean isChecked() {
        return checked;
    }

    public boolean isCorrect() {
        return checked && allCorrect();
    }

    public boolean isIncorrect(int index) {
        return checked && validIndex(index) && positiveNumber(answers[index]) != EXPECTED[index];
    }

    public void reset() {
        for (int i = 0; i < FIELD_COUNT; i++) answers[i] = "";
        checked = false;
    }

    /** Versioned ASCII snapshot; fixed-width hex safely preserves arbitrary written text. */
    public String encode() {
        StringBuilder result = new StringBuilder(SNAPSHOT_VERSION);
        for (String answer : answers) {
            result.append('|');
            for (int i = 0; i < answer.length(); i++) {
                int value = answer.charAt(i);
                result.append(HEX[(value >>> 12) & 15]);
                result.append(HEX[(value >>> 8) & 15]);
                result.append(HEX[(value >>> 4) & 15]);
                result.append(HEX[value & 15]);
            }
        }
        return result.toString();
    }

    /** Invalid saves fail atomically to a blank worksheet; feedback is never restored. */
    public static WorksheetState restore(String encoded) {
        WorksheetState state = new WorksheetState();
        int maximumLength = SNAPSHOT_VERSION.length() + FIELD_COUNT * (1 + MAX_FIELD_LENGTH * 4);
        if (encoded == null || encoded.length() > maximumLength) return state;
        String[] fields = encoded.split("\\|", -1);
        if (fields.length != FIELD_COUNT + 1 || !SNAPSHOT_VERSION.equals(fields[0])) return state;
        String[] decoded = new String[FIELD_COUNT];
        for (int index = 0; index < FIELD_COUNT; index++) {
            String field = fields[index + 1];
            if (field.length() > MAX_FIELD_LENGTH * 4 || field.length() % 4 != 0) return state;
            StringBuilder answer = new StringBuilder();
            for (int offset = 0; offset < field.length(); offset += 4) {
                int value = 0;
                for (int digit = 0; digit < 4; digit++) {
                    char character = field.charAt(offset + digit);
                    int hex = character >= '0' && character <= '9' ? character - '0'
                            : character >= 'a' && character <= 'f' ? character - 'a' + 10 : -1;
                    if (hex < 0) return state;
                    value = (value << 4) | hex;
                }
                answer.append((char) value);
            }
            decoded[index] = answer.toString();
        }
        for (int index = 0; index < FIELD_COUNT; index++) state.answers[index] = decoded[index];
        return state;
    }

    private boolean allCorrect() {
        for (int index = 0; index < FIELD_COUNT; index++) {
            if (positiveNumber(answers[index]) != EXPECTED[index]) return false;
        }
        return true;
    }

    private static long positiveNumber(String answer) {
        if (answer.length() == 0) return -1;
        long value = 0;
        for (int i = 0; i < answer.length(); i++) {
            char character = answer.charAt(i);
            if (character < '0' || character > '9') return -1;
            value = value * 10 + character - '0';
        }
        return value > 0 ? value : -1;
    }

    private static boolean validIndex(int index) {
        return index >= 0 && index < FIELD_COUNT;
    }
}
