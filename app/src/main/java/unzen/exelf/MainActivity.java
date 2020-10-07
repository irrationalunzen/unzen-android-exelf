package unzen.exelf;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import unzen.exelf.cuscuta.Cuscuta;

import static unzen.exelf.Assert.assertFalse;
import static unzen.exelf.Assert.assertTrue;
import static unzen.exelf.FileUtils.fileListedInDir;
import static unzen.exelf.Utils.executeFromAppFiles;
import static unzen.exelf.Utils.format;
import static unzen.exelf.Utils.fullSoName;
import static unzen.exelf.Utils.getExeOutput;
import static unzen.exelf.Utils.parseVerFromFile;
import static unzen.exelf.Utils.parseVerFromOutput;

/**
 * Android 10 W^X policy:
 *  https://issuetracker.google.com/issues/128554619
 *  https://developer.android.com/ndk/guides/wrap-script
 *  https://github.com/termux/termux-packages/wiki/Termux-and-Android-10
 */
public class MainActivity extends Activity {

    static private final String FOO_NAME = "jnifoo";
    static public final String FOO = fullSoName(FOO_NAME);
    static private final String BAR_NAME = "exebar";
    static public final String BAR = fullSoName(BAR_NAME);
    static private final String BAZ_NAME = "exebaz";
    static public final String BAZ = fullSoName(BAZ_NAME);
    static public final String QUX = "qux.sh";
    static private Set<String> MIN_EXES = new HashSet<>(Arrays.asList(FOO, BAR, BAZ));
    static private Set<String> APK_EXES = new HashSet<>(Arrays.asList(FOO, QUX, BAR, BAZ));

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

        public String body() {
            StringBuilder result = new StringBuilder();
            for (Map.Entry<String, Integer> entry : abisToVers.entrySet()) {
                if (result.length() > 0) {
                    result.append(", ");
                }
                result.append(entry.getKey()).append(" ").append("v").append(entry.getValue());
            }
            return Utils.shortenAbisNames(result.toString());
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
    }

    static private void checkOutput(String elfName, String actualOut) {
        String expected = format("I'm %s! UNZEN-VERSION-", elfName);
        String message = format("Expected: %s, actual: %s", expected, actualOut);
        assertTrue(actualOut.startsWith(expected), message);
    }

    private Report getJniReport(File apkDir) throws IOException {
        File apkLibsDir = new File(apkDir, "lib");
        Map<String, Integer> abisToVers = new HashMap<>();
        long totalSize = 0;
        for (File abiDir : Objects.requireNonNull(apkLibsDir.listFiles())) {
            File foo = new File(abiDir, FOO);
            if (!foo.exists()) {
                continue;
            }
            abisToVers.put(abiDir.getName(), parseVerFromFile(foo));
            totalSize += foo.length();
        }
        String output = Cuscuta.getStringFromJni();
        checkOutput(FOO, output);
        return new Report(FOO, abisToVers, totalSize, parseVerFromOutput(output));
    }

    private int exesVerFromOutput(File exesDir, boolean fullName, boolean setExec) throws IOException {
        File barExe = new File(exesDir, fullName ? BAR : BAR_NAME);
        assertTrue(!setExec || barExe.setExecutable(true));
        String barOut = getExeOutput(barExe);
        checkOutput(BAR_NAME, barOut);
        int barVer = parseVerFromOutput(barOut);
        File bazExe = new File(exesDir, fullName ? BAZ : BAZ_NAME);
        assertTrue(!setExec || bazExe.setExecutable(true));
        String bazOut = getExeOutput(bazExe);
        checkOutput(BAZ_NAME, bazOut);
        int bazVer = parseVerFromOutput(barOut);
        assertTrue(barVer == bazVer, format("VerFromOutput %d != %d", barVer, bazVer));
        return barVer;
    }

