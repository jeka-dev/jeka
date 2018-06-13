package org.jerkar.distrib.all;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.jerkar.CoreBuild;
import org.jerkar.api.depmanagement.JkArtifactId;
import org.jerkar.api.file.JkPathTree;
import org.jerkar.api.file.JkPathTreeSet;
import org.jerkar.api.java.JkJavadocMaker;
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
    PluginsSonarBuild sonarPluginBuild;

    @JkImportBuild("../org.jerkar.plugins-jacoco")
    PluginsJacocoBuild jacocoPluginBuild;

    public boolean testSamples = false;

    public boolean javadoc = true;

    @JkDoc("Construct a distrib assuming all dependent sub projects are already built.")
    public void distrib() throws IOException {
        JkLog.info(this,"Copy core distribution locally.");
        CoreBuild core = jacocoPluginBuild.core; // The core project is got by transitivity
                Path distDir = this.outputDir().resolve("dist");
        JkPathTree dist = JkPathTree.of(distDir).merge(core.distribFolder);

        JkLog.info(this,"Add plugins to the distribution");
        JkPathTree ext = dist.goTo("libs/builtins")
                .copyIn(sonarPluginBuild.java().project().maker().mainArtifactPath())
                .copyIn(jacocoPluginBuild.java().project().maker().mainArtifactPath());
        JkPathTree sourceDir = dist.goTo("libs-sources");
        sourceDir.copyIn(sonarPluginBuild.java().project().maker().artifactPath(JkJavaProjectMaker.SOURCES_ARTIFACT_ID))
                .copyIn(jacocoPluginBuild.java().project().maker().artifactPath(JkJavaProjectMaker.SOURCES_ARTIFACT_ID));

        JkLog.info(this,"Add plugins to the fat jar");
        Path fat = dist.get(core.java().project().maker().artifactPath(JkArtifactId.of("all", "jar"))
                .getFileName().toString());
        Files.copy(core.java().project().maker().mainArtifactPath(), fat, StandardCopyOption.REPLACE_EXISTING);
        ext.accept("**.jar").stream().map(path -> JkPathTree.ofZip(path)).forEach(tree -> tree.zipTo(fat));

        JkLog.info(this,"Create a fat source jar");
        Path fatSource = sourceDir.get("org.jerkar.core-all-sources.jar");
        sourceDir.accept("**.jar", "**.zip").refuse(fatSource.getFileName().toString()).stream()
                .map(path -> JkPathTree.ofZip(path)).forEach(tree -> tree.zipTo(fatSource));

        if (javadoc) {
            JkLog.info(this,"Create javadoc");
            JkPathTreeSet sources = this.jacocoPluginBuild.core.java().project().getSourceLayout().sources()
                    .and(this.jacocoPluginBuild.java().project().getSourceLayout().sources())
                    .and(this.sonarPluginBuild.java().project().getSourceLayout().sources());
            Path javadocAllDir = this.outputDir().resolve("javadoc-all");
            Path javadocAllFile = dist.root().resolve("libs-javadoc/org.jerkar.core-fat-javadoc.jar");
            JkJavadocMaker.of(sources, javadocAllDir, javadocAllFile).process();
        }

        JkLog.info(this,"Pack all");
        dist.zipTo(outputDir().resolve("jerkar-distrib.zip"));
    }

    @JkDoc("End to end method to construct a distrib.")
    public void doDefault()  {
        clean();
        this.importedBuilds().all().forEach(JkBuild::clean);
        jacocoPluginBuild.core.java().project().maker().makeArtifact(CoreBuild.DISTRIB_FILE_ID);
        jacocoPluginBuild.java().project().maker().makeAllArtifacts();
        sonarPluginBuild.java().project().maker().makeAllArtifacts();
        try {
            distrib();
            if (testSamples) {
                testSamples();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void testSamples() throws IOException {
        SampleTester sampleTester = new SampleTester(this.baseTree());
        sampleTester.restoreEclipseClasspathFile = true;
        sampleTester.doTest();
    }

    public static void main(String[] args) {
        JkInit.instanceOf(DistribAllBuild.class, args).doDefault();
    }


}
