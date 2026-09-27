package com.n1neto.material;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import java.util.List;

/** One labelled horizontal strip of app tiles (the "row" building block of the home screen). */
public class HomeRow extends LinearLayout {

    public final ListView list;
    public final RowAdapter adapter;
    private final TextView title;

    public HomeRow(Context c, String titleText, List<AppInfo> apps) {
        super(c);
        setOrientation(VERTICAL);
        setClipChildren(false);
        setClipToPadding(false);
        LayoutInflater.from(c).inflate(R.layout.view_home_row, this, true);

        title = findViewById(R.id.row_title);
        list = findViewById(R.id.row_list);
        title.setText(titleText);

        // Horizontal scrolling row built on a plain ListView: no RecyclerView,
        // no leanback — cheapest option that still recycles views on old boxes.
        list.setHorizontalScrollBarEnabled(false);
        adapter = new RowAdapter(c, apps);
        list.setAdapter(adapter);
    }

    public void setTitle(String t) {
        title.setText(t);
    }

    public void applyTheme(N1Theme theme) {
        title.setTextColor(theme.onSurface);
    }

    /** Move focus into the first tile of this row. */
    public boolean requestTileFocus() {
        if (list.getChildCount() == 0) return false;
        list.setSelection(0);
        View first = list.getChildAt(0);
        return first != null && first.requestFocus();
    }

    /** Focus the tile nearest to an x coordinate (used when hopping rows with UP/DOWN). */
    public boolean requestTileFocusNear(int x) {
        if (list.getChildCount() == 0) return false;
        int best = -1, bestDist = Integer.MAX_VALUE;
        for (int i = 0; i < list.getChildCount(); i++) {
            android.view.View child = list.getChildAt(i);
            int cx = (int) (child.getX() + child.getWidth() / 2f);
            int d = Math.abs(cx - x);
            if (d < bestDist) { bestDist = d; best = i; }
        }
        if (best >= 0) {
            View child = list.getChildAt(best);
            if (child != null) return child.requestFocus();
        }
        return requestTileFocus();
    }

    public android.view.View focusedChild() {
        return list.getFocusedChild();
    }
}
