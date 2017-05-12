package org.jerkar.tool.builtins.eclipse;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.jerkar.api.depmanagement.JkDependencies;
import org.jerkar.api.file.JkFileTreeSet;
import org.jerkar.api.system.JkLog;
import org.jerkar.tool.JkBuild;
import org.jerkar.tool.JkDoc;
import org.jerkar.tool.JkException;
import org.jerkar.tool.JkInit;
import org.jerkar.tool.builtins.javabuild.JkJavaBuild;
import org.jerkar.tool.builtins.javabuild.JkJavaBuildPlugin;

/**
 * Provides method to generate and read Eclipse metadata files.
 */
public final class JkBuildPluginEclipse extends JkJavaBuildPlugin {

    static final String OPTION_VAR_PREFIX = "eclipse.var.";

    private JkBuild build;

    @JkDoc("Set it to false to not mention javadoc in generated .classpath file.")
    boolean javadoc = true;

    /** Flag for resolving dependencies against the eclipse classpath */
    @JkDoc({ "Flag for resolving dependencies against the eclipse classpath",
        "but trying to segregate test from production code considering path names : ",
    "if path contains 'test' then this is considered as an entry source for scope 'test'." })
    public boolean smartScope = true;

    /** If not null, this value will be used as the JRE container path when generating .classpath file.*/
    @JkDoc({ "If not null, this value will be used as the JRE container path when generating .classpath file." })
    public String jreContainer = null;

    /** Set the JRE container to the Eclipse Standard VM type with the desired name. */
    public void setStandardJREContainer(String jreName) {
        jreContainer = "org.eclipse.jdt.launching.JRE_CONTAINER/org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType/" + jreName;
    }

    /** Flag to set whether 'generateFiles' task should use absolute paths instead of classpath variables */
    @JkDoc({ "Set it to true to use absolute paths in the classpath instead of classpath variables." })
    public boolean useAbsolutePathsInClasspath = false;

    private DotClasspathModel cachedClasspath = null;

    private final Map<File,File> projectDirsByClassDirs = new HashMap<File,File>();

    /** generate eclipse metadata files (as .classpath or .project) */
    @JkDoc("Generates Eclipse .classpath file according project dependencies.")
    public void generateFiles() {
        if (this.build instanceof JkJavaBuild) {
            final JkJavaBuild jbuild = (JkJavaBuild) build;
            final List<File> depProjects = new LinkedList<File>();
            for (final JkBuild depBuild : build.slaves().directs()) {
                depProjects.add(depBuild.baseDir().root());
            }
            final DotClasspathGenerator generator = new DotClasspathGenerator(build.baseDir().root());
            generator.dependencyResolver = ((JkJavaBuild) build).dependencyResolver();
            generator.buildDefDependencyResolver = ((JkJavaBuild) build).buildDefDependencyResolver();
            generator.includeJavadoc = true;
            generator.jreContainer = this.jreContainer;
            generator.projectDependencies = depProjects;
            generator.sourceJavaVersion = jbuild.javaSourceVersion();
            generator.sources  = jbuild.sources().and(jbuild.resources());
            generator.testSources  = jbuild.unitTestSources().and(jbuild.unitTestResources());
            generator.testClassDir = jbuild.testClassDir();
            generator.projectDirsByClassDirs = this.projectDirsByClassDirs;
            generator.useAbsolutePaths = this.useAbsolutePathsInClasspath;
            generator.generate();
        }
        final File dotProject = this.build.file(".project");
        if (!dotProject.exists()) {
            Project.ofJavaNature(this.javaBuild().moduleId().fullName()).writeTo(dotProject);
        }
    }

    @Override
    public JkFileTreeSet alterSourceDirs(JkFileTreeSet original) {
        final Sources.TestSegregator segregator = smartScope ? Sources.SMART : Sources.ALL_PROD;
        return dotClasspathModel().sourceDirs(build.file(""), segregator).prodSources;
    }

    @Override
    public JkFileTreeSet alterTestSourceDirs(JkFileTreeSet original) {
        final Sources.TestSegregator segregator = smartScope ? Sources.SMART : Sources.ALL_PROD;
        return dotClasspathModel().sourceDirs(build.file(""), segregator).testSources;
    }

    @Override
    public JkFileTreeSet alterResourceDirs(JkFileTreeSet original) {
        final Sources.TestSegregator segregator = smartScope ? Sources.SMART : Sources.ALL_PROD;
        return dotClasspathModel().sourceDirs(build.file(""), segregator).prodSources
                .andFilter(JkJavaBuild.RESOURCE_FILTER);
    }

    @Override
    public JkFileTreeSet alterTestResourceDirs(JkFileTreeSet original) {
        final Sources.TestSegregator segregator = smartScope ? Sources.SMART : Sources.ALL_PROD;
        return dotClasspathModel().sourceDirs(build.file(""), segregator).testSources
                .andFilter(JkJavaBuild.RESOURCE_FILTER);
    }

    @Override
    protected JkDependencies alterDependencies(JkDependencies original) {
        final ScopeResolver scopeResolver = scopeResolver();
        final List<Lib> libs = dotClasspathModel().libs(build.baseDir().root(), scopeResolver);
        return Lib.toDependencies(this.javaBuild(), libs, scopeResolver);
    }

    @Override
    protected void scaffold() {
        try {
            JkLog.startln("Trying to generate Eclipse metadata files");

            // We create a new build instance to avoid duplication in classpath concerning
            // elements already present in this classloader
            final JkBuild newBuild = JkInit.instanceOf(this.build.baseDir().root());
            final JkBuildPluginEclipse pluginEclipse = new JkBuildPluginEclipse();
            pluginEclipse.build = newBuild;
            pluginEclipse.generateFiles();
            JkLog.done("Eclipse file generated successfully");
        } catch (final RuntimeException e) {
            e.printStackTrace(JkLog.warnStream());
            JkLog.done("Eclipse files has not been generated due to a failure");
        }

    }

    private ScopeResolver scopeResolver() {
        if (smartScope) {
            if (WstCommonComponent.existIn(build.baseDir().root())) {
                final WstCommonComponent wstCommonComponent = WstCommonComponent.of(build
                        .baseDir().root());
                return new ScopeResolverSmart(wstCommonComponent);
            }
            return new ScopeResolverSmart(null);
        }
        return new ScopeResolverAllCompile();
    }

    private DotClasspathModel dotClasspathModel() {
        if (cachedClasspath == null) {
            final File dotClasspathFile = new File(build.file(""), ".classpath");
            if (!dotClasspathFile.exists()) {
                throw new JkException(".classpath file not found");
            }
            cachedClasspath = DotClasspathModel.from(dotClasspathFile);
        }
        return cachedClasspath;
    }

    @Override
    public void configure(JkBuild build) {
        this.build = build;
    }

    private JkJavaBuild javaBuild() {
        return (JkJavaBuild) this.build;
    }

    /**
     * When a file dependency is found at classesDir, make a project reference instead of a class folder reference in the .classpath file.
     * @param classesDir directory of the classes
     * @param projectDir directory of the project
     */
    public void addProjectFromClasses(File classesDir, File projectDir) {
        projectDirsByClassDirs.put(classesDir, projectDir);
    }
}
