package com.n1neto.material;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.net.Uri;
import android.os.Handler;
import android.text.format.DateFormat;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * n1netomaterial home screen.
 * Plain Activity (no AppCompat/Material runtime deps) to stay light on old ATV boxes:
 * clock header + rows of Material app tiles, D-pad first, dynamic theme in code.
 */
public class HomeActivity extends Activity implements RowAdapter.RowFocusListener,
        RowAdapter.OnTileActionListener {

    private LinearLayout rowsContainer;
    private TextView clockView, dateView;
    private ImageView wallpaperView, settingsButton;
    private final ArrayList<HomeRow> rows = new ArrayList<>();
    private final Handler clockHandler = new Handler();

    private List<AppInfo> allApps = new ArrayList<>();
    private boolean pendingReload = false;
    private boolean reloadNeeded = true;

    private final Runnable clockTick = new Runnable() {
        @Override public void run() {
            updateClock();
            clockHandler.postDelayed(this, 30_000);
        }
    };

    private final BroadcastReceiver packageChanged = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            // Debounce: rebuild once the current frame settles.
            if (pendingReload) return;
            pendingReload = true;
            clockHandler.postDelayed(new Runnable() {
                @Override public void run() {
                    pendingReload = false;
                    loadRows();
                }
            }, 500);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        rowsContainer = findViewById(R.id.rows_container);
        clockView = findViewById(R.id.clock);
        dateView = findViewById(R.id.date);
        wallpaperView = findViewById(R.id.wallpaper);
        settingsButton = findViewById(R.id.settings_button);

        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { openSettings(); }
        });
        settingsButton.setOnKeyListener(new View.OnKeyListener() {
            @Override public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                    if (!rows.isEmpty()) return rows.get(0).requestTileFocus();
                }
                return false;
            }
        });

        applyTheme();
        loadRows();
        updateClock();
    }

    @Override
    protected void onResume() {
        super.onResume();
        N1Theme.invalidate();
        applyTheme();
        if (reloadNeeded || Prefs.consumeDirty(this)) loadRows();
        updateClock();
        clockHandler.postDelayed(clockTick, 30_000);
        IntentFilter f = new IntentFilter();
        f.addAction(Intent.ACTION_PACKAGE_ADDED);
        f.addAction(Intent.ACTION_PACKAGE_REMOVED);
        f.addDataScheme("package");
        try { registerReceiver(packageChanged, f); } catch (Throwable ignored) {}
    }

    @Override
    protected void onPause() {
        super.onPause();
        clockHandler.removeCallbacks(clockTick);
        try { unregisterReceiver(packageChanged); } catch (Throwable ignored) {}
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // Coming back from an app: recents changed, so re-sort rows.
        reloadNeeded = true;
    }

    // ------------------------------------------------------------------ theme

    private void applyTheme() {
        N1Theme t = N1Theme.current(this);
        getWindow().setBackgroundDrawable(null);
        findViewById(R.id.root).setBackgroundColor(t.background);
        clockView.setTextColor(t.onSurface);
        dateView.setTextColor(t.onSurfaceDim);
        ((TextView) findViewById(R.id.hint)).setTextColor(t.onSurfaceDim);
        settingsButton.clearColorFilter();
        settingsButton.setColorFilter(t.onSurface);
        Prefs.setResolvedAccent(this, t.primary);

        int wp = Prefs.wallpaper(this);
        switch (wp) {
            case Prefs.WALL_AURORA:   wallpaperView.setImageResource(R.drawable.wallpaper_aurora); break;
            case Prefs.WALL_MESH:     wallpaperView.setImageResource(R.drawable.wallpaper_mesh); break;
            case Prefs.WALL_MINIMAL:  wallpaperView.setImageResource(R.drawable.wallpaper_minimal); break;
            default:                  wallpaperView.setImageDrawable(null); break;
        }
        for (HomeRow r : rows) r.applyTheme(t);
    }

    // -------------------------------------------------------------------- rows

    private void loadRows() {
        reloadNeeded = false;
        Prefs.consumeDirty(this);
        allApps = AppRepository.loadLaunchables(this);
        rowsContainer.removeAllViews();
        rows.clear();

        if (Prefs.showRecent(this)) {
            List<AppInfo> recents = recentSlice(allApps);
            if (!recents.isEmpty()) addRow(getString(R.string.row_recent), recents);
        }
        addRow(getString(R.string.row_all_apps), allApps);

        if (rows.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText(R.string.no_apps);
            empty.setTextColor(N1Theme.current(this).onSurfaceDim);
            empty.setTextSize(16);
            empty.setPadding(dp(8), dp(24), dp(8), 0);
            rowsContainer.addView(empty);
            return;
        }
        applyTheme();
        // Give the first row initial focus so the remote feels natural right away.
        rows.get(0).post(new Runnable() {
            @Override public void run() { rows.get(0).requestTileFocus(); }
        });
    }

    private List<AppInfo> recentSlice(List<AppInfo> pool) {
        List<String> recents = Prefs.recentPackages(this);
        ArrayList<AppInfo> out = new ArrayList<>();
        for (String pkg : recents) {
            for (AppInfo a : pool) {
                if (a.packageName.equals(pkg)) { out.add(a); break; }
            }
            if (out.size() >= 10) break;
        }
        return out;
    }

    private void addRow(String title, List<AppInfo> apps) {
        final HomeRow row = new HomeRow(this, title, apps);
        row.adapter.setRowFocusListener(this);
        row.adapter.setOnTileActionListener(this);
        rowsContainer.addView(row, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        rows.add(row);
    }

    // -------------------------------------------------------------- d-pad rows

    @Override
    public void onRowEdge(int direction) {
        View focused = getCurrentFocus();
        int currentRow = -1;
        for (int i = 0; i < rows.size(); i++) {
            if (rows.get(i).focusedChild() != null) { currentRow = i; break; }
        }
        int x = focused != null ? centerOf(focused) : getWidth() / 2;
        int target = currentRow + direction;
        if (target < 0) {
            settingsButton.requestFocus();
            return;
        }
        if (currentRow < 0 || target >= rows.size()) return;
        rows.get(target).requestTileFocusNear(x);
    }

    private int centerOf(View v) {
        int[] loc = new int[2];
        v.getLocationInWindow(loc);
        return loc[0] + v.getWidth() / 2;
    }

    // ------------------------------------------------------------- tile actions

    @Override
    public void onTileLongClick(View anchor, AppInfo app) {
        PopupMenu menu;
        try {
            menu = new PopupMenu(this, anchor);
        } catch (Throwable t) {
            openAppInfo(app);
            return;
        }
        menu.getMenu().add(0, 1, 0, R.string.action_open_info);
        menu.getMenu().add(0, 2, 1, R.string.action_uninstall);
        menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override public boolean onMenuItemClick(android.view.MenuItem item) {
                if (item.getItemId() == 2) {
                    try {
                        startActivity(new Intent(Intent.ACTION_DELETE,
                                Uri.parse("package:" + app.packageName)));
                    } catch (Throwable ignored) {
                        toast(R.string.action_unavailable);
                    }
                } else {
                    openAppInfo(app);
                }
                return true;
            }
        });
        menu.show();
    }

    private void openAppInfo(AppInfo app) {
        try {
            startActivity(new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + app.packageName)));
        } catch (Throwable ignored) {
            toast(R.string.action_unavailable);
        }
    }

    // ------------------------------------------------------------------- clock

    private void updateClock() {
        Date now = new Date();
        clockView.setText(DateFormat.getDateFormat(this).format(now));
        dateView.setText(java.text.DateFormat.getDateInstance(java.text.DateFormat.LONG_FORMAT)
                .format(now));
    }

    // ---------------------------------------------------------------- shortcuts

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            openSettings();
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN && getCurrentFocus() == settingsButton) {
            if (!rows.isEmpty()) rows.get(0).requestTileFocus();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void openSettings() {
        startActivity(new Intent(this, SettingsActivity.class));
    }

    private void toast(int res) {
        Toast.makeText(this, res, Toast.LENGTH_SHORT).show();
    }

    private int dp(float v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
