/*
 * Copyright 2014-2024  the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package dev.jeka.plugins.springboot;

import org.apache.commons.compress.archivers.jar.JarArchiveEntry;
import org.apache.commons.compress.archivers.jar.JarArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.UnixStat;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;

/**
 * Writes JAR content, ensuring valid directory entries are always created and duplicate
 * items are ignored.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 1.0.0
 */
public class SpringbootJarWriter implements  AutoCloseable {

    private static final UnpackHandler NEVER_UNPACK = new NeverUnpackHandler();

    private static final String NESTED_LOADER_JAR = "META-INF/loader/spring-boot-loader.jar";

    private static final int BUFFER_SIZE = 32 * 1024;

    private final JarArchiveOutputStream jarOutput;

    private final Set<String> writtenEntries = new HashSet<>();


    /**
     * Create a new {@link SpringbootJarWriter} instance.
     * @param file the file to write
     * @throws IOException if the file cannot be opened
     * @throws FileNotFoundException if the file cannot be found
     */
    public SpringbootJarWriter(File file)  {
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        this.jarOutput = new JarArchiveOutputStream(fileOutputStream);
        this.jarOutput.setEncoding("UTF-8");
    }

    public void setExecutableFilePermission(File file) {
        try {
            Path path = file.toPath();
            Set<PosixFilePermission> permissions = new HashSet<>(Files.getPosixFilePermissions(path));
            permissions.add(PosixFilePermission.OWNER_EXECUTE);
            Files.setPosixFilePermissions(path, permissions);
        }
        catch (Throwable ex) {
            // Ignore and continue creating the jar
        }
    }

    /**
     * Write the specified manifest.
     * @param manifest the manifest to write
     * @throws IOException of the manifest cannot be written
     */
    public void writeManifest(Manifest manifest)  {
        JarArchiveEntry entry = new JarArchiveEntry("META-INF/MANIFEST.MF");
        writeEntry(entry, manifest::write);
    }

    /**
     * Write all entries from the specified jar file.
     * @param jarFile the source jar file
     * @throws IOException if the entries cannot be written
     */
    public void writeEntries(JarFile jarFile) {
        this.writeEntries(jarFile, new IdentityEntryTransformer(), NEVER_UNPACK);
    }

