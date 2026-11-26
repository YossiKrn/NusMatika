package id.nusmatika.dakon;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Draws the approved transparent Nara v3 motion directly on an Android Canvas. */
public final class NaraRenderer {
    private static final String TAG = "NaraRenderer";
    private static final long DURATION_MILLIS = 20_000L;
    private static final int FRAMES_PER_SECOND = 12;
    private static final int FRAME_WIDTH = 160;
    private static final int FRAME_HEIGHT = 180;
    private static final int COLUMNS = 6;
    private static final int ROWS = 4;
    private static final int FRAMES_PER_SHEET = COLUMNS * ROWS;
    private static final int SHEET_COUNT = 10;
    private static final int CACHE_LIMIT = 2;

    private final AssetManager assets;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Rect source = new Rect();
    private final RectF destination = new RectF();
    private final LinkedHashMap<Integer, Bitmap> cache = new LinkedHashMap<>(3, 0.75f, true);
    private final boolean[] unavailable = new boolean[SHEET_COUNT];
    private boolean released;

    public NaraRenderer(Context context) {
        assets = context.getApplicationContext().getAssets();
    }

    /**
     * Fits the full transparent 320:360 artwork inside bounds, keeping it centered.
     * elapsedMillis is time since animation start. Reduced motion always draws frame 0.
     * Calls are serialized with release so a recycled sheet cannot be drawn.
     */
    public synchronized void draw(Canvas canvas, RectF bounds, long elapsedMillis,
                                  boolean reducedMotion) {
        if (released || canvas == null || bounds == null
                || !(bounds.width() > 0f) || !(bounds.height() > 0f)) {
            return;
        }
        long loopMillis = Math.max(0L, elapsedMillis) % DURATION_MILLIS;
        int frame = reducedMotion ? 0 : (int) (loopMillis * FRAMES_PER_SECOND / 1000L);
        int sheet = frame / FRAMES_PER_SHEET;
        Bitmap bitmap = getSheet(sheet);
        int localFrame = frame % FRAMES_PER_SHEET;

        // Corrupt or missing artwork never interrupts the activity. Use the first
        // available sheet's first frame; failed asset names are only attempted once.
        if (bitmap == null) {
            localFrame = 0;
            for (int fallback = 0; fallback < SHEET_COUNT; fallback++) {
                bitmap = getSheet(fallback);
                if (bitmap != null) {
                    break;
                }
            }
        }
        if (bitmap == null) {
            return;
        }

        int left = (localFrame % COLUMNS) * FRAME_WIDTH;
        int top = (localFrame / COLUMNS) * FRAME_HEIGHT;
        source.set(left, top, left + FRAME_WIDTH, top + FRAME_HEIGHT);
        float scale = Math.min(bounds.width() / FRAME_WIDTH, bounds.height() / FRAME_HEIGHT);
        float width = FRAME_WIDTH * scale;
        float height = FRAME_HEIGHT * scale;
        float destinationLeft = bounds.left + (bounds.width() - width) * 0.5f;
        float destinationTop = bounds.top + (bounds.height() - height) * 0.5f;
        destination.set(destinationLeft, destinationTop,
                destinationLeft + width, destinationTop + height);
        canvas.drawBitmap(bitmap, source, destination, paint);
    }

    private Bitmap getSheet(int index) {
        if (unavailable[index]) {
            return null;
        }
        Bitmap existing = cache.get(index);
        if (existing != null) {
            return existing;
        }

        // Evict before decoding, so even the temporary peak is two RGBA sheets
        // (5,529,600 bytes), rather than briefly retaining a third one.
        if (cache.size() >= CACHE_LIMIT) {
            Iterator<Map.Entry<Integer, Bitmap>> iterator = cache.entrySet().iterator();
            Map.Entry<Integer, Bitmap> eldest = iterator.next();
            Bitmap discarded = eldest.getValue();
            iterator.remove();
            discarded.recycle();
        }

        String file = String.format(Locale.ROOT, "nara/nara-%02d.webp", index);
        try (InputStream stream = assets.open(file)) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            options.inScaled = false;
            Bitmap decoded = BitmapFactory.decodeStream(stream, null, options);
            if (decoded == null) {
                unavailable[index] = true;
                Log.w(TAG, "Unable to decode " + file);
                return null;
            }
            if (decoded.getWidth() != FRAME_WIDTH * COLUMNS
                    || decoded.getHeight() != FRAME_HEIGHT * ROWS) {
                decoded.recycle();
                unavailable[index] = true;
                Log.w(TAG, "Unexpected sprite atlas dimensions: " + file);
                return null;
            }
            cache.put(index, decoded);
            return decoded;
        } catch (IOException | RuntimeException exception) {
            unavailable[index] = true;
            Log.w(TAG, "Unable to load " + file, exception);
            return null;
        }
    }

    /** Permanently releases this renderer; create a new instance to resume drawing. */
    public synchronized void release() {
        if (released) {
            return;
        }
        released = true;
        for (Bitmap bitmap : cache.values()) {
            bitmap.recycle();
        }
        cache.clear();
    }
}
