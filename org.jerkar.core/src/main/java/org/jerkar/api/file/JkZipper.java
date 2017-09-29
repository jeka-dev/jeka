package org.jerkar.api.file;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.jerkar.api.system.JkLog;
import org.jerkar.api.utils.JkUtilsAssert;
import org.jerkar.api.utils.JkUtilsFile;
import org.jerkar.api.utils.JkUtilsIO;
import org.jerkar.api.utils.JkUtilsIterable;
import org.jerkar.api.utils.JkUtilsPath;
import org.jerkar.api.utils.JkUtilsThrowable;
import org.jerkar.api.utils.JkUtilsZip;
import org.jerkar.api.utils.JkUtilsZip.JkZipEntryFilter;

/**
 * Defines elements to embed in a zip archive and methods to write archive on
 * disk.
 */
public final class JkZipper {

    /** Specify compression level */
    public enum JkCompressionLevel {

        /** See  #Deflater.BEST_COMPRESSION */
        BEST_COMPRESSION(Deflater.BEST_COMPRESSION),

        /** See  #Deflater.BEST_SPEED */
        BEST_SPEED(Deflater.BEST_SPEED),

        /** See  #Deflater.DEFAULT_COMPRESSION */
        DEFAULT_COMPRESSION(Deflater.DEFAULT_COMPRESSION),

        /** See  #Deflater.NO_COMPRESSION */
        NO_COMPRESSION(Deflater.NO_COMPRESSION), ;

        private final int level;

        JkCompressionLevel(int level) {
            this.level = level;
        }

    }

    /** Specify compression method */
    public enum JkCompressionMethod {

        /** See  #ZipEntry.DEFLATED */
        DEFLATED(ZipEntry.DEFLATED),

        /** Stored, no compression */
        STORED(0);

        private final int method;

        JkCompressionMethod(int method) {
            this.method = method;
        }
    }

    private final List<?> itemsToZip;

    private final List<File> archivestoMerge;

    private final JkCompressionLevel jkCompressionLevel;

    private final JkCompressionMethod jkCompressionMethod;

    private JkZipper(List<?> itemsToZip, List<File> archivestoMerge,
            JkCompressionLevel level, JkCompressionMethod method) {
        this.itemsToZip = itemsToZip;
        this.archivestoMerge = archivestoMerge;
        this.jkCompressionLevel = level;
        this.jkCompressionMethod = method;
    }

    /**
     * Creates a {@link JkZipper} to an array of directories.
     */
    public static JkZipper of(File... dirs) {
        final List<File> archivestoMerges = new LinkedList<>();
        final List<Object> items = new LinkedList<>();
        for (final File file : dirs) {
            if (file.isDirectory()) {
                items.add(file);
            } else {
                archivestoMerges.add(file);
            }
        }
        return new JkZipper(items, archivestoMerges, JkCompressionLevel.DEFAULT_COMPRESSION,
                JkCompressionMethod.DEFLATED);
    }

    public static JkZipper ofPath(Path... dirs) {
        final List<Path> archivestoMerges = new LinkedList<>();
        final List<Object> items = new LinkedList<>();
        for (final Path file : dirs) {
            if (Files.isDirectory(file)) {
                items.add(file);
            } else {
                archivestoMerges.add(file);
            }
        }
        return new JkZipper(items, JkUtilsPath.filesOf(archivestoMerges), JkCompressionLevel.DEFAULT_COMPRESSION,
                JkCompressionMethod.DEFLATED);
    }

    @SuppressWarnings("unchecked")
    static JkZipper of(JkFileTreeSet... jkDirSets) {
        return new JkZipper(Arrays.asList(jkDirSets), Collections.EMPTY_LIST,
                JkCompressionLevel.DEFAULT_COMPRESSION, JkCompressionMethod.DEFLATED);
    }

    @SuppressWarnings("unchecked")
    static JkZipper of(JkFileTree... jkDirs) {
        return new JkZipper(Arrays.asList(jkDirs), Collections.EMPTY_LIST,
                JkCompressionLevel.DEFAULT_COMPRESSION, JkCompressionMethod.DEFLATED);
    }

