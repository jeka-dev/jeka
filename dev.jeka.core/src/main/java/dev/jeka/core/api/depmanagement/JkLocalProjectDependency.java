package dev.jeka.core.api.depmanagement;

import java.nio.file.Path;
import java.util.Collections;


/*
 * A dependency on a jar produced locally along its dependencies for consumers.
 */
public class JkLocalProjectDependency extends JkComputedDependency  {

    // published dependencies
    private JkDependencySet transitiveDependencies;

    /*
     * Constructs a {@link JkLocalProjectDependency} from an artifact producer and the artifact file id
     * one is interested on.
     */
    private JkLocalProjectDependency(Runnable producer, Path file, Path ideProjectDir, JkDependencySet transitiveDependencies) {
        super(producer, ideProjectDir, Collections.singleton(file));
        this.transitiveDependencies = transitiveDependencies.withIdeProjectDir(ideProjectDir);
    }

    /**
     * Constructs a {@link JkLocalProjectDependency} from an artifact producer and the artifact file id
     * one is interested on.
     * @param producer The runnable producing the jar file.
     * @param file The jar file
     * @param basedir The base directory of the project producing the jar file. Optional (IDE support)
     * @param dependencies The dependencies that will be consumed by the depender. It's not the
     *                     the dependencies needed to compile the jar but the ones that would be
     *                     published.
     */
    public static JkLocalProjectDependency of(Runnable producer, Path file, Path basedir, JkDependencySet dependencies) {
        return new JkLocalProjectDependency(producer, file, basedir, dependencies);

    }

    /**
     * Returns the dependencies that will be consumed by the depender. This is not the
     * the dependencies needed to compile the jar but the ones that would be published.
     */
    public JkDependencySet getTransitiveDependencies() {
        return transitiveDependencies;
    }

    @Override
    public JkLocalProjectDependency withIdeProjectDir(Path path) {
        return new JkLocalProjectDependency(runnable, files.iterator().next(), path, transitiveDependencies);
    }
}
