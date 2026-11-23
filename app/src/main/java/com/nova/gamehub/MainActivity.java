package com.nova.gamehub;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends Activity {
    static final int[] SWATCH = {
            0xFF51D9E8, 0xFF6B8CFF, 0xFFB57BFF, 0xFFFF6BA8,
            0xFFFF7B5C, 0xFFFFC24D, 0xFF57D6A0, 0xFFFFFFFF};
    static final int CARD = 0xFF121A2B;
    static final int DARK_BTN = 0xFF1E2A47;
    static final int MUTED = 0xFF9BA9C5;

    SharedPreferences sp;
    LinearLayout list;
    int accent;

    int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density + .5f); }

    GradientDrawable bg(int c, int r) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(c);
        g.setCornerRadius(dp(r));
        return g;
    }

    RippleDrawable rip(int c, int r) {
        return new RippleDrawable(ColorStateList.valueOf(0x44FFFFFF), bg(c, r), null);
    }

    int onColor(int c) {
        int lum = (Color.red(c) * 299 + Color.green(c) * 587 + Color.blue(c) * 114) / 1000;
        return lum > 150 ? 0xFF08111F : Color.WHITE;
    }

    void press(View b) {
        b.setOnTouchListener((v, e) -> {
            int a = e.getAction();
            if (a == MotionEvent.ACTION_DOWN) {
                v.animate().scaleX(.94f).scaleY(.94f).setDuration(80).start();
            } else if (a == MotionEvent.ACTION_UP || a == MotionEvent.ACTION_CANCEL) {
                v.animate().scaleX(1f).scaleY(1f).setDuration(120).start();
            }
            return false;
        });
    }

    Button btn(String t, int c, int tc, float size, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(t);
        b.setAllCaps(false);
        b.setTextColor(tc);
        b.setTextSize(size);
        b.setTypeface(null, Typeface.BOLD);
        b.setBackground(rip(c, 14));
        b.setStateListAnimator(null);
        b.setMinHeight(0);
        b.setMinimumHeight(dp(48));
        b.setOnClickListener(l);
        press(b);
        return b;
    }

    Button iconBtn(String t, int c, int tc, View.OnClickListener l) {
        Button b = btn(t, c, tc, 16, l);
        b.setMinWidth(0);
        b.setMinimumWidth(0);
        b.setMinimumHeight(0);
        b.setPadding(0, 0, 0, 0);
        return b;
    }

    TextView tv(String t, float size, int col) {
        TextView v = new TextView(this);
        v.setText(t);
        v.setTextSize(size);
        v.setTextColor(col);
        return v;
    }

    TextView section(String t) {
        TextView v = tv(t, 15, accent);
        v.setTypeface(null, Typeface.BOLD);
        v.setPadding(dp(4), dp(22), dp(4), dp(8));
        return v;
    }

    LinearLayout.LayoutParams mt(int top) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, dp(top), 0, 0);
        return lp;
    }

    void roundClip(View v, int radius) {
        v.setClipToOutline(true);
        v.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline o) {
                o.setRoundRect(0, 0, view.getWidth(), view.getHeight(), dp(radius));
            }
        });
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        sp = getSharedPreferences("nova", MODE_PRIVATE);
        accent = sp.getInt("accent", SWATCH[0]);
        buildUI();
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission("android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 1);
        }
    }

    void buildUI() {
        ScrollView sv = new ScrollView(this);
        sv.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xFF070A14, 0xFF0F1630}));
        sv.setFitsSystemWindows(true);
        sv.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(16), dp(16), dp(40));
        sv.addView(root);

        // header
        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.setPadding(dp(14), dp(14), dp(14), dp(14));
        head.setBackground(bg(CARD, 22));
        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.ic_launcher);
        logo.setScaleType(ImageView.ScaleType.CENTER_CROP);
        roundClip(logo, 16);
        head.addView(logo, new LinearLayout.LayoutParams(dp(64), dp(64)));
        LinearLayout txt = new LinearLayout(this);
        txt.setOrientation(LinearLayout.VERTICAL);
        txt.setPaddingRelative(dp(14), 0, 0, 0);
        TextView title = tv("NOVA GAME HUB", 20, accent);
        title.setTypeface(null, Typeface.BOLD);
        txt.addView(title);
        txt.addView(tv("مركز ألعابك وأدواتك العائمة", 12, MUTED));
        head.addView(txt, new LinearLayout.LayoutParams(0, -2, 1f));
        root.addView(head);

        // add game
        root.addView(btn("＋  إضافة لعبة", accent, onColor(accent), 16, v -> pick()), mt(14));

        // games
        root.addView(section("ألعابي"));
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        root.addView(list);

        // tools
        root.addView(section("الأدوات العائمة"));
        LinearLayout tools = new LinearLayout(this);
        tools.setOrientation(LinearLayout.HORIZONTAL);
        Button on = btn("▶ تشغيل", DARK_BTN, Color.WHITE, 14, v -> {
            if (ensureOverlay()) startOverlay();
        });
        Button off = btn("⏹ إيقاف", 0xFF4A2530, 0xFFFFB3BD, 14,
                v -> stopService(new Intent(this, OverlayService.class)));
        LinearLayout.LayoutParams l1 = new LinearLayout.LayoutParams(0, -2, 1f);
        l1.setMargins(0, 0, dp(5), 0);
        LinearLayout.LayoutParams l2 = new LinearLayout.LayoutParams(0, -2, 1f);
        l2.setMargins(dp(5), 0, 0, 0);
        tools.addView(on, l1);
        tools.addView(off, l2);
        root.addView(tools);
        TextView hint = tv("اضغط الزر 🎮 العائم لفتح لوحة المؤشر والميقاتي والمؤقت.", 12, MUTED);
        hint.setPadding(dp(4), dp(8), dp(4), 0);
        root.addView(hint);

        // theme
        root.addView(section("المظهر"));
        LinearLayout themeCard = new LinearLayout(this);
        themeCard.setOrientation(LinearLayout.VERTICAL);
        themeCard.setPadding(dp(14), dp(14), dp(14), dp(14));
        themeCard.setBackground(bg(CARD, 18));
        themeCard.addView(tv("لون الأزرار والعناوين", 13, Color.WHITE));

        LinearLayout sw = new LinearLayout(this);
        sw.setOrientation(LinearLayout.HORIZONTAL);
        sw.setGravity(Gravity.CENTER);
        for (int c : SWATCH) {
            View s = new View(this);
            GradientDrawable g = new GradientDrawable();
            g.setShape(GradientDrawable.OVAL);
            g.setColor(c);
            if (c == accent) g.setStroke(dp(3), Color.WHITE);
            s.setBackground(g);
            s.setOnClickListener(v -> setAccent(c));
            press(s);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(30), dp(30));
            lp.setMargins(dp(4), dp(12), dp(4), dp(4));
            sw.addView(s, lp);
        }
        themeCard.addView(sw);

        themeCard.addView(tv("أو اختر أي لون تريده:", 12, MUTED), mt(8));
        float[] hsv = new float[3];
        Color.colorToHSV(accent, hsv);
        SeekBar hue = new SeekBar(this);
        hue.setMax(360);
        hue.setProgress((int) hsv[0]);
        hue.setProgressTintList(ColorStateList.valueOf(accent));
        hue.setThumbTintList(ColorStateList.valueOf(accent));
        hue.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar s, int p, boolean user) {
                if (user) {
                    int c = Color.HSVToColor(new float[]{p, .62f, 1f});
                    s.setThumbTintList(ColorStateList.valueOf(c));
                    s.setProgressTintList(ColorStateList.valueOf(c));
                }
            }
            @Override public void onStartTrackingTouch(SeekBar s) { }
            @Override
            public void onStopTrackingTouch(SeekBar s) {
                setAccent(Color.HSVToColor(new float[]{s.getProgress(), .62f, 1f}));
            }
        });
        themeCard.addView(hue, mt(4));
        themeCard.addView(btn("↺ اللون الافتراضي", DARK_BTN, Color.WHITE, 13,
                v -> setAccent(SWATCH[0])), mt(8));
        root.addView(themeCard);

        // developer credit
        TextView dev = tv("المطور: جعفر الصادق", 14, accent);
        dev.setTypeface(null, Typeface.BOLD);
        dev.setGravity(Gravity.CENTER);
        dev.setPadding(0, dp(30), 0, 0);
        root.addView(dev);
        TextView ver = tv("NOVA GAME HUB • v1.0", 11, MUTED);
        ver.setGravity(Gravity.CENTER);
        ver.setPadding(0, dp(4), 0, 0);
        root.addView(ver);

        setContentView(sv);
        render();
    }

    void setAccent(int c) {
        accent = c;
        sp.edit().putInt("accent", c).apply();
        buildUI();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (list != null) render();
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
                addRow(p, pm.getApplicationLabel(ai).toString(), ai);
            } catch (PackageManager.NameNotFoundException e) {
                g.remove(p);
                changed = true;
            }
        }
        if (changed) sp.edit().putStringSet("games", g).apply();
        if (g.isEmpty()) {
            TextView e = tv("مكتبتك فارغة.\nاضغط «＋ إضافة لعبة» لاختيار لعبة من هاتفك.", 14, MUTED);
            e.setGravity(Gravity.CENTER);
            e.setPadding(dp(14), dp(26), dp(14), dp(26));
            e.setBackground(bg(CARD, 18));
            list.addView(e);
        }
    }

    void addRow(String pkg, String name, ApplicationInfo ai) {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(dp(12), dp(12), dp(12), dp(12));
        r.setBackground(bg(CARD, 18));
        ImageView icon = new ImageView(this);
        icon.setImageDrawable(ai.loadIcon(getPackageManager()));
        r.addView(icon, new LinearLayout.LayoutParams(dp(52), dp(52)));
        TextView n = tv(name, 16, Color.WHITE);
        n.setTypeface(null, Typeface.BOLD);
        n.setPaddingRelative(dp(12), 0, dp(8), 0);
        r.addView(n, new LinearLayout.LayoutParams(0, -2, 1f));
        LinearLayout.LayoutParams pl = new LinearLayout.LayoutParams(dp(46), dp(46));
        pl.setMarginEnd(dp(8));
        r.addView(iconBtn("▶", accent, onColor(accent), v -> play(pkg)), pl);
        r.addView(iconBtn("✕", DARK_BTN, 0xFFFF9EAA, v -> {
            Set<String> g = games();
            g.remove(pkg);
            sp.edit().putStringSet("games", g).apply();
            render();
        }), new LinearLayout.LayoutParams(dp(46), dp(46)));
        list.addView(r, mt(10));
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
                r.setGravity(Gravity.CENTER_VERTICAL);
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
