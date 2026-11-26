package id.nusmatika.dakon;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Geometri murni: memeriksa pelipatan setiap susunan, bukan daftar jawaban tetap. */
public final class EngklekNet {
    public static final int COLUMNS = 5;
    public static final int ROWS = 6;
    public static final long BOARD_MASK = (1L << (COLUMNS * ROWS)) - 1;
    private static final int[] DX = {1, -1, 0, 0};
    private static final int[] DY = {0, 0, 1, -1};
    private static final int[] BOX_WIDTHS = {2, 3, 2, 3, 2};
    private static final int[] BOX_HEIGHTS = {1, 2, 1, 2, 1, 2};

    private EngklekNet() { }

    public static int index(int column, int row) { return row * COLUMNS + column; }
    public static int column(int cell) { return cell % COLUMNS; }
    public static int row(int cell) { return cell / COLUMNS; }
    public static boolean contains(long mask, int cell) {
        return cell >= 0 && cell < COLUMNS * ROWS && (mask & (1L << cell)) != 0;
    }
    public static long cells(int... coordinates) {
        if (coordinates.length % 2 != 0) throw new IllegalArgumentException("Pasangan koordinat");
        long mask = 0;
        for (int i = 0; i < coordinates.length; i += 2) {
            int x = coordinates[i], y = coordinates[i + 1];
            if (x < 0 || x >= COLUMNS || y < 0 || y >= ROWS) {
                throw new IllegalArgumentException("Koordinat di luar papan");
            }
            mask |= 1L << index(x, y);
        }
        return mask;
    }
    public static int width(boolean cuboid, int cell) {
        return cuboid ? BOX_WIDTHS[column(cell)] : 1;
    }
    public static int height(boolean cuboid, int cell) {
        return cuboid ? BOX_HEIGHTS[row(cell)] : 1;
    }

    public static final class Face {
        public final int cell, parent, direction;
        public final int[] across, down, normal;

        Face(int cell, int parent, int direction, int[] across, int[] down, int[] normal) {
            this.cell = cell;
            this.parent = parent;
            this.direction = direction;
            this.across = across;
            this.down = down;
            this.normal = normal;
        }
    }

    public static final class Result {
        public final boolean valid;
        public final String message;
        /** Urutan induk selalu mendahului anak, juga dipakai oleh animasi lipatan. */
        public final List<Face> faces;

        Result(boolean valid, String message, List<Face> faces) {
            this.valid = valid;
            this.message = message;
            this.faces = Collections.unmodifiableList(faces);
        }
    }

    public static Result inspect(long mask, boolean cuboid) {
        List<Face> faces = new ArrayList<>();
        if ((mask & ~BOARD_MASK) != 0 || Long.bitCount(mask) != 6) {
            return new Result(false, "Pilih tepat 6 bidang sisi terlebih dahulu.", faces);
        }
        Face[] visited = new Face[COLUMNS * ROWS];
        int first = Long.numberOfTrailingZeros(mask);
        Face root = new Face(first, -1, -1, new int[]{1, 0, 0},
                new int[]{0, 1, 0}, new int[]{0, 0, 1});
        ArrayDeque<Face> queue = new ArrayDeque<>();
        queue.add(root);
        visited[first] = root;
        boolean inconsistent = false;
        while (!queue.isEmpty()) {
            Face face = queue.remove();
            faces.add(face);
            for (int direction = 0; direction < 4; direction++) {
                int x = column(face.cell) + DX[direction];
                int y = row(face.cell) + DY[direction];
                if (x < 0 || x >= COLUMNS || y < 0 || y >= ROWS) continue;
                int next = index(x, y);
                if (!contains(mask, next)) continue;
                Face folded = neighbour(face, next, direction);
                if (visited[next] == null) {
                    visited[next] = folded;
                    queue.add(folded);
                } else if (!Arrays.equals(visited[next].across, folded.across)
                        || !Arrays.equals(visited[next].down, folded.down)) {
                    inconsistent = true;
                }
            }
        }
        if (faces.size() != 6) return new Result(false,
                "Semua sisi harus tersambung melalui rusuk. Sentuhan sudut saja belum cukup.", faces);
        if (inconsistent) return new Result(false,
                "Lipatan saling menghalangi. Ada sisi bertumpuk dan bangun tidak menutup.", faces);
        List<String> normals = new ArrayList<>();
        for (Face face : faces) {
            String normal = Arrays.toString(face.normal);
            if (normals.contains(normal)) return new Result(false,
                    "Ada dua sisi yang menempati arah yang sama: bertumpuk, sehingga sisi lain terbuka.", faces);
            normals.add(normal);
        }
        if (cuboid && !dimensionsMatch(faces)) return new Result(false,
                "Ukuran sisi belum cocok. Balok 3 × 2 × 1 memerlukan 2 sisi 3 × 2,"
                        + " 2 sisi 3 × 1, dan 2 sisi 2 × 1 dengan rusuk yang cocok.", faces);
        return new Result(true, cuboid
                ? "Benar! Tiga pasang sisi yang sesuai menutup menjadi balok 3 × 2 × 1."
                : "Benar! Enam persegi menutup menjadi kubus tanpa sisi bertumpuk.", faces);
    }

    private static Face neighbour(Face face, int cell, int direction) {
        if (direction == 0) return new Face(cell, face.cell, direction,
                negative(face.normal), face.down, face.across);
        if (direction == 1) return new Face(cell, face.cell, direction,
                face.normal, face.down, negative(face.across));
        if (direction == 2) return new Face(cell, face.cell, direction,
                face.across, negative(face.normal), face.down);
        return new Face(cell, face.cell, direction,
                face.across, face.normal, negative(face.down));
    }

    private static int[] negative(int[] vector) {
        return new int[]{-vector[0], -vector[1], -vector[2]};
    }

    private static boolean dimensionsMatch(List<Face> faces) {
        Face root = faces.get(0);
        int width = width(true, root.cell), height = height(true, root.cell);
        if (width == height) return false;
        int depth = 6 - width - height;
        int[] sizes = {width, height, depth};
        for (Face face : faces) {
            if (width(true, face.cell) != axisLength(face.across, sizes)
                    || height(true, face.cell) != axisLength(face.down, sizes)) return false;
        }
        return true;
    }

    private static int axisLength(int[] axis, int[] sizes) {
        return Math.abs(axis[0]) * sizes[0] + Math.abs(axis[1]) * sizes[1]
                + Math.abs(axis[2]) * sizes[2];
    }

    /** Identitas pola kubus; geser, putar dan cermin tetap dihitung sebagai satu pola. */
    public static String canonical(long mask) {
        if (mask == 0) return "";
        String best = null;
        for (int transform = 0; transform < 8; transform++) {
            List<int[]> points = new ArrayList<>();
            int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
            for (int cell = 0; cell < COLUMNS * ROWS; cell++) {
                if (!contains(mask, cell)) continue;
                int x = column(cell), y = row(cell);
                if (transform >= 4) x = -x;
                for (int turn = 0; turn < transform % 4; turn++) {
                    int previousX = x;
                    x = -y;
                    y = previousX;
                }
                points.add(new int[]{x, y});
                minX = Math.min(minX, x);
                minY = Math.min(minY, y);
            }
            List<String> normalized = new ArrayList<>();
            for (int[] point : points) normalized.add((point[0] - minX) + "," + (point[1] - minY));
            Collections.sort(normalized);
            String key = String.join(";", normalized);
            if (best == null || key.compareTo(best) < 0) best = key;
        }
        return best;
    }
}
