package unzen.android.test.cpp.exec;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import unzen.android.test.cpp.exec.cppmodule.CppModule;

public class MainActivity extends Activity {

    static private final String FOO_NAME = "jnifoo";
    static private final String FOO = "lib" + FOO_NAME + ".so";
    static private final String BAR = "execbar";
    static private final String BAZ = "execbaz";
    static private Set<String> BARBAZ = new HashSet<>(Arrays.asList(BAR, BAZ));

    static private class Report {

        public final String name;
        public final Map<String, Integer> abisToVers;
        public final long totalSize;
        public final int verFromOutput;

        public boolean versInSync(int version) {
            for (Integer v : abisToVers.values()) {
                if (version != v) {
                    return false;
                }
            }
            return true;
        }

        public String header() {
            return format("%s v%d, %d B", name, verFromOutput, totalSize);
        }

        private String shortenAbisNames(String s) {
            return s.replace("x86_64", "x64").replace("x86", "x32")
                    .replace("armeabi-v7a", "a32").replace("arm64-v8a", "a64");
        }

        public String body() {
            StringBuilder result = new StringBuilder();
            for (Map.Entry<String, Integer> entry : abisToVers.entrySet()) {
                if (result.length() > 0) {
                    result.append(", ");
                }
                result.append(entry.getKey()).append(" ").append("v").append(entry.getValue());
            }
            return shortenAbisNames(result.toString());
        }

        @Override
        public String toString() {
            return header() + "\n" + body();
        }

        private Report(String name, Map<String, Integer> abisToVers, long size, int verFromOutput) {
            this.name = name;
            this.abisToVers = abisToVers;
            this.totalSize = size;
            this.verFromOutput = verFromOutput;
        }

        private Report(String name) {
            this.name = name;
            this.abisToVers = Collections.emptyMap();
            this.totalSize = 0;
            this.verFromOutput = 0;
        }
    }

    static private String format(String format, Object... args) {
        return String.format(Locale.US, format, args);
    }