    private int exesVerFromOutputSymlinks(File exesDir) throws Exception {
        File linksDir = new File(getCacheDir(), "exe-links");
        assertTrue(linksDir.exists() || linksDir.mkdirs());
        for (String exe : new String[] {BAR_NAME, BAZ_NAME}) {
            File target = new File(exesDir, fullSoName(exe));
            assertTrue(target.exists());
            File symlink = new File(linksDir, exe);
            if (symlink.exists()) {
                assertTrue(symlink.delete());
            }
            assertFalse(symlink.exists());
            if (FileUtils.existsNoFollowLinks(symlink)) {
                assertFalse(FileUtils.existsFollowLinks(symlink));
                assertTrue(FileUtils.isSymlink(symlink));
                assertTrue(FileUtils.fileListedInDir(linksDir, symlink));
                File deadTarget = new File(FileUtils.readSymlink(symlink));
                assertFalse(deadTarget.exists());
                assertFalse(target.equals(deadTarget));
                assertTrue(symlink.delete());
                assertFalse(FileUtils.existsNoFollowLinks(symlink));
            }
            assertFalse(fileListedInDir(linksDir, symlink));
            FileUtils.symlink(target.getAbsolutePath(), symlink.getAbsolutePath());
            assertTrue(symlink.exists());
        }
        return exesVerFromOutput(linksDir, false, false);
    }

    private Report getExeReport(File apkDir) throws Exception {
        File apkLibsDir = new File(apkDir, "lib");
        Map<String, Integer> abisToVers = new HashMap<>();
        long totalSize = 0;
        for (File abiDir : Objects.requireNonNull(apkLibsDir.listFiles())) {
            Set<String> names = new HashSet<>(Arrays.asList(Objects.requireNonNull(abiDir.list())));
            if (names.size() == 1 && names.contains(QUX)) {
                continue;
            }
            if (!names.contains(BAR) && !names.contains(BAZ)) {
                error("APK missing ELFs.%n%s", names);
                return null;
            }
            assertTrue(APK_EXES.equals(names), names.toString());
            File bar = new File(abiDir, BAR);
            File baz = new File(abiDir, BAZ);
            int barVer = parseVerFromFile(bar);
            int bazVer = parseVerFromFile(baz);
            assertTrue(barVer == bazVer, format("VerFromFile %d != %d", barVer, bazVer));
            abisToVers.put(abiDir.getName(), barVer);
            totalSize += bar.length() + baz.length();
        }
        File exeDir = new File(getApplicationInfo().nativeLibraryDir);
        int verFromOutputDirect = exesVerFromOutput(exeDir, true, false);
        int verFromOutputLinks = exesVerFromOutputSymlinks(exeDir);
        assertTrue(verFromOutputDirect == verFromOutputLinks);
        int verFromOutput = verFromOutputLinks;
        assertFalse(verFromOutput == -1);
        if (executeFromAppFiles()) {
            for (String abi : Utils.getSupportedAbis()) {
                if (abisToVers.containsKey(abi)) {
                    int ver = exesVerFromOutput(new File(apkLibsDir, abi), true, true);
                    assertTrue(ver == verFromOutput);
                    break;
                }
            }
        }
        return new Report("exebar, exebaz", abisToVers, totalSize, verFromOutput);
    }

    private final ArrayList<String> messages = new ArrayList<>();
    private final ArrayList<String> warns = new ArrayList<>();
    private final ArrayList<String> errors = new ArrayList<>();

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

    private void error(String m) {
        errors.add(m);
    }

    private void error(String format, Object... args) {
        error(format(format, args));
    }

    @SuppressWarnings("ConstantConditions")
    private void checkJniExeReports(Report jniReport, Report exeReport) {
        if (exeReport == null) {
            return;
        }
        assertTrue(jniReport.abisToVers.equals(exeReport.abisToVers));
        assertTrue(jniReport.verFromOutput == BuildConfig.BASE_VERSION_CODE);
        assertTrue(exeReport.verFromOutput == BuildConfig.BASE_VERSION_CODE);
        if (BuildConfig.FLAVOR.equals("fat")) {
            assertTrue(BuildConfig.VERSION_CODE == BuildConfig.BASE_VERSION_CODE);
            assertTrue(Arrays.asList(1, 2, 3, 4).contains(jniReport.abisToVers.size()));
        } else {
            assertTrue(jniReport.abisToVers.size() == 1);
            if (BuildConfig.FLAVOR.equals("a32")) {
                assertTrue(BuildConfig.VERSION_CODE == BuildConfig.BASE_VERSION_CODE + 1);
            } else if (BuildConfig.FLAVOR.equals("a64")) {
                assertTrue(BuildConfig.VERSION_CODE == BuildConfig.BASE_VERSION_CODE + 2);
            } else if (BuildConfig.FLAVOR.equals("x32")) {
                assertTrue(BuildConfig.VERSION_CODE == BuildConfig.BASE_VERSION_CODE + 3);
            } else if (BuildConfig.FLAVOR.equals("x64")) {
                assertTrue(BuildConfig.VERSION_CODE == BuildConfig.BASE_VERSION_CODE + 4);
            }
        }
        if (!jniReport.versInSync(BuildConfig.BASE_VERSION_CODE)) {
            warn("Versions between ABIs doesn't match. That's may be due to build performed by"
                    + " Android Studio's \"Run\" action, that makes new build only for ABI of"
                    + " the \"Run\" target's device.");
        }
        if (BuildConfig.FLAVOR.equals("fat") && jniReport.abisToVers.size() != 4) {
            warn("Flavor \"fat\" has only %d ABIs, expected 4 ABIs. That's may be due to"
                            + " build performed by Android Studio's \"Run\" action, that makes"
                            + " new build only for ABI of the \"Run\" target's device.",
                    jniReport.abisToVers.size());
        }
    }

