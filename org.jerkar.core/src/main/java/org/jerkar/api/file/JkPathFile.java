package org.jerkar.api.file;

import org.jerkar.api.utils.JkUtilsPath;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.util.List;
import java.util.Map;
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

    public static JkPathFile of(Path path) {
        if (Files.isDirectory(path)) {
            throw new IllegalArgumentException(path + " is a directory");
        }
        return new JkPathFile(path);
    }

    public Path get() {
        return path;
    }

    public JkPathFile createIfNotExist() {
        if (!Files.exists(path)) {
            JkUtilsPath.createDirectories(path.getParent());
            JkUtilsPath.createFile(path);
        }
        return this;
    }

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
}
