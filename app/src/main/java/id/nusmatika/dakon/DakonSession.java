package id.nusmatika.dakon;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;

/** Offline lesson state. Hole numbers are one-based; seed order is insertion order. */
public final class DakonSession {
    public static final int NITENI = 0, NIROKKE = 1, NAMBAHI = 2, COMPLETE = 3;
    public static final int BLUE = 0, CORAL = 1, REMOVE = 2, MOVE = 3;
    public enum Feedback { NONE, INVALID, CORRECT, COMMON_NOT_SMALLEST, NOT_COMMON }

    private static final int HOLES = 24;
    private static final int MAX_SEEDS = 99;
    private static final int MAX_HISTORY = 100;
    private static final int[] GUIDE_HOLES = {3, 6, 9, 12, 15, 18, 21, 24, 4, 8, 12, 16, 20, 24};

    private int stage;
    private int tool;
    private int moveSource;
    private int guideStep;
    private int[][] board;
    private String answer;
    private Feedback feedback;
    private final Deque<int[][]> history = new ArrayDeque<int[][]>();

    public DakonSession() {
        reset();
    }

    public int getStage() { return stage; }
    public int getTool() { return tool; }
    public int getMoveSource() { return moveSource; }
    public int getGuideStep() { return guideStep; }
    public int getGuideLength() { return GUIDE_HOLES.length; }

    public int getExpectedHole() {
        return stage == NIROKKE && guideStep < GUIDE_HOLES.length ? GUIDE_HOLES[guideStep] : 0;
    }

    public int getExpectedKind() {
        return stage == NIROKKE && guideStep >= 8 && guideStep < GUIDE_HOLES.length ? CORAL : BLUE;
    }

    public int[] getSeeds(int hole) {
        return validHole(hole) ? board[hole - 1].clone() : new int[0];
    }

    public boolean setTool(int nextTool) {
        if (nextTool < BLUE || nextTool > MOVE || stage == NIROKKE || stage == COMPLETE || nextTool == tool) return false;
        tool = nextTool;
        moveSource = 0;
        return true;
    }

    public boolean tapHole(int hole) {
        if (!validHole(hole) || stage == COMPLETE) return false;
        if (stage == NIROKKE) {
            if (hole != getExpectedHole()) return false;
            append(hole, getExpectedKind());
            guideStep++;
            tool = getExpectedKind();
            feedback = Feedback.NONE;
            return true;
        }
        if (tool == MOVE) {
            if (moveSource == 0) {
                if (board[hole - 1].length == 0) return false;
                moveSource = hole;
                return true;
            }
            if (hole == moveSource) {
                moveSource = 0;
                return true;
            }
            if (board[hole - 1].length >= MAX_SEEDS) return false;
            remember();
            int kind = removeLast(moveSource);
            append(hole, kind);
            moveSource = 0;
        } else if (tool == REMOVE) {
            if (board[hole - 1].length == 0) return false;
            remember();
            removeLast(hole);
        } else {
            if (board[hole - 1].length >= MAX_SEEDS) return false;
            remember();
            append(hole, tool);
        }
        feedback = Feedback.NONE;
        return true;
    }

    public boolean undo() {
        if (!canUndo()) return false;
        if (stage == NIROKKE) {
            guideStep--;
            removeLast(GUIDE_HOLES[guideStep]);
            tool = getExpectedKind();
        } else {
            board = history.removeLast();
        }
        moveSource = 0;
        feedback = Feedback.NONE;
        return true;
    }

    public boolean clearBoard() {
        if (stage == COMPLETE) return false;
        boolean occupied = false;
        for (int[] seeds : board) occupied |= seeds.length > 0;
        if (!occupied && moveSource == 0) return false;
        if (stage == NIROKKE) {
            guideStep = 0;
            tool = BLUE;
            history.clear();
        } else if (occupied) {
            remember();
        }
        board = emptyBoard();
        moveSource = 0;
        feedback = Feedback.NONE;
        return true;
    }

    public boolean canUndo() {
        return stage != COMPLETE && (stage == NIROKKE ? guideStep > 0 : !history.isEmpty());
    }

    public boolean advance() {
        if (stage == COMPLETE) {
            reset();
            return true;
        }
        if (stage == NAMBAHI && feedback == Feedback.CORRECT) {
            stage = COMPLETE;
            moveSource = 0;
            history.clear();
            return true;
        }
        if (stage == NITENI || isGuideComplete()) {
            stage++;
            board = emptyBoard();
            history.clear();
            tool = BLUE;
            moveSource = 0;
            guideStep = 0;
            answer = "";
            feedback = Feedback.NONE;
            return true;
        }
        return false;
    }

    public boolean isGuideComplete() {
        return stage == NIROKKE && guideStep == GUIDE_HOLES.length;
    }
    public String getAnswer() { return answer; }

    public void setAnswer(String answer) {
        this.answer = answer == null ? "" : answer;
        feedback = Feedback.NONE;
    }

    public boolean canSubmit() { return stage == NAMBAHI && parseAnswer(answer) > 0; }

    public Feedback submitAnswer() {
        if (stage != NAMBAHI) return Feedback.NONE;
        int value = parseAnswer(answer);
        if (value < 1) feedback = Feedback.INVALID;
        else if (value == 12) feedback = Feedback.CORRECT;
        else if (value % 3 == 0 && value % 4 == 0) feedback = Feedback.COMMON_NOT_SMALLEST;
        else feedback = Feedback.NOT_COMMON;
        return feedback;
    }