    /**
     * Returns a {@link JkZipFile} identical to this one but containing also the entries
     * contained in the specified archive files.
     */
    @SuppressWarnings("unchecked")
    public JkZipper merge(Iterable<File> archiveFiles) {
        return new JkZipper(itemsToZip, JkUtilsIterable.concatLists(this.archivestoMerge,
                archiveFiles), this.jkCompressionLevel, this.jkCompressionMethod);
    }

    /**
     * Returns a {@link JkZipFile} identical to this one but containing also the entries
     * contained in the specified archive files.
     */
    public JkZipper merge(File... archiveFiles) {
        return merge(Arrays.asList(archiveFiles));
    }

    /**
     * Append the content of this zipper to the specified archive file. If the
     * specified file does not exist, it will be created under the hood.
     */
    public JkCheckSumer appendTo(File archive) {
        final File temp = JkUtilsFile.tempFile(archive.getName(), "");
        JkUtilsFile.move(archive, temp);
        final JkCheckSumer jkCheckSumer = this.merge(temp).to(archive, JkPathFilter.ACCEPT_ALL);
        temp.delete();
        return jkCheckSumer;
    }

    /**
     * Append the content of this zipper to the specified archive file. If the
     * specified file does not exist, it will be created under the hood.
     */
    public JkCheckSumer appendTo(Path archive) {
        return appendTo(archive.toFile());
    }

    /**
     * Returns a {@link JkZipFile} identical to this one but with the specified compression level.
     */
    public JkZipper with(JkCompressionLevel level) {
        return new JkZipper(this.itemsToZip, this.archivestoMerge, level, this.jkCompressionMethod);
    }

    /**
     * Returns a {@link JkZipFile} identical to this one but with the specified compression method.
     */
    public JkZipper with(JkCompressionMethod method) {
        return new JkZipper(this.itemsToZip, this.archivestoMerge, this.jkCompressionLevel, method);
    }

    /**
     * Writes this zip definition to the specified file.
     */
    public JkCheckSumer to(File zipFile) {
        return to(zipFile, JkPathFilter.ACCEPT_ALL);
    }

    public JkCheckSumer to(Path zipFile) {
        return to(zipFile.toFile());
    }



    /**
     * Same as {@link #to(File)} but specifying a filter to exclude entries.
     */
    public JkCheckSumer to(File zipFile, JkPathFilter entryFilter) {
        JkLog.start("Creating zip file : " + JkUtilsFile.canonicalPath(zipFile));
        JkUtilsFile.createFileIfNotExist(zipFile);
        try (final FileOutputStream fos = new FileOutputStream(zipFile);
                final ZipOutputStream zos =  new ZipOutputStream(fos)) {
            zos.setLevel(this.jkCompressionLevel.level);
            zos.setMethod(this.jkCompressionMethod.method);

            // Adding files to archive
            for (final Object item : this.itemsToZip) {
                if (item instanceof File) {
                    final File file = (File) item;
                    JkUtilsZip.addZipEntry(zos, file, file.getParentFile(), storedMethod(), entryFilter.toZipEntryFilter());
                } else if (item instanceof EntryFile) {
                    final EntryFile entryFile = (EntryFile) item;
                    if (entryFilter.accept(((EntryFile) item).path)) {
                        JkUtilsZip.addZipEntry(zos, entryFile.file, entryFile.path, storedMethod());
                    }
                } else if (item instanceof JkFileTree) {
                    final JkFileTree dirView = (JkFileTree) item;
                    addFileTree(zos, dirView, entryFilter);
                } else if (item instanceof JkFileTreeSet) {
                    final JkFileTreeSet dirViews = (JkFileTreeSet) item;
                    for (final JkFileTree dirView : dirViews.fileTrees()) {
                        addFileTree(zos, dirView, entryFilter);
                    }
                } else {
                    throw new IllegalStateException("Items of class " + item.getClass()
                    + " not handled.");
                }
            }

            // Merging archives to this archive
            final JkZipEntryFilter zipEntryFilter = entryFilter.toZipEntryFilter();
            for (final File archiveToMerge : this.archivestoMerge) {
                final ZipFile file;
                try {
                    file = new ZipFile(archiveToMerge);
                } catch (final FileNotFoundException e) {
                    throw new RuntimeException("File  "
                            + archiveToMerge.getPath() + " does not exist.", e);
                } catch (final IOException e) {
                    throw new RuntimeException("Error while opening zip file "
                            + archiveToMerge.getPath(), e);
                }
                JkUtilsZip.mergeZip(zos, file, zipEntryFilter, storedMethod());
            }
            JkUtilsIO.flush(zos);
            JkUtilsIO.finish(zos);
        } catch (final IOException e) {
            JkUtilsThrowable.unchecked(e);
        }
        JkLog.done();
        return new JkCheckSumer(zipFile);
    }

