package org.jerkar;

import static org.jerkar.api.java.project.JkJavaProjectMaker.JAVADOC_ARTIFACT_ID;
import static org.jerkar.api.java.project.JkJavaProjectMaker.SOURCES_ARTIFACT_ID;

import java.nio.file.Path;
import java.util.List;

import org.jerkar.api.depmanagement.*;
import org.jerkar.api.file.JkPathTree;
import org.jerkar.api.java.JkJavaCompiler;
import org.jerkar.api.java.JkJavaVersion;
import org.jerkar.api.java.project.JkJavaProjectMaker;
import org.jerkar.api.system.JkLog;
import org.jerkar.tool.JkDoc;
import org.jerkar.tool.JkInit;
import org.jerkar.tool.builtins.java.JkJavaProjectBuild;

/**
 * Build class for Jerkar. Run main method to create full distrib.
 */
public class CoreBuild extends JkJavaProjectBuild {

    public static final JkArtifactId DISTRIB_FILE_ID = JkArtifactId.of("distrib", "zip");

    private static final String VERSION = "0.7-SNAPSHOT";

    @JkDoc("If true, executes black-box tests on sample projects prior ending the distrib.")
    public boolean testSamples;

    private Path distribFolder;

    protected CoreBuild() {
        java().tests.fork = false;
        java().pack.javadoc = true;
    }

    @Override
    protected void afterOptionsInjected()  {
        project().setVersionedModule(JkModuleId.of("org.jerkar:core").version(VERSION));
        project().setSourceVersion(JkJavaVersion.V8);
        project().setMavenPublicationInfo(mavenPublication());

        maker().setCompiler(JkJavaCompiler.of().fork(true));  // Fork to avoid compile failure bug on github/travis
        maker().setTestCompiler(JkJavaCompiler.of().fork(true));
        maker().setArtifactFileNameSupplier(() -> project().getVersionedModule().moduleId().fullName());
        maker().setPublishRepos(publishRepos());
        maker().defineArtifact(DISTRIB_FILE_ID, this::doDistrib);

        this.distribFolder = maker().getOutLayout().outputPath().resolve("distrib");
    }

    private void doDistrib() {
        final JkJavaProjectMaker maker = this.java().project().maker();
        maker.makeArtifactsIfAbsent(maker.mainArtifactId(), SOURCES_ARTIFACT_ID);
        final JkPathTree distrib = JkPathTree.of(distribFolder);
        distrib.deleteContent();
        JkLog.startTask("Create distrib");
        distrib.copyIn(baseDir().getParent().resolve("LICENSE"));
        distrib.merge(baseDir().resolve("src/main/dist"));
        distrib.merge(baseDir().resolve("src/main/java/META-INF/bin"));
        distrib.copyIn(maker.artifactPath(maker.mainArtifactId()));
        final List<Path> ivySourceLibs = baseTree().goTo("build/libs-sources").andAccept("apache-ivy*.jar").files();
        distrib.goTo("libs-sources")
            .copyIn(ivySourceLibs)
            .copyIn(maker.artifactPath(SOURCES_ARTIFACT_ID));
        if (java().pack.javadoc) {
            maker.makeArtifactsIfAbsent(maker.mainArtifactId(), JAVADOC_ARTIFACT_ID);
            distrib.goTo("libs-javadoc").copyIn(maker.artifactPath(JAVADOC_ARTIFACT_ID));
        }
        JkLog.execute("Making documentation", () -> new DocMaker(baseDir(), distribFolder,
                project().getVersionedModule().version().value()).assembleAllDoc());
        if (testSamples) {
            testSamples();
        }
        JkLog.info("Distribution created in " + distrib.root());
        final Path distripZipFile = maker.artifactPath(DISTRIB_FILE_ID);
        distrib.zipTo(distripZipFile);
        JkLog.info("Distribution zipped in " + distripZipFile);
        JkLog.endTask();
    }

    public static void main(String[] args) {
        JkInit.instanceOf(CoreBuild.class, args).doDefault();
    }

    public void doDefault() {
        clean();
        doDistrib();
    }

    // Necessary to publish on OSSRH
    private static JkMavenPublicationInfo mavenPublication() {
        return JkMavenPublicationInfo
                .of("Jerkar", "Build simpler, stronger, faster", "http://jerkar.github.io")
                .withScm("https://github.com/jerkar/jerkar.git").andApache2License()
                .andGitHubDeveloper("djeang", "djeangdev@yahoo.fr");
    }

    private static JkRepoSet publishRepos() {
        return JkRepoSet.local();
    }

    void testSamples()  {
        JkLog.startTask("Launching integration tests on samples");
        SampleTester sampleTester = new SampleTester(this.baseTree());
        sampleTester.restoreEclipseClasspathFile = true;
        sampleTester.doTest();
        JkLog.endTask();
    }

}
