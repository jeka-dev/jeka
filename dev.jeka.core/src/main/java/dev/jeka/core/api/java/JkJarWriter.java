package dev.jeka.core.api.java;

import dev.jeka.core.api.utils.JkUtilsIO;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.*;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;

/**
 * Writes JAR content, ensuring valid directory entries are always create and
 * duplicate items are ignored.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
 public class JkJarWriter {

    private static final String NESTED_LOADER_JAR = "META-INF/loader/spring-boot-loader.jar";

    private static final int BUFFER_SIZE = 32 * 1024;

    private final JarOutputStream jarOutput;

    private final Set<String> writtenEntries = new HashSet<>();


    private JkJarWriter(Path target) {
        try {
            OutputStream fileOutputStream = Files.newOutputStream(target);
            this.jarOutput = new JarOutputStream(fileOutputStream);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Create a new {@link JkJarWriter} instance.
     *
     * @param target
     *            the file to write
     * @throws UncheckedIOException
     *             if the file cannot be opened or if the file cannot be found
     */
    public static JkJarWriter of(Path target) {
        return new JkJarWriter(target);
    }

    /**
     * Write the specified manifest.
     * 
     * @param manifest
     *            the manifest to write
     * @throws UncheckedIOException
     *             of the manifest cannot be written
     */
    public void writeManifest(final Manifest manifest) {
        JarEntry entry = new JarEntry("META-INF/MANIFEST.MF");
        try {
            writeEntry(entry, manifest::write);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Write all entries from the specified jar file.
     * 
     * @param jarFile
     *            the source jar file
     * @throws UncheckedIOException
     *             if the entries cannot be written
     */
    public void writeEntries(JarFile jarFile)  {
        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            ZipHeaderPeekInputStream inputStream = null;
            try {
                inputStream = new ZipHeaderPeekInputStream(jarFile.getInputStream(entry));
                if (inputStream.hasZipHeader() && entry.getMethod() != ZipEntry.STORED) {
                    new CrcAndSize(inputStream).setupStoredEntry(entry);
                    inputStream.close();
                    inputStream = new ZipHeaderPeekInputStream(jarFile.getInputStream(entry));
                }
                EntryWriter entryWriter = new InputStreamEntryWriter(inputStream, true);
                writeEntry(entry, entryWriter);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            } finally {
                JkUtilsIO.closeQuietly(inputStream);
            }
        }
    }

    /**
     * Writes an entry. The {@code inputStream} is closed once the entry has
     * been written
     * 
     * @param entryName
     *            The name of the entry
     * @param inputStream
     *            The stream from which the entry's data can be read
     * @throws UncheckedIOException
     *             if the write fails
     */
    public void writeEntry(String entryName, InputStream inputStream) {
        JarEntry entry = new JarEntry(entryName);
        try {
            writeEntry(entry, new InputStreamEntryWriter(inputStream, true));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Write a nested library.
     * 
     * @param destination
     *            the destination of the library
     * @param library
     *            the library
     * @throws UncheckedIOException
     *             if the write fails
     */
    public void writeNestedLibrary(String destination, Path library) {
        JarEntry entry = new JarEntry(destination + library.getFileName().toString());
        entry.setTime(getNestedLibraryTime(library));
        try {
            new CrcAndSize(library).setupStoredEntry(entry);
            writeEntry(entry, new InputStreamEntryWriter(Files.newInputStream(library), true));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

    }

    private long getNestedLibraryTime(Path path) {
        try {
            try (JarFile jarFile = new JarFile(path.toFile())) {
                Enumeration<JarEntry> entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    if (!entry.isDirectory()) {
                        return entry.getTime();
                    }
                }
            }
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    /**
     * Write the required spring-boot-loader classes to the JAR.
     * 
     * @throws UncheckedIOException
     *             if the classes cannot be written
     */
    public void writeLoaderClasses(URL loaderJar)  {
        try {
            JarInputStream inputStream = new JarInputStream(new BufferedInputStream(loaderJar.openStream()));
            JarEntry entry;
            while ((entry = inputStream.getNextJarEntry()) != null) {
                if (entry.getName().endsWith(".class")) {
                    writeEntry(entry, new InputStreamEntryWriter(inputStream, false));
                }
            }
            inputStream.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Close the writer.
     * 
     * @throws UncheckedIOException
     *             if the file cannot be closed
     */
    public void close() {
        try {
            this.jarOutput.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Perform the actual write of a {@link JarEntry}. All other {@code write}
     * method delegate to this one.
     * 
     * @param entry
     *            the entry to write
     * @param entryWriter
     *            the entry writer or {@code null} if there is no content
     * @throws IOException
     *             in case of I/O errors
     */
    private void writeEntry(JarEntry entry, EntryWriter entryWriter) throws IOException {
        String parent = entry.getName();
        if (parent.endsWith("/")) {
            parent = parent.substring(0, parent.length() - 1);
        }
        if (parent.lastIndexOf("/") != -1) {
            parent = parent.substring(0, parent.lastIndexOf("/") + 1);
            if (parent.length() > 0) {
                writeEntry(new JarEntry(parent), null);
            }
        }

        if (this.writtenEntries.add(entry.getName())) {
            this.jarOutput.putNextEntry(entry);
            if (entryWriter != null) {
                entryWriter.write(this.jarOutput);
            }
            this.jarOutput.closeEntry();
        }
    }

    /**
     * Interface used to write jar entry date.
     */
    private interface EntryWriter {

        /**
         * Write entry data to the specified output stream.
         * 
         * @param outputStream
         *            the destination for the data
         * @throws IOException
         *             in case of I/O errors
         */
        void write(OutputStream outputStream) throws IOException;

    }

    /**
     * {@link EntryWriter} that writes content from an {@link InputStream}.
     */
    private static class InputStreamEntryWriter implements EntryWriter {

        private final InputStream inputStream;

        private final boolean close;

        InputStreamEntryWriter(InputStream inputStream, boolean close) {
            this.inputStream = inputStream;
            this.close = close;
        }

        @Override
        public void write(OutputStream outputStream) throws IOException {
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead = -1;
            while ((bytesRead = this.inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.flush();
            if (this.close) {
                this.inputStream.close();
            }
        }

    }

    /**
     * {@link InputStream} that can peek ahead at zip header bytes.
     */
    private static class ZipHeaderPeekInputStream extends FilterInputStream {

        private static final byte[] ZIP_HEADER = new byte[] { 0x50, 0x4b, 0x03, 0x04 };

        private final byte[] header;

        private ByteArrayInputStream headerStream;

        protected ZipHeaderPeekInputStream(InputStream in) throws IOException {
            super(in);
            this.header = new byte[4];
            int len = in.read(this.header);
            this.headerStream = new ByteArrayInputStream(this.header, 0, len);
        }

        @Override
        public int read() throws IOException {
            int read = (this.headerStream == null ? -1 : this.headerStream.read());
            if (read != -1) {
                this.headerStream = null;
                return read;
            }
            return super.read();
        }

        @Override
        public int read(byte[] b) throws IOException {
            return read(b, 0, b.length);
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int read = (this.headerStream == null ? -1 : this.headerStream.read(b, off, len));
            if (read != -1) {
                this.headerStream = null;
                return read;
            }
            return super.read(b, off, len);
        }

        public boolean hasZipHeader() {
            return Arrays.equals(this.header, ZIP_HEADER);
        }
    }

    /**
     * Data holder for CRC and Size.
     */
    private static class CrcAndSize {

        private final CRC32 crc = new CRC32();

        private long size;

        CrcAndSize(Path file) throws IOException {
            try (InputStream inputStream = Files.newInputStream(file)) {
                load(inputStream);
            }
        }

        CrcAndSize(InputStream inputStream) throws IOException {
            load(inputStream);
        }

        private void load(InputStream inputStream) throws IOException {
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                this.crc.update(buffer, 0, bytesRead);
                this.size += bytesRead;
            }
        }

        public void setupStoredEntry(JarEntry entry) {
            entry.setSize(this.size);
            entry.setCompressedSize(this.size);
            entry.setCrc(this.crc.getValue());
            entry.setMethod(ZipEntry.STORED);
        }
    }
    
    public void setExecutableFilePermission(Path path) {
        try {
            Set<PosixFilePermission> permissions = new HashSet<>(
                    Files.getPosixFilePermissions(path));
            permissions.add(PosixFilePermission.OWNER_EXECUTE);
            Files.setPosixFilePermissions(path, permissions);
        }
        catch (Throwable ex) {
            // Ignore and continue creating the jar
        }
    }

}