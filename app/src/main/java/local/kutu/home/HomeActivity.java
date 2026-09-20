package local.kutu.home;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HomeActivity extends Activity {
    private final Handler handler = new Handler();
    private TextView clock;
    private TextView date;
    private LinearLayout favoritesRow;
    private SharedPreferences prefs;

    private static final String[] DEFAULT_PACKAGES = new String[] {
            "com.netflix.ninja",
            "com.google.android.youtube.tv",
            "com.spotify.tv.android",
            "com.disney.disneyplus",
            "com.amazon.amazonvideo.livingroom",
            "com.exxen.android",
            "com.mubi",
            "com.turkcell.ott",
            "com.dsmart.blu.android"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        enableTrueFullscreen();
        prefs = getSharedPreferences("kutu_home", MODE_PRIVATE);
        buildUi();
        startClock();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) enableTrueFullscreen();
    }

    private void applyEdgeToEdgeAfterContent() {
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            getWindow().setDecorFitsSystemWindows(false);
            getWindow().setNavigationBarColor(android.graphics.Color.TRANSPARENT);
            getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);

            if (android.os.Build.VERSION.SDK_INT >= 29) {
                getWindow().setNavigationBarContrastEnforced(false);
                getWindow().setStatusBarContrastEnforced(false);
            }

            final View decor = getWindow().getDecorView();
            decor.post(() -> {
                android.view.WindowInsetsController controller = decor.getWindowInsetsController();
                if (controller != null) {
                    controller.hide(
                            android.view.WindowInsets.Type.statusBars()
                                    | android.view.WindowInsets.Type.navigationBars()
                    );
                    controller.setSystemBarsBehavior(
                            android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    );
                }
            });
        }
    }

    private void enableTrueFullscreen() {
        getWindow().setFlags(
                android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN,
                android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN
        );


        View decor = getWindow().getDecorView();
        decor.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (favoritesRow != null) loadFavorites();
    }

    @Override
    public void onBackPressed() {
        // Home launcher should stay on screen.
    }

    private void buildUi() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundResource(R.drawable.background_gradient);

        LinearLayout topRight = new LinearLayout(this);
        topRight.setOrientation(LinearLayout.VERTICAL);
        topRight.setGravity(Gravity.END);

        clock = text("00:00", 34, Color.WHITE);
        clock.setGravity(Gravity.END);
        date = text("", 15, 0xFFB9BEC8);
        date.setGravity(Gravity.END);

        topRight.addView(clock);
        topRight.addView(date);

        FrameLayout.LayoutParams tr = new FrameLayout.LayoutParams(dp(360), ViewGroup.LayoutParams.WRAP_CONTENT);
        tr.gravity = Gravity.TOP | Gravity.END;
        tr.setMargins(0, dp(34), dp(48), 0);
        root.addView(topRight, tr);

        TextView title = text("KUTU", 18, 0xFFE7E9EE);
        title.setLetterSpacing(0.28f);
        FrameLayout.LayoutParams titleLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        titleLp.gravity = Gravity.TOP | Gravity.START;
        titleLp.setMargins(dp(48), dp(42), 0, 0);
        root.addView(title, titleLp);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER_HORIZONTAL);

        favoritesRow = new LinearLayout(this);
        favoritesRow.setOrientation(LinearLayout.HORIZONTAL);
        favoritesRow.setGravity(Gravity.CENTER);

        content.addView(favoritesRow, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(154)));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER);

        actions.addView(actionButton("Ekran Yansıtma", v -> launchMirror()), actionParams());
        actions.addView(actionButton("Ayarlar", v -> startActivity(new Intent(Settings.ACTION_SETTINGS))), actionParams());
        actions.addView(actionButton("Tüm Uygulamalar", v -> startActivity(new Intent(this, AllAppsActivity.class))), actionParams());

        content.addView(actions);

        FrameLayout.LayoutParams contentLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        contentLp.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        contentLp.setMargins(dp(24), 0, dp(24), dp(42));
        root.addView(content, contentLp);

        setContentView(root);
        applyEdgeToEdgeAfterContent();
        loadFavorites();
    }

    private LinearLayout.LayoutParams actionParams() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(180), dp(48));
        lp.setMargins(dp(10), dp(8), dp(10), 0);
        return lp;
    }

    private View actionButton(String label, View.OnClickListener click) {
        TextView b = text(label, 17, Color.WHITE);
        b.setGravity(Gravity.CENTER);
        b.setFocusable(true);
        b.setClickable(true);
        b.setBackgroundResource(R.drawable.card_background);
        b.setOnClickListener(click);
        applyFocus(b);
        return b;
    }

    private void loadFavorites() {
        favoritesRow.removeAllViews();

        List<AppEntry> all = getLaunchableApps(this);
        Map<String, AppEntry> byPkg = new LinkedHashMap<>();
        for (AppEntry e : all) byPkg.put(e.packageName, e);

        List<String> favPkgs = readFavorites();
        if (favPkgs.isEmpty()) {
            for (String pkg : DEFAULT_PACKAGES) {
                if (byPkg.containsKey(pkg) && favPkgs.size() < 7) favPkgs.add(pkg);
            }
            if (favPkgs.size() < 7) {
                for (AppEntry e : all) {
                    if (!favPkgs.contains(e.packageName) && !e.packageName.equals(getPackageName())) {
                        favPkgs.add(e.packageName);
                        if (favPkgs.size() >= 7) break;
                    }
                }
            }
            writeFavorites(favPkgs);
        }

        int count = 0;
        for (String pkg : favPkgs) {
            AppEntry e = byPkg.get(pkg);
            if (e != null && count < 7) {
                favoritesRow.addView(appCard(e));
                count++;
            }
        }

        if (count == 0) {
            TextView empty = text("Tüm Uygulamalar'dan favori ekleyebilirsin", 17, 0xFFB9BEC8);
            empty.setGravity(Gravity.CENTER);
            favoritesRow.addView(empty, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(140)));
        }
    }

    private View appCard(AppEntry e) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setFocusable(true);
        box.setClickable(true);
        box.setBackgroundResource(R.drawable.card_background);
        box.setPadding(dp(12), dp(12), dp(12), dp(10));

        ImageView icon = new ImageView(this);
        icon.setImageDrawable(e.icon);
        icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        box.addView(icon, new LinearLayout.LayoutParams(dp(78), dp(78)));

        TextView label = text(e.label, 13, Color.WHITE);
        label.setGravity(Gravity.CENTER);
        label.setSingleLine(true);
        LinearLayout.LayoutParams labelLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(34));
        labelLp.topMargin = dp(5);
        box.addView(label, labelLp);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(132), 1f);
        lp.setMargins(dp(5), dp(8), dp(5), dp(8));
        box.setLayoutParams(lp);

        box.setOnClickListener(v -> launchPackage(e.packageName));
        box.setOnLongClickListener(v -> {
            removeFavorite(e.packageName);
            Toast.makeText(this, e.label + " ana ekrandan kaldırıldı", Toast.LENGTH_SHORT).show();
            loadFavorites();
            return true;
        });
        applyFocus(box);
        return box;
    }

    private void applyFocus(View v) {
        v.setOnFocusChangeListener((view, focused) -> {
            view.animate().scaleX(focused ? 1.10f : 1f).scaleY(focused ? 1.10f : 1f).setDuration(120).start();
            view.setBackgroundResource(focused ? R.drawable.card_background_focused : R.drawable.card_background);
            view.setElevation(focused ? dp(14) : dp(2));
        });
    }

    private void launchMirror() {
        if (launchPackage("io.github.jqssun.airplay")) return;
        if (launchPackage("com.ionitech.airscreen")) return;
        Toast.makeText(this, "AirPlay uygulaması bulunamadı", Toast.LENGTH_LONG).show();
    }

    private boolean launchPackage(String pkg) {
        Intent i = getPackageManager().getLaunchIntentForPackage(pkg);
        if (i != null) {
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            return true;
        }
        return false;
    }

    private void startClock() {
        handler.post(new Runnable() {
            @Override public void run() {
                Date now = new Date();
                clock.setText(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(now));
                date.setText(new SimpleDateFormat("d MMMM EEEE", Locale.getDefault()).format(now));
                handler.postDelayed(this, 1000);
            }
        });
    }

    private TextView text(String s, int sp, int color) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setFontFeatureSettings("kern");
        return t;
    }

    public List<String> readFavorites() {
        String raw = prefs.getString("favorites", "");
        List<String> out = new ArrayList<>();
        if (!raw.isEmpty()) {
            for (String p : raw.split(",")) if (!p.trim().isEmpty()) out.add(p.trim());
        }
        return out;
    }

    public void writeFavorites(List<String> favs) {
        prefs.edit().putString("favorites", android.text.TextUtils.join(",", favs)).apply();
    }

    private void removeFavorite(String pkg) {
        List<String> favs = readFavorites();
        favs.remove(pkg);
        writeFavorites(favs);
    }

    public static List<AppEntry> getLaunchableApps(Context ctx) {
        PackageManager pm = ctx.getPackageManager();
        Map<String, AppEntry> map = new LinkedHashMap<>();

        Intent tv = new Intent(Intent.ACTION_MAIN);
        tv.addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
        for (ResolveInfo r : pm.queryIntentActivities(tv, 0)) {
            addResolve(pm, map, r, ctx.getPackageName());
        }

        Intent normal = new Intent(Intent.ACTION_MAIN);
        normal.addCategory(Intent.CATEGORY_LAUNCHER);
        for (ResolveInfo r : pm.queryIntentActivities(normal, 0)) {
            addResolve(pm, map, r, ctx.getPackageName());
        }

        List<AppEntry> out = new ArrayList<>(map.values());
        Collections.sort(out, Comparator.comparing(a -> a.label.toLowerCase(Locale.getDefault())));
        return out;
    }

    private static void addResolve(PackageManager pm, Map<String, AppEntry> map, ResolveInfo r, String ownPkg) {
        if (r.activityInfo == null) return;
        String pkg = r.activityInfo.packageName;
        if (pkg == null || pkg.equals(ownPkg)) return;
        if (!map.containsKey(pkg)) {
            CharSequence l = r.loadLabel(pm);
            Drawable icon = r.loadIcon(pm);
            map.put(pkg, new AppEntry(pkg, l == null ? pkg : l.toString(), icon));
        }
    }

    public static int dp(Context c, int v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    private int dp(int v) { return dp(this, v); }

    public static class AppEntry {
        public final String packageName;
        public final String label;
        public final Drawable icon;
        public AppEntry(String packageName, String label, Drawable icon) {
            this.packageName = packageName;
            this.label = label;
            this.icon = icon;
        }
    }
}
