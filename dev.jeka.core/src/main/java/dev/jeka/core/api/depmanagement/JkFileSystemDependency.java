package dev.jeka.core.api.depmanagement;

import dev.jeka.core.api.utils.JkUtilsIterable;
import dev.jeka.core.api.utils.JkUtilsPath;

import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * A dependency on files located on file system.
 */
public final class JkFileSystemDependency implements JkFileDependency {

    private final Path ideProjectDir;

    private JkFileSystemDependency(Iterable<Path> files, Path ideProjectDir) {
        this.files = Collections.unmodifiableList(JkUtilsIterable.listWithoutDuplicateOf(files));
        this.ideProjectDir = ideProjectDir;
    }

    /**
     * Creates a {@link JkFileSystemDependency} on the specified files.
     */
    public static JkFileSystemDependency of(Iterable<Path> files) {
        final Iterable<Path> trueFiles = JkUtilsPath.disambiguate(files);
        return new JkFileSystemDependency(trueFiles, null);
    }

    private final List<Path> files;

    @Override
    public final List<Path> getFiles() {
        return files;
    }

    public JkFileSystemDependency minusFile(Path file) {
        List<Path> result = new LinkedList<>(files);
        result.remove(file);
        return new JkFileSystemDependency(result, ideProjectDir);
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

    @Override
    public Path getIdeProjectDir() {
        return ideProjectDir;
    }

    @Override
    public JkFileSystemDependency withIdeProjectDir(Path path) {
        return new JkFileSystemDependency(files, path);
    }
}