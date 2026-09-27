package com.n1neto.material;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Checkable;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

/**
 * Material You settings screen, built programmatically with lightweight widgets
 * (no PreferenceManager / AppCompat) so it runs on API 17 boxes.
 * Cards: Theme · Accent · Wallpaper · Home layout · About device.
 */
public class SettingsActivity extends Activity {

    private static final int[] SEEDS = {
            0xFF7F62FF, 0xFF3D7AFF, 0xFF00BFA5, 0xFF4CAF50,
            0xFFFFB300, 0xFFFF6E5A, 0xFFE860A8,
    };
    private static final int[] WALL_RES = { 0, R.drawable.wallpaper_aurora,
            R.drawable.wallpaper_mesh, R.drawable.wallpaper_minimal };
    private static final int[] WALL_NAMES = { R.string.wall_none, R.string.wall_aurora,
            R.string.wall_mesh, R.string.wall_minimal };

    private N1Theme theme;
    private LinearLayout content;
    private final List<View> focusables = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        theme = N1Theme.current(this);
        buildUi();
    }

    // ------------------------------------------------------------------- UI

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(theme.background);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(36);
        content.setPadding(pad, dp(24), pad, dp(24));
        scroll.addView(content);
        setContentView(scroll);
        focusables.clear();

        TextView header = new TextView(this);
        header.setText(R.string.settings_title);
        header.setTextSize(TypedValue.COMPLEX_UNIT_SP, 26);
        header.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        header.setTextColor(theme.onSurface);
        header.setPadding(dp(4), 0, 0, dp(16));
        content.addView(header);

        addCard(getString(R.string.set_theme_title), themeRows());
        addCard(getString(R.string.set_accent_title), accentRow());
        addCard(getString(R.string.set_wallpaper), wallpaperRow());
        addCard(getString(R.string.set_home_title), homeLayoutRows());
        addCard(getString(R.string.set_about), aboutRows());

        if (!focusables.isEmpty()) focusables.get(0).requestFocus();
    }

    private void addCard(String title, List<View> rows) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(rounded(theme.surfaceContainer, dp(20)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(16);
        content.addView(card, lp);

        TextView t = new TextView(this);
        t.setText(title);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        t.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        t.setTextColor(theme.primary);
        t.setLetterSpacing(0.06f);
        t.setPadding(dp(20), dp(16), dp(20), dp(6));
        card.addView(t);

        for (int i = 0; i < rows.size(); i++) {
            View v = rows.get(i);
            LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rlp.leftMargin = dp(12);
            rlp.rightMargin = dp(12);
            rlp.bottomMargin = i == rows.size() - 1 ? dp(10) : dp(2);
            card.addView(v, rlp);
        }
    }

    /** Generic selectable pill row used across cards. */
    private View optionRow(String label, boolean selected, Runnable onTap) {
        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        tv.setFocusable(true);
        tv.setGravity(Gravity.CENTER_VERTICAL);
        tv.setPadding(dp(16), dp(12), dp(16), dp(12));
        tv.setMinHeight(dp(48));
        styleOption(tv, selected);
        tv.setOnClickListener(new Clicker(onTap));
        tv.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override public void onFocusChange(View v, boolean hasFocus) {
                styleOption((TextView) v, hasFocus);
            }
        });
        focusables.add(tv);
        return tv;
    }

    private void styleOption(TextView tv, boolean selected) {
        tv.setBackground(rounded(selected ? theme.withAlpha(theme.primary, 0.18f)
                : Color.TRANSPARENT, dp(14)));
        tv.setTextColor(selected ? theme.primary : theme.onSurface);
        tv.setTypeface(Typeface.create("sans-serif-medium",
                selected ? Typeface.BOLD : Typeface.NORMAL));
    }

    private List<View> themeRows() {
        List<View> out = new ArrayList<>();
        boolean dynamic = Build.VERSION.SDK_INT >= 31;
        if (dynamic) {
            out.add(optionRow(getString(R.string.theme_dynamic),
                    Prefs.themeMode(this) == Prefs.THEME_DYNAMIC, new Runnable() {
                        @Override public void run() {
                            Prefs.setThemeMode(SettingsActivity.this, Prefs.THEME_DYNAMIC);
                            rebuild();
                        }
                    }));
        }
        out.add(optionRow(getString(R.string.theme_classic),
                Prefs.themeMode(this) == Prefs.THEME_CLASSIC || !dynamic, new Runnable() {
                    @Override public void run() {
                        Prefs.setThemeMode(SettingsActivity.this, Prefs.THEME_CLASSIC);
                        rebuild();
                    }
                }));
        out.add(optionRow(getString(Prefs.darkMode(this) ? R.string.set_dark_on : R.string.set_dark_off),
                Prefs.darkMode(this), new Runnable() {
                    @Override public void run() {
                        Prefs.setDark(SettingsActivity.this, !Prefs.darkMode(SettingsActivity.this));
                        rebuild();
                    }
                }));
        return out;
    }

    private List<View> accentRow() {
        LinearLayout strip = new LinearLayout(this);
        strip.setOrientation(LinearLayout.HORIZONTAL);
        strip.setGravity(Gravity.CENTER_VERTICAL);
        int currentSeed = Prefs.accentSeed(this);
        for (int i = 0; i < SEEDS.length; i++) {
            final int seed = SEEDS[i];
            View dot = new View(this);
            boolean selected = sameColor(seed, currentSeed);
            dot.setBackground(swatch(seed, selected));
            int size = dp(44);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
            lp.setMargins(dp(6), dp(6), dp(6), dp(6));
            dot.setLayoutParams(lp);
            dot.setFocusable(true);
            dot.setOnClickListener(new Clicker(new Runnable() {
                @Override public void run() {
                    Prefs.setAccentSeed(SettingsActivity.this, seed);
                    rebuild();
                }
            }));
            dot.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override public void onFocusChange(View v, boolean hasFocus) {
                    v.setBackground(swatch(seed, hasFocus
                            || sameColor(seed, Prefs.accentSeed(SettingsActivity.this))));
                }
            });
            focusables.add(dot);
            strip.addView(dot);
        }
        List<View> out = new ArrayList<>();
        out.add(strip);
        return out;
    }

    private GradientDrawable swatch(int color, boolean selected) {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.OVAL);
        d.setColor(color);
        d.setStroke(dp(selected ? 4 : 1),
                selected ? theme.onSurface : theme.withAlpha(theme.onSurface, 0.25f));
        return d;
    }

    private List<View> wallpaperRow() {
        List<View> out = new ArrayList<>();
        int cur = Prefs.wallpaper(this);
        for (int w = 0; w <= 3; w++) {
            final int wall = w;
            out.add(optionRow(getString(WALL_NAMES[w]), cur == wall, new Runnable() {
                @Override public void run() {
                    Prefs.setWallpaper(SettingsActivity.this, wall);
                    rebuild();
                }
            }));
        }
        return out;
    }

    private List<View> homeLayoutRows() {
        List<View> out = new ArrayList<>();
        out.add(optionRow(getString(R.string.sort_alpha),
                Prefs.sortOrder(this) == AppRepository.SORT_ALPHABETICAL, new Runnable() {
                    @Override public void run() {
                        Prefs.setSortOrder(SettingsActivity.this, AppRepository.SORT_ALPHABETICAL);
                        Prefs.markDirty(SettingsActivity.this);
                        finish();
                    }
                }));
        out.add(optionRow(getString(R.string.sort_most_used),
                Prefs.sortOrder(this) == AppRepository.SORT_MOST_USED, new Runnable() {
                    @Override public void run() {
                        Prefs.setSortOrder(SettingsActivity.this, AppRepository.SORT_MOST_USED);
                        Prefs.markDirty(SettingsActivity.this);
                        finish();
                    }
                }));
        out.add(optionRow(getString(R.string.set_row_recent), Prefs.showRecent(this),
                new Runnable() {
                    @Override public void run() {
                        Prefs.setShowRecent(SettingsActivity.this, !Prefs.showRecent(SettingsActivity.this));
                        rebuild();
                    }
                }));
        out.add(optionRow(getString(R.string.set_animations), Prefs.animations(this),
                new Runnable() {
                    @Override public void run() {
                        Prefs.setAnimations(SettingsActivity.this, !Prefs.animations(SettingsActivity.this));
                        rebuild();
                    }
                }));
        return out;
    }

    private List<View> aboutRows() {
        List<View> out = new ArrayList<>();
        Runtime rt = Runtime.getRuntime();
        long totalMb = rt.maxMemory() / (1024 * 1024);
        String device = Build.MANUFACTURER + " " + Build.MODEL;
        out.add(infoLine(getString(R.string.info_device), device));
        out.add(infoLine(getString(R.string.info_android_version),
                "Android " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")"));
        out.add(infoLine(getString(R.string.info_memory), totalMb + " MB heap"));
        out.add(infoLine(getString(R.string.info_launcher_version),
                getString(R.string.app_name) + " " + getString(R.string.launcher_version)));
        return out;
    }

    private View infoLine(String key, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(dp(16), dp(8), dp(16), dp(8));
        TextView k = new TextView(this);
        k.setText(key);
        k.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        k.setTextColor(theme.onSurfaceDim);
        row.addView(k, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        v.setTextColor(theme.onSurface);
        row.addView(v, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return row;
    }

    // ------------------------------------------------------------- plumbing

    private void rebuild() {
        Prefs.markDirty(this);
        N1Theme.invalidate();
        theme = N1Theme.current(this);
        buildUi();
    }

    /** Click-through for D-pad: DPAD_CENTER/ENTER act like a click on focused views. */
    private static final class Clicker implements View.OnClickListener {
        private final Runnable action;
        Clicker(Runnable action) { this.action = action; }
        @Override public void onClick(View v) { action.run(); }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN
                && (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_CENTER
                    || event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
            View f = getCurrentFocus();
            if (f != null) {
                return f.performClick();
            }
        }
        return super.dispatchKeyEvent(event);
    }

    private GradientDrawable rounded(int color, float radiusPx) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radiusPx);
        return d;
    }

    private static boolean sameColor(int a, int b) {
        return (a & 0xFFFFFF) == (b & 0xFFFFFF);
    }

    private int dp(float v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
