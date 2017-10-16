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
@Deprecated
public final class JkZipper {

    /** Specify compression level */
    public enum JkCompressionLevel {

        /** See  #Deflater.DEFAULT_COMPRESSION */
        DEFAULT_COMPRESSION(Deflater.DEFAULT_COMPRESSION),;

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







    private boolean storedMethod() {
        return JkCompressionMethod.STORED.equals(jkCompressionMethod);
    }

    private void addFileTree(ZipOutputStream zos, JkFileTree fileTree, JkPathFilter filter) {
        if (!fileTree.exists()) {
            return;
        }
        final File base = JkUtilsFile.canonicalFile(fileTree.root().toFile());
        for (final Path file : fileTree.andFilter(filter).files()) {
            JkUtilsZip.addZipEntry(zos, file.toFile(), base,
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


}
