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
import org.jerkar.tool.JkBuild;
import org.jerkar.tool.JkConstants;
import org.jerkar.tool.JkDoc;
import org.jerkar.tool.JkPlugin2;
import org.jerkar.tool.Main;
import org.jerkar.tool.builtins.java.JkJavaProjectBuild;

@JkDoc("Generates Eclipse meta data files from a JkJavaProjectBuild")
public final class JkPluginEclipse extends JkPlugin2 {

    @JkDoc("Set it to false to not mention javadoc in generated .classpath file.")
    boolean javadoc = true;

    /** If not null, this value will be used as the JRE container path when generating .classpath file.*/
    @JkDoc({ "If not null, this value will be used as the JRE container path when generating .classpath file." })
    public String jreContainer = null;

    /** Flag to set whether 'generateAll' task should use absolute paths instead of classpath variables */
    @JkDoc({ "Set it to true to use absolute paths in the classpath instead of classpath variables." })
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
    public void postConfigure() {
        build.scaffolder().extraActions.chain(this::generateFiles);  // If this plugin is activated while scaffolding, we want Eclipse metada file be generated.
    }

    @JkDoc("Generates Eclipse .classpath file according project dependencies.")
    public void generateFiles() {
        final Path dotProject = build.baseDir().resolve(".project");
        if (build instanceof JkJavaProjectBuild) {
            final JkJavaProjectBuild javaProjectBuild = (JkJavaProjectBuild) build;
            final JkJavaProject javaProject = javaProjectBuild.java().project();
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
            // generator.fileDependencyToProjectSubstitution = this.fileDependencyToProjectSubstitution;
            // generator.projectDependencyToFileSubstitutions = this.projectDependencyToFileSubstitutions;
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

    @JkDoc("Generate Eclipse files on all subfolder of the current directory. Only subfolder having a build/def directory are impacted.")
    public void generateAll() {
        final Iterable<Path> folders = build.baseTree()
                .accept("**/" + JkConstants.BUILD_DEF_DIR, JkConstants.BUILD_DEF_DIR)
                .refuse("**/build/output/**")
                .stream().collect(Collectors.toList());
        for (final Path folder : folders) {
            final Path projectFolder = folder.getParent().getParent();
            JkLog.startln("Generating Eclipse files on " + projectFolder);
            Main.exec(projectFolder, "eclipse#generateFiles");
            JkLog.done();
        }
    }

}