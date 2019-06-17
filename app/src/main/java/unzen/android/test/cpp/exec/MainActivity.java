package unzen.android.test.cpp.exec;

import android.content.res.AssetManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import net.lingala.zip4j.core.ZipFile;

import org.apache.commons.io.FileUtils;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import unzen.android.test.cpp.exec.cppmodule.CppModule;
import unzen.android.test.cpp.exec.utils.L;
import unzen.android.test.cpp.exec.utils.ZenUtils;

public class MainActivity extends AppCompatActivity {

    static private boolean isEmpty(String[] array) {
        if (array == null) {
            return true;
        }
        return array.length == 0;
    }

    private String getExecOut(String path) throws IOException {
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

    private void install(String name, File dst, String assetsPath) throws IOException {
        AssetManager assets = getAssets();
        if (isEmpty(assets.list(assetsPath))) {
            return;
        }
        String path = dst.getAbsolutePath();
        try (InputStream stream = new BufferedInputStream(assets.open(assetsPath + "/" + name))) {
            FileUtils.copyInputStreamToFile(stream, dst);
            if (!ZenUtils.runtimeExec(new String[] {"chmod", "775", path})) {
                throw new IllegalStateException("chmod error for: " + path);
            }
        }
    }

    private String generateReport() throws Throwable {
        File apkDir = new File(getCacheDir(), "unzen-apk");
        FileUtils.deleteDirectory(apkDir);
        if (apkDir.exists() || !apkDir.mkdirs()) {
            throw new IllegalStateException();
        }
        ZipFile apkZip = new ZipFile(new File(getPackageResourcePath()));
        apkZip.extractAll(apkDir.getAbsolutePath());
        File jniDir = new File(apkDir, "lib");
        File assetsDir = new File(apkDir, "assets");

        ArrayList<String> report = new ArrayList<>();
        report.add("libjnifoo.so");
        long jniSize = 0;
        for (File abiDir : jniDir.listFiles()) {
            File[] files = abiDir.listFiles();
            if (files.length != 1 || !files[0].getName().equals("libjnifoo.so")) {
                throw new IllegalStateException();
            }
            jniSize += files[0].length();
        }
        report.add(TextUtils.join(", ", jniDir.list()));
        report.add(L.format("Output: %s", CppModule.stringFromJNI()));
        report.add(L.format("Total size: %d", jniSize));
        report.add("\n");

        String strippedName = "exec-stripped";
        String unstrippedName = "exec-unstripped";
        AssetManager assets = getAssets();
        String[] abisStripped = assets.list(strippedName);
        String[] abisUnstripped = assets.list(unstrippedName);
        if (isEmpty(abisStripped) && isEmpty(abisUnstripped)) {
            report.add("No executables!");
            return TextUtils.join("\n", report);
        }
        if (!isEmpty(abisStripped) && !isEmpty(abisUnstripped)) {
            if (!Arrays.equals(abisStripped, abisUnstripped)) {
                report.add(L.format("Stripped abis list (%s) not match unstripped abis list (%s)",
                        TextUtils.join(", ", abisStripped),
                        TextUtils.join(", ", abisUnstripped)));
                return TextUtils.join("\n", report);
            }
        }
        File temp = new File(getCacheDir(), "temp");
        String[] abis = abisStripped != null ? abisStripped : abisUnstripped;
        Set<String> deviceAbis = new HashSet<>(Arrays.asList(ZenUtils.getSupportedAbis()));
        String[] execNames = {"execbar", "execbaz"};
        for (int i = 0; i < execNames.length; i++) {
            String execName = execNames[i];
            for (String abi : abis) {
                if (temp.exists() && !temp.delete()) {
                    throw new IllegalStateException();
                }
                String execOut = null;
                String strippedSize = "?";
                String unstrippedSize = "?";
                install(execName, temp, strippedName + "/" + abi);
                if (temp.exists()) {
                    strippedSize = String.valueOf(temp.length());
                    if (deviceAbis.contains(abi)) {
                        execOut = getExecOut(temp.getAbsolutePath());
                    }
                    if (!temp.delete()) {
                        throw new IllegalStateException();
                    }
                }
                install(execName, temp, unstrippedName + "/" + abi);
                if (temp.exists()) {
                    unstrippedSize = String.valueOf(temp.length());
                    if (execOut == null && deviceAbis.contains(abi)) {
                        execOut = getExecOut(temp.getAbsolutePath());
                    }
                }
                report.add(L.format("%s/%s %s/%s (%s)",
                        execName, abi, strippedSize, unstrippedSize, execOut));
            }
            if (i != execNames.length - 1) {
                report.add("\n");
            }
        }
        return TextUtils.join("\n", report);
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
}
