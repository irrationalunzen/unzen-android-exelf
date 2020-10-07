package unzen.exelf;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static unzen.exelf.FileUtils.fileListedInDir;

@RunWith(AndroidJUnit4.class)
public class FileSymlinkTest {

    private void testSymlink(File target, File linkDir, File link) throws Exception {
        String targetPath = target.getAbsolutePath();
        String linkPath = link.getAbsolutePath();
        assertTrue(target.exists());
        assertEquals(linkDir.getAbsolutePath(), link.getParent());
        if (link.exists()) {
            assertTrue(link.delete());
        }
        assertFalse(link.exists());
        String linkMessage = String.format("%nLink: %b, %b, %b, %b, %b, %b%n[%s]%n[%s]%n[%s]",
                link.exists(), link.canRead(), link.canExecute(), link.canWrite(),
                link.isDirectory(), link.isFile(),
                link.getAbsolutePath(), link.getCanonicalPath(), linkDir);
        String targetMessage = String.format("%nTarget: %b, %b, %b, %b, %b, %b%n[%s]%n[%s]",
                target.exists(), target.canRead(), target.canExecute(), target.canWrite(),
                target.isDirectory(), target.isFile(),
                target.getAbsolutePath(), target.getCanonicalPath());
        if (FileUtils.existsNoFollowLinks(link)) {
            assertFalse(FileUtils.existsFollowLinks(link));
            assertTrue(FileUtils.isSymlink(link));
            assertTrue(fileListedInDir(linkDir, link));
            File deadTarget = new File(FileUtils.readSymlink(link));
            assertFalse(deadTarget.exists());
            assertNotEquals(target, deadTarget);
            assertTrue(link.delete());
            assertFalse(FileUtils.existsNoFollowLinks(link));
        }
        assertFalse(linkMessage + targetMessage, fileListedInDir(linkDir, link));
        FileUtils.symlink(targetPath, linkPath);
        assertEquals(linkDir.getAbsolutePath(), link.getParent());
        assertTrue(fileListedInDir(linkDir, link));
        assertTrue(link.exists());
    }

    @Test
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
        assertTrue(soFile.exists());
        testSymlink(soFile, normalDir, link);
    }
}