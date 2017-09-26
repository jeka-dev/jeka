package org.jerkar.api.utils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
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

    private static final JkZipEntryFilter ACCEPT_ALL = entryName -> true;

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
        try (final ZipInputStream zis = new ZipInputStream(new FileInputStream(zip))){
            if (!directory.exists()) {
                directory.mkdirs();
            }
            ZipEntry ze = zis.getNextEntry();
            while (ze != null) {
                final String fileName = ze.getName();
                final File newFile = new File(directory, fileName);
                if (ze.isDirectory()) {
                    newFile.mkdirs();
                } else {
                    newFile.getParentFile().mkdirs();
                    newFile.createNewFile();
                    try (final FileOutputStream fos = new FileOutputStream(newFile)) {
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }
                ze = zis.getNextEntry();
            }
            zis.closeEntry();
            zis.close();
        } catch (final IOException e) {
            throw JkUtilsThrowable.unchecked(e);
        }
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
     * Adds a zip entry into the provided <code>ZipOutputStream</code>. The zip
     * entry is the part of <code>filePathToZip</code> truncated with the
     * <code>baseFolderPath</code>.
     * <p>
     * So a file or folder <code>c:\my\base\folder\my\file\to\zip.txt</code>
     * will be added in archive using <code>my/file/to/zip.txt</code> entry.
     */
    public static void addZipEntry(ZipOutputStream zos, File fileToZip, File baseFolder) {
        addZipEntry(zos, fileToZip, baseFolder, false);
    }

    /**
     * Adds a zip entry into the provided <code>ZipOutputStream</code>. The zip
     * entry is the part of <code>filePathToZip</code> truncated with the
     * <code>baseFolderPath</code>.
     * <p>
     * So a file or folder <code>c:\my\base\folder\my\file\to\zip.txt</code>
     * will be added in archive using <code>my/file/to/zip.txt</code> entry.
     */
    public static void addZipEntry(ZipOutputStream zos, File fileToZip, File baseFolder,
            boolean storeMethod) {
        addZipEntry(zos, fileToZip, baseFolder, storeMethod, ACCEPT_ALL);
    }

    /**
     * Same as {@link #addZipEntry(ZipOutputStream, File, File, boolean)} but with the possibility to filter
     * zip entries to excludes somes.
     */
    public static void addZipEntry(ZipOutputStream zos, File fileOrFolderToZip, File baseFolder,
            boolean storeMethod, JkZipEntryFilter filter) {
        if (!baseFolder.isDirectory()) {
            throw new IllegalArgumentException(baseFolder.getPath() + " is not a directory.");
        }

        if (fileOrFolderToZip.isDirectory()) {
            final File[] files = fileOrFolderToZip.listFiles();
            for (final File file : files) {
                addZipEntry(zos, file, baseFolder, storeMethod, filter);
            }
        } else {
            final String filePathToZip;
            final int start;
            try {
                filePathToZip = fileOrFolderToZip.getCanonicalPath();
                start = baseFolder.getCanonicalPath().length() + 1;
            } catch (final IOException e1) {
                throw new IllegalStateException(e1);
            }

            final int end = filePathToZip.length();
            String entryName = filePathToZip.substring(start, end);
            entryName = entryName.replace(File.separatorChar, '/');
            if (!filter.accept(entryName)) {
                return;
            }
            try (final FileInputStream inputStream = new FileInputStream(filePathToZip)){
                if (storeMethod) {
                    final CrcAndSize crcAndSize = new CrcAndSize(fileOrFolderToZip);
                    addEntryInputStream(zos, entryName, inputStream, true, crcAndSize);
                } else {
                    addEntryInputStream(zos, entryName, inputStream, false, null);
                }
            } catch (final IOException e) {
                throw JkUtilsThrowable.unchecked(e);
            }
        }
    }





    /**
     * Add a zip entry into the provided <code>ZipOutputStream</code>.
     */
    public static void addZipEntry(ZipOutputStream zos, File fileOrFolderToZip, String entryName,
            boolean storedMethod) {
        final FileInputStream inputStream;
        try {
            inputStream = new FileInputStream(fileOrFolderToZip);
        } catch (final FileNotFoundException e) {
            throw new IllegalStateException(e);
        }
        if (storedMethod) {
            final CrcAndSize crcAndSize = new CrcAndSize(fileOrFolderToZip);
            addEntryInputStream(zos, entryName, inputStream, true, crcAndSize);
        } else {
            addEntryInputStream(zos, entryName, inputStream, false, null);
        }
    }

    /**
     * Writes all the entries to a given ZipFile to the specified
     * {@link ZipOutputStream}.
     */
    public static Set<String> mergeZip(ZipOutputStream zos, ZipFile zipFile, boolean storeMethod) {
        return mergeZip(zos, zipFile, ACCEPT_ALL, storeMethod);
    }

    /**
     * Writes all the entries to a given ZipFile to the specified
     * {@link ZipOutputStream}.
     */
    public static Set<String> mergeZip(ZipOutputStream zos, ZipFile zipFile, JkZipEntryFilter filter, boolean storeMethod) {
        final Set<String> duplicateEntries = new HashSet<>();
        final Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            final ZipEntry e = entries.nextElement();
            if (!filter.accept(e.getName())) {
                continue;
            }
            try {
                if (!e.isDirectory()) {
                    final boolean success;
                    if (storeMethod) {
                        final InputStream countInputStream = zipFile.getInputStream(e);
                        final CrcAndSize crcAndSize = new CrcAndSize(countInputStream);
                        countInputStream.close();
                        success = addEntryInputStream(zos, e.getName(), zipFile.getInputStream(e),
                                true, crcAndSize);
                    } else {
                        success = addEntryInputStream(zos, e.getName(), zipFile.getInputStream(e),
                                false, null);
                    }
                    if (!success) {
                        duplicateEntries.add(e.getName());
                    }
                }
            } catch (final IOException e1) {
                throw new RuntimeException("Error while merging entry " + e.getName()
                + " to zip file " + zipFile.getName(), e1);
            }
        }
        return duplicateEntries;
    }

    /**
     * Writes all the entries to a given ZipFile to the specified
     * {@link ZipOutputStream}.
     */
    public static Set<String> mergeZip(ZipOutputStream zos, ZipFile zipFile) {
        return mergeZip(zos, zipFile, false);
    }

    /**
     * Reads the specified zip stream and position it at the beginning of the
     * specified entry. The specified entry is case insensitive. An exception is
     * thrown if no such entry exist.
     */
    public static ZipInputStream readZipEntry(InputStream inputStream,
            String caseInsensitiveEntryName) {
        final ZipInputStream result = readZipEntryOrNull(inputStream, caseInsensitiveEntryName);
        if (result == null) {
            throw new IllegalArgumentException("Zip " + inputStream + " has no entry "
                    + caseInsensitiveEntryName);
        }
        return result;
    }

    /**
     * As {@link #readZipEntry(InputStream, String)} )} but returns <code>null</code> instead of throwing an exception
     * if no such entry exist.
     */
    public static ZipInputStream readZipEntryOrNull(File zipFile, String caseInsensitiveEntryName) {
        try (final FileInputStream fileInputStream = JkUtilsIO.inputStream(zipFile)) {
            return readZipEntryOrNull(fileInputStream, caseInsensitiveEntryName);
        } catch (IOException e) {
            throw JkUtilsThrowable.unchecked(e);
        }
    }

    /**
     * Returns all zip entry of the specified zip file.
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
     * Returns the zip entry havinbf a name equals ignoring case to the specified one.
     */
    @SuppressWarnings("unchecked")
    public static ZipEntry zipEntryIgnoreCase(ZipFile zipFile, String entryName) {
        final Enumeration<ZipEntry> en = (Enumeration<ZipEntry>) zipFile.entries();
        while (en.hasMoreElements()) {
            final ZipEntry entry = en.nextElement();
            if (entry.getName().equalsIgnoreCase(entryName)) {
                return entry;
            }
        }
        return null;
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

    /**
     * Reads the specified zip stream and position it at the beginning of the
     * specified entry. The specified entry is case insensitive. It returns
     * <code>null</code> if no such entry exist.
     */
    public static ZipInputStream readZipEntryOrNull(InputStream inputStream,
            String caseInsensitiveEntryNAme) {
        final ZipInputStream zipInputStream = new ZipInputStream(inputStream);
        try {
            ZipEntry entry = zipInputStream.getNextEntry();
            boolean found = false;
            while (entry != null && !found) {
                if (entry.getName().equalsIgnoreCase(caseInsensitiveEntryNAme)) {
                    found = true;
                } else {
                    entry = zipInputStream.getNextEntry();
                }
            }
            if (!found) {
                inputStream.close();
                return null;
            }
            return zipInputStream;
        } catch (final Exception e) {
            throw JkUtilsThrowable.unchecked(e);
        }
    }

    /**
     * Filter on {@link ZipEntry} name
     */
    public interface JkZipEntryFilter {

        /**
         * Returns <code>true</code> if the specified entry name should be accepted.
         */
        boolean accept(String entryName);

    }

}
