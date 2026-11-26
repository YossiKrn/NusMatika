package id.nusmatika.dakon;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Gaya, font dan area aman yang dipakai kedua halaman Engklek. */
public final class EngklekUi {
    public static final int INK = 0xFF26364A, MUTED = 0xFF667085;
    public static final int GREEN = 0xFF58A700, BLUE = 0xFF4285D4;
    public static final int[] FACE_COLORS = {
        0xFF85C8F3, 0xFFFFCD70, 0xFF97D8A5, 0xFFF3A5B8, 0xFFBFA7EF, 0xFF80D4CF
    };

    private EngklekUi() { }
    public static int dp(Context context, float value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
    public static Typeface font(Context context) {
        try {
            return new Typeface.Builder(context.getAssets(), "fonts/Nunito.ttf")
                    .setFontVariationSettings("'wght' 650").build();
        }
        catch (RuntimeException error) { return Typeface.create("sans-serif", Typeface.NORMAL); }
    }
    public static GradientDrawable background(int fill, int border, Context context) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(fill);
        shape.setCornerRadius(dp(context, 16));
        shape.setStroke(dp(context, 1), border);
        return shape;
    }
    public static TextView text(Context context, String text, int size, boolean bold) {
        TextView label = new TextView(context);
        label.setText(text);
        label.setTextSize(size);
        label.setTextColor(INK);
        label.setTypeface(font(context), bold ? Typeface.BOLD : Typeface.NORMAL);
        label.setLineSpacing(dp(context, 2), 1f);
        return label;
    }
    public static Button button(Context context, String text, Runnable action) {
        Button button = new Button(context);
        button.setText(text);
        button.setTextSize(15);
        button.setAllCaps(false);
        button.setTypeface(font(context), Typeface.BOLD);
        button.setTextColor(INK);
        button.setMinHeight(dp(context, 50));
        button.setPadding(dp(context, 8), dp(context, 6), dp(context, 8), dp(context, 6));
        button.setBackground(background(0xFFF1F7E9, 0xFFD2E3BF, context));
        button.setOnClickListener(view -> action.run());
        return button;
    }
    public static LinearLayout column(Context context) {
        LinearLayout column = new LinearLayout(context);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setPadding(dp(context, 18), dp(context, 10), dp(context, 18), dp(context, 24));
        column.setBackgroundColor(Color.WHITE);
        return column;
    }
    public static void add(LinearLayout parent, View child, int top) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(parent.getContext(), top);
        parent.addView(child, params);
    }
    public static void install(Activity activity, LinearLayout column) {
        ScrollView scroll = new ScrollView(activity);
        scroll.setFillViewport(true);
        scroll.addView(column, new ScrollView.LayoutParams(-1, -2));
        FrameLayout safe = new FrameLayout(activity);
        safe.setBackgroundColor(Color.WHITE);
        safe.addView(scroll);
        if (Build.VERSION.SDK_INT >= 30) activity.getWindow().setDecorFitsSystemWindows(false);
        activity.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        safe.setOnApplyWindowInsetsListener((view, insets) -> {
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets bars = insets.getInsets(
                        WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            } else view.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                    insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            return insets;
        });
        activity.setContentView(safe);
        safe.requestApplyInsets();
    }
}
