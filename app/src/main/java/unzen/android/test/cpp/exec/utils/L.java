package unzen.android.test.cpp.exec.utils;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Size;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import java.lang.reflect.Field;
import java.util.Locale;

public class L {
    static public final String TAG = "zen.java.";
    static private volatile Analytics sAnalytics;
    static private final boolean DEBUG = true;
    
    public interface Analytics {

        class AnalyticsReportException extends RuntimeException {
            public AnalyticsReportException(String message, Throwable cause) {
                super(message, cause);
            }
            public AnalyticsReportException(String message) {
                super(message);
            }
            public AnalyticsReportException() { }
        }

        void log(int level, String tag, String message);
        void report(Throwable throwable);
        void report(Throwable throwable, boolean throwIfDebug);
        void event(@NonNull @Size(min = 1L, max = 40L) String event);
        void event(@NonNull @Size(min = 1L, max = 40L) String event, String... params);
        /**
         * @param name 1 to 24 alphanumeric characters or underscores,
         *             must start with an alphabetic character.
         * @param val  Up to 36 characters long. Setting to null removes the user property.
         */
        void prop(@NonNull @Size(min = 1L, max = 24L) String name,
                @Nullable @Size(max = 36L) String val);
    }

    public static void vv(String m) {
        if (DEBUG) {
            Log.v(TAG + Thread.currentThread().getName(), m);
        }
    }

    public static void vv(String format, Object... args) {
        if (DEBUG) {
            Log.v(TAG + Thread.currentThread().getName(), String.format(format, args));
        }
    }

    public static void vv(String m, final Throwable th) {
        if (DEBUG) {
            Log.v(TAG + Thread.currentThread().getName(), m, th);
        }
    }

    public static void dd(String m) {
        if (DEBUG) {
            Log.d(TAG + Thread.currentThread().getName(), m);
        }
    }

    public static void dd(String format, Object... args) {
        if (DEBUG) {
            Log.d(TAG + Thread.currentThread().getName(), String.format(format, args));
        }
    }

    public static void dd(String m, final Throwable th) {
        if (DEBUG) {
            Log.d(TAG + Thread.currentThread().getName(), m, th);
        }
    }

    public static void ii(String m) {
        if (DEBUG) {
            Log.i(TAG + Thread.currentThread().getName(), m);
        }
    }

    public static void ii(String format, Object... args) {
        if (DEBUG) {
            Log.i(TAG + Thread.currentThread().getName(), String.format(format, args));
        }
    }

    public static void ii(String m, final Throwable th) {
        if (DEBUG) {
            Log.i(TAG + Thread.currentThread().getName(), m, th);
        }
    }

    public static void ww(String m) {
        if (DEBUG) {
            Log.w(TAG + Thread.currentThread().getName(), m);
        }
    }

    public static void ww(String format, Object... args) {
        if (DEBUG) {
            Log.w(TAG + Thread.currentThread().getName(), String.format(format, args));
        }
    }

    public static void ww(String m, final Throwable th) {
        if (DEBUG) {
            Log.w(TAG + Thread.currentThread().getName(), m, th);
        }
    }

    public static void ee(String m) {
        if (DEBUG) {
            Log.e(TAG + Thread.currentThread().getName(), m);
        }
    }

    public static void ee(String m, Throwable th) {
        if (DEBUG) {
            Log.e(TAG + Thread.currentThread().getName(), m, th);
        }
    }

    public static void ee(String format, Object... args) {
        if (DEBUG) {
            Log.e(TAG + Thread.currentThread().getName(), String.format(format, args));
        }
    }

    public final String NAME;
    public final boolean ENABLED;

    private String addName(String m) {
        return NAME + " " + m;
    }

    public void v(String m) {
        if (ENABLED) {
            L.vv(addName(m));
        }
    }

    public void v(String m, final Throwable th) {
        if (ENABLED) {
            L.vv(addName(m), th);
        }
    }

    public void v(String m, Object... args) {
        if (ENABLED) {
            L.vv(addName(m), args);
        }
    }

    public void d(String m) {
        if (ENABLED) {
            L.dd(addName(m));
        }
    }

    public void d(String m, final Throwable th) {
        if (ENABLED) {
            L.dd(addName(m), th);
        }
    }

    public void d(String m, Object... args) {
        if (ENABLED) {
            L.dd(addName(m), args);
        }
    }

    public void i(String m) {
        if (ENABLED) {
            L.ii(addName(m));
        }
    }

    public void i(String m, final Throwable th) {
        if (ENABLED) {
            L.ii(addName(m), th);
        }
    }

    public void i(String m, Object... args) {
        if (ENABLED) {
            L.ii(addName(m), args);
        }
    }

    public void w(String m) {
        if (ENABLED) {
            L.ww(addName(m));
        }
    }

    public void w(String m, final Throwable th) {
        if (ENABLED) {
            L.ww(addName(m), th);
        }
    }

    public void w(String m, Object... args) {
        if (ENABLED) {
            L.ww(addName(m), args);
        }
    }

    public void e(String m) {
        if (ENABLED) {
            L.ee(addName(m));
        }
    }

    public void e(String m, final Throwable th) {
        if (ENABLED) {
            L.ee(addName(m), th);
        }
    }

