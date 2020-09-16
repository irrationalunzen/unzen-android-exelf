package unzen.android.test.cpp.exec;

import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ZipUtils {

    static private void extractEntry(ZipInputStream src, File dst, ZipEntry entry) throws IOException {
        if (entry.getName().equals("")) {
            //TODO: special stuff in Android APK?
            return;
        }
        File f = new File(dst, entry.getName());
        if (entry.isDirectory()) {
            if (!f.mkdirs()) {
                throw new IOException("mkdirs() fail " + entry.getName());
            }
            return;
        }
        File parent = f.getParentFile();
        if (parent == null) {
            throw new IOException("getParentFile() fail " + entry.getName());
        }
        if (!parent.exists()) {
            if (!parent.mkdirs()) {
                throw new IOException("parent.mkdirs() fail " + entry.getName());
            }
        }
        Log.i("AAA", "f: " + f.getAbsolutePath() + ", entry: " + entry.getName());
        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(f))) {
            byte[] buf = new byte[4096];
            int read;
            while ((read = src.read(buf)) != -1) {
                bos.write(buf, 0, read);
            }
        }
    }

    static public void extract(File src, File dst) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(src))) {
            ZipEntry entry = zis.getNextEntry();
            while (entry != null) {
                extractEntry(zis, dst, entry);
                entry = zis.getNextEntry();
            }
        }
    }

}
