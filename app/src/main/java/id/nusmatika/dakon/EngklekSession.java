package id.nusmatika.dakon;

import java.util.ArrayList;
import java.util.List;

/** Aturan kegiatan dan penyimpanan sesi. Tidak bergantung pada Android. */
public final class EngklekSession {
    private final int level;
    private int taskIndex;
    private long selected;
    private boolean passed, folded, unfolded;
    private int choice = -1, reason = -1;
    private final List<Long> discoveries = new ArrayList<>();

    public EngklekSession(int level) {
        if (level < 0 || level >= 3) throw new IllegalArgumentException("Tahap tidak dikenal");
        this.level = level;
        resetTask();
    }

    public int level() { return level; }
    public int taskIndex() { return taskIndex; }
    public boolean complete() { return taskIndex == EngklekCatalog.count(level); }
    public boolean passed() { return passed; }
    public long selected() { return selected; }
    public int choice() { return choice; }
    public int reason() { return reason; }
    public int discoveredCount() { return discoveries.size() + 1; }
    public EngklekCatalog.Task task() {
        return EngklekCatalog.task(level, Math.min(taskIndex, EngklekCatalog.count(level) - 1));
    }

    public String toggle(int cell) {
        if (complete() || !task().editable()) return "Pola ini untuk diamati.";
        if (cell < 0 || cell >= EngklekNet.COLUMNS * EngklekNet.ROWS) return "";
        if (EngklekNet.contains(task().initial, cell)) return "Bidang biru sudah ditetapkan.";
        if (!EngklekNet.contains(selected, cell) && Long.bitCount(selected) == 6) {
            return "Enam sisi sudah dipilih. Batalkan satu pilihan untuk menggantinya.";
        }
        selected ^= 1L << cell;
        passed = false;
        return "Terpilih " + Long.bitCount(selected) + " dari 6 sisi.";
    }

    public void choose(int value) {
        if (complete() || (task().kind != EngklekCatalog.JUDGE && task().kind != EngklekCatalog.GIFT)) return;
        if (value < 0 || value > 1) return;
        choice = value;
        passed = false;
    }

    public void giveReason(int value) {
        if (complete() || (task().kind != EngklekCatalog.JUDGE && task().kind != EngklekCatalog.GIFT)) return;
        if (value < 0 || value >= EngklekCatalog.REASONS.length) return;
        reason = value;
        passed = false;
    }

    public void observed(boolean closed) {
        if (complete() || task().kind != EngklekCatalog.OBSERVE) return;
        if (closed) folded = true;
        else if (folded) unfolded = true;
        passed = folded && unfolded;
    }

    public long previewMask() {
        if (task().kind == EngklekCatalog.GIFT) return task().options[Math.max(0, choice)];
        return selected;
    }

    public String check() {
        if (complete()) return "Tahap sudah selesai.";
        EngklekCatalog.Task activity = task();
        if (activity.kind == EngklekCatalog.OBSERVE) {
            return passed ? "Pengamatan selesai. Kamu boleh melanjutkan."
                    : "Tekan Lipat sampai selesai, lalu Bentangkan sampai selesai.";
        }
        if (activity.kind == EngklekCatalog.JUDGE || activity.kind == EngklekCatalog.GIFT) {
            if (choice < 0 || reason < 0) return "Pilih jawaban dan alasannya terlebih dahulu.";
            if (choice != activity.correctChoice) return "Jawaban belum tepat. " + activity.hint;
            if (reason != activity.correctReason) return "Jawabanmu tepat, tetapi alasannya perlu diperiksa lagi.";
            passed = true;
            return "Benar! " + EngklekCatalog.REASONS[reason];
        }
        EngklekNet.Result result = EngklekNet.inspect(selected, activity.cuboid);
        if (!result.valid) return result.message;
        if (activity.kind == EngklekCatalog.OTHER_NET && !passed) {
            String pattern = EngklekNet.canonical(selected);
            if (pattern.equals(EngklekNet.canonical(activity.reference))) {
                return "Ini masih sama dengan contoh setelah diputar atau dicerminkan. Coba pola lain.";
            }
            for (long found : discoveries) if (pattern.equals(EngklekNet.canonical(found))) {
                return "Pola ini sudah kamu temukan. Coba susunan lain.";
            }
            discoveries.add(selected);
        }
        passed = true;
        return result.message;
    }

    public boolean advance() {
        if (complete() || !passed) return false;
        taskIndex++;
        if (!complete()) resetTask();
        return true;
    }

    public void resetTask() {
        if (complete()) return;
        selected = task().initial;
        passed = folded = unfolded = false;
        choice = reason = -1;
    }

    public String encode() {
        StringBuilder data = new StringBuilder("EN1|").append(level).append('|').append(taskIndex)
                .append('|').append(selected).append('|').append(passed ? 1 : 0)
                .append('|').append(folded ? 1 : 0).append('|').append(unfolded ? 1 : 0)
                .append('|').append(choice).append('|').append(reason).append('|');
        for (long found : discoveries) data.append(found).append(',');
        return data.toString();
    }

    public static EngklekSession restore(int level, String text) {
        EngklekSession session = new EngklekSession(level);
        if (text == null || text.length() > 4096) return session;
        try {
            String[] fields = text.split("\\|", -1);
            if (fields.length != 10 || !"EN1".equals(fields[0])
                    || Integer.parseInt(fields[1]) != level) return session;
            int index = Integer.parseInt(fields[2]);
            if (index < 0 || index > EngklekCatalog.count(level)) return session;
            session.taskIndex = index;
            long mask = Long.parseLong(fields[3]);
            if ((mask & ~EngklekNet.BOARD_MASK) != 0 || Long.bitCount(mask) > 6) {
                return new EngklekSession(level);
            }
            session.selected = session.task().editable() ? mask : session.task().initial;
            if ((session.selected & session.task().initial) != session.task().initial) {
                return new EngklekSession(level);
            }
            session.folded = "1".equals(fields[5]);
            session.unfolded = session.folded && "1".equals(fields[6]);
            session.choose(Integer.parseInt(fields[7]));
            session.giveReason(Integer.parseInt(fields[8]));
            for (String encoded : fields[9].split(",")) {
                if (encoded.isEmpty()) continue;
                long found = Long.parseLong(encoded);
                if (!EngklekNet.inspect(found, false).valid) continue;
                String key = EngklekNet.canonical(found);
                boolean duplicate = key.equals(EngklekNet.canonical(EngklekCatalog.CUBE));
                for (long previous : session.discoveries) {
                    duplicate |= key.equals(EngklekNet.canonical(previous));
                }
                if (!duplicate) session.discoveries.add(found);
            }
            if (!session.complete() && "1".equals(fields[4])) {
                if (session.task().kind == EngklekCatalog.OBSERVE) {
                    session.passed = session.folded && session.unfolded;
                } else if (session.task().kind == EngklekCatalog.OTHER_NET) {
                    String key = EngklekNet.canonical(session.selected);
                    for (long found : session.discoveries) {
                        session.passed |= key.equals(EngklekNet.canonical(found));
                    }
                } else session.check();
            }
            return session;
        } catch (RuntimeException ignored) {
            return new EngklekSession(level);
        }
    }
}
