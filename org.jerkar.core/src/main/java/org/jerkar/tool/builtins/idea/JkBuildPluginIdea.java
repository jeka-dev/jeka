package org.jerkar.tool.builtins.idea;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import org.jerkar.api.system.JkLog;
import org.jerkar.tool.JkBuild;
import org.jerkar.tool.JkDoc;
import org.jerkar.tool.builtins.javabuild.JkJavaBuild;
import org.jerkar.tool.builtins.javabuild.JkJavaBuildPlugin;

/**
 * Provides method to generate and read Eclipse metadata files.
 */
public final class JkBuildPluginIdea extends JkJavaBuildPlugin {

    private JkJavaBuild build;

    @Override
    public void configure(JkBuild build) {
        this.build = (JkJavaBuild) build;
    }

    /** Generates Idea [my-module].iml file */
    @JkDoc("Generates Idea [my-module].iml file")
    public void generateFiles() {
        if (this.build instanceof JkJavaBuild) {
            final JkJavaBuild jbuild = build;
            final List<File> depProjects = new LinkedList<File>();
            for (final JkBuild depBuild : build.slaves().directs()) {
                depProjects.add(depBuild.baseDir().root());
            }
            final ImlGenerator generator = new ImlGenerator(build.baseDir().root());
            generator.dependencyResolver = build.dependencyResolver();
            generator.buildDefDependencyResolver = build.buildDefDependencyResolver();
            generator.includeJavadoc = true;
            generator.projectDependencies = depProjects;
            generator.sourceJavaVersion = jbuild.javaSourceVersion();
            generator.sources = jbuild.sources();
            generator.testSources = jbuild.unitTestSources();
            generator.generate();
            JkLog.info(generator.outputFile.getPath() + " generated.");
        }
    }


}