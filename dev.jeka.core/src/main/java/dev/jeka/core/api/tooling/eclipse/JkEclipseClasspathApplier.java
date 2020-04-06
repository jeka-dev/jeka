package dev.jeka.core.api.tooling.eclipse;


import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.file.JkPathTreeSet;
import dev.jeka.core.api.java.project.JkCompileLayout;
import dev.jeka.core.api.java.project.JkJavaProject;
import dev.jeka.core.api.system.JkException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Provides methods to modify a given {@link JkJavaProject} in order it reflects a given .classpath file.
 */
public class JkEclipseClasspathApplier {

    private final boolean smartScope;

    /**
     * Constructs a {@link JkEclipseClasspathApplier}.
     * @param smartScope if <code>true</code> dependencies on test libraries will be affected with TEST scope.
     *                   Otherwise every libs is affected with default scope.
     */
    public JkEclipseClasspathApplier(boolean smartScope) {
        this.smartScope = smartScope;
    }

    /**
     * Modifies the specified javaProject in a way it reflects its eclipse .classpath file.
     */
    public void apply(JkJavaProject javaProject) {
        final Path dotClasspathFile = javaProject.getBaseDir().resolve(".classpath");
        if (!Files.exists(dotClasspathFile)) {
            throw new JkException(".classpath file not found in " + javaProject.getBaseDir());
        }
        apply(javaProject, DotClasspathModel.from(dotClasspathFile));
    }

    private void apply(JkJavaProject javaProject, DotClasspathModel dotClasspathModel) {
        final Sources.TestSegregator segregator = smartScope ? Sources.SMART : Sources.ALL_PROD;
        final Path baseDir = javaProject.getBaseDir();
        final JkPathTreeSet sources = dotClasspathModel.sourceDirs(baseDir, segregator).prodSources;
        final JkPathTreeSet testSources = dotClasspathModel.sourceDirs(baseDir, segregator).testSources;
        final JkPathTreeSet resources = dotClasspathModel.sourceDirs(baseDir, segregator).prodSources
                .andMatcher(JkCompileLayout.JAVA_RESOURCE_MATCHER);
        final JkPathTreeSet testResources = dotClasspathModel.sourceDirs(baseDir, segregator).testSources
                .andMatcher(JkCompileLayout.JAVA_RESOURCE_MATCHER);
        final ScopeResolver scopeResolver = scopeResolver(baseDir);
        final List<Lib> libs = dotClasspathModel.libs(baseDir, scopeResolver);
        final JkDependencySet dependencies = Lib.toDependencies(javaProject.getBaseDir(), libs, this);
        javaProject.getCompilation().getLayout()
                .setSources(sources)
                .setResources(resources);
        javaProject.getTesting().getCompilation().getLayout()
                .setSources(testSources)
                .setResources(testResources);
        javaProject.getDependencyManagement()
                .removeDependencies()
                .addDependencies(dependencies);
    }

    private ScopeResolver scopeResolver(Path baseDir) {
        if (smartScope) {
            if (WstCommonComponent.existIn(baseDir)) {
                final WstCommonComponent wstCommonComponent = WstCommonComponent.of(baseDir);
                return new ScopeResolverSmart(wstCommonComponent);
            }
            return new ScopeResolverSmart(null);
        }
        return new ScopeResolverAllCompile();
    }


}
