package dev.jeka.core.api.file;

import dev.jeka.core.api.system.JkLog;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.security.MessageDigest;
import java.util.Arrays;
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
     * Creates a {@link JkPathFile} instance from the specified path.
     */
    public static JkPathFile of(Path path) {
        if (Files.isDirectory(path)) {
            throw new IllegalArgumentException(path + " is a directory");
        }
        return new JkPathFile(path);
    }

    /**
     * @see #of(Path)
     */
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
     * Creates a temp file wrapped by a JkPathFile.
     */
    public static JkPathFile ofTemp(String prefix, String extension, FileAttribute... fileAttributes) {
        Path tempFile = JkUtilsPath.createTempFile(prefix, extension, fileAttributes);
        return of(tempFile);
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
            if (path.getParent() != null) {
                JkUtilsPath.createDirectories(path.getParent());
            }
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
     * Moves this file to the specified file location
     * @param to The destination file path. This is not the parent directory destination.
     */
    public JkPathFile move(Path to, CopyOption ... options) {
        try {
            Files.move(path, to, options);
            return JkPathFile.of(to);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Copies the content of the specified url into this file, replacing the previous content.
     * A file is created silently if not already exists.
     */
    public JkPathFile fetchContentFrom(URL url) {
        createIfNotExist();
        JkUtilsIO.copyUrlToFile(url, this.path);
        return this;
    }

    /**
     * Gets the contents of the specified url, and write it to this file.
     * A file is created silently if not already exists.
     * @see #fetchContentFrom(URL)
     */
    public JkPathFile fetchContentFrom(String urlString) {
        return fetchContentFrom(JkUtilsIO.toUrl(urlString));
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
     * Returns an ASCII string representation of the checksum of this file for the specified algorithm.
     * @param algorithm Hashing algorithm as MD5, SHA-2, ...
     */
    public String getChecksum(String algorithm) {
        assertExist();
        try (final InputStream is = Files.newInputStream(path)) {
            final MessageDigest md = MessageDigest.getInstance(algorithm);
            md.reset();
            updateDigest(md);
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
        Path parent = path.getParent();
        if (parent != null && !Files.exists(parent)) {
            JkUtilsPath.createDirectories(parent);
        }
        JkUtilsPath.write(path, bytes, options);
        return this;
    }

    /**
     * Writes the specified UTF-8 encoded String to this file.
     */
    public JkPathFile write(String contentUtf8, OpenOption ... options) {
        return write(contentUtf8.getBytes(StandardCharsets.UTF_8), options);
    }

    /**
     * Returns the content of this file as a String encoded in UTF-8
     */
    public String readAsString() {
        return new String(JkUtilsPath.readAllBytes(path), StandardCharsets.UTF_8);
    }

    /**
     * Returns the content of this file as a List of strings, where each element represents a line in the file.
     */
    public List<String> readAsLines() {
        return Arrays.stream(readAsString().split("\n")).collect(Collectors.toList());
    }

    /**
     * Creates sibling files containing the checksum of this file.
     * Checksum files are created in the same directory as this file.
     */
    public JkPathFile createChecksumFiles(String ... algorithms) {
        assertExist();
        for (String algorithm : algorithms) {
            final String fileName = this.path.getFileName().toString() + "." + algorithm.toLowerCase();
            JkPathFile.of(path.resolveSibling(fileName)).deleteIfExist().write(
                    this.getChecksum(algorithm).getBytes(StandardCharsets.US_ASCII));
        }
        return this;
    }

    /**
     * Adds execute permissions on this file. No effect on Windows system.
     */
    public JkPathFile setPosixExecPermissions(boolean owner, boolean group, boolean other) {
        assertExist();
        Set<PosixFilePermission> perms = null;
        try {
            perms = Files.getPosixFilePermissions(this.path);
            if (owner) perms.add(PosixFilePermission.OWNER_EXECUTE);
            if (group) perms.add(PosixFilePermission.GROUP_EXECUTE);
            if (other) perms.add(PosixFilePermission.OTHERS_EXECUTE);
            Files.setPosixFilePermissions(this.path, perms);
        } catch (UnsupportedOperationException e) {
            JkLog.warn("Can not set POSIX permissions to file " + this.path);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return this;
    }

    public JkPathFile setPosixExecPermissions() {
        return setPosixExecPermissions(true, true, true);
    }

    void updateDigest(MessageDigest messageDigest) {
        assertExist();
        try (final InputStream is = Files.newInputStream(path)) {
            final byte[] buf = new byte[2048];
            int len;
            while ((len = is.read(buf)) != -1) {
                messageDigest.update(buf, 0, len);
            }
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
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

    public String getExtension() {
        String filename = path.getFileName().toString();
        int index = filename.lastIndexOf(".");
        if (index < 0) {
            return null;
        }
        return  filename.substring(index + 1);
    }

    @Override
    public String toString() {
        return path.toString();
    }
}