    public void e(String m, Object... args) {
        if (ENABLED) {
            L.ee(addName(m), args);
        }
    }

    public L(String name) {
        NAME = name;
        ENABLED = DEBUG;
    }

    public L(String name, boolean enabled) {
        NAME = name;
        ENABLED = enabled;
    }

    static public String tag(Activity activity, int activityId) {
        if (activity == null) {
            return "null";
        }
        String activityClass = activity.getClass().getSimpleName();
        return format("%s/%d/%d", activityClass, activity.getTaskId(), activityId);
    }

    static public String tag(Fragment fragment, int fragmentId) {
        String fragmentClass = fragment.getClass().getSimpleName();
        return format("%s/%d", fragmentClass, fragmentId);
    }

    static public String lifecycle(String path) {
        return format("Lifecycle/%s", path);
    }

    static public void activityLifecycle(Activity activity, int id, String m, boolean prime,
            Object... args) {
        String s = lifecycle(format("A/%s %s", tag(activity, id), format(m, args)));
        if (prime) {
            ii(s);
        } else {
            dd(s);
        }
    }

    static public void fragmentLifecycle(String activityTag, String tag, String m, boolean prime) {
        String s = lifecycle(format("F/%s/%s %s", activityTag, tag, m));
        if (prime) {
            ii(s);
        } else {
            dd(s);
        }
    }

    static public void dialogLifecycle(String activityTag, String tag, String m, boolean prime) {
        String s = lifecycle(format("D/%s/%s %s", activityTag, tag, m));
        if (prime) {
            ii(s);
        } else {
            dd(s);
        }
    }

    static public String format(String format, Object... args) {
        return String.format(Locale.US, format, args);
    }

    static public String toLowerCase(Object object) {
        if (object == null) {
            return "null";
        }
        return object.toString().toLowerCase(Locale.US);
    }

    static public void setAnalyticsReporter(Analytics reporter) {
        sAnalytics = reporter;
    }

    public void logv(String message) {
        if (sAnalytics != null) {
            sAnalytics.log(Log.VERBOSE, TAG, addName(message));
        }
    }

    public void logd(String message) {
        if (sAnalytics != null) {
            sAnalytics.log(Log.DEBUG, TAG, addName(message));
        }
    }

    public void logi(String message) {
        if (sAnalytics != null) {
            sAnalytics.log(Log.INFO, TAG, addName(message));
        }
    }

    public void logw(String message) {
        if (sAnalytics != null) {
            sAnalytics.log(Log.WARN, TAG, addName(message));
        }
    }

    public void loge(String message) {
        if (sAnalytics != null) {
            sAnalytics.log(Log.ERROR, TAG, addName(message));
        }
    }

    static public void ignore(Throwable ignore) {
        // Marker-method for suppressing warnings
        ignore(ignore, true);
    }

    static public void ignore(Throwable ignore, boolean throwIfDebug) {
        // Marker-method for suppressing warnings
        if (DEBUG && throwIfDebug) {
            throw new IllegalStateException(ignore);
        }
    }

    static public void report(Throwable throwable) {
        if (sAnalytics != null) {
            sAnalytics.report(throwable);
        }
    }

    static public void report(Throwable throwable, boolean throwIfDebug) {
        if (sAnalytics != null) {
            sAnalytics.report(throwable, throwIfDebug);
        }
    }

    static public void event(@NonNull @Size(min = 1L, max = 40L) String event) {
        if (sAnalytics != null) {
            sAnalytics.event(event);
        }
    }

    static public void event(@NonNull @Size(min = 1L, max = 40L) String event, String... params) {
        if (sAnalytics != null) {
            sAnalytics.event(event, params);
        }
    }

    static public void prop(@NonNull @Size(min = 1L, max = 24L) String name,
            @Nullable @Size(max = 36L) String val) {
        if (sAnalytics != null) {
            sAnalytics.prop(name, val);
        }
    }

    @Nullable static public String decodedUri(@Nullable Uri uri) {
        if (uri == null) {
            return null;
        }
        String string = uri.getScheme() + ":" + uri.getSchemeSpecificPart();
        if (uri.getFragment() != null) {
            string += "#" + uri.getFragment();
        }
        return string;
    }

    static public void printIntentFlags(int flags, String tag) {
        Field[] declaredFields = Intent.class.getDeclaredFields();
        for (Field field : declaredFields) {
            if (!field.getName().startsWith("FLAG_")) {
                continue;
            }
            if (field.getName().startsWith("FLAG_RECEIVER_")) {
                continue;
            }
            try {
                int flag = field.getInt(null);
                if ((flags & flag) != 0) {
                    L.dd(tag + " " + field.getName());
                }
            } catch (IllegalArgumentException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    static public void printIntent(Activity activity, Intent intent, String tag) {
        L.dd(tag + " action=" + intent.getAction());
        L.dd(tag + " categories=" + intent.getCategories());
        L.dd(tag + " type=" + intent.getType());
        L.dd(tag + " data=" + intent.getDataString());
        L.dd(tag + " flags=" + intent.getFlags());
        printIntentFlags(intent.getFlags(), tag);
        L.dd(tag + " referrer=" + ActivityCompat.getReferrer(activity));
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            for (String key : bundle.keySet()) {
                L.dd(tag + " extra: " + key + "=" + bundle.get(key));
            }
        }
    }
}