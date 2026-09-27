package com.n1neto.material;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.List;

/**
 * Horizontal row of Material app tiles backed by a plain ListView adapter.
 * No RecyclerView / leanback dependency: this is the lightest recycling list
 * available on every Android version (works on API 17 ATV boxes).
 *
 * D-pad behaviour:
 *  - LEFT/RIGHT move between tiles (ListView default)
 *  - UP/DOWN bubble up to HomeActivity via RowFocusListener so focus can hop
 *    to the neighbouring row
 *  - DPAD_CENTER / click launches, MENU or long-press opens tile options
 */
public class RowAdapter extends BaseAdapter {

    public interface RowFocusListener {
        /** direction: -1 = UP pressed, +1 = DOWN pressed. */
        void onRowEdge(int direction);
    }

    public interface OnTileActionListener {
        void onTileLongClick(View anchor, AppInfo app);
    }

    private final Context context;
    private final List<AppInfo> items;
    private final boolean animations;
    private final float focusScale;
    private RowFocusListener rowListener;
    private OnTileActionListener actionListener;

    public RowAdapter(Context c, List<AppInfo> items) {
        this.context = c;
        this.items = items;
        this.animations = Prefs.animations(c);
        this.focusScale = c.getResources().getDimension(R.dimen.focus_scale);
    }

    public void setRowFocusListener(RowFocusListener l) { rowListener = l; }
    public void setOnTileActionListener(OnTileActionListener l) { actionListener = l; }

    @Override public int getCount() { return items.size(); }
    @Override public AppInfo getItem(int position) { return items.get(position); }
    @Override public long getItemId(int position) { return position; }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        if (v == null) {
            v = LayoutInflater.from(context).inflate(R.layout.item_app_tile, parent, false);
            TileHolder h = new TileHolder(v);
            v.setTag(h);
        }
        ((TileHolder) v.getTag()).bind(items.get(position));
        return v;
    }

    /** Crash-safe icon binding for old devices with odd drawables. */
    static void applyIcon(ImageView iv, Drawable d) {
        if (d != null) {
            iv.setImageDrawable(d);
        } else {
            iv.setImageResource(R.drawable.ic_apps_placeholder);
        }
    }

    class TileHolder {
        final View root;
        final ImageView icon;
        final TextView label;

        TileHolder(View itemView) {
            root = itemView;
            icon = itemView.findViewById(R.id.tile_icon);
            label = itemView.findViewById(R.id.tile_label);

            root.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    float target = hasFocus ? focusScale : 1f;
                    if (animations) {
                        v.animate().scaleX(target).scaleY(target).setDuration(140).start();
                    } else {
                        v.setScaleX(target);
                        v.setScaleY(target);
                    }
                    label.setTextColor(hasFocus
                            ? N1Theme.current(context).primary
                            : N1Theme.current(context).onSurfaceDim);
                }
            });

            root.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Object tag = v.getTag(R.id.tag_action);
                    if (tag instanceof AppInfo) AppRepository.launch(context, (AppInfo) tag);
                }
            });

            root.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    Object tag = v.getTag(R.id.tag_action);
                    if (actionListener != null && tag instanceof AppInfo) {
                        actionListener.onTileLongClick(v, (AppInfo) tag);
                    }
                    return true;
                }
            });

            root.setOnKeyListener(new View.OnKeyListener() {
                @Override
                public boolean onKey(View v, int keyCode, KeyEvent event) {
                    if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
                    if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                        if (rowListener != null) { rowListener.onRowEdge(-1); return true; }
                    } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                        if (rowListener != null) { rowListener.onRowEdge(1); return true; }
                    } else if (keyCode == KeyEvent.KEYCODE_MENU) {
                        Object tag = v.getTag(R.id.tag_action);
                        if (actionListener != null && tag instanceof AppInfo) {
                            actionListener.onTileLongClick(v, (AppInfo) tag);
                            return true;
                        }
                    }
                    return false;
                }
            });
        }

        void bind(AppInfo app) {
            label.setText(app.label);
            applyIcon(icon, app.ensureIcon(context.getPackageManager()));
            // Reset per-bind state (views are recycled).
            root.setScaleX(1f);
            root.setScaleY(1f);
            label.setTextColor(N1Theme.current(context).onSurfaceDim);
            root.setTag(R.id.tag_action, app);
        }
    }
}
