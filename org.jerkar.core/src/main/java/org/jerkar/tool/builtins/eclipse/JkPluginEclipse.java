package org.jerkar.tool.builtins.eclipse;


import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.jerkar.api.ide.eclipse.JkEclipseClasspathGenerator;
import org.jerkar.api.ide.eclipse.JkEclipseProject;
import org.jerkar.api.project.java.JkJavaProject;
import org.jerkar.api.system.JkLog;
import org.jerkar.api.utils.JkUtilsPath;
import org.jerkar.tool.*;
import org.jerkar.tool.builtins.java.JkJavaProjectBuild;
import org.jerkar.tool.builtins.java.JkPluginJava;

@JkDoc("Generation of Eclipse files (.project and .classpath) from actual project structure and dependencies.")
@JkDocPluginDeps(JkPluginJava.class)
public final class JkPluginEclipse extends JkPlugin {

    @JkDoc("If true, .classpath will include javadoc reference for declared dependencies.")
    boolean javadoc = true;

    /** If not null, this value will be used as the JRE container path when generating .classpath file.*/
    @JkDoc({ "If not null, this value will be used as the JRE container path in .classpath." })
    public String jreContainer = null;

    /** Flag to set whether 'generateAll' task should use absolute paths instead of classpath variables */
    @JkDoc({ "If true, dependency paths will be expressed relatively to Eclipse path variables instead of absolute paths." })
    public boolean useVarPath = false;

    protected JkPluginEclipse(JkBuild build) {
        super(build);
    }

    // ------------------------- setters ----------------------------

    /** Set the JRE container to the Eclipse Standard VM type with the desired name. */
    public void setStandardJREContainer(String jreName) {
        jreContainer = "org.eclipse.jdt.launching.JRE_CONTAINER/org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType/" + jreName;
    }

    // ------------------------ plugin methods ----------------------

    @Override
    @JkDoc("Adds .classpath and .project generation to scaffolding.")
    protected void decorateBuild() {
        build.scaffolder().extraActions.chain(this::generateFiles);  // If this plugin is activated while scaffolding, we want Eclipse metada file be generated.
    }

    @JkDoc("Generates Eclipse files (.classpath and .project) in the current directory. The files reflect project " +
            "dependencies and source layout.")
    public void generateFiles() {
        final Path dotProject = build.baseDir().resolve(".project");
        if (build.plugins.has(JkPluginJava.class)) {
            final JkJavaProjectBuild javaBuild = (JkJavaProjectBuild) build;
            final JkJavaProject javaProject = javaBuild.java().project();
            final List<Path> importedBuildProjects = new LinkedList<>();
            for (final JkBuild depBuild : build.importedBuilds().directs()) {
                importedBuildProjects.add(depBuild.baseTree().root());
            }
            final JkEclipseClasspathGenerator classpathGenerator = new JkEclipseClasspathGenerator(javaProject);
            classpathGenerator.setBuildDependencyResolver(build.buildDependencyResolver(), build.buildDependencies());
            classpathGenerator.setIncludeJavadoc(true);
            classpathGenerator.setJreContainer(this.jreContainer);
            classpathGenerator.setImportedBuildProjects(importedBuildProjects);
            classpathGenerator.setUsePathVariables(this.useVarPath);
            final String result = classpathGenerator.generate();
            final Path dotClasspath = build.baseDir().resolve(".classpath");
            JkUtilsPath.write(dotClasspath, result.getBytes(Charset.forName("UTF-8")));

            if (!Files.exists(dotProject)) {
                JkEclipseProject.ofJavaNature(build.baseTree().root().getFileName().toString()).writeTo(dotProject);
            }
        } else {
            if (!Files.exists(dotProject)) {
                JkEclipseProject.ofSimpleNature(build.baseTree().root().getFileName().toString()).writeTo(dotProject);
            }
        }
    }

    @JkDoc("Generates Eclipse files (.project and .classpath) on all sub-folders of the current directory. Only sub-folders having a build/def directory are taken in account. See generateFiles.")
    public void generateAll() {
        final Iterable<Path> folders = build.baseTree()
                .accept("**/" + JkConstants.BUILD_DEF_DIR, JkConstants.BUILD_DEF_DIR)
                .refuse("**/build/output/**")
                .stream().collect(Collectors.toList());
        for (final Path folder : folders) {
            final Path projectFolder = folder.getParent().getParent();
            JkLog.start(this,"Generating Eclipse files on " + projectFolder);
            Main.exec(projectFolder, "eclipse#generateFiles");
            JkLog.end(this, "");
        }
    }

}