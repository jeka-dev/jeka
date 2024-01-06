package dev.jeka.core.api.utils;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;


/**
 * Low level utility method to deal with zip files.
 */
public final class JkUtilsZip {

    private JkUtilsZip() {
    }

    /**
     * Returns all zip entry of the specified zip file.
     */
    @SuppressWarnings("unchecked")
    public static List<ZipEntry> getZipEntries(ZipFile zipFile) {
        final List<ZipEntry> result = new LinkedList<>();
        final Enumeration<ZipEntry> en = (Enumeration<ZipEntry>) zipFile.entries();
        while (en.hasMoreElements()) {
            result.add(en.nextElement());
        }
        return result;
    }

    /**
     * Creates a {@link ZipFile} to file without checked exception.
     */
    public static ZipFile getZipFile(File file) {
        try {
            return new ZipFile(file);
        } catch (final Exception e) {
            throw JkUtilsThrowable.unchecked(e);
        }
    }

    public static JarFile jarFile(Path path) {
        try {
            return new JarFile(path.toFile());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
