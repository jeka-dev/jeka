package dev.jeka.core.api.depmanagement;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


/*
 * A dependency on a jar produced locally along its dependencies for consumers.
 */
public class JkLocalProjectDependency extends JkComputedDependency
        implements JkFileDependency.JkTransitivityDependency {

    // exported dependencies
    private List<JkDependency> exportedDependencies;

    private JkTransitivity transitivity;

    /*
     * Constructs a {@link JkLocalProjectDependency} from an artifact producer and the artifact file id
     * one is interested on.
     */
    private JkLocalProjectDependency(Runnable producer, Path file, Path ideProjectDir,
                                     List<JkDependency> exportedDependencies,
                                     JkTransitivity transitivity) {
        super(producer, ideProjectDir, Collections.singleton(file));
        this.exportedDependencies = exportedDependencies.stream()
                .map(dep -> dep.withIdeProjectDir(ideProjectDir))
                .collect(Collectors.toList());
        this.transitivity = transitivity;
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
    public static JkLocalProjectDependency of(Runnable producer, Path file, Path basedir,
                                              List<JkDependency> dependencies) {
        return new JkLocalProjectDependency(producer, file, basedir, dependencies, null);
    }

    /**
     * Returns the dependencies that will be consumed by the depender. This is not the
     * the dependencies needed to compile the jar but the ones that would be published.
     */
    public List<JkDependency> getExportedDependencies() {
        if (this.transitivity == null || this.transitivity == JkTransitivity.RUNTIME) {
            return exportedDependencies;
        }
        if (transitivity == JkTransitivity.COMPILE) {
            return exportedDependencies.stream()
                    .filter(JkTransitivityDependency.class::isInstance)
                    .map(JkTransitivityDependency.class::cast)
                    .filter(dep -> JkTransitivity.COMPILE.equals(dep.getTransitivity()))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();  // Transitivity == NONE
    }

    public JkTransitivity getTransitivity() {
        return transitivity;
    }

    @Override
    public JkLocalProjectDependency withIdeProjectDir(Path path) {
        return new JkLocalProjectDependency(runnable, files.iterator().next(), path, exportedDependencies,
                transitivity);
    }

    public JkLocalProjectDependency withTransitivity(JkTransitivity transitivity) {
        return new JkLocalProjectDependency(runnable, files.iterator().next(), getIdeProjectDir(),
                exportedDependencies, transitivity);
    }

    public JkLocalProjectDependency withoutExportedDependencies() {
        return new JkLocalProjectDependency(runnable, files.iterator().next(), getIdeProjectDir(),
                Collections.emptyList(), transitivity);
    }
}
