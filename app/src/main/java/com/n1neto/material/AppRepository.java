package com.n1neto.material;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Enumerates launchable apps with D-pad friendly sorting.
 * Uses only Intent queries that work back to API 17 (no PackageInfo shortcuts
 * flag tricks), so it behaves the same on ancient ATV boxes and new ones.
 */
public final class AppRepository {

    public static final int SORT_ALPHABETICAL = 0;
    public static final int SORT_MOST_USED = 1;

    private AppRepository() {}

    /** All apps that expose a launcher entry (LEANBACK_LAUNCHER + LAUNCHER). */
    public static List<AppInfo> loadLaunchables(Context c) {
        PackageManager pm = c.getPackageManager();
        ArrayList<AppInfo> out = new ArrayList<>();
        try {
            addMatches(pm, out, new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER));
            addMatches(pm, out, new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER));
        } catch (Throwable ignored) {
        }
        // de-dupe by package/activity, drop ourselves
        ArrayList<AppInfo> unique = new ArrayList<>();
        ArrayList<String> seen = new ArrayList<>();
        for (AppInfo a : out) {
            if (AppInfo.LAUNCHER_SELF.equals(a.packageName)) continue;
            if (!seen.contains(a.key())) {
                seen.add(a.key());
                unique.add(a);
            }
        }
        sort(unique, Prefs.sortOrder(c));
        return unique;
    }

    private static void addMatches(PackageManager pm, List<AppInfo> out, Intent intent) {
        List<ResolveInfo> matches = pm.queryIntentActivities(intent, 0);
        if (matches == null) return;
        for (ResolveInfo ri : matches) {
            if (ri.activityInfo == null) continue;
            String pkg = ri.activityInfo.packageName;
            if (AppInfo.LAUNCHER_SELF.equals(pkg)) continue;
            out.add(new AppInfo(pkg, ri.activityInfo.name, AppInfo.loadLabel(pm, ri)));
        }
    }

    public static void sort(List<AppInfo> apps, int order) {
        Comparator<AppInfo> cmp;
        if (order == SORT_MOST_USED) {
            final Context c = N1App.get();
            final List<String> usage = Prefs.usageOrder(c);          // most used first
            final List<String> recents = Prefs.recentPackages(c);    // recently launched first
            cmp = new Comparator<AppInfo>() {
                @Override
                public int compare(AppInfo a, AppInfo b) {
                    int ra = rank(usage, a.packageName), rb = rank(usage, b.packageName);
                    if (ra != rb) return ra - rb;
                    int ca = rank(recents, a.packageName), cb = rank(recents, b.packageName);
                    if (ca != cb) return ca - cb;
                    return a.label.compareToIgnoreCase(b.label);
                }
            };
        } else {
            cmp = new Comparator<AppInfo>() {
                @Override
                public int compare(AppInfo a, AppInfo b) {
                    return a.label.compareToIgnoreCase(b.label);
                }
            };
        }
        Collections.sort(apps, cmp);
    }

    private static int rank(List<String> list, String pkg) {
        int i = list.indexOf(pkg);
        return i < 0 ? Integer.MAX_VALUE : i;
    }

    /** Launch an app and record it in recents/usage stats. */
    public static boolean launch(Context c, AppInfo app) {
        try {
            Intent i = new Intent(Intent.ACTION_MAIN);
            i.addCategory(Intent.CATEGORY_LAUNCHER);
            i.setClassName(app.packageName, app.activityName);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            c.startActivity(i);
            Prefs.pushRecentPackage(c, app.packageName);
            Prefs.bumpUsage(c, app.packageName);
            return true;
        } catch (Throwable t) {
            // Fallback: resolve via leanback category (some TV apps hide from LAUNCHER)
            try {
                Intent i = new Intent(Intent.ACTION_MAIN);
                i.addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
                i.setPackage(app.packageName);
                ResolveInfo ri = c.getPackageManager().resolveActivity(i, 0);
                if (ri != null) {
                    i.setClassName(ri.activityInfo.packageName, ri.activityInfo.name);
                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    c.startActivity(i);
                    Prefs.pushRecentPackage(c, app.packageName);
                    Prefs.bumpUsage(c, app.packageName);
                    return true;
                }
            } catch (Throwable ignored) {
            }
            return false;
        }
    }
}
