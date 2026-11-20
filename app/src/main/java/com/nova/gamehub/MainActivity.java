package com.nova.gamehub;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends Activity {
    SharedPreferences sp;
    LinearLayout list;

    int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density + .5f); }

    GradientDrawable bg(int c, int r) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(c);
        g.setCornerRadius(dp(r));
        return g;
    }

    Button btn(String t, int c, int tc, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(t);
        b.setAllCaps(false);
        b.setTextColor(tc);
        b.setBackground(bg(c, 12));
        b.setOnClickListener(l);
        return b;
    }

    TextView tv(String t, float size, int col) {
        TextView v = new TextView(this);
        v.setText(t);
        v.setTextSize(size);
        v.setTextColor(col);
        return v;
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        sp = getSharedPreferences("nova", MODE_PRIVATE);
        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(0xFF090D18);
        sv.setFitsSystemWindows(true);
        sv.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(16), dp(16), dp(30));
        sv.addView(root);

        TextView title = tv("NOVA GAME HUB", 22, 0xFF51D9E8);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        root.addView(title);
        root.addView(tv("اضغط ＋ لاختيار لعبتك من ألعابك المثبتة، ثم ▶ لتشغيلها مع الزر العائم", 13, 0xFF9BA9C5));

        Button add = btn("＋ إضافة لعبة", 0xFF6B8CFF, Color.WHITE, v -> pick());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, dp(14), 0, dp(10));
        root.addView(add, lp);

        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        root.addView(list);

        LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(-1, -2);
        lp2.setMargins(0, dp(10), 0, 0);
        root.addView(btn("🎮 تشغيل الأدوات العائمة بدون لعبة", 0xFF273B67, Color.WHITE, v -> {
            if (ensureOverlay()) startOverlay();
        }), lp2);
        root.addView(btn("⏹ إيقاف الأدوات العائمة", 0xFF4A2530, 0xFFFFB3BD,
                v -> stopService(new Intent(this, OverlayService.class))), lp2);

        setContentView(sv);
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission("android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 1);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        render();
    }

    Set<String> games() { return new HashSet<>(sp.getStringSet("games", new HashSet<String>())); }

    void render() {
        list.removeAllViews();
        PackageManager pm = getPackageManager();
        Set<String> g = games();
        List<String> pk = new ArrayList<>(g);
        Collections.sort(pk);
        boolean changed = false;
        for (String p : pk) {
            try {
                ApplicationInfo ai = pm.getApplicationInfo(p, 0);
                addRow(p, pm.getApplicationLabel(ai).toString(), new ImageView(this), ai);
            } catch (PackageManager.NameNotFoundException e) {
                g.remove(p);
                changed = true;
            }
        }
        if (changed) sp.edit().putStringSet("games", g).apply();
        if (g.isEmpty()) list.addView(tv("مكتبتك فارغة. اضغط ＋ إضافة لعبة.", 14, 0xFF9BA9C5));
    }

    void addRow(String pkg, String name, ImageView icon, ApplicationInfo ai) {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(android.view.Gravity.CENTER_VERTICAL);
        r.setPadding(dp(10), dp(10), dp(10), dp(10));
        r.setBackground(bg(0xFF121A2B, 16));
        icon.setImageDrawable(ai.loadIcon(getPackageManager()));
        r.addView(icon, new LinearLayout.LayoutParams(dp(48), dp(48)));
        TextView n = tv(name, 15, Color.WHITE);
        n.setPadding(dp(10), 0, dp(10), 0);
        r.addView(n, new LinearLayout.LayoutParams(0, -2, 1f));
        r.addView(btn("▶", 0xFF57D6A0, 0xFF08271E, v -> play(pkg)));
        r.addView(btn("✕", 0xFF2A2F44, 0xFFFF9EAA, v -> {
            Set<String> g = games();
            g.remove(pkg);
            sp.edit().putStringSet("games", g).apply();
            render();
        }));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 0, 0, dp(10));
        list.addView(r, lp);
    }

    void pick() {
        PackageManager pm = getPackageManager();
        Intent main = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> all = pm.queryIntentActivities(main, 0);
        Set<String> have = games();
        List<ResolveInfo> items = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (ResolveInfo ri : all) {
            String p = ri.activityInfo.packageName;
            if (p.equals(getPackageName()) || have.contains(p) || !seen.add(p)) continue;
            items.add(ri);
        }
        Collections.sort(items, (a, c) -> a.loadLabel(pm).toString().compareToIgnoreCase(c.loadLabel(pm).toString()));
        if (items.isEmpty()) {
            Toast.makeText(this, "ما لقيت تطبيقات جديدة للإضافة", Toast.LENGTH_SHORT).show();
            return;
        }
        ArrayAdapter<ResolveInfo> ad = new ArrayAdapter<ResolveInfo>(this, 0, items) {
            @Override
            public View getView(int pos, View cv, ViewGroup parent) {
                ResolveInfo ri = getItem(pos);
                LinearLayout r = new LinearLayout(MainActivity.this);
                r.setGravity(android.view.Gravity.CENTER_VERTICAL);
                r.setPadding(dp(12), dp(8), dp(12), dp(8));
                ImageView im = new ImageView(MainActivity.this);
                im.setImageDrawable(ri.loadIcon(pm));
                r.addView(im, new LinearLayout.LayoutParams(dp(40), dp(40)));
                TextView t = tv(ri.loadLabel(pm).toString(), 15, Color.WHITE);
                t.setPadding(dp(12), 0, 0, 0);
                r.addView(t);
                return r;
            }
        };
        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("اختر لعبتك")
                .setAdapter(ad, (d, which) -> {
                    Set<String> g = games();
                    g.add(items.get(which).activityInfo.packageName);
                    sp.edit().putStringSet("games", g).apply();
                    render();
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }

    boolean ensureOverlay() {
        if (Settings.canDrawOverlays(this)) return true;
        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("صلاحية مطلوبة")
                .setMessage("حتى يظهر الزر الجانبي فوق اللعبة، فعّل «السماح بالظهور فوق التطبيقات الأخرى» لتطبيق NOVA GAME HUB، ثم ارجع واضغط تشغيل.")
                .setPositiveButton("فتح الإعدادات", (d, w) -> startActivity(new Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()))))
                .setNegativeButton("إلغاء", null)
                .show();
        return false;
    }

    void startOverlay() {
        Intent i = new Intent(this, OverlayService.class);
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(i);
        else startService(i);
    }

    void play(String pkg) {
        if (!ensureOverlay()) return;
        Intent li = getPackageManager().getLaunchIntentForPackage(pkg);
        if (li == null) {
            Toast.makeText(this, "تعذّر فتح اللعبة", Toast.LENGTH_SHORT).show();
            return;
        }
        startOverlay();
        startActivity(li);
    }
}
