package dev.jeka.core.api.depmanagement;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


/*
 * A dependency on a jar produced locally along its dependencies for consumers.
 */
public class JkLocalProjectDependency extends JkComputedDependency
        implements JkTransitivityDependency {

    // exported dependencies
    private JkDependencySet exportedDependencies;

    private JkTransitivity transitivity;

    /*
     * Constructs a {@link JkLocalProjectDependency} from an artifact producer and the artifact file id
     * one is interested on.
     */
    private JkLocalProjectDependency(Runnable producer, Path file, Path ideProjectDir,
                                     JkDependencySet exportedDependencies,
                                     JkTransitivity transitivity) {
        super(producer, ideProjectDir, Collections.singleton(file));
        List<JkDependency> relocatedDependencies = exportedDependencies.getEntries().stream()
                .map(dep -> dep.withIdeProjectDir(ideProjectDir))
                .collect(Collectors.toList());
        this.exportedDependencies = JkDependencySet.of(relocatedDependencies)
                .withGlobalExclusions(exportedDependencies.getGlobalExclusions())
                .withVersionProvider(exportedDependencies.getVersionProvider());
        this.transitivity = transitivity;
    }

    /**
     * Constructs a {@link JkLocalProjectDependency} from an artifact producer and the artifact file id
     * one is interested on.
     * @param producer The runnable producing the jar file.
     * @param file The jar file
     * @param basedir The base directory of the project producing the jar file. Optional (IDE support)
     * @param dependencies The dependencies that will be consumed by the depender. It's not
     *                     the dependencies needed to compile the jar but the ones that would be
     *                     published.
     */
    public static JkLocalProjectDependency of(Runnable producer, Path file, Path basedir,
                                              JkDependencySet dependencies) {
        return new JkLocalProjectDependency(producer, file, basedir, dependencies, null);
    }

    /**
     * Returns the dependencies that will be consumed by the depender. This is not
     * the dependencies needed to compile the jar but the ones that would be published.
     */
    public JkDependencySet getExportedDependencies() {
        if (this.transitivity == null || this.transitivity == JkTransitivity.RUNTIME) {
            return exportedDependencies;
        }
        if (transitivity == JkTransitivity.COMPILE) {
            List<JkDependency> filteredDependencies = exportedDependencies.getEntries().stream()
                    .filter(JkTransitivityDependency.class::isInstance)
                    .map(JkTransitivityDependency.class::cast)
                    .filter(dep -> JkTransitivity.COMPILE.equals(dep.getTransitivity()))
                    .collect(Collectors.toList());
            return JkDependencySet.of(filteredDependencies)
                    .withVersionProvider(exportedDependencies.getVersionProvider())
                    .withGlobalExclusions(exportedDependencies.getGlobalExclusions());
        }
        return JkDependencySet.of();
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
                JkDependencySet.of(), transitivity);
    }

    @Override
    public String toString() {
        return "Project : " + this.getIdeProjectDir();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        JkLocalProjectDependency that = (JkLocalProjectDependency) o;

        if (!exportedDependencies.equals(that.exportedDependencies)) return false;
        return transitivity != null ? transitivity.equals(that.transitivity) : that.transitivity == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + exportedDependencies.hashCode();
        result = 31 * result + (transitivity != null ? transitivity.hashCode() : 0);
        return result;
    }
}
