package dev.jeka.core.api.depmanagement;

import java.nio.file.Path;
import java.util.Collections;


/*
 * A dependency on a jar produced locally along its dependencies for consumers.
 */
public class JkLocalLibDependency extends JkComputedDependency  {

    // published dependencies
    private JkDependencySet dependencies;

    /*
     * Constructs a {@link JkLocalLibDependency} from an artifact producer and the artifact file id
     * one is interested on.
     */
    private JkLocalLibDependency(Runnable producer, Path file, Path ideProjectDir, JkDependencySet dependencies) {
        super(producer, ideProjectDir, Collections.singleton(file));
        this.dependencies = dependencies.withIdeProjectDir(ideProjectDir);
    }

    /**
     * Constructs a {@link JkLocalLibDependency} from an artifact producer and the artifact file id
     * one is interested on.
     * @param producer The runnable producing the jar file.
     * @param file The jar file
     * @param basedir The base directory of the project producing the jar file. Optional (IDE support)
     * @param dependencies The dependencies that will be consumed by the depender. It's not the
     *                     the dependencies needed to compile the jar but the ones that would be
     *                     published.
     */
    public static JkLocalLibDependency of(Runnable producer, Path file, Path basedir, JkDependencySet dependencies) {
        return new JkLocalLibDependency(producer, file, basedir, dependencies);

    }

    /**
     * Returns the dependencies that will be consumed by the depender. This is not the
     * the dependencies needed to compile the jar but the ones that would be published.
     */
    public JkDependencySet getDependencies() {
        return dependencies;
    }

    @Override
    public JkLocalLibDependency withIdeProjectDir(Path path) {
        return new JkLocalLibDependency(runnable, files.iterator().next(), path, dependencies);
    }
}
