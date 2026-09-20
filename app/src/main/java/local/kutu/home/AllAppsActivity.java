package local.kutu.home;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class AllAppsActivity extends Activity {
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        enableTrueFullscreen();
        prefs = getSharedPreferences("kutu_home", MODE_PRIVATE);
        buildUi();
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

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(44), dp(28), dp(44), dp(84));
        root.setBackgroundResource(R.drawable.background_gradient);

        TextView title = new TextView(this);
        title.setText("Tüm Uygulamalar");
        title.setTextColor(Color.WHITE);
        title.setTextSize(28);
        root.addView(title);

        TextView hint = new TextView(this);
        hint.setText("OK: aç  •  Uzun OK: ana ekrana ekle / kaldır");
        hint.setTextColor(0xFFB9BEC8);
        hint.setTextSize(14);
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hp.bottomMargin = dp(18);
        root.addView(hint, hp);

        ScrollView scroll = new ScrollView(this);
        GridLayout grid = new GridLayout(this);
        int columns = 6;
        grid.setColumnCount(columns);
        grid.setAlignmentMode(GridLayout.ALIGN_BOUNDS);
        grid.setUseDefaultMargins(false);

        List<HomeActivity.AppEntry> apps = HomeActivity.getLaunchableApps(this);
        for (HomeActivity.AppEntry e : apps) {
            grid.addView(makeCard(e));
        }

        scroll.addView(grid);
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        setContentView(root);
        applyEdgeToEdgeAfterContent();
    }

    private View makeCard(HomeActivity.AppEntry e) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(dp(10), dp(12), dp(10), dp(10));
        box.setFocusable(true);
        box.setClickable(true);
        box.setBackgroundResource(R.drawable.card_background);

        ImageView icon = new ImageView(this);
        icon.setImageDrawable(e.icon);
        icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        box.addView(icon, new LinearLayout.LayoutParams(dp(72), dp(72)));

        TextView label = new TextView(this);
        label.setText(e.label);
        label.setTextColor(Color.WHITE);
        label.setTextSize(12);
        label.setGravity(Gravity.CENTER);
        label.setSingleLine(true);
        box.addView(label, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(34)));

        int screenWidthPx = getResources().getDisplayMetrics().widthPixels;
        int availablePx = screenWidthPx - dp(88);
        int cardWidthPx = Math.max(dp(116), (availablePx / 6) - dp(12));

        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.width = cardWidthPx;
        lp.height = dp(116);
        lp.setMargins(dp(6), dp(6), dp(6), dp(6));
        box.setLayoutParams(lp);

        box.setOnClickListener(v -> {
            android.content.Intent i = getPackageManager().getLaunchIntentForPackage(e.packageName);
            if (i != null) startActivity(i);
        });

        box.setOnLongClickListener(v -> {
            List<String> favs = readFavorites();
            if (favs.contains(e.packageName)) {
                favs.remove(e.packageName);
                Toast.makeText(this, "Ana ekrandan kaldırıldı", Toast.LENGTH_SHORT).show();
            } else {
                if (favs.size() >= 7) {
                    Toast.makeText(this, "Ana ekranda en fazla 7 uygulama", Toast.LENGTH_SHORT).show();
                    return true;
                }
                favs.add(e.packageName);
                Toast.makeText(this, "Ana ekrana eklendi", Toast.LENGTH_SHORT).show();
            }
            writeFavorites(favs);
            return true;
        });

        box.setOnFocusChangeListener((v, focused) -> {
            v.animate().scaleX(focused ? 1.08f : 1f).scaleY(focused ? 1.08f : 1f).setDuration(100).start();
            v.setBackgroundResource(focused ? R.drawable.card_background_focused : R.drawable.card_background);
        });

        return box;
    }

    private List<String> readFavorites() {
        String raw = prefs.getString("favorites", "");
        List<String> out = new ArrayList<>();
        if (!raw.isEmpty()) {
            for (String p : raw.split(",")) if (!p.trim().isEmpty()) out.add(p.trim());
        }
        return out;
    }

    private void writeFavorites(List<String> favs) {
        prefs.edit().putString("favorites", android.text.TextUtils.join(",", favs)).apply();
    }

    private int dp(int v) { return HomeActivity.dp(this, v); }
}