    private boolean storedMethod() {
        return JkCompressionMethod.STORED.equals(jkCompressionMethod);
    }

    /**
     * Returns a {@link JkZipFile} identical to this one but containing also the specified entry.
     */
    public JkZipper andEntryName(String entryName, File file) {
        final List<Object> list = new LinkedList<>(this.itemsToZip);
        list.add(new EntryFile(entryName, file));
        return new JkZipper(list, archivestoMerge, this.jkCompressionLevel,
                this.jkCompressionMethod);
    }

    /**
     * Returns a {@link JkZipFile} identical to this one but containing also the specified entry.
     */
    public JkZipper andEntryPath(String entryPath, File file) {
        final List<Object> list = new LinkedList<>(this.itemsToZip);
        final String path = entryPath.endsWith("/") ? entryPath + file.getName() : entryPath + "/"
                + file.getName();
        list.add(new EntryFile(path, file));
        return new JkZipper(list, archivestoMerge, this.jkCompressionLevel,
                this.jkCompressionMethod);
    }

    private void addFileTree(ZipOutputStream zos, JkFileTree fileTree, JkPathFilter filter) {
        if (!fileTree.exists()) {
            return;
        }
        final File base = JkUtilsFile.canonicalFile(fileTree.rootDir());
        for (final File file : fileTree.andFilter(filter)) {
            JkUtilsZip.addZipEntry(zos, file, base,
                    JkCompressionMethod.STORED.equals(this.jkCompressionMethod));
        }
    }

    private static class EntryFile {
        final String path;
        final File file;

        public EntryFile(String path, File file) {
            super();
            this.path = path;
            this.file = file;
        }
    }



    /**
     * Wrapper on <code>File</code> allowing to creates digests on it.
     *
     * @author Jerome Angibaud
     */
    public static final class JkCheckSumer {

        /**
         * Creates an instance of {@link JkCheckSumer} wrapping the specified
         * file.
         */
        public static JkCheckSumer of(File file) {
            return new JkCheckSumer(file);
        }

        private final File file;

        private JkCheckSumer(File file) {
            JkUtilsAssert.isTrue(file.isFile(), file.getAbsolutePath()
                    + " is a directory, not a file.");
            this.file = file;
        }

        /**
         * Creates an MD5 digest for this wrapped file. The digest file is
         * written in the same directory as the digested file and has the same
         * name + '.md5' extension.
         */
        public JkCheckSumer makeMd5File() {
            return makeSumFiles(file, "md5");
        }

        /**
         * Creates an SHA-1 digest for this wrapped file. The digest file is
         * written in the same directory as the digested file and has the same
         * name + '.sha1' extension.
         */
        public JkCheckSumer makeSha1File() {
            return makeSumFiles(file, "sha1");
        }

        /**
         * Creates a digest for this wrapped file. The digest file is
         * written in the same directory as the digested file and has the same
         * name + algorithm name extension.
         */
        public JkCheckSumer makeSumFiles(File file, String ...algorithms) {
            for (final String algorithm : algorithms) {
                JkLog.start("Creating check sum with algorithm " +  algorithm + " for file : " + file);
                final File parent = file.getParentFile();
                final String checksum = JkUtilsFile.checksum(file, algorithm);
                final String fileName = file.getName() + "." + algorithm.toLowerCase();
                JkUtilsFile.writeString(new File(parent, fileName), checksum, false);
                JkLog.done("File " + fileName + " created");
            }
            return this;
        }

    }

}
