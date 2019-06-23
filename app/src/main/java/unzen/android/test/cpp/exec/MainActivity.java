package unzen.android.test.cpp.exec;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import net.lingala.zip4j.core.ZipFile;

import org.apache.commons.io.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import unzen.android.test.cpp.exec.cppmodule.CppModule;
import unzen.android.test.cpp.exec.utils.ZenUtils;

import static android.content.pm.PackageManager.GET_SHARED_LIBRARY_FILES;
import static unzen.android.test.cpp.exec.utils.L.format;

public class MainActivity extends AppCompatActivity {

    static private final String FOO_NAME = "jnifoo";
    static private final String FOO = "lib" + FOO_NAME + ".so";
    static private final String BAR = "execbar";
    static private final String BAZ = "execbaz";
    static private Set<String> BARBAZ = new HashSet<>(Arrays.asList(BAR, BAZ));

    static private class Report {

        public final @NonNull String name;
        public final @NonNull Set<String> abis;
        public final @NonNull String abisString;
        public final long totalSize;

        private Report(@NonNull String name, @NonNull Set<String> abis, long totalSize) {
            this.name = name;
            this.abis = abis;
            this.totalSize = totalSize;
            this.abisString = TextUtils.join(", ", abis);
        }

        private Report(@NonNull String name) {
            this.name = name;
            this.abis = Collections.emptySet();
            this.totalSize = 0;
            this.abisString = "";
        }
    }

    private String getExecOutput(String path) throws IOException {
        ProcessBuilder builder = new ProcessBuilder(path);
        Process process = builder.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            if (sb.length() > 0) {
                sb.append("\n");
            }
            sb.append(line);
        }
        return sb.toString();
    }

    private String getExecOutput(File dir, String abi, String name) throws IOException {
        File exec = new File(dir.getAbsolutePath() + "/" + abi + "/" + name);
        if (!exec.setExecutable(true)) {
            throw new IllegalStateException();
        }
        return getExecOutput(exec.getAbsolutePath());
    }

    private void checkOutput(String elfName, String actualOut) {
        String expected = format("I'm %s!", elfName);
        if (!expected.equals(actualOut)) {
            String message = format("Expected: %s, actual: %s", expected, actualOut);
            throw new IllegalStateException(message);
        }
    }

    private Report getJniReport(File apkDir) {
        Set<String> abis = new HashSet<>();
        long totalSize = 0;
        for (File abiDir : new File(apkDir, "lib").listFiles()) {
            abis.add(abiDir.getName());
            File[] files = abiDir.listFiles();
            if (files.length != 1 || !files[0].getName().equals(FOO)) {
                throw new IllegalStateException();
            }
            totalSize += files[0].length();
        }
        checkOutput(FOO, CppModule.getStringFromJni());
        return new Report(FOO, abis, totalSize);
    }

    private Report getExecReport(File assetsDir, String name) throws IOException {
        File dir = new File(assetsDir, name);
        if (!dir.exists()) {
            return new Report(name);
        }
        Set<String> abis = new HashSet<>();
        long totalSize = 0;
        for (File abiDir : dir.listFiles()) {
            abis.add(abiDir.getName());
            Set<String> names = new HashSet<>(Arrays.asList(abiDir.list()));
            if (!BARBAZ.equals(names)) {
                throw new IllegalStateException(names.toString());
            }
            totalSize += new File(abiDir, BAR).length();
            totalSize += new File(abiDir, BAZ).length();
        }
        boolean outputChecked = false;
        for (String abi : ZenUtils.getSupportedAbis()) {
            if (!abis.contains(abi)) {
                continue;
            }
            outputChecked = true;
            for (String exec : BARBAZ) {
                String output = getExecOutput(dir, abi, exec);
                checkOutput(exec, output);
            }
        }
        if (!outputChecked) {
            throw new IllegalStateException();
        }
        return new Report(name, abis, totalSize);
    }

    private String generateReport() throws Throwable {
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
        Report unstrippedReport = getExecReport(assetsDir, "exec-unstripped");

        boolean abiMatch = true;
        if (!strippedReport.abis.isEmpty() && !unstrippedReport.abis.isEmpty()) {
            abiMatch = strippedReport.abis.equals(unstrippedReport.abis);
        }
        if (!strippedReport.abis.isEmpty()) {
            abiMatch = abiMatch && jniReport.abis.equals(strippedReport.abis);
        }
        if (!unstrippedReport.abis.isEmpty()) {
            abiMatch = abiMatch && jniReport.abis.equals(unstrippedReport.abis);
        }

        Report[] reports = {jniReport, strippedReport, unstrippedReport};
        ArrayList<String> result = new ArrayList<>();
        if (abiMatch) {
            result.add(jniReport.abisString);
            result.add("\n");
        }
        for (Report report : reports) {
            if (abiMatch) {
                result.add(format("%s: %d", report.name, report.totalSize));
            } else {
                result.add(format("%s: %d%n%s%n",
                        report.name, report.totalSize, report.abisString));
            }
        }
        return TextUtils.join("\n", result);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        TextView textView = findViewById(R.id.main_text);
        try {
            textView.setText(generateReport());
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private void checkSharedLibraryFiles(ArrayList<String> report) throws Throwable {
        PackageManager pm = getPackageManager();
        ApplicationInfo info = pm.getApplicationInfo(getPackageName(), GET_SHARED_LIBRARY_FILES);
        if (info.sharedLibraryFiles != null) {
            report.add("sharedLibraryFiles: " + TextUtils.join(", ", info.sharedLibraryFiles));
        }
        File nativeLibraryDir = new File(info.nativeLibraryDir);
        String[] libsNames = nativeLibraryDir.list();
        if (libsNames.length != 1 || !libsNames[0].equals(FOO)) {
            //throw new IllegalStateException(String.valueOf(libsNames.length));
        }
        report.add(format("nativeLibraryDir: %s %d", nativeLibraryDir.getName(), nativeLibraryDir));
    }
}
