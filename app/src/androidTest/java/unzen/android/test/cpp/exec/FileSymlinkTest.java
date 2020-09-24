package unzen.android.test.cpp.exec;

import android.content.Context;
import android.os.Build;
import android.system.Os;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.FlakyTest;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static unzen.android.test.cpp.exec.FileUtils.fileListedInDir;

/**
 * Investigating symlink exists() issues. Possible related issues list below.
 *  https://stackoverflow.com/questions/57655086
 *  https://stackoverflow.com/questions/4226650
 */
@RunWith(AndroidJUnit4.class)
public class FileSymlinkTest {

    private void testSymlink(File target, File linkDir, File link) throws Exception {
        String targetPath = target.getAbsolutePath();
        String linkPath = link.getAbsolutePath();
        assertEquals(linkDir.getAbsolutePath(), link.getParent());
        assertTrue(!link.exists() || link.delete());
        assertFalse(link.exists());
        // Next assert sometimes fails. It's never fails after app data clear.
        // Runs normal some time after app data clear. Then, after pause in testing, starts
        // failing until next app data clear. Sample output:
        //    Link: false, false, false, false, false, false
        //    [/data/user/0/unzen.android.test.cpp.exec/files/link]
        //    [/data/data/unzen.android.test.cpp.exec/files/link]
        //    [/data/user/0/unzen.android.test.cpp.exec/files]
        //    Target: true, true, false, true, false, true
        //    [/data/user/0/unzen.android.test.cpp.exec/files/temp]
        //    [/data/data/unzen.android.test.cpp.exec/files/temp]
        String linkMessage = String.format("%nLink: %b, %b, %b, %b, %b, %b%n[%s]%n[%s]%n[%s]",
                link.exists(), link.canRead(), link.canExecute(), link.canWrite(),
                link.isDirectory(), link.isFile(),
                link.getAbsolutePath(), link.getCanonicalPath(), linkDir);
        String targetMessage = String.format("%nTarget: %b, %b, %b, %b, %b, %b%n[%s]%n[%s]",
                target.exists(), target.canRead(), target.canExecute(), target.canWrite(),
                target.isDirectory(), target.isFile(),
                target.getAbsolutePath(), target.getCanonicalPath());
        assertFalse(linkMessage + targetMessage, fileListedInDir(linkDir, link));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Files.createSymbolicLink(Paths.get(linkPath), Paths.get(targetPath));
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Os.symlink(targetPath, linkPath);
        } else {
            Runtime.getRuntime().exec(new String[] {"ln", "-s", targetPath, linkPath}).waitFor();
        }
        assertEquals(linkDir.getAbsolutePath(), link.getParent());
        assertTrue(fileListedInDir(linkDir, link));
        assertTrue(link.exists());
    }

    @Test @FlakyTest
    public void test() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        File normalDir = context.getFilesDir();
        File link = new File(normalDir, "link");
        File normalFile = new File(normalDir, "temp");
        assertTrue(!normalFile.exists() || normalFile.delete());
        assertTrue(normalFile.createNewFile());
        testSymlink(normalFile, normalDir, link);
        File systemDir = new File(context.getApplicationInfo().nativeLibraryDir);
        File soFile = new File(systemDir, MainActivity.FOO);
        assertTrue(soFile.exists() && fileListedInDir(systemDir, soFile));
        testSymlink(soFile, normalDir, link);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            File execFile = new File(systemDir, MainActivity.BAR);
            assertTrue(execFile.exists() && fileListedInDir(systemDir, execFile));
            testSymlink(execFile, normalDir, link);
        }
    }
}