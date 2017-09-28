package org.jerkar.distrib.all;

import org.jerkar.CoreBuild;
import org.jerkar.api.depmanagement.JkArtifactFileId;
import org.jerkar.api.file.JkFileTree;
import org.jerkar.api.file.JkFileTreeSet;
import org.jerkar.api.file.JkZipper;
import org.jerkar.api.java.JkJavadocMaker;
import org.jerkar.api.project.java.JkJavaProject;
import org.jerkar.api.system.JkLog;
import org.jerkar.api.utils.JkUtilsFile;
import org.jerkar.plugins.jacoco.PluginsJacocoBuild;
import org.jerkar.plugins.sonar.PluginsSonarBuild;
import org.jerkar.tool.JkBuild;
import org.jerkar.tool.JkDoc;
import org.jerkar.tool.JkImportBuild;
import org.jerkar.tool.JkInit;

import java.io.File;
import java.nio.file.Path;

class DistribAllBuild extends JkBuild {

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
        CoreBuild core = pluginsJacoco.core; // The core project is got by transitivity
        Path distDir = this.outputDir().resolve("dist");
        JkFileTree dist = JkFileTree.of(distDir).importDirContent(core.distribFolder.toFile());

        JkLog.info("Add plugins to the distribution");
        JkFileTree ext = dist.go("libs/builtins").importFiles(pluginsSonar.project().mainArtifactFile(),
                pluginsJacoco.project().mainArtifactFile());
        JkFileTree sourceDir = dist.go("libs-sources");
        sourceDir.importFiles(pluginsSonar.project().artifactFile(JkJavaProject.SOURCES_FILE_ID),
                pluginsJacoco.project().artifactFile(JkJavaProject.SOURCES_FILE_ID));

        JkLog.info("Add plugins to the fat jar");
        File fat = dist.file(core.project().artifactFile(JkArtifactFileId.of("all", "jar")).getName());
        JkUtilsFile.copyFile(core.project().mainArtifactFile(), fat);
        JkZipper.of().merge(ext.include("**/*.jar")).appendTo(fat);

        JkLog.info("Create a fat source jar");
        File fatSource = sourceDir.file("org.jerkar.core-all-sources.jar");
        JkZipper.of().merge(sourceDir.include("**.jar", "**.zip").exclude(fatSource.getName())).to(fatSource);

        if (javadoc) {
            JkLog.info("Create javadoc");
            JkFileTreeSet sources = this.pluginsJacoco.core.project().getSourceLayout().sources()
                    .and(this.pluginsJacoco.project().getSourceLayout().sources())
                    .and(this.pluginsSonar.project().getSourceLayout().sources());
            Path javadocAllDir = this.outputDir().resolve("javadoc-all");
            File javadocAllFile = dist.file("libs-javadoc/org.jerkar.core-fat-javadoc.jar");
            JkJavadocMaker.of(sources, javadocAllDir.toFile(), javadocAllFile).process();
        }

        JkLog.info("Pack all");
        dist.zip().to(outputDir().resolve("jerkar-distrib.zip").toFile());

        JkLog.done();
    }

    @JkDoc("End to end method to construct a distrib.")
    public void doDefault() {
        super.doDefault();
        this.importedBuilds().all().forEach(JkBuild::clean);
        pluginsJacoco.core.project().makeArtifactFile(CoreBuild.DISTRIB_FILE_ID);
        pluginsJacoco.project().makeMainJar();
        pluginsSonar.project().makeMainJar();
        distrib();
        if (testSamples) {
            testSamples();
        }
    }

    public void testSamples() {
        JkLog.startHeaded("Testing Samples");
        new SampleTester(this.baseTree()).doTest();
        JkLog.done();
    }

    public static void main(String[] args) {
        JkInit.instanceOf(DistribAllBuild.class, "-testSamples=true").doDefault();
    }

}
