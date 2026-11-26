package id.nusmatika.dakon;

/** Posisi memakai satuan kisi agar tetap sama ketika ukuran layar berubah. */
public final class PetakUmpetAssemblyModel {
    static final int COUNT = PetakUmpetCatalog.ITEM_COUNT, COLS = 7, ROWS = 7;
    static final float SNAP = .32f;
    final int[] x = new int[COUNT], y = new int[COUNT], w = new int[COUNT], h = new int[COUNT];
    final boolean[] placed = new boolean[COUNT];
    int selected = -1;
    PetakUmpetAssemblyModel() { reset(); }
    boolean isBalok(int i) { return PetakUmpetCatalog.piece(i).kind == PetakUmpetCatalog.Kind.BALOK; }
    void reset() {
        selected = -1;
        for (int i = 0; i < COUNT; i++) {
            placed[i] = false; x[i] = 0; y[i] = 0;
            w[i] = isBalok(i) ? 2 : 1; h[i] = 1;
        }
    }
    int placedCount() { int n = 0; for (boolean value : placed) if (value) n++; return n; }
    boolean fits(int item, int px, int py, int pw, int ph) {
        if (px < 0 || py < 0 || px + pw > COLS || py + ph > ROWS) return false;
        for (int j = 0; j < COUNT; j++)
            if (j != item && placed[j] && px < x[j] + w[j] && px + pw > x[j]
                    && py < y[j] + h[j] && py + ph > y[j]) return false;
        return true;
    }
    boolean adjacent(int item, int px, int py) {
        for (int j = 0; j < COUNT; j++) {
            if (j == item || !placed[j]) continue;
            boolean vertical = (px + w[item] == x[j] || x[j] + w[j] == px)
                    && Math.min(py + h[item], y[j] + h[j]) > Math.max(py, y[j]);
            boolean horizontal = (py + h[item] == y[j] || y[j] + h[j] == py)
                    && Math.min(px + w[item], x[j] + w[j]) > Math.max(px, x[j]);
            if (vertical || horizontal) return true;
        }
        return false;
    }
    boolean magnet(int item, float px, float py) {
        int gx = Math.round(px), gy = Math.round(py);
        return Math.abs(px - gx) <= SNAP && Math.abs(py - gy) <= SNAP
                && fits(item, gx, gy, w[item], h[item]) && adjacent(item, gx, gy);
    }
    boolean place(int item, int px, int py) {
        if (!fits(item, px, py, w[item], h[item])) return false;
        x[item] = px; y[item] = py; placed[item] = true;
        return true;
    }
    boolean rotate(int item) {
        if (!isBalok(item)) return false;
        int pw = h[item], ph = w[item];
        int px = Math.min(x[item], COLS - pw), py = Math.min(y[item], ROWS - ph);
        if (placed[item] && !fits(item, px, py, pw, ph)) return false;
        w[item] = pw; h[item] = ph; x[item] = px; y[item] = py;
        return true;
    }
    boolean isSolved() {
        if (placedCount() != COUNT) return false;
        int minX = COLS, minY = ROWS, cells = 0;
        for (int i = 0; i < COUNT; i++) {
            if (!fits(i, x[i], y[i], w[i], h[i])) return false;
            minX = Math.min(minX, x[i]); minY = Math.min(minY, y[i]);
        }
        for (int i = 0; i < COUNT; i++)
            for (int dx = 0; dx < w[i]; dx++) for (int dy = 0; dy < h[i]; dy++) {
                int cx = x[i] + dx - minX, cy = y[i] + dy - minY;
                if (!((cx == 0 && cy >= 0 && cy < 6)
                        || (cy == 5 && cx >= 0 && cx < 5))) return false;
                cells++;
            }
        return cells == 10;
    }
    String encode() {
        StringBuilder s = new StringBuilder("DD1|").append(selected);
        for (int i = 0; i < COUNT; i++)
            s.append('|').append(placed[i] ? 1 : 0).append(',').append(x[i]).append(',')
                    .append(y[i]).append(',').append(w[i]).append(',').append(h[i]);
        return s.toString();
    }
    static PetakUmpetAssemblyModel restore(String data) {
        if (data == null || data.length() > 240) return null;
        try {
            String[] parts = data.split("\\|", -1);
            if (parts.length != COUNT + 2 || !"DD1".equals(parts[0])) return null;
            PetakUmpetAssemblyModel m = new PetakUmpetAssemblyModel();
            m.selected = Integer.parseInt(parts[1]);
            if (m.selected < -1 || m.selected >= COUNT) return null;
            for (int i = 0; i < COUNT; i++) {
                String[] p = parts[i + 2].split(",", -1);
                if (p.length != 5 || !("0".equals(p[0]) || "1".equals(p[0]))) return null;
                m.x[i] = Integer.parseInt(p[1]); m.y[i] = Integer.parseInt(p[2]);
                m.w[i] = Integer.parseInt(p[3]); m.h[i] = Integer.parseInt(p[4]);
                if (m.x[i] < 0 || m.y[i] < 0 || m.x[i] >= COLS || m.y[i] >= ROWS) return null;
                if (m.isBalok(i) ? !((m.w[i] == 2 && m.h[i] == 1)
                        || (m.w[i] == 1 && m.h[i] == 2))
                        : m.w[i] != 1 || m.h[i] != 1) return null;
                if ("1".equals(p[0]) && !m.place(i, m.x[i], m.y[i])) return null;
            }
            return m;
        } catch (IllegalArgumentException invalid) { return null; }
    }
}
