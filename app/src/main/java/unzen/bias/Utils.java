package unzen.bias;

import android.os.Build;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Locale;

public class Utils {

    static public String format(String format, Object... args) {
        return String.format(Locale.US, format, args);
    }

    static public String fullSoName(String name) {
        return "lib" + name + ".so";
    }

    static public String shortenAbisNames(String s) {
        return s.replace("x86_64", "x64").replace("x86", "x32")
                .replace("armeabi-v7a", "a32").replace("arm64-v8a", "a64");
    }

    // Suppress warnings for Gradle build output
    @SuppressWarnings("deprecation")
    static public String[] getSupportedAbis() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            return Build.SUPPORTED_ABIS;
        } else {
            return new String[]{Build.CPU_ABI, Build.CPU_ABI2};
        }
    }

    static public int parseVerFromOutput(String output) {
        return Integer.parseInt(output.substring(output.lastIndexOf("-") + 1));
    }

    static public int parseVerFromFile(File file) throws IOException {
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

    static public boolean executeFromAppFiles() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.Q;
    }

    static public String getExeOutput(File exe) throws IOException {
        if (!exe.exists()) {
            throw new IllegalStateException("Exe not exists: " + exe.getAbsolutePath());
        }
        if (!exe.canExecute()) {
            throw new IllegalStateException("Exe not executable: " + exe.getAbsolutePath());
        }
        ProcessBuilder builder = new ProcessBuilder(exe.getAbsolutePath());
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
}
