package org.jerkar.distrib.all;

import org.jerkar.V07CoreBuild;
import org.jerkar._CoreProject;
import org.jerkar.api.file.JkFileTree;
import org.jerkar.api.file.JkFileTreeSet;
import org.jerkar.api.file.JkZipper;
import org.jerkar.api.java.JkJavadocMaker;
import org.jerkar.api.depmanagement.JkArtifactFileId;
import org.jerkar.api.project.java.JkJavaProject;
import org.jerkar.api.system.JkLog;
import org.jerkar.api.utils.JkUtilsFile;
import org.jerkar.plugins.jacoco.V07PluginsJacocoBuild;
import org.jerkar.plugins.sonar._PluginsSonarBuild;
import org.jerkar.tool.*;

import java.io.File;

class _DistribAllBuild extends JkBuild {

    @JkImportBuild("../org.jerkar.plugins-sonar")
    _PluginsSonarBuild pluginsSonar;

    @JkImportBuild("../org.jerkar.plugins-jacoco")
    V07PluginsJacocoBuild pluginsJacoco;

    public boolean testSamples = false;

    public boolean javadoc = true;

    @JkDoc("Construct a distrib assuming all dependent sub projects are already built.")
    public void distrib() {

        JkLog.startln("Creating distribution file");

        JkLog.info("Copy core distribution locally.");
        V07CoreBuild core = pluginsJacoco.core; // The core project is got by transitivity
        File distDir = this.ouputDir("dist");
        _CoreProject coreProject = (_CoreProject) core.project();
        JkFileTree dist = JkFileTree.of(distDir).importDirContent(coreProject.distribFolder);

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
        pluginsJacoco.core.clean();
        pluginsJacoco.clean();
        pluginsSonar.clean();
        pluginsJacoco.core.project().makeArtifactFile(_CoreProject.DISTRIB_FILE_ID);
        pluginsJacoco.project().makeMainJar();
        pluginsSonar.project().makeMainJar();
        //slaves().invokeOnAll("clean");
       // slaves().invokeDoDefaultMethodOnAll();
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
        JkInit.instanceOf(_DistribAllBuild.class, args).doDefault();
    }

}
