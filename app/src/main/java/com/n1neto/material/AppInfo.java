package com.n1neto.material;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;

/** Tiny immutable model for one launchable app (label + icon loaded lazily, cached by package). */
public final class AppInfo {

    public static final String LAUNCHER_SELF = "com.n1neto.material";

    public final String packageName;
    public final String activityName;
    public final String label;

    private Drawable icon;

    public AppInfo(String packageName, String activityName, String label) {
        this.packageName = packageName;
        this.activityName = activityName;
        this.label = label;
    }

    public String key() {
        return packageName + "/" + activityName;
    }

    /** Load the icon once and keep it in the process-wide cache. */
    public Drawable ensureIcon(PackageManager pm) {
        if (icon != null) return icon;
        try {
            ApplicationInfo ai = pm.getApplicationInfo(packageName, 0);
            Drawable d = ai.loadIcon(pm);
            if (d != null) {
                int size = (int) (N1App.get().getResources().getDisplayMetrics().density * 52f);
                d.setBounds(0, 0, size, size);
                icon = d;
            }
        } catch (Throwable ignored) {
            // package may have been uninstalled since enumeration
        }
        return icon;
    }

    public static String loadLabel(PackageManager pm, ResolveInfo ri) {
        CharSequence cs = ri.activityInfo.loadLabel(pm);
        if (TextUtils.isEmpty(cs)) cs = ri.activityInfo.packageName;
        return cs.toString();
    }
}
