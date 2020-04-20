package dev.jeka.core.api.tooling.eclipse;

import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.depmanagement.JkPopularModules;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.java.project.JkJavaProject;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

// Test on a project structure as Base -> Core -> Desktop
public class JkEclipseClasspathGeneratorTest {

    static final String ZIP_NAME = "sample-multi-scriptless.zip";

    @Test
    public void generate() throws Exception {
        final Path top = unzipToDir(ZIP_NAME);

        final JkJavaProject baseProject = JkJavaProject.of()
            .apply(this::configureCompileLayout)
            .apply(this::configureTestCompileLayout)
            .setBaseDir(top.resolve("base"))
            .getDependencyManagement()
                .addDependencies(JkDependencySet.of()
                    .and(JkPopularModules.APACHE_HTTP_CLIENT, "4.5.6")).__;
        final JkEclipseClasspathGenerator baseGenerator = JkEclipseClasspathGenerator.of(baseProject.getJavaIdeSupport())
            .setUsePathVariables(true)
            .setDefDependencies(baseProject.getDependencyManagement().getResolver(),
                    JkDependencySet.of().and(JkPopularModules.GUAVA, "21.0"));
        final String baseClasspath = baseGenerator.generate();
        System.out.println("\nbase .classpath");
        System.out.println(baseClasspath);

        final JkJavaProject coreProject = JkJavaProject.of()
            .apply(this::configureCompileLayout)
            .setBaseDir(top.resolve("core"))
            .getDependencyManagement()
                .addDependencies(JkDependencySet.of().and(baseProject.asDependency())).__
            .getTesting()
                .getCompilation()
                    .getLayout()
                        .emptySources().addSource("test")
                        .emptyResources().addResource("res-test").__.__
                .getTestProcessor()
                    .setForkingProcess(true).__.__;
        final JkEclipseClasspathGenerator coreGenerator =
                JkEclipseClasspathGenerator.of(coreProject.getJavaIdeSupport());
        final String coreClasspath = coreGenerator.generate();
        System.out.println("\ncore .classpath");
        System.out.println(coreClasspath);

        final JkJavaProject desktopProject = JkJavaProject.of()
            .apply(this::configureCompileLayout)
            .apply(this::configureTestCompileLayout)
            .setBaseDir(top.resolve("desktop"))
            .getDependencyManagement()
                .addDependencies(JkDependencySet.of().and(coreProject.asDependency())).__;
        desktopProject.getArtifactProducer().makeAllArtifacts();
        final JkEclipseClasspathGenerator desktopGenerator =
                JkEclipseClasspathGenerator.of(desktopProject.getJavaIdeSupport());
        final String result2 = desktopGenerator.generate();
        System.out.println("\ndesktop .classpath");
        System.out.println(result2);
    }

    private void configureCompileLayout(JkJavaProject javaProject) {
        javaProject
                .getCompilation()
                    .getLayout()
                        .emptySources().addSource("src")
                        .emptyResources().addResource("res");
    }

    private void configureTestCompileLayout(JkJavaProject javaProject) {
        javaProject
                .getTesting()
                    .getCompilation()
                        .getLayout()
                            .emptySources()
                            .emptyResources();
    }

    private static Path unzipToDir(String zipName) throws IOException, URISyntaxException {
        final Path dest = Files.createTempDirectory(JkEclipseClasspathGeneratorTest.class.getName());
        final Path zip = Paths.get(JkEclipseClasspathGeneratorTest.class.getResource(zipName).toURI());
        JkPathTree.ofZip(zip).copyTo(dest);
        System.out.println("unzipped in " + dest);
        return dest;
    }

}
