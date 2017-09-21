package org.jerkar.tool.builtins.eclipse;

import org.jerkar.api.depmanagement.JkDependencies;
import org.jerkar.api.file.JkFileTreeSet;
import org.jerkar.api.ide.eclipse.JkEclipseClasspathGenerator;
import org.jerkar.api.project.JkProjectSourceLayout;
import org.jerkar.api.project.java.JkJavaProject;
import org.jerkar.api.system.JkLog;
import org.jerkar.api.utils.JkUtilsFile;
import org.jerkar.tool.*;
import org.jerkar.tool.builtins.javabuild.JkJavaBuild;
import org.jerkar.tool.builtins.javabuild.JkJavaProjectBuild;

import java.io.File;
import java.util.*;

/**
 * Provides method to generate and read Eclipse metadata files.
 */
public final class JkBuildPlugin2Eclipse extends JkBuildPlugin2<JkBuild> {

    static final String OPTION_VAR_PREFIX = "eclipse.var.";

    @JkDoc("Set it to false to not mention javadoc in generated .classpath file.")
    boolean javadoc = true;

    /** Flag for resolving dependencies against the eclipse classpath */
    @JkDoc({ "Flag for resolving dependencies against the eclipse classpath",
            "but trying to segregate test to production code considering path names : ",
            "if path contains 'test' then this is considered as an entry source for scope 'test'." })
    public boolean smartScope = true;

    /** If not null, this value will be used as the JRE container path when generating .classpath file.*/
    @JkDoc({ "If not null, this value will be used as the JRE container path when generating .classpath file." })
    public String jreContainer = null;

    /** Flag to set whether 'generateAll' task should use absolute paths instead of classpath variables */
    @JkDoc({ "Set it to true to use absolute paths in the classpath instead of classpath variables." })
    public boolean useAbsolutePathsInClasspath = false;

    private DotClasspathModel cachedClasspath = null;

    protected JkBuildPlugin2Eclipse(JkBuild build) {
        super(build);
    }

    // ------------------------- setters ----------------------------

    /** Set the JRE container to the Eclipse Standard VM type with the desired name. */
    public void setStandardJREContainer(String jreName) {
        jreContainer = "org.eclipse.jdt.launching.JRE_CONTAINER/org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType/" + jreName;
    }

    // ------------------------ plugin methods ----------------------

    @Override
    protected void apply() {
        if (! (build() instanceof JkJavaProjectBuild)) {
            return;
        }
        JkJavaProjectBuild build = (JkJavaProjectBuild) build();
        final Sources.TestSegregator segregator = smartScope ? Sources.SMART : Sources.ALL_PROD;
        JkFileTreeSet sources = dotClasspathModel().sourceDirs(build.file(""), segregator).prodSources;
        JkFileTreeSet testSources = dotClasspathModel().sourceDirs(build.file(""), segregator).testSources;
        JkFileTreeSet resources = dotClasspathModel().sourceDirs(build.file(""), segregator).prodSources
                .andFilter(JkJavaBuild.RESOURCE_FILTER);
        JkFileTreeSet testResources = dotClasspathModel().sourceDirs(build.file(""), segregator).testSources
                .andFilter(JkJavaBuild.RESOURCE_FILTER);

        final ScopeResolver scopeResolver = scopeResolver();
        final List<Lib> libs = dotClasspathModel().libs(build.baseTree().root(), scopeResolver);
        JkDependencies dependencies = Lib.toDependencies(build, libs, scopeResolver);

        JkJavaProject project = build.project();
        JkProjectSourceLayout sourceLayout = project.getSourceLayout();
        sourceLayout = sourceLayout.withSources(sources).withResources(resources)
                .withTests(testSources).withTestResources(testResources);
        project.setSourceLayout(sourceLayout);
        project.setDependencies(dependencies);
        build.scaffolder().extraActions.chain(this::generateFiles);
    }

    /** generate eclipse metadata files (as .classpath or .project) */
    @JkDoc("Generates Eclipse .classpath file according project dependencies.")
    public void generateFiles() {
        final File dotProject = build().file(".project");
        if (build() instanceof JkJavaProjectBuild) {
            final JkJavaProjectBuild javaProjectBuild = (JkJavaProjectBuild) build();
            JkJavaProject javaProject = javaProjectBuild.project();
            final List<File> importedBuildProjects = new LinkedList<>();
            for (final JkBuild depBuild : build().importedBuilds().directs()) {
                importedBuildProjects.add(depBuild.baseTree().root());
            }
            JkEclipseClasspathGenerator classpathGenerator = new JkEclipseClasspathGenerator(javaProject);
            classpathGenerator.setBuildDependencyResolver(build().buildDependencyResolver(), build().buildDependencies());
            classpathGenerator.setIncludeJavadoc(true);
            classpathGenerator.setJreContainer(this.jreContainer);
            classpathGenerator.setImportedBuildProjects(importedBuildProjects);
            classpathGenerator.setUsePathVariables( !this.useAbsolutePathsInClasspath);
            // generator.fileDependencyToProjectSubstitution = this.fileDependencyToProjectSubstitution;
            // generator.projectDependencyToFileSubstitutions = this.projectDependencyToFileSubstitutions;
            String result = classpathGenerator.generate();
            File dotClasspath = build().file(".classpath");
            JkUtilsFile.writeString(dotClasspath, result, false);

            if (!dotProject.exists()) {
                Project.ofJavaNature(build().baseTree().root().getName()).writeTo(dotProject);
            }
        } else {
            if (!dotProject.exists()) {
                Project.ofSimpleNature(build().baseTree().root().getName()).writeTo(dotProject);
            }
        }
    }

    /** Generate Eclipse files on all sub folders of the current directory **/
    @JkDoc("Generate Eclipse files on all subfolder of the current directory. Only subfolder having a build/def directory are impacted.")
    public void generateAll() {
        final Iterable<File> folders = build().baseTree()
                .include("**/" + JkConstants.BUILD_DEF_DIR)
                .exclude("**/build/output/**")
                .files(true);
        for (File folder : folders) {
            File projectFolder = folder.getParentFile().getParentFile();
            JkLog.startln("Generating Eclipse files on " + projectFolder);
            Main.exec(projectFolder, "eclipse#generateFiles");
            JkLog.done();
        }
    }

    protected void scaffold() {
        generateFiles();
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