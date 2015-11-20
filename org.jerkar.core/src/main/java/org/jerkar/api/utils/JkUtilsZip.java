package org.jerkar.api.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Low level utility method to deal with zip files.
 */
public final class JkUtilsZip {

    private JkUtilsZip() {
    }

    /**
     * Unzip the specified zip files into the specified directory. If the
     * specified directory doen snot exist, it is created.
     */
    public static void unzip(Iterable<File> zips, File folder) {
        for (final File zip : zips) {
            unzip(zip, folder);
        }
    }

    /**
     * Unzip the specified zip file into the specified directory. If the
     * specified directory doen snot exist, it is created.
     */
    public static void unzip(File zip, File directory) {
        final byte[] buffer = new byte[1024];
        try {
            if (!directory.exists()) {
                directory.mkdirs();
            }
            final ZipInputStream zis = new ZipInputStream(new FileInputStream(zip));
            ZipEntry ze = zis.getNextEntry();
            while (ze != null) {
                final String fileName = ze.getName();
                final File newFile = new File(directory, fileName);
                if (ze.isDirectory()) {
                    newFile.mkdirs();
                } else {
                    newFile.getParentFile().mkdirs();
                    newFile.createNewFile();
                    final FileOutputStream fos = new FileOutputStream(newFile);
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }
                    fos.close();
                }
                ze = zis.getNextEntry();
            }
            zis.closeEntry();
            zis.close();
        } catch (final IOException e) {
            throw JkUtilsThrowable.unchecked(e);
        }
    }
}
