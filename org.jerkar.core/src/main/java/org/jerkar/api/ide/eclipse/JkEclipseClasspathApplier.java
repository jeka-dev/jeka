package org.jerkar.api.ide.eclipse;


import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.jerkar.api.depmanagement.JkDependencies;
import org.jerkar.api.file.JkFileTreeSet;
import org.jerkar.api.project.JkProjectSourceLayout;
import org.jerkar.api.project.java.JkJavaProject;
import org.jerkar.tool.JkException;

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
        final Path dotClasspathFile =javaProject.getSourceLayout().baseDir().toPath().resolve(".classpath");
        if (!Files.exists(dotClasspathFile)) {
            throw new JkException(".classpath file not found in " + javaProject.getSourceLayout().baseDir());
        }
        apply(javaProject, DotClasspathModel.from(dotClasspathFile));
    }

    private void apply(JkJavaProject javaProject, DotClasspathModel dotClasspathModel) {
        final Sources.TestSegregator segregator = smartScope ? Sources.SMART : Sources.ALL_PROD;
        final Path baseDir = javaProject.getSourceLayout().baseDir().toPath();
        final JkFileTreeSet sources = dotClasspathModel.sourceDirs(baseDir, segregator).prodSources;
        final JkFileTreeSet testSources = dotClasspathModel.sourceDirs(baseDir, segregator).testSources;
        final JkFileTreeSet resources = dotClasspathModel.sourceDirs(baseDir, segregator).prodSources
                .andFilter(JkProjectSourceLayout.JAVA_RESOURCE_FILTER);
        final JkFileTreeSet testResources = dotClasspathModel.sourceDirs(baseDir, segregator).testSources
                .andFilter(JkProjectSourceLayout.JAVA_RESOURCE_FILTER);

        final ScopeResolver scopeResolver = scopeResolver(baseDir);
        final List<Lib> libs = dotClasspathModel.libs(baseDir, scopeResolver);
        final JkDependencies dependencies = Lib.toDependencies(/*build*/
                javaProject.getSourceLayout().baseDir().toPath(), libs, this);

        JkProjectSourceLayout sourceLayout = javaProject.getSourceLayout();
        sourceLayout = sourceLayout.withSources(sources).withResources(resources)
                .withTests(testSources).withTestResources(testResources);
        javaProject.setSourceLayout(sourceLayout);
        javaProject.setDependencies(dependencies);
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
