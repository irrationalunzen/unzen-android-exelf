package unzen.exelf;

import android.util.Log;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Using Apache Commons Compress instead of system ZIP because system ZIP
 * fails to unpack APK on old Android versions.
 */
public class ZipUtils {

    static private final boolean DEBUG = false;
    static private final String TAG = "ZipUtils";

    static private void extractEntry(ZipArchiveInputStream src, File dst, ArchiveEntry e)
            throws IOException {
        File f = new File(dst, e.getName());
        if (e.isDirectory()) {
            if (!f.mkdirs()) {
                throw new IOException("mkdirs() fail " + e.getName());
            }
            return;
        }
        File parent = f.getParentFile();
        if (parent == null) {
            throw new IOException("getParentFile() fail " + e.getName());
        }
        if (!parent.exists()) {
            if (!parent.mkdirs()) {
                throw new IOException("parent.mkdirs() fail " + e.getName());
            }
        }
        if (DEBUG) {
            Log.i(TAG, "extractEntry: " + f.getAbsolutePath() + ", e: " + e.getName());
        }
        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(f))) {
            byte[] buf = new byte[4096];
            int read;
            while ((read = src.read(buf)) != -1) {
                bos.write(buf, 0, read);
            }
        }
    }

    static private ArchiveEntry nextEntry(ZipArchiveInputStream zis) throws IOException {
        ArchiveEntry e = zis.getNextEntry();
        if (e != null && "".equals(e.getName())) {
            // Skip special stuff in Android APK.
            if (DEBUG) {
                Log.i(TAG, "Skip special stuff in Android APK");
            }
            return nextEntry(zis);
        }
        return e;
    }

    static public void extract(File src, File dst) throws IOException {
        try (ZipArchiveInputStream zis = new ZipArchiveInputStream(new FileInputStream(src))) {
            ArchiveEntry entry = nextEntry(zis);
            while (entry != null) {
                extractEntry(zis, dst, entry);
                entry = nextEntry(zis);
            }
        }
    }

}
