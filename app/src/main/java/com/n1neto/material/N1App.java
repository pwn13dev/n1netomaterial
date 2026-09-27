package com.n1neto.material;

import android.app.Application;
import android.content.Context;

/** Minimal Application holding a context for static helpers (clock format, theming). */
public class N1App extends Application {

    private static Context sContext;

    public static Context get() {
        return sContext;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sContext = getApplicationContext();
    }
}
