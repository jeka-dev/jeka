package org.jerkar.tool.builtins.eclipse;

import java.io.File;
import java.util.List;

import org.jerkar.api.depmanagement.JkDependencies;
import org.jerkar.api.file.JkFileTreeSet;
import org.jerkar.api.project.JkProjectSourceLayout;
import org.jerkar.api.project.java.JkJavaProject;
import org.jerkar.tool.JkBuildPlugin2;
import org.jerkar.tool.JkDoc;
import org.jerkar.tool.JkException;
import org.jerkar.tool.builtins.javabuild.JkJavaBuild;
import org.jerkar.tool.builtins.javabuild.JkJavaProjectBuild;

/**
 * Plugin to use Eclipse .classpath file as build definition. When this plugin is activated the .classpath
 * file is used over project setting set in the build class.
 */
public final class JkBuildPlugin2EclipseBase extends JkBuildPlugin2<JkJavaProjectBuild> {

    static final String OPTION_VAR_PREFIX = "eclipse.var.";

    /** Flag for resolving dependencies against the eclipse classpath */
    @JkDoc({ "Flag for resolving dependencies against the eclipse classpath",
        "but trying to segregate test to production code considering path names : ",
    "if path contains 'test' then this is considered as an entry source for scope 'test'." })
    public boolean smartScope = true;


    private DotClasspathModel cachedClasspath = null;

    /**
     * Constructs a JkBuildPlugin2EclipseBase.
     */
    protected JkBuildPlugin2EclipseBase(JkJavaProjectBuild build) {
        super(build);
    }


    // ------------------------ plugin methods ----------------------

    @Override
    protected void apply() {
        if (! (build() instanceof JkJavaProjectBuild)) {
            return;
        }
        final JkJavaProjectBuild build = build();
        final Sources.TestSegregator segregator = smartScope ? Sources.SMART : Sources.ALL_PROD;
        final JkFileTreeSet sources = dotClasspathModel().sourceDirs(build.file(""), segregator).prodSources;
        final JkFileTreeSet testSources = dotClasspathModel().sourceDirs(build.file(""), segregator).testSources;
        final JkFileTreeSet resources = dotClasspathModel().sourceDirs(build.file(""), segregator).prodSources
                .andFilter(JkJavaBuild.RESOURCE_FILTER);
        final JkFileTreeSet testResources = dotClasspathModel().sourceDirs(build.file(""), segregator).testSources
                .andFilter(JkJavaBuild.RESOURCE_FILTER);

        final ScopeResolver scopeResolver = scopeResolver();
        final List<Lib> libs = dotClasspathModel().libs(build.baseTree().root(), scopeResolver);
        final JkDependencies dependencies = Lib.toDependencies(build, libs, scopeResolver);

        final JkJavaProject project = build.project();
        JkProjectSourceLayout sourceLayout = project.getSourceLayout();
        sourceLayout = sourceLayout.withSources(sources).withResources(resources)
                .withTests(testSources).withTestResources(testResources);
        project.setSourceLayout(sourceLayout);
        project.setDependencies(dependencies);
    }

    private ScopeResolver scopeResolver() {
        if (smartScope) {
            if (WstCommonComponent.existIn(build().baseTree().root())) {
                final WstCommonComponent wstCommonComponent = WstCommonComponent.of(build()
                        .baseTree().root());
                return new ScopeResolverSmart(wstCommonComponent);
            }
            return new ScopeResolverSmart(null);
        }
        return new ScopeResolverAllCompile();
    }

    private DotClasspathModel dotClasspathModel() {
        if (cachedClasspath == null) {
            final File dotClasspathFile = new File(build().file(""), ".classpath");
            if (!dotClasspathFile.exists()) {
                throw new JkException(".classpath file not found");
            }
            cachedClasspath = DotClasspathModel.from(dotClasspathFile);
        }
        return cachedClasspath;
    }

}