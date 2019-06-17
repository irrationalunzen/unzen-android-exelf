package unzen.android.test.cpp.exec.utils;

import android.content.Context;
import android.os.Build;
import android.text.format.Formatter;

import java.io.Closeable;
import java.io.IOException;
import java.text.Collator;
import java.util.Collection;
import java.util.Locale;
import java.util.regex.Pattern;

public class ZenUtils {

    static public boolean runtimeExec(String[] cmd) throws IOException {
        try {
            Process p = Runtime.getRuntime().exec(cmd);
            int res = p.waitFor();
            return res == 0;
        } catch (InterruptedException e) {
            return false;
        }
    }

    public static String humanReadableByteCount(long bytes, boolean si) {
        int unit = si ? 1000 : 1024;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp-1) + (si ? "" : "i");
        return String.format(Locale.getDefault(), "%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }

    static public String formatFileSize(Context context, long fileSize) {
        return Formatter.formatShortFileSize(context, fileSize);
        /*
         * Format file size 1000: 1 KB
         * Format file size 1499: 1 KB
         * Format file size 1500: 2 KB
         * Format file size 1501: 2 KB
         * Format file size 499999: 500 KB
         * Format file size 500000: 1 MB
         * Format file size 500001: 1 MB
         * Format file size 999999: 1 MB
         * Format file size 1000000: 1 MB
         * Format file size 1000001: 1 MB

        if (kbAbbrev == null || mbAbbrev == null) {
            kbAbbrev = ZenApp.getStr(R.string.kilobyte_abbreviation);
            mbAbbrev = ZenApp.getStr(R.string.megabyte_abbreviation);
        }
        if (fileSize >= 500000) {
            return Math.round(fileSize / 1000000F) + " MB";
        } else if (fileSize >= 1000) {
            return Math.round(fileSize / 1000F) + " KB";
        } else {
            return "1 KB";
        }
        */
    }

    static public float bytesToMebibytes(long bytes) {
        return bytes / 1048576f;
    }

    /**
     * 1 MB = 1048576 B = 1024^2
     */
    static public String bytesToMegabytes(long bytes, int presign) {
        float megabytes = bytes / 1048576f;
        // return new DecimalFormat("#.##").format(megabytes);
        return String.format("%." + presign + "f", megabytes);
    }

    static public String bytesToKilobytes(long bytes, int presign) {
        float kilobytes = bytes / 1024f;
        // return new DecimalFormat("#.##").format(kilobytes);
        return String.format("%." + presign + "f", kilobytes);
    }

    public static void closeTalkative(Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    @SuppressWarnings("deprecation")
    public static String[] getSupportedAbis() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            return Build.SUPPORTED_ABIS;
        } else {
            return new String[]{Build.CPU_ABI, Build.CPU_ABI2};
        }
    }

    static private final Pattern sSplitWhitespaces = Pattern.compile("\\s+");

    static public String[] splitByWhitespaces(String s) {
        return sSplitWhitespaces.split(s);
    }

    public static Long parseLongOrNull(String s) {
        if (isEmpty(s)) {
            return null;
        }
        try {
            return Long.parseLong(s);
        } catch (Throwable e) {
            return null;
        }
    }

    static public String stripPrefix(String s, String prefix) {
        if (isEmpty(s) || isEmpty(prefix)) {
            return s;
        }
        if (s.startsWith(prefix)) {
            return s.substring(prefix.length());
        }
        return s;
    }

    static public String stripSuffix(String s, String suffix) {
        if (isEmpty(s) || isEmpty(suffix) ) {
            return s;
        }
        if (s.endsWith(suffix)) {
            return s.substring(0, s.length() - suffix.length());
        }
        return s;
    }

    static public String getDigitsPrefix(String s) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (!Character.isDigit(c)) {
                break;
            }
            builder.append(c);
        }
        return builder.toString();
    }

    static public String getDigitsSuffix(String s) {
        StringBuilder builder = new StringBuilder();
        for (int i = s.length() - 1; i >= 0; i--) {
            char c = s.charAt(i);
            if (!Character.isDigit(c)) {
                break;
            }
            builder.append(c);
        }
        return builder.reverse().toString();
    }

    static private final char[] sHexArray = "0123456789abcdef".toCharArray();

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = sHexArray[v >>> 4];
            hexChars[j * 2 + 1] = sHexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    static public int firstNonZero(int... args) {
        for (int arg : args) {
            if (arg != 0) {
                return arg;
            }
        }
        return 0;
    }

    public static int compareNullLesser(Long x, Long y) {
        if (x == null && y == null) {
            return 0;
        }
        if (x == null) {
            return -1;
        }
        if (y == null) {
            return 1;
        }
        return Long.compare(x, y);
    }

    public static int compareNullGreater(Long x, Long y) {
        if (x == null && y == null) {
            return 0;
        }
        if (x == null) {
            return 1;
        }
        if (y == null) {
            return -1;
        }
        return Long.compare(x, y);
    }

    public static int compare(long lhs, long rhs) {
        return lhs < rhs ? -1 : (lhs == rhs ? 0 : 1);
    }

    public static int compare(int lhs, int rhs) {
        return lhs < rhs ? -1 : (lhs == rhs ? 0 : 1);
    }

    static public int compare(Collator collator, String source, String target) {
        if (source == null && target == null) {
            return 0;
        }
        if (source == null) {
            return 1;
        }
        if (target == null) {
            return -1;
        }
        return collator.compare(source, target);
    }

    public static boolean equals(Object a, Object b) {
        return (a == b) || (a != null && a.equals(b));
    }

    static public int compare(String str1, String str2) {
        if (str1 == null && str2 == null) {
            return 0;
        }
        if (str1 == null) {
            return 1;
        }
        if (str2 == null) {
            return -1;
        }
        return str1.compareTo(str2);
    }

    /**
     * Null safe
     */
    static public String trimOrNull(String string) {
        if (string == null) {
            return null;
        }
        string = string.trim();
        if (string.length() == 0) {
            return null;
        }
        return string;
    }

    static public int length(String s) {
        return s != null ? s.length() : 0;
    }

    static public boolean isEmpty(String s) {
        return length(s) == 0;
    }

    static public boolean isNotEmpty(String s) {
        return length(s) > 0;
    }

    static public int length(Collection<?> c) {
        return c != null ? c.size() : 0;
    }

    static public boolean isEmpty(Collection<?> c) {
        return c == null || c.isEmpty();
    }

    static public boolean isNotEmpty(Collection<?> c) {
        return c != null && !c.isEmpty();
    }
}