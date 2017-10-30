package org.jerkar.api.utils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;



/**
 * Low level utility method to deal with zip files.
 */
public final class JkUtilsZip {

    private static class CrcAndSize {

        private static final int BUFFER_SIZE = 32 * 1024;

        private final CRC32 crc = new CRC32();

        private long size;

        CrcAndSize(File file) {
            try (final FileInputStream inputStream = JkUtilsIO.inputStream(file)){
                load(inputStream);
            } catch (final IOException e) {
                JkUtilsThrowable.unchecked(e);
            }
        }

        CrcAndSize(InputStream inputStream) throws IOException {
            load(inputStream);
        }

        private void load(InputStream inputStream) throws IOException {
            final byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead = -1;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                this.crc.update(buffer, 0, bytesRead);
                this.size += bytesRead;
            }
        }

        public void setupStoredEntry(ZipEntry entry) {
            entry.setSize(this.size);
            entry.setCompressedSize(this.size);
            entry.setCrc(this.crc.getValue());
            entry.setMethod(ZipEntry.STORED);
        }
    }

    private JkUtilsZip() {
    }

    private static boolean addEntryInputStream(ZipOutputStream zos, String entryName,
            InputStream inputStream, boolean storedMethod, CrcAndSize crcAndSize) {
        final ZipEntry zipEntry = new ZipEntry(entryName);
        if (storedMethod) {
            crcAndSize.setupStoredEntry(zipEntry);
        }
        try {
            zos.putNextEntry(zipEntry);
        } catch (final ZipException e) {

            // Ignore duplicate entry - no overwriting
            return false;
        } catch (final IOException e) {
            throw new RuntimeException("Error while adding zip entry " + zipEntry, e);
        }
        final int buffer = 2048;
        final BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream, buffer);
        int count;
        try {
            final byte data[] = new byte[buffer];
            while ((count = bufferedInputStream.read(data, 0, buffer)) != -1) {
                zos.write(data, 0, count);
            }
            bufferedInputStream.close();
            inputStream.close();
            zos.closeEntry();
            return true;
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * Returns all zip entry ofMany the specified zip file.
     */
    @SuppressWarnings("unchecked")
    public static List<ZipEntry> zipEntries(ZipFile zipFile) {
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
    public static ZipFile zipFile(File file) {
        try {
            return new ZipFile(file);
        } catch (final Exception e) {
            throw JkUtilsThrowable.unchecked(e);
        }
    }

}
