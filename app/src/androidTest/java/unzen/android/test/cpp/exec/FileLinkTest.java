package unzen.android.test.cpp.exec;

import android.content.Context;
import android.os.Build;
import android.system.Os;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Looks like creating hard links without root was disabled since Android M.
 * See: https://seandroid-list.tycho.nsa.narkive.com/r5ZNxgkh/selinux-hardlink-brain-damage-in-android-m
 * See: https://code.google.com/archive/p/android-developer-preview/issues/3150
 */
@RunWith(AndroidJUnit4.class)
public class FileLinkTest {

    private boolean link(String oldPath, String newPath) throws IOException, InterruptedException {
        File link = new File(newPath);
        Runtime.getRuntime().exec(new String[] {"ln", oldPath, newPath}).waitFor();
        if (link.exists()) {
            return true;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                Os.link(oldPath, newPath);
                assertTrue(link.exists());
                return true;
            } catch (Exception e) {
                // Exception instead of ErrnoException to awoid crashes on old platforms
                e.printStackTrace();
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                Files.createLink(Paths.get(newPath), Paths.get(oldPath));
                assertTrue(link.exists());
                return true;
            } catch (Exception e) {
                // Exception instead of AccessDeniedException to awoid crashes on old platforms
                e.printStackTrace();
            }
        }
        return false;
    }

    private boolean fileListedInDir(File dir, File file) {
        File[] list = dir.listFiles();
        assertNotNull(list);
        for (File fileInDir : list) {
            if (fileInDir.getAbsolutePath().equals(file.getAbsolutePath())) {
                return true;
            }
        }
        return false;
    }

    @Test
    public void test() throws IOException, InterruptedException {
        // Context of the app under test.
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        File normalDir = context.getFilesDir();

        File link = new File(normalDir, "link");
        assertTrue(!link.exists() || link.delete());
        assertFalse(link.exists());

        File normalFile = new File(normalDir, "temp");
        assertTrue(!normalFile.exists() || normalFile.delete());
        assertTrue(normalFile.createNewFile());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // This test branch succeed on emulator with API 29 with and without Google Play
            // This test branch succeed on Asus Nexus 7 3G with custom Android 7.1.2
            // This test branch succeed on Xiaomi Redmi Note 8 Pro with MIUI Android 9
            assertFalse(link(normalFile.getAbsolutePath(), link.getAbsolutePath()));
            assertFalse(link.exists());
            assertFalse(fileListedInDir(normalDir, link));
            assertFalse(link.delete());
        } else {
            // This test branch succeed on emulator with API 17 and API 16
            assertTrue(link(normalFile.getAbsolutePath(), link.getAbsolutePath()));
            assertTrue(link.exists());
            assertTrue(fileListedInDir(normalDir, link));
            assertTrue(link.delete());
            assertFalse(fileListedInDir(normalDir, link));
            assertFalse(link.exists());
        }
    }
}