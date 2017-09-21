package org.jerkar.api.ide.eclipse;

import java.io.File;
import java.util.List;

import org.jerkar.api.depmanagement.JkDependencies;
import org.jerkar.api.file.JkFileTreeSet;
import org.jerkar.api.project.JkProjectSourceLayout;
import org.jerkar.api.project.java.JkJavaProject;
import org.jerkar.tool.JkException;
import org.jerkar.tool.builtins.javabuild.JkJavaBuild;

/**
 * Provides methods to modify a given {@link JkJavaProject} in order it reflects a given .classpath file.
 *
 */
public class JkEclipseJavaProjectApplier {

    private final boolean smartScope;

    public JkEclipseJavaProjectApplier(boolean smartScope) {
        this.smartScope = smartScope;
    }

    public void apply(JkJavaProject javaProject, File dotClasspath) {
        final Sources.TestSegregator segregator = smartScope ? Sources.SMART : Sources.ALL_PROD;
        final File baseDir = javaProject.getSourceLayout().baseDir();
        final DotClasspathModel dotClasspathModel = dotClasspathModel(baseDir);
        final JkFileTreeSet sources = dotClasspathModel.sourceDirs(baseDir, segregator).prodSources;
        final JkFileTreeSet testSources = dotClasspathModel.sourceDirs(baseDir, segregator).testSources;
        final JkFileTreeSet resources = dotClasspathModel.sourceDirs(baseDir, segregator).prodSources
                .andFilter(JkJavaBuild.RESOURCE_FILTER);
        final JkFileTreeSet testResources = dotClasspathModel.sourceDirs(baseDir, segregator).testSources
                .andFilter(JkJavaBuild.RESOURCE_FILTER);

        final ScopeResolver scopeResolver = scopeResolver(baseDir);
        final List<Lib> libs = dotClasspathModel.libs(baseDir, scopeResolver);
        final JkDependencies dependencies = Lib.toDependencies(/*build*/ null, libs, scopeResolver);

        JkProjectSourceLayout sourceLayout = javaProject.getSourceLayout();
        sourceLayout = sourceLayout.withSources(sources).withResources(resources)
                .withTests(testSources).withTestResources(testResources);
        javaProject.setSourceLayout(sourceLayout);
        javaProject.setDependencies(dependencies);
    }

    private ScopeResolver scopeResolver(File baseDir) {
        if (smartScope) {
            if (WstCommonComponent.existIn(baseDir)) {
                final WstCommonComponent wstCommonComponent = WstCommonComponent.of(baseDir);
                return new ScopeResolverSmart(wstCommonComponent);
            }
            return new ScopeResolverSmart(null);
        }
        return new ScopeResolverAllCompile();
    }

    private DotClasspathModel dotClasspathModel(File baseDir) {
        final File dotClasspathFile = new File(baseDir, ".classpath");
        if (!dotClasspathFile.exists()) {
            throw new JkException(".classpath file not found");
        }
        return DotClasspathModel.from(dotClasspathFile);
    }

}