    public void writeLoaderClasses(URL loaderJar)  {
        try {
            JarInputStream inputStream = new JarInputStream(new BufferedInputStream(loaderJar.openStream()));
            JarEntry entry;
            while ((entry = inputStream.getNextJarEntry()) != null) {
                if (entry.getName().endsWith(".class")) {
                    EntryWriter entryWriter = new InputStreamEntryWriter(inputStream, false);
                    writeEntry(new JarArchiveEntry(entry), entryWriter);
                }
            }
            inputStream.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    void writeEntries(JarFile jarFile, UnpackHandler unpackHandler) throws IOException {
        this.writeEntries(jarFile, new IdentityEntryTransformer(), unpackHandler);
    }

    void writeEntries(JarFile jarFile, EntryTransformer entryTransformer, UnpackHandler unpackHandler)
            {
        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            JarArchiveEntry entry = null;
            try {
                entry = new JarArchiveEntry(entries.nextElement());
            } catch (ZipException e) {
                throw new RuntimeException(e);
            }
            try {
                setUpEntry(jarFile, entry);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            try (ZipHeaderPeekInputStream inputStream = new ZipHeaderPeekInputStream(jarFile.getInputStream(entry))) {
                EntryWriter entryWriter = new InputStreamEntryWriter(inputStream, true);
                JarArchiveEntry transformedEntry = entryTransformer.transform(entry);
                if (transformedEntry != null) {
                    writeEntry(transformedEntry, entryWriter, unpackHandler);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void setUpEntry(JarFile jarFile, JarArchiveEntry entry) throws IOException {
        try (ZipHeaderPeekInputStream inputStream = new ZipHeaderPeekInputStream(jarFile.getInputStream(entry))) {
            if (inputStream.hasZipHeader() && entry.getMethod() != ZipEntry.STORED) {
                new CrcAndSize(inputStream).setupStoredEntry(entry);
            }
            else {
                entry.setCompressedSize(-1);
            }
        }
    }

    /**
     * Writes an entry. The {@code inputStream} is closed once the entry has been written
     * @param entryName the name of the entry
     * @param inputStream the stream from which the entry's data can be read
     * @throws IOException if the write fails
     */
    public void writeEntry(String entryName, InputStream inputStream)  {
        JarArchiveEntry entry = new JarArchiveEntry(entryName.replace('\\', '/'));
        writeEntry(entry, new InputStreamEntryWriter(inputStream, true));
    }

    /**
     * Write a nested library.
     * @param destination the destination of the library
     * @param library the library
     * @throws IOException if the write fails
     */
    public void writeNestedLibrary(String destination, Path library)  {
        File file = library.toFile();
        JarArchiveEntry entry = new JarArchiveEntry(destination + library.getFileName());
        entry.setTime(getNestedLibraryTime(file));
        try {
            new CrcAndSize(file).setupStoredEntry(entry);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            writeEntry(entry, new InputStreamEntryWriter(new FileInputStream(file), true),
                    NEVER_UNPACK);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private long getNestedLibraryTime(File file) {
        try {
            try (JarFile jarFile = new JarFile(file)) {
                Enumeration<JarEntry> entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    if (!entry.isDirectory()) {
                        return entry.getTime();
                    }
                }
            }
        }
        catch (Exception ex) {
            // Ignore and just use the source file timestamp
        }
        return file.lastModified();
    }


    /**
     * Close the writer.
     * @throws IOException if the file cannot be closed
     */
    @Override
    public void close() throws IOException {
        this.jarOutput.close();
    }

    private void writeEntry(JarArchiveEntry entry, EntryWriter entryWriter) {
        writeEntry(entry, entryWriter, NEVER_UNPACK);
    }

    /**
     * Perform the actual write of a {@link JarEntry}. All other write methods delegate to
     * this one.
     * @param entry the entry to write
     * @param entryWriter the entry writer or {@code null} if there is no content
     * @param unpackHandler handles possible unpacking for the entry
     * @throws IOException in case of I/O errors
     */
    private void writeEntry(JarArchiveEntry entry, EntryWriter entryWriter, UnpackHandler unpackHandler) {
        String parent = entry.getName();
        if (parent.endsWith("/")) {
            parent = parent.substring(0, parent.length() - 1);
            entry.setUnixMode(UnixStat.DIR_FLAG | UnixStat.DEFAULT_DIR_PERM);
        }
        else {
            entry.setUnixMode(UnixStat.FILE_FLAG | UnixStat.DEFAULT_FILE_PERM);
        }
        if (parent.lastIndexOf('/') != -1) {
            parent = parent.substring(0, parent.lastIndexOf('/') + 1);
            if (!parent.isEmpty()) {
                writeEntry(new JarArchiveEntry(parent), null, unpackHandler);
            }
        }
        try {
            if (this.writtenEntries.add(entry.getName())) {
                entryWriter = addUnpackCommentIfNecessary(entry, entryWriter, unpackHandler);
                this.jarOutput.putArchiveEntry(entry);
                if (entryWriter != null) {
                    entryWriter.write(this.jarOutput);
                }
                this.jarOutput.closeArchiveEntry();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }



    /**
     * Interface used to write jar entry date.
     */
    private interface EntryWriter {

        /**
         * Write entry data to the specified output stream.
         * @param outputStream the destination for the data
         * @throws IOException in case of I/O errors
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
            int bytesRead;
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
    static class ZipHeaderPeekInputStream extends FilterInputStream {

        private static final byte[] ZIP_HEADER = new byte[] { 0x50, 0x4b, 0x03, 0x04 };

        private final byte[] header;

        private final int headerLength;

        private int position;

        private ByteArrayInputStream headerStream;

        protected ZipHeaderPeekInputStream(InputStream in) throws IOException {
            super(in);
            this.header = new byte[4];
            this.headerLength = in.read(this.header);
            this.headerStream = new ByteArrayInputStream(this.header, 0, this.headerLength);
        }

        @Override
        public int read() throws IOException {
            int read = (this.headerStream != null) ? this.headerStream.read() : -1;
            if (read != -1) {
                this.position++;
                if (this.position >= this.headerLength) {
                    this.headerStream = null;
                }
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
            int read = (this.headerStream != null) ? this.headerStream.read(b, off, len) : -1;
            if (read <= 0) {
                return readRemainder(b, off, len);
            }
            this.position += read;
            if (read < len) {
                int remainderRead = readRemainder(b, off + read, len - read);
                if (remainderRead > 0) {
                    read += remainderRead;
                }
            }
            if (this.position >= this.headerLength) {
                this.headerStream = null;
            }
            return read;
        }

        public boolean hasZipHeader() {
            return Arrays.equals(this.header, ZIP_HEADER);
        }

        private int readRemainder(byte[] b, int off, int len) throws IOException {
            int read = super.read(b, off, len);
            if (read > 0) {
                this.position += read;
            }
            return read;
        }

    }

    /**
     * Data holder for CRC and Size.
     */
    private static class CrcAndSize {

        private final CRC32 crc = new CRC32();

        private long size;

        CrcAndSize(File file) throws IOException {
            try (FileInputStream inputStream = new FileInputStream(file)) {
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

        public void setupStoredEntry(JarArchiveEntry entry) {
            entry.setSize(this.size);
            entry.setCompressedSize(this.size);
            entry.setCrc(this.crc.getValue());
            entry.setMethod(ZipEntry.STORED);
        }

    }

    /**
     * An {@code EntryTransformer} enables the transformation of {@link JarEntry jar
     * entries} during the writing process.
     */
    interface EntryTransformer {

        JarArchiveEntry transform(JarArchiveEntry jarEntry);

    }

    /**
     * An {@code EntryTransformer} that returns the entry unchanged.
     */
    private static final class IdentityEntryTransformer implements EntryTransformer {

        @Override
        public JarArchiveEntry transform(JarArchiveEntry jarEntry) {
            return jarEntry;
        }

    }

    /**
     * An {@code UnpackHandler} determines whether or not unpacking is required and
     * provides a SHA1 hash if required.
     */
    interface UnpackHandler {

        boolean requiresUnpack(String name);

        String sha1Hash(String name) throws IOException;

    }

    private static final class NeverUnpackHandler implements UnpackHandler {

        @Override
        public boolean requiresUnpack(String name) {
            return false;
        }

        @Override
        public String sha1Hash(String name) {
            throw new UnsupportedOperationException();
        }

    }

    private EntryWriter addUnpackCommentIfNecessary(JarArchiveEntry entry, EntryWriter entryWriter,
                                                    UnpackHandler unpackHandler) throws IOException {
        if (entryWriter == null || !unpackHandler.requiresUnpack(entry.getName())) {
            return entryWriter;
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        entryWriter.write(output);
        entry.setComment("UNPACK:" + unpackHandler.sha1Hash(entry.getName()));
        return new InputStreamEntryWriter(new ByteArrayInputStream(output.toByteArray()), true);
    }



}