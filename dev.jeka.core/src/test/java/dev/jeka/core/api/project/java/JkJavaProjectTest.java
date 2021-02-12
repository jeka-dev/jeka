package dev.jeka.core.api.project.java;

import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.depmanagement.JkPopularModules;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.java.project.JkCompileLayout;
import dev.jeka.core.api.java.project.JkJavaProject;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


public class JkJavaProjectTest {

    @Test
    public void makeAllArtifacts() throws Exception {
        final Path top = unzipToDir("sample-multi-scriptless.zip");

        Path base = top.resolve("base");
        JkJavaProject baseProject = JkJavaProject.of().simpleFacade()
            .setBaseDir(base)
            .addCompileDependencies(JkDependencySet.of(JkPopularModules.APACHE_HTTP_CLIENT.version("4.5.6")))
            .getProject().getConstruction()
                .getCompilation()
                    .getLayout()
                        .emptySources().addSource("src")
                        .emptyResources().addResource("res").includeSourceDirsInResources().__.__.__;
        baseProject.getPublication().getArtifactProducer().makeAllArtifacts();

        final Path core = top.resolve("core");
        final JkJavaProject coreProject = JkJavaProject.of()
            .setBaseDir(core)
            .getConstruction()
                .getCompilation()
                    .addDependencies(JkDependencySet.of().and(baseProject.toDependency())).__
                .getCompilation()
                    .getLayout()
                        .setSourceSimpleStyle(JkCompileLayout.Concern.PROD).__.__.__;
        //Desktop.getDesktop().open(core.toFile());
        coreProject.getPublication().getArtifactProducer().makeAllArtifacts();

        final Path desktop = top.resolve("desktop");
        final JkJavaProject desktopProject = JkJavaProject.of()
            .setBaseDir(desktop)
            .getConstruction()
            .getCompilation()
                .addDependencies(JkDependencySet.of()
                    .and(coreProject.toDependency())).__
                .getCompilation()
                    .getLayout()
                        .setSourceSimpleStyle(JkCompileLayout.Concern.PROD).__.__.__;
        //Desktop.getDesktop().open(desktop.toFile());
        //desktopProject.getArtifactProducer().makeAllArtifacts();

        // Desktop.getDesktop().open(desktop);
        JkPathTree.of(top).deleteRoot();
    }

    private static Path unzipToDir(String zipName) throws IOException, URISyntaxException {
        final Path dest = Files.createTempDirectory(JkJavaProjectTest.class.getName());
        final Path zip = Paths.get(JkJavaProjectTest.class.getResource(zipName).toURI());
        JkPathTree.ofZip(zip).copyTo(dest);
        System.out.println("unzipped in " + dest);
        return dest;
    }

}