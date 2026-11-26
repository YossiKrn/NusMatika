package id.nusmatika.dakon;

/** Isi kegiatan 3N berdasarkan dokumen pemetaan CP, terpisah dari tampilan. */
public final class EngklekCatalog {
    public static final int OBSERVE = 0, BUILD = 1, OTHER_NET = 2, JUDGE = 3, GIFT = 4;
    public static final String[] STAGES = {"Niteni", "Nirokke", "Nambahi"};
    public static final String[] SUBTITLES = {
        "Amati dan lipat", "Lengkapi dan uraikan", "Temukan pola dan berikan alasan"
    };
    public static final long CUBE = EngklekNet.cells(1, 1, 0, 2, 1, 2, 2, 2, 3, 2, 1, 3);
    public static final long BOX = CUBE;
    public static final long RECTANGLE = EngklekNet.cells(1, 1, 2, 1, 3, 1, 1, 2, 2, 2, 3, 2);
    public static final long SECOND_CUBE = EngklekNet.cells(1, 0, 1, 1, 1, 2, 1, 3, 0, 1, 2, 2);
    public static final String[] REASONS = {
        "Enam sisi sesuai ukurannya dan menutup tanpa bertumpuk.",
        "Ada sisi bertumpuk sehingga bangun tidak menutup.",
        "Ukuran pasangan sisi tidak cocok untuk balok yang diminta.",
        "Bidang-bidangnya tidak tersambung melalui rusuk."
    };

    public static final class Task {
        public final int kind;
        public final boolean cuboid;
        public final String title, instruction, hint;
        public final long initial, reference;
        public final long[] options;
        public final int correctChoice, correctReason;

        Task(int kind, boolean cuboid, String title, String instruction, String hint,
                long initial, long reference, long[] options, int correctChoice, int correctReason) {
            this.kind = kind;
            this.cuboid = cuboid;
            this.title = title;
            this.instruction = instruction;
            this.hint = hint;
            this.initial = initial;
            this.reference = reference;
            this.options = options;
            this.correctChoice = correctChoice;
            this.correctReason = correctReason;
        }

        public boolean editable() { return kind == BUILD || kind == OTHER_NET; }
        public String solidName() { return cuboid ? "balok 3 × 2 × 1 satuan" : "kubus"; }
    }

