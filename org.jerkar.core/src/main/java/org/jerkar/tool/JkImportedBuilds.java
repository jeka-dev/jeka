package org.jerkar.tool;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.jerkar.api.utils.JkUtilsFile;
import org.jerkar.api.utils.JkUtilsIterable;

/**
 * Defines importedBuilds of a given master build.
 * 
 * @author Jerome Angibaud
 */
public final class JkImportedBuilds {

    static JkImportedBuilds of(File masterRootDir, List<JkBuild> builds) {
        return new JkImportedBuilds(masterRootDir, new ArrayList<>(builds));
    }

    private final List<JkBuild> directImports;

    private List<JkBuild> transitiveImports;

    private final File masterBuildRoot;

    private JkImportedBuilds(File masterDir, List<JkBuild> buildDeps) {
        super();
        this.masterBuildRoot = masterDir;
        this.directImports = Collections.unmodifiableList(buildDeps);
    }

    /**
     * Returns a {@link JkImportedBuilds} identical to this one but augmented with
     * specified slave builds.
     */
    @SuppressWarnings("unchecked")
    public JkImportedBuilds and(List<JkBuild> slaves) {
        return new JkImportedBuilds(this.masterBuildRoot, JkUtilsIterable.concatLists(
                this.directImports, slaves));
    }



    /**
     * Returns only the direct slave of this master build.
     */
    public List<JkBuild> directs() {
        return Collections.unmodifiableList(directImports);
    }

    /**
     * Returns direct and transitive importedBuilds. Transitive importedBuilds are resolved by
     * invoking recursively <code>JkBuildDependencySupport#importedBuilds()</code> on
     * direct importedBuilds.
     * 
     */
    public List<JkBuild> all() {
        if (transitiveImports == null) {
            transitiveImports = resolveTransitiveBuilds(new HashSet<>());
        }
        return transitiveImports;
    }

    /**
     * Same as {@link #all()} but only returns builds instance of the specified class or its subclasses.
     */
    public <T extends JkBuild> List<T> allOf(Class<T> ofClass) {
        List<T> result = new LinkedList<>();
        for (JkBuild build : all()) {
            if (ofClass.isAssignableFrom(build.getClass())) {
                result.add((T) build);
            }
        }
        return result;
    }

    private List<JkBuild> resolveTransitiveBuilds(Set<File> files) {
        final List<JkBuild> result = new LinkedList<>();
        for (final JkBuild build : directImports) {
            final File dir = JkUtilsFile.canonicalFile(build.baseTree().root());
            if (!files.contains(dir)) {
                result.addAll(build.importedBuilds().resolveTransitiveBuilds(files));
                result.add(build);
                files.add(dir);
            }
        }
        return result;
    }

}
