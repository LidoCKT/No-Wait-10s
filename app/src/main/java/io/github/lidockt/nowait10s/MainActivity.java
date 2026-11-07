package io.github.lidockt.nowait10s;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsets;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import io.github.libxposed.service.XposedService;
import io.github.libxposed.service.XposedServiceHelper;

public final class MainActivity extends Activity {
    private TextView status;
    private LinearLayout controls;
    private XposedService service;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        // Handle bars explicitly, including Android 15's enforced edge-to-edge layout.
        if (Build.VERSION.SDK_INT >= 30) getWindow().setDecorFitsSystemWindows(false);
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.rgb(247, 248, 250));
        scroll.setOnApplyWindowInsetsListener((view, insets) -> {
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets bars = insets.getInsets(
                    WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            } else {
                int left = insets.getSystemWindowInsetLeft();
                int top = insets.getSystemWindowInsetTop();
                int right = insets.getSystemWindowInsetRight();
                int bottom = insets.getSystemWindowInsetBottom();
                if (insets.getDisplayCutout() != null) {
                    left = Math.max(left, insets.getDisplayCutout().getSafeInsetLeft());
                    top = Math.max(top, insets.getDisplayCutout().getSafeInsetTop());
                    right = Math.max(right, insets.getDisplayCutout().getSafeInsetRight());
                    bottom = Math.max(bottom, insets.getDisplayCutout().getSafeInsetBottom());
                }
                view.setPadding(left, top, right, bottom);
            }
            return insets;
        });
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(24), dp(32), dp(24), dp(32));
        scroll.addView(body);
        TextView title = new TextView(this);
        title.setText("HyperOS 节约10s");
        title.setTextSize(28);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        body.addView(title);
        TextView intro = new TextView(this);
        intro.setText("\nNoWait10s  ·  0.1.0\n\n"
            + "让倒计时按钮提前可点，确认仍由你决定。\n");
        body.addView(intro);
        status = new TextView(this);
        status.setText("正在等待 LSPosed 服务；若一直未连接，请先启用模块后重新打开。\n");
        body.addView(status);
        controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.VERTICAL);
        body.addView(controls);
        setContentView(scroll);
        scroll.requestApplyInsets();
        XposedServiceHelper.registerListener(new XposedServiceHelper.OnServiceListener() {
            @Override public void onServiceBind(XposedService bound) {
                runOnUiThread(() -> { service = bound; showSettings(); });
            }
            @Override public void onServiceDied(XposedService dead) {
                runOnUiThread(() -> {
                    if (service == dead) {
                        service = null;
                        controls.removeAllViews();
                        status.setText("LSPosed 服务已断开，请重新打开应用。");
                    }
                });
            }
        });
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void showSettings() {
        try {
            SharedPreferences prefs = service.getRemotePreferences("config");
            // Match the original device configuration: unlock only, never auto-click by default.
            if (!prefs.contains("initialized")) {
                boolean saved = prefs.edit().putBoolean("pref_miui_master", true)
                    .putBoolean("pref_force_unlock", true).putBoolean("pref_auto_click", false)
                    .putBoolean("pref_should_notify", false).putBoolean("initialized", true).commit();
                if (!saved) throw new IllegalStateException("初始配置保存失败");
            }
            status.setText("已连接 " + service.getFrameworkName() + " " + service.getFrameworkVersion()
                + " · API " + service.getApiVersion()
                + "\n作用域：手机管家 / 安全中心\n");
            controls.removeAllViews();
            addSwitch(prefs, "pref_miui_master", "启用小米功能");
            addSwitch(prefs, "pref_force_unlock", "解禁倒计时按钮");
            addSwitch(prefs, "pref_auto_click", "自动点下一步和允许");
            addSwitch(prefs, "pref_should_notify", "跳过时提示（自动点击时）");
            TextView note = new TextView(this);
            note.setText("\n只开启「解禁倒计时按钮」时，按钮提前可点，仍需手动确认；倒计时文字可能继续显示。\n\n"
                + "首次启用或更新后，请重启手机管家进程或手机。请勿同时启用同类模块。\n\n"
                + "源码\nhttps://github.com/LidoCKT/No-Wait-10s");
            controls.addView(note);
        } catch (RuntimeException error) {
            status.setText("设置服务错误：" + error.getMessage());
            controls.removeAllViews();
        }
    }

    private void addSwitch(SharedPreferences prefs, String key, String label) {
        Switch control = new Switch(this);
        control.setText(label);
        control.setPadding(0, dp(12), 0, dp(12));
        control.setChecked(prefs.getBoolean(key, false));
        control.setOnCheckedChangeListener((button, checked) -> {
            try {
                if (!prefs.edit().putBoolean(key, checked).commit())
                    throw new IllegalStateException("配置保存失败");
            } catch (RuntimeException error) {
                status.setText("保存失败：" + error.getMessage());
                button.setChecked(prefs.getBoolean(key, false));
            }
        });
        controls.addView(control);
    }
}
