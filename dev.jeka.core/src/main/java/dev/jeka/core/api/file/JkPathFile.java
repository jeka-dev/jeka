package dev.jeka.core.api.file;

import dev.jeka.core.api.utils.JkUtilsIO;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.api.utils.JkUtilsThrowable;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermission;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A path standing for a file (not a directory). This class provides path methods relevant for files only.
 */
public final class JkPathFile {

    private final Path path;

    private JkPathFile(Path path) {
        this.path = path;
    }

    /**
     * Creates a {@link JkPathFile instance from the specified path.}
     */
    public static JkPathFile of(Path path) {
        if (Files.isDirectory(path)) {
            throw new IllegalArgumentException(path + " is a directory");
        }
        return new JkPathFile(path);
    }

    public static JkPathFile of(String path) {
        return of(Paths.get(path));
    }

    public static JkPathFile of(URL url) {
        try {
            return of(Paths.get(url.toURI()));
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Returns the underlying path.
     */
    public Path get() {
        return path;
    }

    /**
     * Creates a file at this location if such file does not exist yet.
     */
    public JkPathFile createIfNotExist() {
        if (!Files.exists(path)) {
            JkUtilsPath.createDirectories(path.getParent());
            JkUtilsPath.createFile(path);
        }
        return this;
    }

    /**
     * Creates a copy of this file replacing all occurrences of specified map keys by their matching value.
     * Keys may value '${my.key.1}', '[myKey]' or whatever.
     */
    public JkPathFile copyReplacingTokens(Path to, Map<String, String> tokens, Charset charset) {
        JkPathFile.of(to).createIfNotExist();
        if (tokens.isEmpty()) {
            JkUtilsPath.copy(path, to, StandardCopyOption.REPLACE_EXISTING);
            return this;
        }
        try (Stream<String> stream = Files.lines(path, charset)) {
            List<String> result = stream.map(line -> interpolated(line, tokens))
                .collect(Collectors.toList());
            Files.write(to, result, charset, StandardOpenOption.CREATE);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return this;
    }

    /**
     * Copies this file to the specified directory. If the specified directory does not exist, it is created.
     */
    public JkPathFile copyToDir(Path dir, CopyOption ... options) {
        Path target = dir.resolve(this.path.getFileName());
        if (!Files.exists(dir)) {
            JkUtilsPath.createDirectories(dir);
        }
        JkUtilsPath.copy(this.path, target, options);
        return this;
    }

    /**
     * Copies the content of the specified url into this files, replacing the previous content.
     */
    public JkPathFile replaceContentBy(URL url) {
        createIfNotExist();
        JkUtilsIO.copyUrlToFile(url, this.path);
        return this;
    }

    /**
     * Returns <code>true</code> if a file already exists at this location.
     */
    public boolean exists() {
        return Files.exists(path);
    }

    /**
     * Deletes this file if exists.
     */
    public JkPathFile deleteIfExist() {
        if (exists()) {
            JkUtilsPath.deleteFile(this.path);
        }
        return this;
    }

    /**
     * Returns a ASCII string representation of the checksum of this file for the specified algorithm.
     * @param algorithm Hashing algorithm as MD5, SHA-2, ...
     */
    public String getChecksum(String algorithm) {
        assertExist();
        try (final InputStream is = Files.newInputStream(path)) {
            final MessageDigest md = MessageDigest.getInstance(algorithm);
            md.reset();
            final byte[] buf = new byte[2048];
            int len;
            while ((len = is.read(buf)) != -1) {
                md.update(buf, 0, len);
            }
            final byte[] bytes = md.digest();
            return JkUtilsString.toHexString(bytes);
        } catch (final Exception e) {
            throw JkUtilsThrowable.unchecked(e);
        }
    }

    /**
     * Shorthand for {@link Files#write(Path, byte[], OpenOption...)}
     */
    public JkPathFile write(byte[] bytes, OpenOption ... options) {
        JkUtilsPath.write(path, bytes, options);
        return this;
    }

    /**
     * Produces a files, in the same directory, that contains the checksum of this file.
     */
    public JkPathFile checksum(String ... algorithms) {
        assertExist();
        for (String algorithm : algorithms) {
            final String fileName = this.path.getFileName().toString() + "." + algorithm.toLowerCase();
            JkPathFile.of(path.resolveSibling(fileName)).deleteIfExist().write(
                    this.getChecksum(algorithm).getBytes(Charset.forName("ASCII")));
        }
        return this;
    }

    /**
     * Adds execute permition on this files. No effect on windows system.
     */
    public JkPathFile addExecPerm(boolean owner, boolean group, boolean other) {
        assertExist();
        Set<PosixFilePermission> perms = null;
        try {
            perms = Files.getPosixFilePermissions(this.path);
            if (owner) perms.add(PosixFilePermission.OWNER_EXECUTE);
            if (group) perms.add(PosixFilePermission.GROUP_EXECUTE);
            if (other) perms.add(PosixFilePermission.OTHERS_EXECUTE);
            Files.setPosixFilePermissions(this.path, perms);
        } catch (UnsupportedOperationException e) {
            // Windows system
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return this;
    }

    private static String interpolated(String original, Map<String, String> tokenValues) {
        boolean changed = false;
        String result = original;
        for(Map.Entry<String, String> entry : tokenValues.entrySet()) {
            String newResult = result.replace(entry.getKey(), entry.getValue());
            if (!newResult.equals(result)) {
                changed = true;
                result = newResult;
            }
        }
        if (changed) {
            return interpolated(result, tokenValues);
        }
        return result;
    }

    public void assertExist() {
        if (!this.exists()) {
            throw new IllegalStateException("File " + this.path.toAbsolutePath().normalize() + " does not exist.");
        }
    }

}
