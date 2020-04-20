package dev.jeka.core.api.depmanagement;

import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.java.JkJavaProcess;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.system.JkProcess;
import dev.jeka.core.api.utils.JkUtilsIterable;
import dev.jeka.core.api.utils.JkUtilsPath;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Dependency on computed resource. More concretely, this is a file dependency on files that might not
 * be present at the time of the build and that has to be generated. Instances of this class are
 * responsible to generate the missing files. <p>
 *
 * Computed dependencies are instantiated by providing expected files and a {@link Runnable} that
 * generates these files in case one of them misses. <p>
 *
 * This is yet simple but quite powerful mechanism, cause the runnable can be anything as Maven or ANT build
 * of another project, a Jeka build of another project, ... <p>
 *
 * This is the way for creating multi-projet (and multi-techno if desired) builds.
 *
 */
public class JkComputedDependency implements JkFileDependency {

    protected final Runnable runnable;

    protected final Iterable<Path> files;

    private final Path ideProjectDir; // Helps to generate ide metadata

    /**
     * Constructs a computed dependency to the specified files and the specified {@link Runnable} to run for
     * generating them.
     */
    protected JkComputedDependency(Runnable runnable, Path ideProjectBaseDir, Iterable<Path> files)  {
        super();
        this.runnable = runnable;
        this.files = files;
        this.ideProjectDir = ideProjectBaseDir;
    }

    /**
     * Creates a computed dependency to the specified files and {@link JkProcess} to run for
     * generating them.
     */
    public static JkComputedDependency of(final JkProcess process, Path... files) {
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
        return new JkComputedDependency(runnable, null, fileSet);
    }


    /**
     * Creates a computed dependency to the specified files and the specified {@link Runnable} to run for
     * generating them.
     */
    public static JkComputedDependency of(Runnable runnable, Path... files) {
        final List<Path> fileSet = JkUtilsIterable.listWithoutDuplicateOf(Arrays.asList(files));
        return new JkComputedDependency(runnable, null, fileSet);
    }

    /**
     * Same as {@link #of(Path, JkJavaProcess, String, String...)} but you must specify a set of files
     * instead of a single one.
     */
    public static JkComputedDependency of(Iterable<Path> files, final JkJavaProcess process,
            final String className, final String... args) {
        final List<Path> fileSet = JkUtilsIterable.listWithoutDuplicateOf(JkUtilsPath.disambiguate(files));
        final Runnable runnable = () -> process.runClassSync(className, args);
        return new JkComputedDependency(runnable, null, fileSet);
    }

    /**
     * Creates a computed dependency to the specified file and the specified java program to run for
     * generating them.
     */
    public static JkComputedDependency of(Path file, final JkJavaProcess process,
            final String className, final String... args) {
        return of(JkUtilsIterable.setOf(file), process, className, args);
    }



    /**
     * Returns <code>true</code> if at least one of these files is missing or one of these directory is empty.
     */
    public final boolean hasMissingFilesOrEmptyDirs() {
        return !getMissingFilesOrEmptyDirs().isEmpty();
    }

    /**
     * Returns the missing files or empty directory for this dependency.
     */
    public final Set<Path> getMissingFilesOrEmptyDirs() {
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
    public List<Path> getFiles() {
        if (this.hasMissingFilesOrEmptyDirs()) {
            JkLog.startTask("Building dependency : " + this);
            runnable.run();
            JkLog.endTask();
        }
        final Set<Path> missingFiles = this.getMissingFilesOrEmptyDirs();
        if (!missingFiles.isEmpty()) {
            throw new IllegalStateException(this + " didn't generate " + missingFiles);
        }
        final List<Path> result = new LinkedList<>();
        files.forEach(path -> result.add(path));
        return result;
    }

    /**
     * If this dependency can be represented as a project dependency in a IDE,
     * this field mentions the baseTree dir of the project.
     */
    public Path getIdeProjectDir() {
        return ideProjectDir;
    }

    @Override
    public JkComputedDependency withIdeProjectDir(Path path) {
        return new JkComputedDependency(runnable, path, files);
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
