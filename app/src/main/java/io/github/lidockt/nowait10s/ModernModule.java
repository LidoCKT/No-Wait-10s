package io.github.lidockt.nowait10s;

import android.content.SharedPreferences;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.regex.Pattern;
import io.github.libxposed.api.XposedModule;

public final class ModernModule extends XposedModule {
    private static final String TAG = "NoWait10s";
    private static final Pattern COUNTDOWN = Pattern.compile(
        ".*?[\\p{L}\\p{N}]+\\s*[（(]\\s*[0-9]+\\s*[）)].*", Pattern.DOTALL);
    private SharedPreferences prefs;
    private boolean installed;
    private long lastNotified;
    private final ThreadLocal<Boolean> processing = ThreadLocal.withInitial(() -> false);
    private final Set<TextView> clicked = Collections.newSetFromMap(new WeakHashMap<>());
    private int matchLogs;

    @Override public void onModuleLoaded(ModuleLoadedParam param) {
        log(Log.INFO, TAG, "API 102 module loaded in " + param.getProcessName());
    }

    @Override public void onPackageReady(PackageReadyParam param) {
        if (installed || !"com.miui.securitycenter".equals(param.getPackageName())) return;
        // Install even if the master switch is off, so remote changes need no hook reinstall.
        prefs = getRemotePreferences("config");
        try {
            Method setText = TextView.class.getDeclaredMethod("setText", CharSequence.class,
                TextView.BufferType.class, boolean.class, int.class);
            Method setEnabled = TextView.class.getDeclaredMethod("setEnabled", boolean.class);
            hook(setText).setId("countdown-text").intercept(chain -> {
                Object result = chain.proceed();
                process((TextView) chain.getThisObject());
                return result;
            });
            hook(setEnabled).setId("countdown-enabled").intercept(chain -> {
                Object result = chain.proceed();
                if (Boolean.FALSE.equals(chain.getArg(0))) process((TextView) chain.getThisObject());
                return result;
            });
            // Process after attachment as constructor/setText may precede OnClickListener setup.
            hook(View.class.getDeclaredMethod("onAttachedToWindow")).setId("countdown-attached")
                .intercept(chain -> {
                    Object result = chain.proceed();
                    if (chain.getThisObject() instanceof TextView)
                        process((TextView) chain.getThisObject());
                    return result;
                });
            installed = true;
            log(Log.INFO, TAG, "Installed 3 modern hooks in " + param.getPackageName());
        } catch (ReflectiveOperationException error) {
            log(Log.ERROR, TAG, "Unsupported TextView methods", error);
        }
    }

    private void process(TextView view) {
        if (view == null || processing.get() || prefs == null
            || !prefs.getBoolean("initialized", false)
            || !prefs.getBoolean("pref_miui_master", false)) return;
        String text = String.valueOf(view.getText());
        if (text.contains("拒绝") || text.contains("Deny") || text.contains("No") || text.contains("Reject")) return;
        // Avoid the original's broad "any disabled text containing a digit" rule.
        // Current HyperOS has a stable resource name for the positive countdown button.
        String id = "";
        try { id = view.getResources().getResourceEntryName(view.getId()); }
        catch (android.content.res.Resources.NotFoundException ignored) { }
        boolean knownButton = id.equals("intercept_warn_allow");
        boolean countdown = COUNTDOWN.matcher(text).matches();
        if (!knownButton && !(view instanceof android.widget.Button && countdown)) return;
        processing.set(true);
        try {
            if (prefs.getBoolean("pref_force_unlock", false)) view.setEnabled(true);
            if (matchLogs < 5) {
                log(Log.INFO, TAG, "Matched countdown button id=" + id + " enabled=" + view.isEnabled());
                matchLogs++;
            }
            if (prefs.getBoolean("pref_auto_click", false) && view.isAttachedToWindow()
                && view.isShown() && view.isEnabled() && view.hasOnClickListeners()
                && !clicked.contains(view)) {
                clicked.add(view);
                if (!view.performClick()) clicked.remove(view);
                else if (prefs.getBoolean("pref_should_notify", false)
                    && SystemClock.elapsedRealtime() - lastNotified > 5000) {
                    Toast.makeText(view.getContext(), "SKIP", Toast.LENGTH_SHORT).show();
                    lastNotified = SystemClock.elapsedRealtime();
                }
            }
        } finally {
            processing.set(false);
        }
    }
}
