package org.jerkar.api.depmanagement;

import org.jerkar.api.file.JkPathTree;
import org.jerkar.api.java.JkJavaProcess;
import org.jerkar.api.system.JkLog;
import org.jerkar.api.system.JkProcess;
import org.jerkar.api.utils.JkUtilsIterable;
import org.jerkar.api.utils.JkUtilsPath;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Supplier;


/**
 * Dependency on computed resource. More concretely, this is a file dependency on files that might not
 * be present at the time of the build and that has to be generated. Instances of this class are
 * responsible to generate the missing files. <p>
 *
 * Computed dependencies are instantiated by providing expected files and a {@link Runnable} that
 * generates these files in case one of them misses. <p>
 *
 * This is yet simple but quite powerful mechanism, cause the runnable can be anything as Maven or ANT build
 * of another project, a Jerkar build of another project, ... <p>
 *
 * This is the way for creating multi-projet (and multi-techno if desired) builds.
 *
 */
public class JkComputedDependency implements JkFileDependency {

    private static final Supplier<Iterable<Path>> EMPTY_SUPPLIER = LinkedList::new;

    /**
     * Creates a computed dependency to the specified files and {@link JkProcess} to run for
     * generating them.
     */
    public static final JkComputedDependency of(final JkProcess process, Path... files) {
        final List<Path> fileSet = JkUtilsIterable.listWithoutDuplicateOf(Arrays.asList(files));
        final Runnable runnable = new Runnable() {

            @Override
            public void run() {
                process.runSync();
            }

            @Override
            public String toString() {
                return process.toString();
            }
        };
        return new JkComputedDependency(runnable, null, fileSet, EMPTY_SUPPLIER);
    }


    /**
     * Creates a computed dependency to the specified files and the specified {@link Runnable} to run for
     * generating them.
     */
    public static final JkComputedDependency of(Runnable runnable, Path... files) {
        final List<Path> fileSet = JkUtilsIterable.listWithoutDuplicateOf(Arrays.asList(files));
        return new JkComputedDependency(runnable, null, fileSet, EMPTY_SUPPLIER);
    }

    /**
     * Identical to {@link #of(Path, JkJavaProcess, String, String...)} but you specified a set of files
     * instead of a single one.
     */
    public static final JkComputedDependency of(Iterable<Path> files, final JkJavaProcess process,
            final String className, final String... args) {
        final List<Path> fileSet = JkUtilsIterable.listWithoutDuplicateOf(JkUtilsPath.disambiguate(files));
        final Runnable runnable = () -> process.runClassSync(className, args);
        return new JkComputedDependency(runnable, null, fileSet, EMPTY_SUPPLIER);
    }

    /**
     * Creates a computed dependency to the specified file and the specified java program to run for
     * generating them.
     */
    public static final JkComputedDependency of(Path file, final JkJavaProcess process,
            final String className, final String... args) {
        return of(JkUtilsIterable.setOf(file), process, className, args);
    }

    private static final long serialVersionUID = 1L;

    private final Runnable runnable;

    private final Iterable<Path> files;

    private final Supplier<Iterable<Path>> extraFileSupplier;

    private final Path ideProjectBaseDir; // Helps to generate ide metadata

    /**
     * Constructs a computed dependency to the specified files and the specified {@link Runnable} to run for
     * generating them.
     */
    protected JkComputedDependency(Runnable runnable, Path ideProjectBaseDir, Iterable<Path> files, Supplier<Iterable<Path>> extraFileSupplier)  {
        super();
        this.runnable = runnable;
        this.files = files;
        this.ideProjectBaseDir = ideProjectBaseDir;
        this.extraFileSupplier = extraFileSupplier;
    }

    /**
     * Returns a duplicate of this computed dependency but specifying that it can be replaced by a project dependency in a IDE.
     */
    public JkComputedDependency withIdeProjectBaseDir(Path baseDir) {
        return new JkComputedDependency(this.runnable, baseDir, this.files, EMPTY_SUPPLIER);
    }

    /**
     * Returns <code>true</code> if at least one of these files is missing or one of these directory is empty.
     */
    public final boolean hasMissingFilesOrEmptyDirs() {
        return !missingFilesOrEmptyDirs().isEmpty();
    }

    /**
     * Returns the missing files or empty directory for this dependency.
     */
    public final Set<Path> missingFilesOrEmptyDirs() {
        final Set<Path> files = new LinkedHashSet<>();
        for (final Path file : this.files) {
            if (!Files.exists(file)
                    || (Files.isDirectory(file) && JkPathTree.of(file).count(0, true) == 0)) {
                files.add(file);
            }
        }
        return files;
    }

    @Override
    public List<Path> paths() {
        if (this.hasMissingFilesOrEmptyDirs()) {
            JkLog.execute(this, "Building dependency : " + this, runnable);
        }
        final Set<Path> missingFiles = this.missingFilesOrEmptyDirs();
        if (!missingFiles.isEmpty()) {
            throw new IllegalStateException(this + " didn't generate " + missingFiles);
        }
        List<Path> result = new LinkedList<>();
        files.forEach(path -> result.add(path));
        extraFileSupplier.get().forEach(path -> result.add(path));
        return result;
    }

    /**
     * If this dependency can be represented as a project dependency in a IDE,
     * this field mentions the baseTree dir of the project.
     */
    public Path ideProjectBaseDir() {
        return ideProjectBaseDir;
    }


    @Override
    public String toString() {
        return this.runnable.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final JkComputedDependency that = (JkComputedDependency) o;

        return files.equals(that.files);
    }

    @Override
    public int hashCode() {
        return files.hashCode();
    }

}