    // Suppress warnings for Gradle build output
    @SuppressWarnings("deprecation")
    static private String[] getSupportedAbis() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            return Build.SUPPORTED_ABIS;
        } else {
            return new String[]{Build.CPU_ABI, Build.CPU_ABI2};
        }
    }

    static private String getExecOutput(File dir, String abi, String name) throws IOException {
        File exec = new File(dir.getAbsolutePath() + "/" + abi + "/" + name);
        if (!exec.setExecutable(true)) {
            throw new IllegalStateException();
        }
        ProcessBuilder builder = new ProcessBuilder(exec.getAbsolutePath());
        Process process = builder.start();
        StringBuilder sb = new StringBuilder();
        try (InputStream stream = process.getInputStream()) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            String line;
            while ((line = reader.readLine()) != null) {
                if (sb.length() > 0) {
                    sb.append("\n");
                }
                sb.append(line);
            }
        }
        return sb.toString();
    }

    static private void checkOutput(String elfName, String actualOut) {
        String expected = format("I'm %s! UNZEN-VERSION-", elfName);
        if (!actualOut.startsWith(expected)) {
            String message = format("Expected: %s, actual: %s", expected, actualOut);
            throw new IllegalStateException(message);
        }
    }

    static private int parseVerFromOutput(String output) {
        return Integer.parseInt(output.substring(output.lastIndexOf("-") + 1));
    }

    static private int parseVerFromFile(File file) throws IOException {
        try (InputStream stream = new FileInputStream(file)) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            // Search for string UNZEN-VERSION-XXXX in ELF file
            char[] buf = new char[17];
            int c;
            while ((c = reader.read()) != -1) {
                if (c == 'U') {
                    reader.mark(17);
                    int readed = reader.read(buf);
                    if (readed == -1) {
                        break;
                    } else if (readed == 17) {
                        String ver = new String(buf);
                        if (ver.startsWith("NZEN-VERSION-")) {
                            return Integer.parseInt(ver.substring(ver.lastIndexOf("-") + 1));
                        }
                    }
                    reader.reset();
                }
            }
        }
        return 0;
    }

    static private Report getJniReport(File apkDir) throws IOException {
        Map<String, Integer> abisToVers = new HashMap<>();
        long totalSize = 0;
        for (File abiDir : new File(apkDir, "lib").listFiles()) {
            File[] files = abiDir.listFiles();
            if (files.length != 1 || !files[0].getName().equals(FOO)) {
                throw new IllegalStateException();
            }
            abisToVers.put(abiDir.getName(), parseVerFromFile(files[0]));
            totalSize += files[0].length();
        }
        String output = CppModule.getStringFromJni();
        checkOutput(FOO, output);
        return new Report(FOO, abisToVers, totalSize, parseVerFromOutput(output));
    }

    static private Report getExecReport(File assetsDir) throws IOException {
        String name = "execbarbaz";
        File dir = new File(assetsDir, name);
        if (!dir.exists()) {
            return new Report(name);
        }
        Map<String, Integer> abisToVers = new HashMap<>();
        long totalSize = 0;
        for (File abiDir : dir.listFiles()) {
            Set<String> names = new HashSet<>(Arrays.asList(abiDir.list()));
            if (!BARBAZ.equals(names)) {
                throw new IllegalStateException(names.toString());
            }
            File bar = new File(abiDir, BAR);
            File baz = new File(abiDir, BAZ);
            int barVer = parseVerFromFile(bar);
            int bazVer = parseVerFromFile(baz);
            if (barVer != bazVer) {
                throw new IllegalStateException();
            }
            abisToVers.put(abiDir.getName(), barVer);
            totalSize += bar.length();
            totalSize += baz.length();
        }
        int verFromOutput = -1;
        for (String abi : getSupportedAbis()) {
            if (!abisToVers.containsKey(abi)) {
                continue;
            }
            String barOut = getExecOutput(dir, abi, BAR);
            checkOutput(BAR, barOut);
            int barVer = parseVerFromOutput(barOut);
            String bazOut = getExecOutput(dir, abi, BAZ);
            checkOutput(BAZ, bazOut);
            int bazVer = parseVerFromOutput(barOut);
            if (barVer != bazVer) {
                throw new IllegalStateException(format("%d != %d", barVer, bazVer));
            }
            verFromOutput = barVer;
            break;
        }
        if (verFromOutput == -1) {
            throw new IllegalStateException();
        }
        return new Report(name, abisToVers, totalSize, verFromOutput);
    }

    private TextView textView;
    private final ArrayList<String> messages = new ArrayList<>();
    private final ArrayList<String> warns = new ArrayList<>();

    private void message(String m) {
        messages.add(m);
    }

    private void message(String format, Object... args) {
        message(format(format, args));
    }

    private void warn(String m) {
        warns.add(m);
    }

    private void warn(String format, Object... args) {
        warn(format(format, args));
    }

    @SuppressWarnings("ConstantConditions")
    private void displayReport() throws Throwable {
        File apkDir = new File(getCacheDir(), "unzen-apk");
        FileUtils.deleteDirectory(apkDir);
        if (apkDir.exists() || !apkDir.mkdirs()) {
            throw new IllegalStateException();
        }
        ZipUtils.extract(new File(getPackageResourcePath()), apkDir);
        Report jniReport = getJniReport(apkDir);
        File assetsDir = new File(apkDir, "assets");
        Report execReport = getExecReport(assetsDir);

        File dummy = new File(assetsDir, "dummy.txt");
        if (!dummy.exists() || dummy.length() == 0) {
            throw new IllegalStateException();
        }
        File dummyLib = new File(assetsDir, "dummy-lib.txt");
        if (!dummyLib.exists() || dummyLib.length() == 0) {
            throw new IllegalStateException();
        }

        boolean error = !jniReport.abisToVers.equals(execReport.abisToVers);
        error = error || jniReport.verFromOutput != BuildConfig.VERSION_CODE_BASE;
        error = error || execReport.verFromOutput != BuildConfig.VERSION_CODE_BASE;
        if (BuildConfig.FLAVOR.equals("fat")) {
            error = error || !Arrays.asList(1, 2, 3, 4).contains(jniReport.abisToVers.size());
            error = error || BuildConfig.VERSION_CODE != BuildConfig.VERSION_CODE_BASE;
        } else {
            error = error || jniReport.abisToVers.size() != 1;
            if (BuildConfig.FLAVOR.equals("a32")) {
                error = error || BuildConfig.VERSION_CODE != BuildConfig.VERSION_CODE_BASE + 1;
            } else if (BuildConfig.FLAVOR.equals("a64")) {
                error = error || BuildConfig.VERSION_CODE != BuildConfig.VERSION_CODE_BASE + 2;
            } else if (BuildConfig.FLAVOR.equals("x32")) {
                error = error || BuildConfig.VERSION_CODE != BuildConfig.VERSION_CODE_BASE + 3;
            } else if (BuildConfig.FLAVOR.equals("x64")) {
                error = error || BuildConfig.VERSION_CODE != BuildConfig.VERSION_CODE_BASE + 4;
            }
        }

        if (!jniReport.versInSync(BuildConfig.VERSION_CODE_BASE)) {
            warn("Versions between ABIs doesn't match. That's may be due to build performed by"
                    + " Android Studio's \"Run\" action, that makes new build only for ABI of"
                    + " the \"Run\" target's device.");
        }
        if (BuildConfig.FLAVOR.equals("fat")) {
            if (jniReport.abisToVers.size() != 4) {
                warn("Flavor \"fat\" has only %d ABIs, expected 4 ABIs. That's may be due to"
                        + " build performed by Android Studio's \"Run\" action, that makes"
                        + " new build only for ABI of the \"Run\" target's device.",
                        jniReport.abisToVers.size());
            }
        }

        Report[] reports = {jniReport, execReport};
        message("Java v%s", BuildConfig.VERSION_NAME);
        message("Cpp v%d", BuildConfig.VERSION_CODE_BASE);
        for (Report report : reports) {
            message("\n");
            message(report.toString());
        }
        String text = TextUtils.join("\n", messages);
        if (!error && !warns.isEmpty()) {
            text = format("%s%n%n%nWARNINGS%n%n%s", text, TextUtils.join("\n\n", warns));
        }
        textView.setText(text);
        if (error) {
            textView.setTextColor(0xffff0000);
        } else if (!warns.isEmpty()) {
            textView.setTextColor(0xfffc940a);
        } else {
            textView.setTextColor(0xff00ff55);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = findViewById(R.id.main_text);
        try {
            displayReport();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
