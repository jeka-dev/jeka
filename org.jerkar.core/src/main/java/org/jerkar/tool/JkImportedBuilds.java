package org.jerkar.tool;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.jerkar.api.depmanagement.JkDependencies;
import org.jerkar.api.depmanagement.JkScopedDependency;
import org.jerkar.api.system.JkLog;
import org.jerkar.api.utils.JkUtilsFile;
import org.jerkar.api.utils.JkUtilsIterable;

/**
 * Defines importedBuilds of a given master build.
 * 
 * @author Jerome Angibaud
 */
public final class JkImportedBuilds {

    static JkImportedBuilds of(File masterRootDir, List<JkBuild> builds) {
        return new JkImportedBuilds(masterRootDir, new ArrayList<JkBuild>(builds));
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
     * Returns a {@link JkImportedBuilds} identical to this one but augmented with
     * the {@link JkBuildDependency} contained in the the specified dependencies.
     */
    public JkImportedBuilds and(JkDependencies dependencies) {
        final List<JkBuild> list = projectBuildDependencies(dependencies);
        return this.and(list);
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
            transitiveImports = resolveTransitiveBuilds(new HashSet<File>());
        }
        return transitiveImports;
    }

    /**
     * Execute the <code>doDefault</code> on all importedBuilds.
     */
    public void invokeDoDefaultMethodOnAll() {
        this.invokeOnAll(JkConstants.DEFAULT_METHOD);
    }

    /**
     * Executes the specified methods on all importedBuilds.
     */
    public void invokeOnAll(String... methods) {
        this.executeOnAll(JkModelMethod.normals(methods));
    }

    private void executeOnAll(Iterable<JkModelMethod> methods) {
        JkLog.startln("Invoke " + methods + " on all dependents projects");
        for (final JkBuild build : all()) {
            build.execute(methods, this.masterBuildRoot);
        }
        JkLog.done("invoking " + methods + " on all dependents projects");
    }

    private List<JkBuild> resolveTransitiveBuilds(Set<File> files) {
        final List<JkBuild> result = new LinkedList<JkBuild>();
        for (final JkBuild build : directImports) {
            final File dir = JkUtilsFile.canonicalFile(build.baseDir().root());
            if (!files.contains(dir)) {
                if (build instanceof JkBuildDependencySupport) {
                    final JkBuildDependencySupport buildDependencySupport = (JkBuildDependencySupport) build;
                    result.addAll(buildDependencySupport.importedBuilds().resolveTransitiveBuilds(files));
                }
                result.add(build);
                files.add(dir);
            }
        }
        return result;
    }

    private static List<JkBuild> projectBuildDependencies(JkDependencies dependencies) {
        final List<JkBuild> result = new LinkedList<JkBuild>();
        for (final JkScopedDependency scopedDependency : dependencies) {
            if (scopedDependency.dependency() instanceof JkBuildDependency) {
                final JkBuildDependency projectDependency = (JkBuildDependency) scopedDependency
                        .dependency();
                result.add(projectDependency.projectBuild());
            }
        }
        return result;
    }



}
