package com.n1neto.material;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.text.format.DateFormat;

/**
 * Lightweight settings store for the launcher.
 * Uses plain SharedPreferences (no AppCompat/Material dependency) so it stays
 * cheap on older ATV boxes with limited RAM.
 */
public final class Prefs {

    public static final String FILE = "n1neto_prefs";

    public static final String KEY_THEME_MODE   = "theme_mode";    // 0 dynamic, 1 classic
    public static final String KEY_ACCENT_SEED  = "accent_seed";    // int color seed
    public static final String KEY_DARK         = "dark_mode";      // bool
    public static final String KEY_WALLPAPER    = "wallpaper";      // 0 none .. 3
    public static final String KEY_SHOW_RECENT  = "show_recent";    // bool
    public static final String KEY_ANIMATIONS   = "animations";     // bool
    public static final String KEY_SORT_ORDER   = "sort_order";     // 0 alpha, 1 most used
    public static final String KEY_USAGE_COUNTS = "usage_counts";   // pkg:count,pkg:count
    public static final String KEY_DIRTY        = "ui_dirty";       // home must reload
    public static final String KEY_RECENT_JSON  = "recent_apps";    // csv of package names

    public static final int THEME_DYNAMIC = 0;
    public static final int THEME_CLASSIC = 1;

    public static final int WALL_NONE = 0;
    public static final int WALL_AURORA = 1;
    public static final int WALL_MESH = 2;
    public static final int WALL_MINIMAL = 3;

    private Prefs() {}

    public static SharedPreferences get(Context c) {
        return c.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public static int themeMode(Context c) {
        boolean supportsDynamic = Build.VERSION.SDK_INT >= 31;
        int def = supportsDynamic ? THEME_DYNAMIC : THEME_CLASSIC;
        return get(c).getInt(KEY_THEME_MODE, def);
    }

    public static boolean darkMode(Context c) {
        // Dark first: TVs are almost always in dim rooms.
        return get(c).getBoolean(KEY_DARK, true);
    }

    public static int accentSeed(Context c) {
        return get(c).getInt(KEY_ACCENT_SEED, 0xFF7F62FF);
    }

    /** Store the resolved accent so focus rings in XML drawables match the live theme. */
    public static void setResolvedAccent(Context c, int color) {
        get(c).edit().putInt(KEY_ACCENT_SEED, color).apply();
    }

    public static int wallpaper(Context c) {
        return get(c).getInt(KEY_WALLPAPER, WALL_AURORA);
    }

    public static boolean showRecent(Context c) {
        return get(c).getBoolean(KEY_SHOW_RECENT, true);
    }

    public static boolean animations(Context c) {
        return get(c).getBoolean(KEY_ANIMATIONS, true);
    }

    public static int sortOrder(Context c) {
        return get(c).getInt(KEY_SORT_ORDER, 0); // 0 alpha, 1 most used
    }

    // ---- setters used by the settings screen -------------------------------

    public static void setThemeMode(Context c, int mode) {
        get(c).edit().putInt(KEY_THEME_MODE, mode).apply();
    }

    public static void setDark(Context c, boolean dark) {
        get(c).edit().putBoolean(KEY_DARK, dark).apply();
    }

    public static void setAccentSeed(Context c, int seed) {
        get(c).edit().putInt(KEY_ACCENT_SEED, seed).apply();
    }

    public static void setWallpaper(Context c, int wallpaper) {
        get(c).edit().putInt(KEY_WALLPAPER, wallpaper).apply();
    }

    public static void setShowRecent(Context c, boolean show) {
        get(c).edit().putBoolean(KEY_SHOW_RECENT, show).apply();
    }

    public static void setAnimations(Context c, boolean on) {
        get(c).edit().putBoolean(KEY_ANIMATIONS, on).apply();
    }

    public static void setSortOrder(Context c, int order) {
        get(c).edit().putInt(KEY_SORT_ORDER, order).apply();
    }

    /** Settings marks the home UI dirty; HomeActivity consumes it on resume. */
    public static void markDirty(Context c) {
        get(c).edit().putBoolean(KEY_DIRTY, true).apply();
    }

    public static boolean consumeDirty(Context c) {
        boolean dirty = get(c).getBoolean(KEY_DIRTY, false);
        if (dirty) get(c).edit().remove(KEY_DIRTY).apply();
        return dirty;
    }

    // ---- usage statistics ("most used" sorting) ----------------------------

    public static void bumpUsage(Context c, String pkg) {
        java.util.Map<String, Integer> counts = usageCounts(c);
        Integer old = counts.get(pkg);
        counts.put(pkg, old == null ? 1 : old + 1);
        StringBuilder sb = new StringBuilder();
        for (java.util.Map.Entry<String, Integer> e : counts.entrySet()) {
            if (sb.length() > 0) sb.append(',');
            sb.append(e.getKey()).append(':').append(e.getValue());
        }
        get(c).edit().putString(KEY_USAGE_COUNTS, sb.toString()).apply();
    }

    private static java.util.Map<String, Integer> usageCounts(Context c) {
        java.util.Map<String, Integer> map = new java.util.HashMap<>();
        String raw = get(c).getString(KEY_USAGE_COUNTS, "");
        if (raw != null && !raw.isEmpty()) {
            for (String pair : raw.split(",")) {
                int idx = pair.lastIndexOf(':');
                if (idx <= 0) continue;
                try {
                    map.put(pair.substring(0, idx), Integer.parseInt(pair.substring(idx + 1)));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return map;
    }

    /** Packages ordered by launch count, most-used first. */
    public static java.util.List<String> usageOrder(Context c) {
        java.util.Map<String, Integer> counts = usageCounts(c);
        java.util.List<String> keys = new java.util.ArrayList<>(counts.keySet());
        java.util.Collections.sort(keys, new java.util.Comparator<String>() {
            @Override public int compare(String a, String b) {
                return counts.get(b) - counts.get(a);
            }
        });
        return keys;
    }

    /** Ordered list (most recent first) of recently launched package names. */
    public static java.util.List<String> recentPackages(Context c) {
        String raw = get(c).getString(KEY_RECENT_JSON, "");
        java.util.List<String> out = new java.util.ArrayList<>();
        if (raw != null && !raw.isEmpty()) {
            for (String s : raw.split(",")) {
                if (!s.isEmpty()) out.add(s);
            }
        }
        return out;
    }

    public static void pushRecentPackage(Context c, String pkg) {
        java.util.List<String> recents = recentPackages(c);
        recents.remove(pkg);
        recents.add(0, pkg);
        while (recents.size() > 12) recents.remove(recents.size() - 1);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < recents.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append(recents.get(i));
        }
        get(c).edit().putString(KEY_RECENT_JSON, sb.toString()).apply();
    }

    public static String clockPattern() {
        return DateFormat.is24HourFormat(N1App.get()) ? "HH:mm" : "h:mm";
    }
}
