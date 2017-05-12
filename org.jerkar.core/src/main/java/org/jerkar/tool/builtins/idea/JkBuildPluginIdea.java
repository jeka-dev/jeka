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

    private JkBuild build;

    @Override
    public void configure(JkBuild build) {
        this.build = build;
    }

    /** Generates Idea [my-module].iml file */
    @JkDoc("Generates Idea [my-module].iml file")
    public void generateIml() {
        final List<File> depProjects = new LinkedList<File>();
        for (final JkBuild depBuild : build.slaves().directs()) {
            depProjects.add(depBuild.baseDir().root());
        }
        final ImlGenerator generator = new ImlGenerator(build.baseDir().root());
        generator.buildDefDependencyResolver = build.buildDefDependencyResolver();
        generator.projectDependencies = depProjects;
        if (this.build instanceof JkJavaBuild) {
            final JkJavaBuild jbuild = (JkJavaBuild) build;
            generator.dependencyResolver = jbuild.dependencyResolver();
            generator.includeJavadoc = true;
            generator.sourceJavaVersion = jbuild.javaSourceVersion();
            generator.sources = jbuild.sources();
            generator.testSources = jbuild.unitTestSources();
            generator.outputClassFolder = jbuild.classDir();
            generator.outputTestClassFolder = jbuild.testClassDir();
        }
        generator.generate();
        JkLog.info(generator.outputFile.getPath() + " generated.");
    }

    @JkDoc("Generates ./idea/modules.xml file")
    public void generateModulesXml() {
        File current = build.baseDir().root();
        Iterable<File> imls = build.baseDir().include("**/*.iml");
        ModulesXmlGenerator modulesXmlGenerator = new ModulesXmlGenerator(current, imls);
        modulesXmlGenerator.generate();
    }


}