package dev.jeka.core.integrationtest.tooling.intellij;

import dev.jeka.core.api.depmanagement.JkPopularLibs;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.file.JkZipTree;
import dev.jeka.core.api.project.JkProject;
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
        final JkProject baseProject = JkProject.of();
        baseProject
            .apply(this::configureCompileLayout)
            .apply(this::configureEmptyTestCompileLayout)
            .setBaseDir(base);
        baseProject.compilation.dependencies
            .add(JkPopularLibs.APACHE_HTTP_CLIENT.toCoordinate("4.5.6"));
        final JkImlGenerator baseGenerator = JkImlGenerator.of()
                .setIdeSupport(baseProject.getJavaIdeSupport());
        final String result0 = baseGenerator.computeIml().toDoc().toXml();
        System.out.println("\nbase .classpath");
        System.out.println(result0);

        final Path core = top.resolve("core");
        final JkProject coreProject = JkProject.of();
        coreProject
                .apply(this::configureCompileLayout)
                .setBaseDir(core)
                .compilation.dependencies.add(baseProject.toDependency());
        coreProject
                .testing
                    .compilation
                        .layout
                            .emptySources().addSource("test")
                            .emptyResources().addResource("res-test");
        coreProject
                .testing
                    .testProcessor
                        .setForkingProcess(true);
        final JkImlGenerator coreGenerator = JkImlGenerator.of()
                .setIdeSupport(coreProject.getJavaIdeSupport());
        final String result1 = coreGenerator.computeIml().toDoc().toXml();
        System.out.println("\ncore .classpath");
        System.out.println(result1);

        final Path desktop = top.resolve("desktop");
        final JkProject desktopProject = JkProject.of();
        desktopProject
            .apply(this::configureCompileLayout)
            .apply(this::configureEmptyTestCompileLayout)
            .setBaseDir(desktop);
        desktopProject.compilation.dependencies.add(coreProject.toDependency());
        final JkImlGenerator desktopGenerator = JkImlGenerator.of()
                .setIdeSupport(desktopProject.getJavaIdeSupport());
        final String result2 = desktopGenerator.computeIml().toDoc().toXml();
        System.out.println("\ndesktop .classpath");
        System.out.println(result2);

        desktopProject.pack();
        JkPathTree.of(top).deleteContent();
    }

    private void configureCompileLayout(JkProject javaProject) {
        javaProject
                .compilation
                    .layout
                        .emptySources().addSource("src")
                        .emptyResources().addResource("res");
    }

    private void configureEmptyTestCompileLayout(JkProject javaProject) {
        javaProject
                .testing
                    .compilation
                        .layout
                            .emptySources()
                            .emptyResources();
    }

    private static Path unzipToDir(String zipName) throws IOException, URISyntaxException {
        final Path dest = Files.createTempDirectory(JkEclipseClasspathGeneratorIT.class.getName());
        final Path zip = Paths.get(JkEclipseClasspathGeneratorIT.class.getResource(zipName).toURI());
        JkZipTree.of(zip).copyTo(dest);
        System.out.println("unzipped in " + dest);
        return dest;
    }

}