package org.jerkar.distrib.all;

import java.io.File;

import org.jerkar.CoreBuild;
import org.jerkar.api.file.JkFileTree;
import org.jerkar.api.file.JkFileTreeSet;
import org.jerkar.api.file.JkZipper;
import org.jerkar.api.java.JkJavadocMaker;
import org.jerkar.api.system.JkLog;
import org.jerkar.api.utils.JkUtilsFile;
import org.jerkar.plugins.jacoco.PluginsJacocoBuild;
import org.jerkar.plugins.sonar.PluginsSonarBuild;
import org.jerkar.tool.JkBuildDependencySupport;
import org.jerkar.tool.JkDoc;
import org.jerkar.tool.JkImportBuild;
import org.jerkar.tool.JkInit;

class DistribAllBuild extends JkBuildDependencySupport {

    @JkImportBuild("../org.jerkar.plugins-sonar")
    PluginsSonarBuild pluginsSonar;

    @JkImportBuild("../org.jerkar.plugins-jacoco")
    PluginsJacocoBuild pluginsJacoco;

    public boolean testSamples = false;

    public boolean javadoc = true;

    @JkDoc("Construct a distrib assuming all dependent sub projects are already built.")
    public void distrib() {

        JkLog.startln("Creating distribution file");

        JkLog.info("Copy core distribution locally.");
        CoreBuild core = pluginsJacoco.core; // The core project is got by
        // transitivity
        File distDir = this.ouputDir("dist");
        JkFileTree dist = JkFileTree.of(distDir).importDirContent(core.distribFolder);

        JkLog.info("Add plugins to the distribution");
        JkFileTree ext = dist.go("libs/builtins").importFiles(pluginsSonar.packer().jarFile(),
                pluginsJacoco.packer().jarFile());
        JkFileTree sourceDir = dist.go("libs-sources");
        sourceDir.importFiles(pluginsSonar.packer().jarSourceFile(), pluginsJacoco.packer().jarSourceFile());

        JkLog.info("Add plugins to the fat jar");
        File fat = dist.file(core.packer().fatJarFile().getName());
        JkUtilsFile.copyFile(core.packer().jarFile(), fat);
        JkZipper.of().merge(ext.include("**/*.jar")).appendTo(fat);

        JkLog.info("Create a fat source jar");
        File fatSource = sourceDir.file("org.jerkar.core-all-sources.jar");
        JkZipper.of().merge(sourceDir.include("**.jar", "**.zip").exclude(fatSource.getName())).to(fatSource);

        if (javadoc) {
            JkLog.info("Create a fat javadoc");
            JkFileTreeSet sources = this.pluginsJacoco.core.sources().and(this.pluginsJacoco.sources())
                    .and(this.pluginsSonar.sources());
            File javadocAllDir = this.ouputDir("javadoc-all");
            File javadocAllFile = dist.file("libs-javadoc/org.jerkar.core-fat-javadoc.jar");
            JkJavadocMaker.of(sources, javadocAllDir, javadocAllFile).process();
        }

        JkLog.info("Pack all");
        dist.zip().to(ouputDir("jerkar-distrib.zip"));

        JkLog.done();
    }

    @JkDoc("End to end method to construct a distrib.")
    public void doDefault() {
        super.doDefault();
        pluginsJacoco.core.pack.javadoc = false;
        importedBuilds().invokeDoDefaultMethodOnAll();
        distrib();
        if (testSamples) {
            testSamples();
        }
    }

    public void testSamples() {
        JkLog.startHeaded("Testing Samples");
        new SampleTester(this.baseDir()).doTest();
        JkLog.done();
    }

    public static void main(String[] args) {
        JkInit.instanceOf(DistribAllBuild.class, args).doDefault();
    }

}
