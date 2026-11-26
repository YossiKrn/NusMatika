package id.nusmatika.dakon;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;

/** Menggambar contoh, kisi, bangun dan tempat temuan serta menentukan area sentuhnya. */
public final class PetakUmpetAssemblyRenderer {
    static final float WIDTH = 360, HEIGHT = 640, UNIT = 44, LEFT = 20, TOP = 148;
    private final PetakUmpetAssemblyModel model;
    private final Typeface font, headingFont;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF work = new RectF(LEFT, TOP,
            LEFT + UNIT * PetakUmpetAssemblyModel.COLS,
            TOP + UNIT * PetakUmpetAssemblyModel.ROWS);
    private final RectF tray = new RectF(6, 484, 354, 634);

    PetakUmpetAssemblyRenderer(PetakUmpetAssemblyModel model, Typeface font, Typeface headingFont) {
        this.model = model;
        this.font = font;
        this.headingFont = headingFont;
    }

    boolean isInTray(float x, float y) { return tray.contains(x, y); }

    private RectF slot(int item) {
        float x = 17 + (item % 3) * 111, y = 515 + (item / 3) * 57;
        return new RectF(x, y, x + 103, y + 52);
    }
    RectF face(int item) {
        return new RectF(LEFT + model.x[item] * UNIT, TOP + model.y[item] * UNIT,
                LEFT + (model.x[item] + model.w[item]) * UNIT,
                TOP + (model.y[item] + model.h[item]) * UNIT);
    }
    private int[] drawingOrder() {
        int[] order = new int[PetakUmpetAssemblyModel.COUNT];
        for (int i = 0; i < order.length; i++) order[i] = i;
        for (int i = 1; i < order.length; i++) {
            int id = order[i], j = i - 1;
            while (j >= 0 && (model.y[order[j]] < model.y[id]
                    || model.y[order[j]] == model.y[id] && model.x[order[j]] > model.x[id])) {
                order[j + 1] = order[j]; j--;
            }
            order[j + 1] = id;
        }
        return order;
    }
    void draw(Canvas c, int active, boolean moved, boolean sticky, float rawX, float rawY) {
        box(c, 6, 2, 354, 108, 16, 0xFFF4F8FC);
        text(c, "CONTOH", 55, 20, 11, 0xFF527080, true);
        for (int i : PetakUmpetCatalog.assemblyDrawingOrder()) {
            PetakUmpetCatalog.Piece p = PetakUmpetCatalog.piece(i);
            solid(c, 32 + p.x * 12, 99 - (p.z + p.height) * 12,
                    p.width * 12, p.height * 12, 4, i, false);
        }
        text(c, "Susun di papan baru", 132, 33, 15, DakonArtwork.INK, true);
        text(c, "Dekatkan sisi agar menempel.", 132, 56, 12, 0xFF527080, false);
        text(c, "Tarik lagi untuk melepasnya.", 132, 76, 12, 0xFF527080, false);
        text(c, "Gunakan semua 6 bangun.", 132, 96, 12, 0xFF527080, false);
        box(c, 6, 116, 354, 474, 16, 0xFFF1FAEE);
        text(c, "AREA SUSUN", 20, 136, 12, 0xFF52773E, true);
        paint.setColor(0xFFD5E7CF); paint.setStrokeWidth(.8f);
        for (int col = 0; col <= 7; col++) c.drawLine(LEFT + col * UNIT, TOP,
                LEFT + col * UNIT, work.bottom, paint);
        for (int row = 0; row <= 7; row++) c.drawLine(LEFT, TOP + row * UNIT,
                work.right, TOP + row * UNIT, paint);
        for (int i : drawingOrder()) {
            if (!model.placed[i] || i == active && moved) continue;
            RectF r = face(i);
            solid(c, r.left, r.top, r.width(), r.height(), 7, i, model.selected == i);
        }
        box(c, tray.left, tray.top, tray.right, tray.bottom, 16, 0xFFFFF5E5);
        text(c, "BANGUN TEMUANMU", 20, 505, 12, 0xFF8A672F, true);
        for (int i = 0; i < PetakUmpetAssemblyModel.COUNT; i++) {
            RectF r = slot(i);
            box(c, r.left, r.top, r.right, r.bottom, 9,
                    model.selected == i ? 0xFFFFE2A5 : Color.WHITE);
            if (!model.placed[i] && !(i == active && moved)) {
                float u = Math.min(24, 38f / model.h[i]);
                solid(c, r.left + 10, r.centerY() - model.h[i] * u / 2,
                        model.w[i] * u, model.h[i] * u, 5, i, false);
            }
            text(c, model.placed[i] ? "di papan" : "#" + (i + 1),
                    r.right - 4, r.centerY() + 4, 10, 0xFF806B49, false, Paint.Align.RIGHT);
        }
        if (active >= 0 && moved) {
            float px = sticky ? Math.round(rawX) : rawX;
            float py = sticky ? Math.round(rawY) : rawY;
            c.save(); c.clipRect(0, 110, WIDTH, HEIGHT);
            solid(c, LEFT + px * UNIT, TOP + py * UNIT,
                    model.w[active] * UNIT, model.h[active] * UNIT, 7, active, true);
            c.restore();
            if (sticky) text(c, "Menempel · tarik untuk lepas", 354, 133, 10,
                    0xFF448D00, true, Paint.Align.RIGHT);
        }
    }
    private void box(Canvas c, float l, float t, float r, float b, float radius, int color) {
        paint.setStyle(Paint.Style.FILL); paint.setColor(color);
        c.drawRoundRect(l, t, r, b, radius, radius, paint);
    }
    private void text(Canvas c, String value, float x, float y, float size, int color, boolean bold) {
        text(c, value, x, y, size, color, bold, Paint.Align.LEFT);
    }
    private void text(Canvas c, String value, float x, float y, float size,
            int color, boolean bold, Paint.Align align) {
        paint.setStyle(Paint.Style.FILL); paint.setColor(color); paint.setTextSize(size);
        paint.setTypeface(bold ? headingFont : font); paint.setTextAlign(align);
        c.drawText(value, x, y, paint); paint.setTextAlign(Paint.Align.LEFT);
    }
    private void polygon(Canvas c, int color, float... points) {
        Path path = new Path(); path.moveTo(points[0], points[1]);
        for (int i = 2; i < points.length; i += 2) path.lineTo(points[i], points[i + 1]);
        path.close(); paint.setStyle(Paint.Style.FILL); paint.setColor(color); c.drawPath(path, paint);
    }
    private void solid(Canvas c, float x, float y, float width, float height,
            float depth, int item, boolean selected) {
        boolean balok = model.isBalok(item);
        float rise = depth * .75f;
        polygon(c, balok ? 0xFFC68B3F : 0xFF359CC7,
                x + width, y, x + width + depth, y - rise,
                x + width + depth, y + height - rise, x + width, y + height);
        polygon(c, balok ? 0xFFFFD98D : 0xFFACEDFF,
                x, y, x + depth, y - rise, x + width + depth, y - rise, x + width, y);
        box(c, x, y, x + width, y + height, 0, balok ? 0xFFEFBF68 : 0xFF75CFF0);
        paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(selected ? 2.5f : 1.2f);
        paint.setColor(selected ? 0xFF409300 : balok ? 0xFFAB7836 : 0xFF258BB4);
        c.drawRect(x, y, x + width, y + height, paint);
        paint.setStyle(Paint.Style.FILL);
    }
    int hit(float px, float py) {
        int[] order = drawingOrder();
        for (int pass = 0; pass < 2; pass++) for (int j = order.length - 1; j >= 0; j--) {
            int i = order[j];
            if (!model.placed[i]) continue;
            RectF r = face(i);
            if (pass == 1) r.inset(-6, -6);
            if (r.contains(px, py)) return i;
        }
        for (int i = 0; i < PetakUmpetAssemblyModel.COUNT; i++)
            if (!model.placed[i] && slot(i).contains(px, py)) return i;
        return -1;
    }
}
