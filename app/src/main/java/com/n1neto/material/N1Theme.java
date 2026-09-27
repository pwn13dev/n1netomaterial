package com.n1neto.material;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;

/**
 * Resolves the live Material You palette for widgets that need it in code
 * (focus rings, wallpaper tinting, selection states). On Android 12+ we read
 * the system dynamic colors; on older boxes we generate a tonal palette from
 * the user's accent seed with the built-in TonalPalette engine.
 */
public final class N1Theme {

    public int primary;
    public int onPrimary;
    public int surface;
    public int surfaceContainer;
    public int surfaceHigh;
    public int onSurface;
    public int onSurfaceDim;
    public int background;
    public int outline;

    private static N1Theme sInstance;

    public static N1Theme current(Context c) {
        if (sInstance == null) sInstance = new N1Theme(c.getApplicationContext());
        return sInstance;
    }

    public static void invalidate() {
        sInstance = null;
    }

    private N1Theme(Context c) {
        boolean dark = Prefs.darkMode(c);
        int mode = Prefs.themeMode(c);

        if (mode == Prefs.THEME_DYNAMIC && Build.VERSION.SDK_INT >= 31) {
            // Material You: pull the system dynamic tonal palette.
            try {
                primary = sysColor(c, "system_accent1_300", 0xFF9A82FF);
                onPrimary = sysColor(c, "system_accent1_100", Color.WHITE);
                surface = sysColor(c, dark ? "system_neutral1_900" : "system_neutral1_10",
                        dark ? 0xFF121316 : 0xFFFFFBFE);
                surfaceContainer = sysColor(c, dark ? "system_neutral1_800" : "system_neutral1_20",
                        dark ? 0xFF1E1F24 : 0xFFF0EFF4);
                surfaceHigh = sysColor(c, dark ? "system_neutral1_700" : "system_neutral1_30",
                        dark ? 0xFF272830 : 0xFFE7E0EC);
                background = sysColor(c, dark ? "system_neutral1_1000" : "system_neutral1_0",
                        dark ? 0xFF0D0E11 : 0xFFFFFBFE);
                onSurface = sysColor(c, dark ? "system_neutral1_100" : "system_neutral1_900",
                        dark ? 0xFFE6E1E5 : 0xFF1C1B1F);
                onSurfaceDim = sysColor(c, dark ? "system_neutral2_400" : "system_neutral2_500",
                        dark ? 0xFF9E9EA7 : 0xFF49454F);
                outline = sysColor(c, dark ? "system_neutral2_500" : "system_neutral2_600",
                        dark ? 0xFF49474E : 0xFF79747E);
                return;
            } catch (Throwable ignored) {
                // fall through to generated palette
            }
        }

        // Classic / pre-S generation from the accent seed.
        int seed = Prefs.accentSeed(c);
        TonalPalette p = TonalPalette.fromColor(seed);
        float hue = p.hue;
        if (dark) {
            primary = TonalPalette.argbFromHct(hue, Math.min(p.chroma, 70f), 70f);
            onPrimary = TonalPalette.argbFromHct(hue, 30f, 20f);
            surface = TonalPalette.argbFromHct(hue, 6f, 12f);
            surfaceContainer = TonalPalette.argbFromHct(hue, 8f, 16f);
            surfaceHigh = TonalPalette.argbFromHct(hue, 10f, 22f);
            background = TonalPalette.argbFromHct(hue, 6f, 8f);
            onSurface = TonalPalette.argbFromHct(hue, 4f, 92f);
            onSurfaceDim = TonalPalette.argbFromHct(hue, 4f, 72f);
            outline = TonalPalette.argbFromHct(hue, 6f, 45f);
        } else {
            primary = TonalPalette.argbFromHct(hue, Math.min(p.chroma, 80f), 50f);
            onPrimary = Color.WHITE;
            surface = TonalPalette.argbFromHct(hue, 4f, 98f);
            surfaceContainer = TonalPalette.argbFromHct(hue, 6f, 94f);
            surfaceHigh = TonalPalette.argbFromHct(hue, 8f, 90f);
            background = TonalPalette.argbFromHct(hue, 4f, 98f);
            onSurface = TonalPalette.argbFromHct(hue, 8f, 18f);
            onSurfaceDim = TonalPalette.argbFromHct(hue, 6f, 40f);
            outline = TonalPalette.argbFromHct(hue, 6f, 60f);
        }
    }

    /** Look up @android:color/system_* dynamic resources on Android 12+. */
    private static int sysColor(Context c, String name, int fallback) {
        int id = c.getResources().getIdentifier("android:color/" + name, null, null);
        if (id == 0) return fallback;
        try {
            return c.getColor(id);
        } catch (Throwable t) {
            return fallback;
        }
    }

    public boolean isDark(Configuration cfg) {
        return (cfg.uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
    }

    /** Rounded rect drawable tinted with the live palette (used for wallpapers/headers). */
    public Drawable roundedOverlay(float radiusDp, int color) {
        GradientDrawable d = new GradientDrawable();
        d.setCornerRadius(radiusDp * N1App.get().getResources().getDisplayMetrics().density);
        d.setColor(color);
        return d;
    }

    public int withAlpha(int color, float alpha) {
        int a = Math.round(255 * Math.max(0f, Math.min(1f, alpha)));
        return (color & 0x00FFFFFF) | (a << 24);
    }
}
