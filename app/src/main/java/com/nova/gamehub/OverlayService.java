package com.nova.gamehub;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.os.Vibrator;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Locale;

public class OverlayService extends Service {
    WindowManager wm;
    TextView bubble, swTv, cdTv;
    Button crossBtn, swBtn, cdBtn, presetBtn;
    ScrollView panel;
    CrossView cross;
    WindowManager.LayoutParams bp, pp, cp;
    Handler h = new Handler(Looper.getMainLooper());
    boolean panelOn, crossOn, swRun, cdRun;
    int style = 0, size = 36, colorI = 0, ox = 0, oy = 0, presetI = 1;
    final int[] colors = {0xFFFF3B3B, 0xFF57FF8A, 0xFF51D9E8, 0xFFFFE14D, 0xFFFFFFFF};
    final int[] presets = {1, 5, 10, 15, 30};
    long swAcc, swStart, cdEnd, cdLeft = 5 * 60000L;

    class CrossView extends View {
        int style = 0, color = 0xFFFF3B3B;
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        CrossView(Context c) { super(c); }
        @Override
        protected void onDraw(Canvas c) {
            float w = getWidth(), m = w / 2;
            p.setColor(color);
            p.setStrokeWidth(Math.max(3, w / 16));
            p.setStrokeCap(Paint.Cap.ROUND);
            p.setStyle(Paint.Style.STROKE);
            if (style == 0) {
                c.drawLine(m, 0, m, w, p);
                c.drawLine(0, m, w, m, p);
            } else if (style == 1) {
                p.setStyle(Paint.Style.FILL);
                c.drawCircle(m, m, w * .14f, p);
            } else if (style == 2) {
                c.drawLine(w * .1f, w * .1f, w * .9f, w * .9f, p);
                c.drawLine(w * .9f, w * .1f, w * .1f, w * .9f, p);
            } else {
                c.drawCircle(m, m, w * .42f, p);
                p.setStyle(Paint.Style.FILL);
                c.drawCircle(m, m, w * .07f, p);
            }
        }
    }

