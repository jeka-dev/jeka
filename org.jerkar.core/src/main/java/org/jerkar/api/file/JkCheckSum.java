package org.jerkar.api.file;

import org.jerkar.api.system.JkLog;
import org.jerkar.api.utils.JkUtilsAssert;
import org.jerkar.api.utils.JkUtilsFile;
import org.jerkar.api.utils.JkUtilsString;
import org.jerkar.api.utils.JkUtilsThrowable;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;

/**
 * Wrapper on <code>File</code> allowing to creates digests on it.
 *
 * @author Jerome Angibaud
 */
public final class JkCheckSum {

    /**
     * Creates an instance ofMany {@link JkCheckSum} wrapping the specified
     * file.
     */
    public static JkCheckSum of(Path file) {
        return new JkCheckSum(file);
    }

    private final Path file;

    private JkCheckSum(Path file) {
        JkUtilsAssert.isTrue(Files.isRegularFile(file), file + " is a directory, not a file.");
        this.file = file;
    }

    /**
     * Creates a digest for this wrapped file. The produced file is
     * written in the same directory as the digested file and has the same
     * name + algorithm name extension.
     */
    public JkCheckSum produce(String ...algorithms) {
        for (final String algorithm : algorithms) {
            JkLog.start("Creating check sum with algorithm " +  algorithm + " for file : " + file);
            final Path parent = file.getParent();
            final String checksum = checksum(file, algorithm);
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

    private static String checksum(Path file, String algorithm) {
        try (final InputStream is = Files.newInputStream(file)) {
            final MessageDigest md = MessageDigest.getInstance(algorithm);
            md.reset();
            final byte[] buf = new byte[2048];
            int len = 0;
            while ((len = is.read(buf)) != -1) {
                md.update(buf, 0, len);
            }
            final byte[] bytes = md.digest();
            return JkUtilsString.toHexString(bytes);
        } catch (final Exception e) {
            throw JkUtilsThrowable.unchecked(e);
        }
    }

}
