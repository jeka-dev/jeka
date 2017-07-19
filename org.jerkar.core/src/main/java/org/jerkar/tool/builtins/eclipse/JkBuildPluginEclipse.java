package org.jerkar.tool.builtins.eclipse;

import java.io.File;
import java.util.*;

import org.jerkar.api.depmanagement.JkDependencies;
import org.jerkar.api.file.JkFileTreeSet;
import org.jerkar.api.system.JkLog;
import org.jerkar.tool.*;
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
        "but trying to segregate test go production code considering path names : ",
    "if path contains 'test' then this is considered as an entry source for scope 'test'." })
    public boolean smartScope = true;

    /** If not null, this value will be used as the JRE container path when generating .classpath file.*/
    @JkDoc({ "If not null, this value will be used as the JRE container path when generating .classpath file." })
    public String jreContainer = null;

    /** Set the JRE container to the Eclipse Standard VM type with the desired name. */
    public void setStandardJREContainer(String jreName) {
        jreContainer = "org.eclipse.jdt.launching.JRE_CONTAINER/org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType/" + jreName;
    }

    /** Flag to set whether 'generateAll' task should use absolute paths instead of classpath variables */
    @JkDoc({ "Set it to true to use absolute paths in the classpath instead of classpath variables." })
    public boolean useAbsolutePathsInClasspath = false;

    private DotClasspathModel cachedClasspath = null;

    /* see #useProjectDependencyInsteadOfFileFor */
    private final Map<File,File> fileDependencyToProjectSubstitution = new HashMap<File,File>();

    /* see #useFileDependencyInsteadOfProjectFor */
    private final Set<File> projectDependencyToFileSubstitutions = new HashSet<File>();

    /** generate eclipse metadata files (as .classpath or .project) */
    @JkDoc("Generates Eclipse .classpath file according project dependencies.")
    public void generateFiles() {
        final File dotProject = this.build.file(".project");
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
            generator.fileDependencyToProjectSubstitution = this.fileDependencyToProjectSubstitution;
            generator.useAbsolutePaths = this.useAbsolutePathsInClasspath;
            generator.projectDependencyToFileSubstitutions = this.projectDependencyToFileSubstitutions;
            generator.generate();

            if (!dotProject.exists()) {
                Project.ofJavaNature(this.javaBuild().moduleId().fullName()).writeTo(dotProject);
            }
        } else {
            if (!dotProject.exists()) {
                Project.ofSimpleNature(this.build.baseDir().root().getName()).writeTo(dotProject);
            }
        }
    }

    /** Generate Eclipse files on all sub folders of the current directory **/
    @JkDoc("Generate Eclipse files on all subfolder of the current directory. Only subfolder having a build/def directory are impacted.")
    public void generateAll() {
        final Iterable<File> folders = build.baseDir()
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

    /**
     * If your project has a dependency on file/folder (a jar or a class dir) that is the result of the compilation
     * of another Eclipse project, then you can generate .classpath in such it has a dependency on the project itself
     * instead of the file/folder.
     *
     * @param projectDir root folder of the project generating the class jar
     * @param dependencyFile the dependency file your project depends on.
     */
    public void useProjectDependencyInsteadOfFileFor(File projectDir, File dependencyFile) {
        fileDependencyToProjectSubstitution.put(dependencyFile, projectDir);
    }

    /**
     * If your project has a dependency on computed dependency go a slave project
     * (generally declared as <code>.on(slaveBuild.asJavaDependency())</code>), Eclipse will generate a .classpath
     * with a dependency of slave project. <br/>
     * If you want Eclipse .classpath uses jar file produced by this project along its transitive dependencies instead
     * of the project itself, this method will tell Eclipse plugin to use jar file + transitive dependencies for the
     * specified project
     *
     * @param projectDirs root folder of the projects for which you don't want to use Eclipse project dependency.
     */
    public void useFileDependencyInsteadOfProjectFor(File ... projectDirs) {
        for (File projectDir : projectDirs) {
            projectDependencyToFileSubstitutions.add(projectDir);
        }
    }

    /**
     * Shorthand for {@link #useFileDependencyInsteadOfProjectFor(File...)}
     */
    public void useFileDependencyInsteadOfProjectFor(JkJavaBuild ... javaBuilds) {
        for (JkJavaBuild javaBuild : javaBuilds) {
            projectDependencyToFileSubstitutions.add(javaBuild.baseDir().root());
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
     * @deprecated  Use the opposite {@link #useProjectDependencyInsteadOfFileFor(File, File)} instead (be careful arguments are inverted).
     */
    @Deprecated
    public void addProjectFromClasses(File classesDir, File projectDir) {
        fileDependencyToProjectSubstitution.put(classesDir, projectDir);
    }



}
