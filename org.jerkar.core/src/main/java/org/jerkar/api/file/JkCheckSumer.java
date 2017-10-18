package org.jerkar.api.file;

import org.jerkar.api.system.JkLog;
import org.jerkar.api.utils.JkUtilsAssert;
import org.jerkar.api.utils.JkUtilsFile;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Wrapper on <code>File</code> allowing to creates digests on it.
 *
 * @author Jerome Angibaud
 */
public final class JkCheckSumer {

    /**
     * Creates an instance ofMany {@link JkCheckSumer} wrapping the specified
     * file.
     */
    public static JkCheckSumer of(Path file) {
        return new JkCheckSumer(file);
    }

    private final Path file;

    private JkCheckSumer(Path file) {
        JkUtilsAssert.isTrue(Files.isRegularFile(file), file + " is a directory, not a file.");
        this.file = file;
    }

    /**
     * Creates a digest for this wrapped file. The digest file is
     * written in the same directory as the digested file and has the same
     * name + algorithm name extension.
     */
    public JkCheckSumer digest(String ...algorithms) {
        for (final String algorithm : algorithms) {
            JkLog.start("Creating check sum with algorithm " +  algorithm + " for file : " + file);
            final Path parent = file.getParent();
            final String checksum = JkUtilsFile.checksum(file, algorithm);
            final String fileName = file.getFileName().toString() + "." + algorithm.toLowerCase();
            Path path = parent.resolve(fileName);
            try {
                Files.deleteIfExists(path);
                Files.write(path, checksum.getBytes(Charset.forName("utf-8")));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            JkLog.done("File " + fileName + " created");
        }
        return this;
    }

}