    int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density + .5f); }
    int type() {
        return Build.VERSION.SDK_INT >= 26 ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
    }
    long now() { return SystemClock.elapsedRealtime(); }

    GradientDrawable bg(int c, int r) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(c);
        g.setCornerRadius(dp(r));
        return g;
    }

    Button btn(String t, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(t);
        b.setAllCaps(false);
        b.setTextColor(Color.WHITE);
        b.setTextSize(13);
        b.setMinHeight(0);
        b.setMinimumHeight(dp(36));
        b.setPadding(dp(6), 0, dp(6), 0);
        b.setBackground(bg(0xFF273B67, 10));
        b.setOnClickListener(l);
        return b;
    }

    TextView tv(String t, float size, int col) {
        TextView v = new TextView(this);
        v.setText(t);
        v.setTextSize(size);
        v.setTextColor(col);
        v.setGravity(Gravity.CENTER);
        return v;
    }

    LinearLayout row(View... vs) {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        for (View v : vs) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            lp.setMargins(dp(2), dp(2), dp(2), dp(2));
            r.addView(v, lp);
        }
        return r;
    }

    @Override
    public IBinder onBind(Intent i) { return null; }

    @Override
    public int onStartCommand(Intent i, int f, int id) {
        if (wm == null) init();
        return START_STICKY;
    }

    void init() {
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        startFg();
        cross = new CrossView(this);
        cp = new WindowManager.LayoutParams(dp(size), dp(size), type(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, PixelFormat.TRANSLUCENT);
        cp.gravity = Gravity.CENTER;
        buildBubble();
        buildPanel();
        h.post(tick);
    }

    void startFg() {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Notification.Builder nb;
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(new NotificationChannel("nova", "NOVA", NotificationManager.IMPORTANCE_LOW));
            nb = new Notification.Builder(this, "nova");
        } else {
            nb = new Notification.Builder(this);
        }
        nb.setContentTitle("NOVA GAME HUB").setContentText("الأدوات العائمة شغّالة")
                .setSmallIcon(android.R.drawable.ic_menu_compass).setOngoing(true)
                .setContentIntent(PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class),
                        PendingIntent.FLAG_IMMUTABLE));
        Notification n = nb.build();
        if (Build.VERSION.SDK_INT >= 34) startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        else startForeground(1, n);
    }

    void buildBubble() {
        bubble = new TextView(this);
        bubble.setText("🎮");
        bubble.setTextSize(18);
        bubble.setTextColor(Color.WHITE);
        bubble.setGravity(Gravity.CENTER);
        bubble.setMinWidth(dp(48));
        bubble.setMinHeight(dp(48));
        bubble.setPadding(dp(8), dp(4), dp(8), dp(4));
        bubble.setBackground(bg(0xDD5868DF, 24));
        bp = new WindowManager.LayoutParams(-2, -2, type(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);
        bp.gravity = Gravity.TOP | Gravity.LEFT;
        bp.x = 0;
        bp.y = dp(120);
        final float[] d = new float[4];
        final boolean[] mv = {false};
        bubble.setOnTouchListener((v, e) -> {
            switch (e.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    d[0] = e.getRawX(); d[1] = e.getRawY(); d[2] = bp.x; d[3] = bp.y;
                    mv[0] = false;
                    return true;
                case MotionEvent.ACTION_MOVE: {
                    float dx = e.getRawX() - d[0], dy = e.getRawY() - d[1];
                    if (Math.abs(dx) > 10 || Math.abs(dy) > 10) mv[0] = true;
                    if (mv[0]) {
                        bp.x = (int) (d[2] + dx);
                        bp.y = (int) (d[3] + dy);
                        wm.updateViewLayout(bubble, bp);
                    }
                    return true;
                }
                case MotionEvent.ACTION_UP:
                    if (!mv[0]) togglePanel();
                    return true;
            }
            return false;
        });
        wm.addView(bubble, bp);
    }

    void buildPanel() {
        panel = new ScrollView(this);
        panel.setBackground(bg(0xF2101828, 16));
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(8), dp(8), dp(8), dp(8));
        c.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);

        crossBtn = btn("المؤشر: إيقاف", v -> { crossOn = !crossOn; applyCross(); });
        c.addView(tv("المؤشر", 13, 0xFF51D9E8));
        c.addView(crossBtn);
        c.addView(row(btn("الشكل", v -> { style = (style + 1) % 4; applyCross(); }),
                btn("اللون", v -> { colorI = (colorI + 1) % colors.length; applyCross(); })));
        c.addView(row(btn("−", v -> { size = Math.max(16, size - 6); applyCross(); }),
                btn("+", v -> { size = Math.min(140, size + 6); applyCross(); })));
        c.addView(row(btn("◀", v -> { ox -= 4; applyCross(); }), btn("▶", v -> { ox += 4; applyCross(); }),
                btn("▲", v -> { oy -= 4; applyCross(); }), btn("▼", v -> { oy += 4; applyCross(); })));

        c.addView(tv("ميقاتي", 13, 0xFF51D9E8));
        swTv = tv("00:00.00", 24, Color.WHITE);
        c.addView(swTv);
        swBtn = btn("ابدأ", v -> {
            if (swRun) { swAcc += now() - swStart; swRun = false; } else { swStart = now(); swRun = true; }
            swBtn.setText(swRun ? "إيقاف" : "ابدأ");
        });
        c.addView(row(swBtn, btn("تصفير", v -> {
            swAcc = 0; swRun = false; swBtn.setText("ابدأ");
        })));

        c.addView(tv("مؤقت تنازلي", 13, 0xFF51D9E8));
        cdTv = tv("05:00", 24, Color.WHITE);
        c.addView(cdTv);
        presetBtn = btn("5 د", v -> {
            presetI = (presetI + 1) % presets.length;
            if (!cdRun) cdLeft = presets[presetI] * 60000L;
            presetBtn.setText(presets[presetI] + " د");
        });
        cdBtn = btn("ابدأ", v -> {
            if (cdRun) { cdLeft = cdEnd - now(); cdRun = false; }
            else { if (cdLeft <= 0) cdLeft = presets[presetI] * 60000L; cdEnd = now() + cdLeft; cdRun = true; }
            cdBtn.setText(cdRun ? "إيقاف" : "ابدأ");
        });
        c.addView(row(presetBtn, cdBtn, btn("تصفير", v -> {
            cdRun = false; cdLeft = presets[presetI] * 60000L; cdBtn.setText("ابدأ");
        })));

        c.addView(row(btn("إخفاء", v -> togglePanel()), btn("❌ إنهاء", v -> stopSelf())));
        panel.addView(c);
        pp = new WindowManager.LayoutParams(dp(230), dp(320), type(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);
        pp.gravity = Gravity.TOP | Gravity.LEFT;
    }

    void togglePanel() {
        try {
            if (panelOn) { wm.removeView(panel); panelOn = false; return; }
            DisplayMetrics dm = getResources().getDisplayMetrics();
            int pw = dp(230);
            pp.width = pw;
            pp.height = Math.min(dp(320), dm.heightPixels - dp(24));
            int bw = bubble.getWidth() > 0 ? bubble.getWidth() : dp(48);
            int x = bp.x + bw + dp(6);
            if (x + pw > dm.widthPixels) x = bp.x - pw - dp(6);
            pp.x = Math.max(0, x);
            pp.y = Math.max(0, Math.min(bp.y, dm.heightPixels - pp.height - dp(12)));
            wm.addView(panel, pp);
            panelOn = true;
        } catch (Exception e) { }
    }

    void applyCross() {
        cross.style = style;
        cross.color = colors[colorI];
        cp.width = dp(size);
        cp.height = dp(size);
        cp.x = ox;
        cp.y = oy;
        cross.invalidate();
        crossBtn.setText(crossOn ? "المؤشر: شغّال" : "المؤشر: إيقاف");
        try {
            if (crossOn) {
                if (cross.getParent() == null) wm.addView(cross, cp);
                else wm.updateViewLayout(cross, cp);
            } else if (cross.getParent() != null) {
                wm.removeView(cross);
            }
        } catch (Exception e) { }
    }

    String fmt(long ms, boolean cs) {
        long s = ms / 1000;
        if (cs) return String.format(Locale.US, "%02d:%02d.%02d", s / 60, s % 60, (ms / 10) % 100);
        return String.format(Locale.US, "%02d:%02d", s / 60, s % 60);
    }

    final Runnable tick = new Runnable() {
        @Override
        public void run() { update(); h.postDelayed(this, 200); }
    };

    void update() {
        long n = now();
        long sw = swAcc + (swRun ? n - swStart : 0);
        swTv.setText(fmt(sw, true));
        long left = cdRun ? cdEnd - n : cdLeft;
        if (cdRun && left <= 0) {
            cdRun = false;
            cdLeft = presets[presetI] * 60000L;
            left = cdLeft;
            cdBtn.setText("ابدأ");
            alarm();
        }
        cdTv.setText(fmt(left, false));
        String bt = cdRun ? fmt(left, false) : swRun ? fmt(sw, false) : "🎮";
        if (!bt.contentEquals(bubble.getText())) bubble.setText(bt);
    }

    void alarm() {
        try { new ToneGenerator(AudioManager.STREAM_ALARM, 100).startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 1500); } catch (Exception e) { }
        Vibrator v = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (v != null) v.vibrate(600);
        Toast.makeText(this, "انتهى الوقت ⏳", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onDestroy() {
        h.removeCallbacks(tick);
        try { wm.removeView(bubble); } catch (Exception e) { }
        try { if (panelOn) wm.removeView(panel); } catch (Exception e) { }
        try { if (cross.getParent() != null) wm.removeView(cross); } catch (Exception e) { }
        super.onDestroy();
    }
}
