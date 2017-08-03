package org.jerkar.api.depmanagement;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jerkar.api.depmanagement.JkDependency.JkFileDependency;
import org.jerkar.api.utils.JkUtilsAssert;
import org.jerkar.api.utils.JkUtilsIterable;

/**
 * A dependency on files located on file system.
 */
public final class JkFileSystemDependency extends JkFileDependency {

    private static final long serialVersionUID = 1079527121988214989L;

    /**
     * Creates a {@link JkFileSystemDependency} on the specified file.
     */
    public static JkFileSystemDependency ofFile(File baseDir, String relativePath) {
        final File file = new File(relativePath);
        if (!file.isAbsolute()) {
            return JkFileSystemDependency.of(new File(baseDir, relativePath));
        }
        return JkFileSystemDependency.of(file);
    }

    /**
     * Creates a {@link JkFileSystemDependency} on the specified files.
     */
    public static JkFileSystemDependency of(Iterable<File> files) {
        return new JkFileSystemDependency(files);
    }

    /**
     * Creates a {@link JkFileSystemDependency} on the specified files.
     */
    public static JkFileSystemDependency of(File... files) {
        return new JkFileSystemDependency(Arrays.asList(files));
    }

    private final List<File> files;

    private JkFileSystemDependency(Iterable<File> files) {
        this.files = Collections.unmodifiableList(JkUtilsIterable.listWithoutDuplicateOf(files));
    }

    @Override
    public final List<File> files() {
        for (final File file : files) {
            JkUtilsAssert.isTrue(file.exists(), "The file " + file.getAbsolutePath()
            + " does not exist.");
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