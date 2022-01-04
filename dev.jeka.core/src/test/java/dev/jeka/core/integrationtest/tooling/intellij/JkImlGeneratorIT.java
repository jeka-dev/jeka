package dev.jeka.core.integrationtest.tooling.intellij;

import dev.jeka.core.api.depmanagement.JkPopularModules;
import dev.jeka.core.api.file.JkPathTree;
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
        final JkProject baseProject = JkProject.of()
            .apply(this::configureCompileLayout)
            .apply(this::configureEmptyTestCompileLayout)
            .setBaseDir(base)
            .getConstruction()
                .getCompilation()
                    .configureDependencies(deps -> deps
                        .and(JkPopularModules.APACHE_HTTP_CLIENT.version("4.5.6"))).__.__;
        final JkImlGenerator baseGenerator = JkImlGenerator.of()
                .setIdeSupport(baseProject.getJavaIdeSupport());
        final String result0 = baseGenerator.computeIml().toDoc().toXml();
        System.out.println("\nbase .classpath");
        System.out.println(result0);

        final Path core = top.resolve("core");
        final JkProject coreProject = JkProject.of()
                .apply(this::configureCompileLayout)
                .setBaseDir(core)
                .getConstruction()
                    .getCompilation()
                        .configureDependencies(deps -> deps.and(baseProject.toDependency())).__
                    .getTesting()
                        .getCompilation()
                            .getLayout()
                                .emptySources().addSource("test")
                                .emptyResources().addResource("res-test").__.__
                        .getTestProcessor()
                            .setForkingProcess(true).__.__.__;
        final JkImlGenerator coreGenerator = JkImlGenerator.of()
                .setIdeSupport(coreProject.getJavaIdeSupport());
        final String result1 = coreGenerator.computeIml().toDoc().toXml();
        System.out.println("\ncore .classpath");
        System.out.println(result1);

        final Path desktop = top.resolve("desktop");
        final JkProject desktopProject = JkProject.of()
            .apply(this::configureCompileLayout)
            .apply(this::configureEmptyTestCompileLayout)
            .setBaseDir(desktop)
            .getConstruction()
                .getCompilation()
                    .configureDependencies(deps -> deps
                        .and(coreProject.toDependency())).__.__;
        final JkImlGenerator desktopGenerator = JkImlGenerator.of()
                .setIdeSupport(desktopProject.getJavaIdeSupport());
        final String result2 = desktopGenerator.computeIml().toDoc().toXml();
        System.out.println("\ndesktop .classpath");
        System.out.println(result2);

        desktopProject.getArtifactProducer().makeAllArtifacts();
        JkPathTree.of(top).deleteContent();
    }

    private void configureCompileLayout(JkProject javaProject) {
        javaProject
            .getConstruction()
                .getCompilation()
                    .getLayout()
                        .emptySources().addSource("src")
                        .emptyResources().addResource("res");
    }

    private void configureEmptyTestCompileLayout(JkProject javaProject) {
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