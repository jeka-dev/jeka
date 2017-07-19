package org.jerkar.tool.builtins.idea;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import org.jerkar.api.system.JkLog;
import org.jerkar.tool.*;
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

    @JkDoc("If true, path to cache repository and to Jerkar install will be replaces by $JERKAR_REPO$ and $JERKAR_HOME$ path variable")
    boolean useVarPath = false;

    /** Generates Idea [my-module].iml file */
    @JkDoc("Generates Idea [my-module].iml file")
    public void generateIml() {
        final List<File> depProjects = new LinkedList<File>();
        for (final JkBuild depBuild : build.slaves().directs()) {
            depProjects.add(depBuild.baseDir().root());
        }
        final ImlGenerator generator = new ImlGenerator(build.baseDir().root());
        generator.useVarPath = useVarPath;
        generator.buildDefDependencyResolver = build.buildDefDependencyResolver();
        generator.projectDependencies = depProjects;
        if (this.build instanceof JkBuildDependencySupport) {
            final JkBuildDependencySupport dsbuild = (JkBuildDependencySupport) build;
            generator.dependencyResolver = dsbuild.dependencyResolver();
        }
        if (this.build instanceof JkJavaBuild) {
            final JkJavaBuild jbuild = (JkJavaBuild) build;
            generator.includeJavadoc = true;
            generator.sourceJavaVersion = jbuild.javaSourceVersion();
            generator.forceJdkVersion = true;
            generator.sources = jbuild.sources();
            generator.testSources = jbuild.unitTestSources();
            generator.outputClassFolder = jbuild.classDir();
            generator.outputTestClassFolder = jbuild.testClassDir();
        }
        generator.generate();
        JkLog.info(generator.outputFile.getPath() + " generated.");
    }

    /** Generate modules.xml files */
    @JkDoc("Generates ./idea/modules.xml file")
    public void generateModulesXml() {
        File current = build.baseDir().root();
        Iterable<File> imls = build.baseDir().include("**/*.iml");
        ModulesXmlGenerator modulesXmlGenerator = new ModulesXmlGenerator(current, imls);
        modulesXmlGenerator.generate();
    }

    @JkDoc("Generates iml files on this folder and its descendant recursively.")
    public void generateAllIml() {
        Iterable<File> folders = build.baseDir()
                .include("**/" + JkConstants.BUILD_DEF_DIR)
                .exclude("**/build/output/**")
                .files(true);
        for (File folder : folders) {
            File projectFolder = folder.getParentFile().getParentFile();
            JkLog.startln("Generating iml file on " + projectFolder);
            Main.exec(projectFolder, "idea#generateIml");
            JkLog.done();
        }
    }

    @JkDoc(("Shorthand for #generateAllIml + generateModulesXml"))
    public void generateAll() {
        generateAllIml();
        generateModulesXml();
    }

}