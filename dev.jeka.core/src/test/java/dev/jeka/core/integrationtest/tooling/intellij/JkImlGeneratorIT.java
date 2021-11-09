package dev.jeka.core.integrationtest.tooling.intellij;

import dev.jeka.core.api.depmanagement.JkPopularModules;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.project.JkJavaProject;
import dev.jeka.core.api.tooling.eclipse.JkEclipseClasspathGeneratorIT;
import dev.jeka.core.api.tooling.intellij.JkImlGenerator;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by angibaudj on 21-09-17.
 */
public class JkImlGeneratorIT {

    @Test
    public void generate() throws Exception {
        final Path top = unzipToDir("sample-multi-scriptless.zip");

        final Path base = top.resolve("base");
        final JkJavaProject baseProject = JkJavaProject.of()
            .apply(this::configureCompileLayout)
            .apply(this::configureEmptyTestCompileLayout)
            .setBaseDir(base)
            .getConstruction()
                .getCompilation()
                    .setDependencies(deps -> deps
                        .and(JkPopularModules.APACHE_HTTP_CLIENT.version("4.5.6"))).__.__;
        final JkImlGenerator baseGenerator = JkImlGenerator.of(baseProject.getJavaIdeSupport());
        final String result0 = baseGenerator.generate();
        System.out.println("\nbase .classpath");
        System.out.println(result0);

        final Path core = top.resolve("core");
        final JkJavaProject coreProject = JkJavaProject.of()
                .apply(this::configureCompileLayout)
                .setBaseDir(core)
                .getConstruction()
                    .getCompilation()
                        .setDependencies(deps -> deps.and(baseProject.toDependency())).__
                    .getTesting()
                        .getCompilation()
                            .getLayout()
                                .emptySources().addSource("test")
                                .emptyResources().addResource("res-test").__.__
                        .getTestProcessor()
                            .setForkingProcess(true).__.__.__;
        final JkImlGenerator coreGenerator = JkImlGenerator.of(coreProject.getJavaIdeSupport());
        final String result1 = coreGenerator.generate();
        System.out.println("\ncore .classpath");
        System.out.println(result1);

        final Path desktop = top.resolve("desktop");
        final JkJavaProject desktopProject = JkJavaProject.of()
            .apply(this::configureCompileLayout)
            .apply(this::configureEmptyTestCompileLayout)
            .setBaseDir(desktop)
            .getConstruction()
                .getCompilation()
                    .setDependencies(deps -> deps
                        .and(coreProject.toDependency())).__.__;
        final JkImlGenerator desktopGenerator = JkImlGenerator.of(desktopProject.getJavaIdeSupport());
        final String result2 = desktopGenerator.generate();
        System.out.println("\ndesktop .classpath");
        System.out.println(result2);

        desktopProject.getPublication().getArtifactProducer().makeAllArtifacts();
        JkPathTree.of(top).deleteContent();
    }

    private void configureCompileLayout(JkJavaProject javaProject) {
        javaProject
            .getConstruction()
                .getCompilation()
                    .getLayout()
                        .emptySources().addSource("src")
                        .emptyResources().addResource("res");
    }

    private void configureEmptyTestCompileLayout(JkJavaProject javaProject) {
        javaProject
            .getConstruction()
                .getTesting()
                    .getCompilation()
                        .getLayout()
                            .emptySources()
                            .emptyResources();
    }

    private static Path unzipToDir(String zipName) throws IOException, URISyntaxException {
        final Path dest = Files.createTempDirectory(JkEclipseClasspathGeneratorIT.class.getName());
        final Path zip = Paths.get(JkEclipseClasspathGeneratorIT.class.getResource(zipName).toURI());
        JkPathTree.ofZip(zip).copyTo(dest);
        System.out.println("unzipped in " + dest);
        return dest;
    }

}