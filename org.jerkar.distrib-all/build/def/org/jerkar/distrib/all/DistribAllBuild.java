package org.jerkar.distrib.all;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.jerkar.CoreBuild;
import org.jerkar.api.depmanagement.JkArtifactFileId;
import org.jerkar.api.file.JkPathTree;
import org.jerkar.api.file.JkPathTreeSet;
import org.jerkar.api.java.JkJavadocMaker;
import org.jerkar.api.project.java.JkJavaProject;
import org.jerkar.api.project.java.JkJavaProjectMaker;
import org.jerkar.api.system.JkLog;
import org.jerkar.plugins.jacoco.PluginsJacocoBuild;
import org.jerkar.plugins.sonar.PluginsSonarBuild;
import org.jerkar.tool.JkBuild;
import org.jerkar.tool.JkDoc;
import org.jerkar.tool.JkImportBuild;
import org.jerkar.tool.JkInit;

class DistribAllBuild extends JkBuild {

    @JkImportBuild("../org.jerkar.plugins-sonar")
    PluginsSonarBuild pluginsSonar;

    @JkImportBuild("../org.jerkar.plugins-jacoco")
    PluginsJacocoBuild pluginsJacoco;

    public boolean testSamples = false;

    public boolean skipTests = false;

    public boolean javadoc = true;

    @Override
    protected void init() {
        pluginsJacoco.core.tests.skip = skipTests;
    }

    @JkDoc("Construct a distrib assuming all dependent sub projects are already built.")
    public void distrib() throws Exception {

        JkLog.startln("Creating distribution file");

        JkLog.info("Copy core distribution locally.");
        CoreBuild core = pluginsJacoco.core; // The core project is got by transitivity
                Path distDir = this.outputDir().resolve("dist");
        JkPathTree dist = JkPathTree.of(distDir).merge(core.distribFolder);

        JkLog.info("Add plugins to the distribution");
        JkPathTree ext = dist.goTo("libs/builtins")
                .copyIn(pluginsSonar.project().maker().mainArtifactPath())
                .copyIn(pluginsJacoco.project().maker().mainArtifactPath());
        JkPathTree sourceDir = dist.goTo("libs-sources");
        sourceDir.copyIn(pluginsSonar.project().maker().artifactPath(JkJavaProjectMaker.SOURCES_FILE_ID))
                .copyIn(pluginsJacoco.project().maker().artifactPath(JkJavaProjectMaker.SOURCES_FILE_ID));

        JkLog.info("Add plugins to the fat jar");
        Path fat = dist.get(core.project().maker().artifactPath(JkArtifactFileId.of("all", "jar"))
                .getFileName().toString());
        Files.copy(core.project().maker().mainArtifactPath(), fat, StandardCopyOption.REPLACE_EXISTING);
        ext.accept("**.jar").stream().map(path -> JkPathTree.ofZip(path)).forEach(tree -> tree.zipTo(fat));

        JkLog.info("Create a fat source jar");
        Path fatSource = sourceDir.get("org.jerkar.core-all-sources.jar");
        sourceDir.accept("**.jar", "**.zip").refuse(fatSource.getFileName().toString()).stream()
                .map(path -> JkPathTree.ofZip(path)).forEach(tree -> tree.zipTo(fatSource));

        if (javadoc) {
            JkLog.info("Create javadoc");
            JkPathTreeSet sources = this.pluginsJacoco.core.project().getSourceLayout().getSources()
                    .and(this.pluginsJacoco.project().getSourceLayout().getSources())
                    .and(this.pluginsSonar.project().getSourceLayout().getSources());
            Path javadocAllDir = this.outputDir().resolve("javadoc-all");
            Path javadocAllFile = dist.root().resolve("libs-javadoc/org.jerkar.core-fat-javadoc.jar");
            JkJavadocMaker.of(sources, javadocAllDir, javadocAllFile).process();
        }

        JkLog.info("Pack all");
        dist.zipTo(outputDir().resolve("jerkar-distrib.zip"));

        JkLog.done();
    }

    @JkDoc("End to end method to construct a distrib.")
    public void doDefault() throws Exception {
        clean();
        this.importedBuilds().all().forEach(JkBuild::clean);
        pluginsJacoco.core.project().maker().makeArtifactFile(CoreBuild.DISTRIB_FILE_ID);
        pluginsJacoco.project().maker().makeAllArtifactFiles();
        pluginsSonar.project().maker().makeAllArtifactFiles();
        distrib();
        if (testSamples) {
            testSamples();
        }
    }

    public void testSamples() throws Exception {
        JkLog.startHeaded("Testing Samples");
        SampleTester sampleTester = new SampleTester(this.baseTree());
        sampleTester.restoreEclipseClasspathFile = true;
        sampleTester.doTest();
        JkLog.done();
    }

    public static void main(String[] args) throws Exception {
        JkInit.instanceOf(DistribAllBuild.class, args).doDefault();
    }

}
