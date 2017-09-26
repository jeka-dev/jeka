package org.jerkar.tool.builtins.eclipse;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import org.jerkar.api.ide.eclipse.JkEclipseClasspathGenerator;
import org.jerkar.api.project.java.JkJavaProject;
import org.jerkar.api.system.JkLog;
import org.jerkar.api.utils.JkUtilsFile;
import org.jerkar.tool.JkBuild;
import org.jerkar.tool.JkBuildPlugin2;
import org.jerkar.tool.JkConstants;
import org.jerkar.tool.JkDoc;
import org.jerkar.tool.Main;
import org.jerkar.tool.builtins.javabuild.JkJavaProjectBuild;

/**
 * Plugin to generate Eclipse meta data files from a JkJavaProjectBuild
 */
public final class JkBuildPlugin2Eclipse implements JkBuildPlugin2 {

    @JkDoc("Set it to false to not mention javadoc in generated .classpath file.")
    boolean javadoc = true;

    /** If not null, this value will be used as the JRE container path when generating .classpath file.*/
    @JkDoc({ "If not null, this value will be used as the JRE container path when generating .classpath file." })
    public String jreContainer = null;

    /** Flag to set whether 'generateAll' task should use absolute paths instead of classpath variables */
    @JkDoc({ "Set it to true to use absolute paths in the classpath instead of classpath variables." })
    public boolean useVarPath = false;


    // ------------------------- setters ----------------------------

    /** Set the JRE container to the Eclipse Standard VM type with the desired name. */
    public void setStandardJREContainer(String jreName) {
        jreContainer = "org.eclipse.jdt.launching.JRE_CONTAINER/org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType/" + jreName;
    }

    // ------------------------ plugin methods ----------------------

    @Override
    public void apply(final JkBuild build) {
        build.scaffolder().extraActions.chain(()-> this.generateFiles(build));  // If this plugin is activated while scaffolding, we want Eclipse metada file be generated.
    }

    /** generate eclipse metadata files (as .classpath or .project) */
    @JkDoc("Generates Eclipse .classpath file according project dependencies.")
    public void generateFiles(JkBuild build) {
        final File dotProject = build.file(".project");
        if (build instanceof JkJavaProjectBuild) {
            final JkJavaProjectBuild javaProjectBuild = (JkJavaProjectBuild) build;
            final JkJavaProject javaProject = javaProjectBuild.project();
            final List<File> importedBuildProjects = new LinkedList<>();
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
            final File dotClasspath = build.file(".classpath");
            JkUtilsFile.writeString(dotClasspath, result, false);

            if (!dotProject.exists()) {
                Project.ofJavaNature(build.baseTree().root().getName()).writeTo(dotProject);
            }
        } else {
            if (!dotProject.exists()) {
                Project.ofSimpleNature(build.baseTree().root().getName()).writeTo(dotProject);
            }
        }
    }

    /** Generate Eclipse files on all sub folders of the current directory **/
    @JkDoc("Generate Eclipse files on all subfolder of the current directory. Only subfolder having a build/def directory are impacted.")
    public void generateAll(JkBuild build) {
        final Iterable<File> folders = build.baseTree()
                .include("**/" + JkConstants.BUILD_DEF_DIR)
                .exclude("**/build/output/**")
                .files(true);
        for (final File folder : folders) {
            final File projectFolder = folder.getParentFile().getParentFile();
            JkLog.startln("Generating Eclipse files on " + projectFolder);
            Main.exec(projectFolder, "eclipse#generateFiles");
            JkLog.done();
        }
    }

}