    private static final Task[][] TASKS = {
        {
            observe(false, "Enam persegi menjadi kubus", CUBE,
                    "Amati kotak berwarna pada papan engklek. Tekan Lipat lalu Bentangkan."
                    + " Enam persegi sama besar menjadi enam sisi kubus."),
            observe(true, "Tiga pasang sisi menjadi balok", BOX,
                    "Perhatikan ukuran pada tiap bidang. Lipat lalu bentangkan: balok 3 × 2 × 1"
                    + " memiliki dua sisi 3 × 2, dua sisi 3 × 1, dan dua sisi 2 × 1."),
            observe(false, "Enam kotak belum tentu jaring-jaring", RECTANGLE,
                    "Lipat pola ini dan amati sisi yang bertumpuk. Meskipun ada enam persegi,"
                    + " pola ini tidak dapat menutup menjadi kubus.")
        },
        {
            build(false, "Level 1 · Lengkapi satu sisi kubus", without(CUBE, 1, 3), CUBE,
                    "Lima kotak biru sudah tersedia. Ketuk satu kotak untuk melompat dan"
                    + " melengkapi jaring-jaring kubus.", "Cari sisi yang belum menutup kubus."),
            build(true, "Level 1 · Lengkapi satu sisi balok", without(BOX, 1, 3), BOX,
                    "Lima bidang biru sudah tersedia. Pilih satu bidang lagi agar balok"
                    + " 3 × 2 × 1 dapat tertutup.", "Perhatikan pasangan sisi 3 × 2 yang belum lengkap."),
            build(false, "Level 2 · Lengkapi dua sisi kubus",
                    without(without(CUBE, 1, 1), 1, 3), CUBE,
                    "Empat kotak sudah ada. Pilih dua kotak tambahan. Ketuk lagi kotak"
                    + " pilihanmu untuk membatalkannya.", "Susun enam persegi yang tersambung melalui rusuk."),
            build(true, "Level 2 · Lengkapi tiga sisi balok",
                    EngklekNet.cells(0, 2, 1, 2, 2, 2), BOX,
                    "Tiga bidang sudah ada. Lengkapi tiga bidang lain sesuai ukuran rusuknya.",
                    "Lengkapi dua sisi 3 × 2 dan satu sisi 3 × 1."),
            build(false, "Level 3 · Uraikan kubus", 0, CUBE,
                    "Kubus pada pratinjau dibuka rusuknya. Pilih enam kotak pada papan kosong"
                    + " untuk membuat jaring-jaringnya, lalu periksa dengan melipat.",
                    "Buat empat persegi berjajar, lalu tambahkan satu di atas dan satu di bawah."),
            build(true, "Level 3 · Uraikan balok", 0, BOX,
                    "Amati balok 3 × 2 × 1 yang dibuka. Pilih enam bidang pada papan kosong"
                    + " dengan tiga pasang ukuran yang sesuai.",
                    "Gunakan baris C kolom 1–4, lalu baris B dan D kolom 2.")
        },
        {
            new Task(OTHER_NET, false, "Temukan jaring-jaring lain",
                    "Buat pola kubus yang berbeda dari contoh. Pola yang hanya digeser,"
                    + " diputar, atau dicerminkan tetap dihitung sama. Ada 11 pola berbeda;"
                    + " temukan minimal satu pola baru untuk melanjutkan.",
                    "Coba empat kotak tegak. Letakkan dua kotak tambahan pada sisi berbeda.",
                    0, CUBE, null, -1, -1),
            judge(false, "Apakah pola Rani dapat dilipat?", RECTANGLE, false, 1),
            judge(false, "Apakah pola Bima dapat dilipat?", SECOND_CUBE, true, 0),
            new Task(GIFT, false, "Karton untuk kotak kado kubus",
                    "Sari ingin membuat kotak kado tertutup berbentuk kubus. Pilih pola karton"
                    + " A atau B yang dapat digunakan, lalu pilih alasannya.",
                    "Enam persegi saja belum cukup; perhatikan kemungkinan sisi bertumpuk.",
                    0, 0, new long[]{RECTANGLE, SECOND_CUBE}, 1, 0),
            new Task(GIFT, true, "Karton untuk kotak kado balok",
                    "Doni ingin membuat kotak kado tertutup berukuran 3 × 2 × 1 satuan."
                    + " Pilih pola karton A atau B dan jelaskan alasannya.",
                    "Selain posisi lipatan, cocokkan tiga pasang ukuran bidang sisinya.",
                    0, 0, new long[]{BOX, RECTANGLE}, 0, 0)
        }
    };

    private EngklekCatalog() { }
    public static int count(int level) { return TASKS[level].length; }
    public static Task task(int level, int index) { return TASKS[level][index]; }

    private static long without(long mask, int x, int y) {
        return mask & ~(1L << EngklekNet.index(x, y));
    }
    private static Task observe(boolean box, String title, long mask, String text) {
        return new Task(OBSERVE, box, title, text, "Amati perbedaan susunan sebelum dan setelah dilipat.",
                mask, mask, null, -1, -1);
    }
    private static Task build(boolean box, String title, long initial, long reference,
            String text, String hint) {
        return new Task(BUILD, box, title, text, hint, initial, reference, null, -1, -1);
    }
    private static Task judge(boolean box, String title, long mask, boolean valid, int reason) {
        return new Task(JUDGE, box, title,
                "Amati pola. Tentukan apakah dapat membentuk " + (box ? "balok" : "kubus")
                        + " tertutup, lalu pilih alasanmu sebelum mencoba lipatan.",
                "Perhatikan jumlah sisi, hubungan rusuk, dan kemungkinan sisi bertumpuk.",
                mask, mask, null, valid ? 1 : 0, reason);
    }
}
