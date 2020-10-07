package unzen.exelf;

import android.os.Build;
import android.system.Os;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;
import java.util.Objects;

import static java.lang.String.format;

/**
 * Some methods copied from Apache Commons IO.
 */
public class FileUtils {

    static public void symlink(String target, String link) throws Exception {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Files.createSymbolicLink(Paths.get(link), Paths.get(target));
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Os.symlink(target, link);
        } else {
            Runtime.getRuntime().exec(new String[] {"ln", "-s", target, link}).waitFor();
        }
    }

    public static String readSymlink(File symlink) throws IOException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String target = Files.readSymbolicLink(symlink.toPath()).toString();
            if (symlink.exists()) {
                // Since getCanonicalPath() and getCanonicalFile() works only for non broken
                // links, this check has sense only if link is not broken. Broken symlinks
                // returns false from exists().
                String compatTarget = symlink.getCanonicalPath();
                if (!target.equals(compatTarget)) {
                    throw new IOException(format("!target.equals(compatTarget) [%s] -> [%s][%s]",
                            symlink, target, compatTarget));
                }
            }
            return target;
        }
        String target = symlink.getCanonicalPath();
        if (target.equals(symlink.getAbsolutePath())) {
            throw new IOException(format("Not a symlink: %s", symlink));
        }
        return target;
    }

    public static boolean existsFollowLinks(File file) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return Files.exists(file.toPath());
        }
        return file.exists();
    }

    public static boolean existsNoFollowLinks(File file) throws IOException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return Files.exists(file.toPath(), LinkOption.NOFOLLOW_LINKS);
        }
        boolean res = file.exists();
        if (res) {
            return true;
        }
        return fileListedInDir(file.getParentFile(), file);
    }

    static public boolean fileListedInDir(File dir, File file) {
        for (File fileInDir : Objects.requireNonNull(dir.listFiles())) {
            if (fileInDir.getAbsolutePath().equals(file.getAbsolutePath())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determines whether the specified file is a Symbolic Link rather than an actual file.
     * <p>
     * Will not return true if there is a Symbolic Link anywhere in the path,
     * only if the specific file is.
     * @param file the file to check
     * @return true if the file is a Symbolic Link
     * @throws IOException if an IO error occurs while checking the file
     */
    public static boolean isSymlink(File file) throws IOException {
        if (file == null) {
            throw new NullPointerException("File must not be null");
        }
        // We dont use here simple getCanonicalPath() equals getAbsolutePath() because they
        // might be not equals when there is symlink in upper path, but the file that
        // we currently checking is not a symlink.
        File fileInCanonicalDir;
        if (file.getParent() == null) {
            fileInCanonicalDir = file;
        } else {
            File canonicalDir = Objects.requireNonNull(file.getParentFile()).getCanonicalFile();
            fileInCanonicalDir = new File(canonicalDir, file.getName());
        }
        boolean res;
        if (fileInCanonicalDir.getCanonicalFile().equals(fileInCanonicalDir.getAbsoluteFile())) {
            // If file exists then if it is a symlink it's not broken.
            if (file.exists()) {
                res = false;
            } else {
                // Broken symlink will show up in the list of files of its parent directory.
                File canon = file.getCanonicalFile();
                File parentDir = canon.getParentFile();
                if (parentDir == null || !parentDir.exists()) {
                    res = false;
                } else {
                    File[] fileInDir = parentDir.listFiles(aFile -> aFile.equals(canon));
                    res = fileInDir != null && fileInDir.length > 0;
                }
            }
        } else {
            res = true;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            boolean resNormal = Files.isSymbolicLink(file.toPath());
            if (res != resNormal) {
                throw new IOException(format("%b, %b, %s", resNormal, res, file));
            }
        }
        return res;
    }

    /**
     * Lists files in a directory, asserting that the supplied directory satisfies exists and is a directory
     * @param directory The directory to list
     * @return The files in the directory, never null.
     * @throws IOException if an I/O error occurs
     */
    private static File[] verifiedListFiles(File directory) throws IOException {
        if (!directory.exists()) {
            final String message = directory + " does not exist";
            throw new IllegalArgumentException(message);
        }
        if (!directory.isDirectory()) {
            final String message = directory + " is not a directory";
            throw new IllegalArgumentException(message);
        }
        final File[] files = directory.listFiles();
        if (files == null) {  // null if security restricted
            throw new IOException("Failed to list contents of " + directory);
        }
        return files;
    }

    /**
     * Deletes a file. If file is a directory, delete it and all sub-directories.
     * <p>
     * The difference between File.delete() and this method are:
     * <ul>
     * <li>A directory to be deleted does not have to be empty.</li>
     * <li>You get exceptions when a file or directory cannot be deleted.
     * (java.io.File methods returns a boolean)</li>
     * </ul>
     *
     * @param file file or directory to delete, must not be {@code null}
     * @throws NullPointerException  if the directory is {@code null}
     * @throws FileNotFoundException if the file was not found
     * @throws IOException           in case deletion is unsuccessful
     */
    public static void forceDelete(final File file) throws IOException {
        if (file.isDirectory()) {
            deleteDirectory(file);
        } else {
            final boolean filePresent = file.exists();
            if (!file.delete()) {
                if (!filePresent) {
                    throw new FileNotFoundException("File does not exist: " + file);
                }
                final String message = "Unable to delete file: " + file;
                throw new IOException(message);
            }
        }
    }

    /**
     * Cleans a directory without deleting it.
     *
     * @param directory directory to clean
     * @throws IOException              in case cleaning is unsuccessful
     * @throws IllegalArgumentException if {@code directory} does not exist or is not a directory
     */
    public static void cleanDirectory(final File directory) throws IOException {
        final File[] files = verifiedListFiles(directory);
        IOException exception = null;
        for (final File file : files) {
            try {
                forceDelete(file);
            } catch (final IOException ioe) {
                exception = ioe;
            }
        }
        if (null != exception) {
            throw exception;
        }
    }

    /**
     * Deletes a directory recursively.
     *
     * @param directory directory to delete
     * @throws IOException              in case deletion is unsuccessful
     * @throws IllegalArgumentException if {@code directory} does not exist or is not a directory
     */
    public static void deleteDirectory(final File directory) throws IOException {
        if (!directory.exists()) {
            return;
        }
        if (!isSymlink(directory)) {
            cleanDirectory(directory);
        }
        if (!directory.delete()) {
            final String message = "Unable to delete directory " + directory + ".";
            throw new IOException(message);
        }
    }
}
