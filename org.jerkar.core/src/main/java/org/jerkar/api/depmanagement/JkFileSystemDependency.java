package org.jerkar.api.depmanagement;

import org.jerkar.api.utils.JkUtilsAssert;
import org.jerkar.api.utils.JkUtilsIterable;
import org.jerkar.api.utils.JkUtilsPath;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

/**
 * A dependency on files located on file ofSystem.
 */
public final class JkFileSystemDependency implements JkFileDependency {

    private static final long serialVersionUID = 1079527121988214989L;

    /**
     * Creates a {@link JkFileSystemDependency} on the specified files.
     */
    public static JkFileSystemDependency ofPaths(Iterable<Path> files) {
        Iterable<Path> trueFiles = JkUtilsPath.disambiguate(files);
        return new JkFileSystemDependency(trueFiles);
    }

    private final List<Path> files;

    private JkFileSystemDependency(Iterable<Path> files) {
        this.files = Collections.unmodifiableList(JkUtilsIterable.listWithoutDuplicateOf(files));
    }

    @Override
    public final List<Path> getFiles() {
        for (final Path file : files) {
            JkUtilsAssert.isTrue(Files.exists(file), "File " + file + " does not exist.");
        }
        return files;
    }

    @Override
    public String toString() {
        return "Files=" + files.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final JkFileSystemDependency that = (JkFileSystemDependency) o;
        return files.equals(that.files);
    }

    @Override
    public int hashCode() {
        return files.hashCode();
    }
}