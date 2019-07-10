package dev.jeka.core.api.depmanagement;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import dev.jeka.core.api.utils.JkUtilsIterable;
import dev.jeka.core.api.utils.JkUtilsPath;

/**
 * A dependency on files located on file ofSystem.
 */
public final class JkFileSystemDependency implements JkFileDependency {

    /**
     * Creates a {@link JkFileSystemDependency} on the specified files.
     */
    public static JkFileSystemDependency of(Iterable<Path> files) {
        final Iterable<Path> trueFiles = JkUtilsPath.disambiguate(files);
        return new JkFileSystemDependency(trueFiles);
    }

    private final List<Path> files;

    private JkFileSystemDependency(Iterable<Path> files) {
        this.files = Collections.unmodifiableList(JkUtilsIterable.listWithoutDuplicateOf(files));
    }

    @Override
    public final List<Path> getFiles() {
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