    private void displayReport(Report jniReport, Report exeReport, TextView textView) {
        ArrayList<String> header = new ArrayList<>();
        header.add(format("Java v%s, Cpp v%d", BuildConfig.VERSION_NAME, BuildConfig.BASE_VERSION_CODE));
        header.add("\n" + jniReport.toString());
        if (exeReport != null) {
            header.add("\n" + exeReport.toString());
        }
        if (!messages.isEmpty()) {
            header.add("\n");
            header.addAll(messages);
        }
        String text = TextUtils.join("\n", header);
        if (!warns.isEmpty()) {
            text = format("%s%n%n%nWARNINGS%n%n%s", text, TextUtils.join("\n\n", warns));
        }
        if (!errors.isEmpty()) {
            text = format("%s%n%n%nERRORS%n%n%s", text, TextUtils.join("\n\n", errors));
        }

        textView.setText(text);
        if (!errors.isEmpty()) {
            textView.setTextColor(0xffff0000);
        } else if (!warns.isEmpty()) {
            textView.setTextColor(0xfffc940a);
        } else {
            textView.setTextColor(0xff00ff55);
        }
    }

    private Set<String> getExpectedInstalledExes() {
        // https://developer.android.com/ndk/guides/wrap-script
        if (BuildConfig.DEBUG && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            return APK_EXES;
        }
        return MIN_EXES;
    }

    private String nativeLibraryDirMessage(File dir, Set<String> elfs) throws IOException {
        String message = "getApplicationInfo().nativeLibraryDir";
        message += "\n" + elfs.toString();
        message += "\n" + getApplicationInfo().nativeLibraryDir;
        if (FileUtils.isSymlink(dir) || !dir.getAbsolutePath().equals(dir.getCanonicalPath())) {
            message += "\n" + FileUtils.readSymlink(dir);
        }
        return message;
    }

    private void nativeLibraryDirReport() throws IOException {
        File dir = new File(getApplicationInfo().nativeLibraryDir);
        Set<String> elfs = new HashSet<>(Arrays.asList(Objects.requireNonNull(dir.list())));
        if (elfs.equals(getExpectedInstalledExes())) {
            if (FileUtils.isSymlink(dir) || !dir.getAbsolutePath().equals(dir.getCanonicalPath())) {
                message(nativeLibraryDirMessage(dir, elfs));
            }
        } else {
            warn(nativeLibraryDirMessage(dir, elfs));
        }
    }

    private File unpackApk() throws IOException {
        File apkDir = new File(getCacheDir(), "unzen-apk");
        FileUtils.deleteDirectory(apkDir);
        assertTrue(!apkDir.exists() && apkDir.mkdirs());
        ZipUtils.extract(new File(getPackageResourcePath()), apkDir);
        File assetsDir = new File(apkDir, "assets");
        File dummy = new File(assetsDir, "dummy.txt");
        assertTrue(dummy.exists() && dummy.length() > 0);
        File dummyLib = new File(assetsDir, "dummy-lib.txt");
        assertTrue(dummyLib.exists() && dummyLib.length() > 0);
        return apkDir;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        try {
            nativeLibraryDirReport();
            File apkDir = unpackApk();
            Report jniReport = getJniReport(apkDir);
            Report exeReport = getExeReport(apkDir);
            checkJniExeReports(jniReport, exeReport);
            displayReport(jniReport, exeReport, findViewById(R.id.main_text));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