    public Feedback getFeedback() { return feedback; }
    /** Versioned text snapshot; undo, transient move selection, and feedback are not persisted. */
    public String encode() {
        StringBuilder encoded = new StringBuilder("D1|");
        encoded.append(stage).append('|').append(tool).append('|').append(guideStep).append('|');
        final String hex = "0123456789abcdef";
        for (int i = 0; i < answer.length(); i++) {
            int character = answer.charAt(i);
            for (int shift = 12; shift >= 0; shift -= 4) encoded.append(hex.charAt((character >> shift) & 15));
        }
        encoded.append('|');
        for (int i = 0; i < HOLES; i++) {
            if (i > 0) encoded.append(';');
            for (int kind : board[i]) encoded.append(kind);
        }
        return encoded.toString();
    }

    /** Rejects the entire snapshot if any field or the guided board is inconsistent. */
    public static DakonSession restore(String encoded) {
        DakonSession restored = new DakonSession();
        if (encoded == null) return restored;
        String[] fields = encoded.split("\\|", -1);
        if (fields.length != 6 || !"D1".equals(fields[0])) return restored;
        int savedStage = parseRange(fields[1], COMPLETE);
        int savedTool = parseRange(fields[2], MOVE);
        int savedGuideStep = parseRange(fields[3], GUIDE_HOLES.length);
        if (savedStage < 0 || savedTool < 0 || savedGuideStep < 0) return restored;
        if (savedStage != NIROKKE && savedGuideStep != 0) return restored;
        if (fields[4].length() % 4 != 0) return restored;
        StringBuilder savedAnswer = new StringBuilder();
        for (int i = 0; i < fields[4].length(); i += 4) {
            int character = 0;
            for (int offset = 0; offset < 4; offset++) {
                int digit = hexDigit(fields[4].charAt(i + offset));
                if (digit < 0) return restored;
                character = character * 16 + digit;
            }
            savedAnswer.append((char) character);
        }
        String[] holes = fields[5].split(";", -1);
        if (holes.length != HOLES) return restored;
        int[][] savedBoard = emptyBoard();
        for (int i = 0; i < HOLES; i++) {
            if (holes[i].length() > MAX_SEEDS) return restored;
            savedBoard[i] = new int[holes[i].length()];
            for (int j = 0; j < holes[i].length(); j++) {
                char kind = holes[i].charAt(j);
                if (kind != '0' && kind != '1') return restored;
                savedBoard[i][j] = kind - '0';
            }
        }
        if (savedStage == NIROKKE) {
            int expectedTool = savedGuideStep >= 8 && savedGuideStep < GUIDE_HOLES.length ? CORAL : BLUE;
            if (savedTool != expectedTool) return restored;
            int[][] expectedBoard = emptyBoard();
            for (int i = 0; i < savedGuideStep; i++) {
                int hole = GUIDE_HOLES[i] - 1;
                int length = expectedBoard[hole].length;
                expectedBoard[hole] = Arrays.copyOf(expectedBoard[hole], length + 1);
                expectedBoard[hole][length] = i < 8 ? BLUE : CORAL;
            }
            if (!Arrays.deepEquals(expectedBoard, savedBoard)) return restored;
        }
        restored.stage = savedStage;
        restored.tool = savedTool;
        restored.guideStep = savedGuideStep;
        restored.board = savedBoard;
        restored.answer = savedAnswer.toString();
        return restored;
    }

    private void reset() {
        stage = NITENI;
        tool = BLUE;
        moveSource = 0;
        guideStep = 0;
        board = emptyBoard();
        board[4] = new int[] {BLUE};
        answer = "";
        feedback = Feedback.NONE;
        history.clear();
    }

    private static boolean validHole(int hole) { return hole >= 1 && hole <= HOLES; }

    private static int parseAnswer(String text) {
        if (text == null || text.length() == 0) return -1;
        int value = 0;
        for (int i = 0; i < text.length(); i++) {
            char digit = text.charAt(i);
            if (digit < '0' || digit > '9') return -1;
            value = value * 10 + digit - '0';
            if (value > 999999) return -1;
        }
        return value;
    }

    private static int parseRange(String text, int maximum) {
        if (text.length() == 0) return -1;
        int value = 0;
        for (int i = 0; i < text.length(); i++) {
            char digit = text.charAt(i);
            if (digit < '0' || digit > '9') return -1;
            value = value * 10 + digit - '0';
            if (value > maximum) return -1;
        }
        return value;
    }

    private static int hexDigit(char character) {
        if (character >= '0' && character <= '9') return character - '0';
        if (character >= 'a' && character <= 'f') return character - 'a' + 10;
        if (character >= 'A' && character <= 'F') return character - 'A' + 10;
        return -1;
    }

    private static int[][] emptyBoard() {
        int[][] empty = new int[HOLES][];
        for (int i = 0; i < HOLES; i++) empty[i] = new int[0];
        return empty;
    }

    private void remember() {
        if (history.size() == MAX_HISTORY) history.removeFirst();
        // Hole arrays are immutable: each edit replaces its array.
        history.addLast(board.clone());
    }

    private void append(int hole, int kind) {
        int[] seeds = board[hole - 1];
        int[] added = Arrays.copyOf(seeds, seeds.length + 1);
        added[seeds.length] = kind;
        board[hole - 1] = added;
    }

    private int removeLast(int hole) {
        int[] seeds = board[hole - 1];
        int removed = seeds[seeds.length - 1];
        board[hole - 1] = Arrays.copyOf(seeds, seeds.length - 1);
        return removed;
    }
}
