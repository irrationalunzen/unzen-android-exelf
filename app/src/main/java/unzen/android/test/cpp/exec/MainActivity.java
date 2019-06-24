package unzen.android.test.cpp.exec;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import net.lingala.zip4j.core.ZipFile;

import org.apache.commons.io.FileUtils;

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
import java.util.Map;
import java.util.Set;

import unzen.android.test.cpp.exec.cppmodule.CppModule;
import unzen.android.test.cpp.exec.utils.ZenUtils;

import static unzen.android.test.cpp.exec.utils.L.format;

public class MainActivity extends AppCompatActivity {

    static private final String FOO_NAME = "jnifoo";
    static private final String FOO = "lib" + FOO_NAME + ".so";
    static private final String BAR = "execbar";
    static private final String BAZ = "execbaz";
    static private Set<String> BARBAZ = new HashSet<>(Arrays.asList(BAR, BAZ));

    static private class Report {

        public final @NonNull String name;
        public final @NonNull Map<String, Integer> abisToVers;
        public final long totalSize;

        public String header() {
            return format("%s %d", name, totalSize);
        }

        public String body() {
            return format("%s%n%s", abisString(), TextUtils.join(" ", abisToVers.values()));
        }

        public String abisString() {
            String text = TextUtils.join(" ", abisToVers.keySet());
            return text.replace("x86_64", "x64").replace("x86", "x32")
                    .replace("armeabi-v7a", "a32").replace("arm64-v8a", "a64");
        }

        public boolean isEmpty() {
            return abisToVers.isEmpty();
        }

        public boolean versInSync(int version) {
            for (Integer v : abisToVers.values()) {
                if (version != v) {
                    return false;
                }
            }
            return true;
        }

        private Report(@NonNull String name, @NonNull Map<String, Integer> abisToVers, long size) {
            this.name = name;
            this.abisToVers = abisToVers;
            this.totalSize = size;
        }

        private Report(@NonNull String name) {
            this.name = name;
            this.abisToVers = Collections.emptyMap();
            this.totalSize = 0;
        }
    }

    static private String getExecOutput(String path) throws IOException {
        ProcessBuilder builder = new ProcessBuilder(path);
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

    static private String getExecOutput(File dir, String abi, String name) throws IOException {
        File exec = new File(dir.getAbsolutePath() + "/" + abi + "/" + name);
        if (!exec.setExecutable(true)) {
            throw new IllegalStateException();
        }
        return getExecOutput(exec.getAbsolutePath());
    }

    static private void checkOutput(String elfName, String actualOut) {
        String expected = format("I'm %s! UNZEN-VERSION-", elfName);
        if (!actualOut.startsWith(expected)) {
            String message = format("Expected: %s, actual: %s", expected, actualOut);
            throw new IllegalStateException(message);
        }
    }

    static private int parseVersion(File file) throws IOException {
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
            abisToVers.put(abiDir.getName(), parseVersion(files[0]));
            totalSize += files[0].length();
        }
        checkOutput(FOO, CppModule.getStringFromJni());
        return new Report(FOO, abisToVers, totalSize);
    }

    static private Report getExecReport(File assetsDir, String name) throws IOException {
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
            int barVer = parseVersion(bar);
            int bazVer = parseVersion(baz);
            if (barVer != bazVer) {
                throw new IllegalStateException();
            }
            abisToVers.put(abiDir.getName(), barVer);
            totalSize += bar.length();
            totalSize += baz.length();
        }
        boolean outputChecked = false;
        for (String abi : ZenUtils.getSupportedAbis()) {
            if (!abisToVers.keySet().contains(abi)) {
                continue;
            }
            outputChecked = true;
            for (String exec : BARBAZ) {
                checkOutput(exec, getExecOutput(dir, abi, exec));
            }
            break;
        }
        if (!outputChecked) {
            throw new IllegalStateException();
        }
        return new Report(name, abisToVers, totalSize);
    }

    private final ArrayList<String> messages = new ArrayList<>();
    private final ArrayList<String> errors = new ArrayList<>();

    private void message(String m) {
        messages.add(m);
    }

    private void error(String e) {
        errors.add(e);
    }

    private void generateReport() throws Throwable {
        File apkDir = new File(getCacheDir(), "unzen-apk");
        FileUtils.deleteDirectory(apkDir);
        if (apkDir.exists() || !apkDir.mkdirs()) {
            throw new IllegalStateException();
        }
        ZipFile apkZip = new ZipFile(new File(getPackageResourcePath()));
        apkZip.extractAll(apkDir.getAbsolutePath());
        Report jniReport = getJniReport(apkDir);
        File assetsDir = new File(apkDir, "assets");
        Report strippedReport = getExecReport(assetsDir, "exec-stripped");
        Report notstripReport = getExecReport(assetsDir, "exec-notstrip");

        File dummy = new File(assetsDir, "dummy.txt");
        if (!dummy.exists() || dummy.length() == 0) {
            throw new IllegalStateException();
        }

        if (strippedReport.totalSize > 0 && notstripReport.totalSize > 0) {
            if (strippedReport.totalSize >= notstripReport.totalSize) {
                error("strippedReport.totalSize >= notstripReport.totalSize");
            }
        }

        boolean inSync = !jniReport.isEmpty() && jniReport.versInSync(BuildConfig.VERSION_CODE);
        if (!strippedReport.isEmpty() && !notstripReport.isEmpty()) {
            inSync = inSync && strippedReport.abisToVers.equals(notstripReport.abisToVers);
        }
        if (!strippedReport.isEmpty()) {
            inSync = inSync && jniReport.abisToVers.equals(strippedReport.abisToVers);
        }
        if (!notstripReport.isEmpty()) {
            inSync = inSync && jniReport.abisToVers.equals(notstripReport.abisToVers);
        }

        Report[] reports = {jniReport, strippedReport, notstripReport};
        message(String.valueOf(BuildConfig.VERSION_CODE));
        message("\n");
        if (inSync) {
            message(jniReport.abisString());
            message("\n");
        }
        for (Report report : reports) {
            message(report.header());
            if (!inSync) {
                message(report.body());
                message("\n");
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        TextView textView = findViewById(R.id.main_text);
        try {
            generateReport();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        String errorsText = TextUtils.join("\n", errors);
        String messagesText = TextUtils.join("\n", messages);
        textView.setText(format("%s%n%n%s", errorsText, messagesText));
    }
}
