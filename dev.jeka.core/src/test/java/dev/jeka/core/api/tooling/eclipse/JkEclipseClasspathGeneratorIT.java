package dev.jeka.core.api.tooling.eclipse;

import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.depmanagement.JkPopularLibs;
import dev.jeka.core.api.file.JkZipTree;
import dev.jeka.core.api.project.JkProject;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

// Test on a project structure as Base -> Core -> Desktop
public class JkEclipseClasspathGeneratorIT {

    static final String ZIP_NAME = "sample-multi-scriptless.zip";

    @Test
    public void generate() throws Exception {
        final Path top = unzipToDir(ZIP_NAME);

        final JkProject baseProject = JkProject.of()
            .apply(this::configureCompileLayout)
            .apply(this::configureTestCompileLayout)
            .setBaseDir(top.resolve("base"))
            .simpleFacade()
                .configureCompileDeps(deps -> deps
                        .and("org.apache.httpcomponents.client5:httpclient5:5.1.3"))
                .getProject();
        final JkEclipseClasspathGenerator baseGenerator = JkEclipseClasspathGenerator.of(baseProject.getJavaIdeSupport())
            .setUsePathVariables(true)
            .setDefDependencies(baseProject.getConstruction().getDependencyResolver(),
                    JkDependencySet.of().and(JkPopularLibs.GUAVA.toCoordinate("21.0").toString()));
        final String baseClasspath = baseGenerator.generate();
        System.out.println("\nbase .classpath");
        System.out.println(baseClasspath);

        final JkProject coreProject = JkProject.of()
            .apply(this::configureCompileLayout)
            .setBaseDir(top.resolve("core"))
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
        final JkEclipseClasspathGenerator coreGenerator =
                JkEclipseClasspathGenerator.of(coreProject.getJavaIdeSupport());
        final String coreClasspath = coreGenerator.generate();
        System.out.println("\ncore .classpath");
        System.out.println(coreClasspath);

        final JkProject desktopProject = JkProject.of()
            .apply(this::configureCompileLayout)
            .apply(this::configureTestCompileLayout)
            .setBaseDir(top.resolve("desktop"))
            .getConstruction()
                .getCompilation()
                    .configureDependencies(deps -> deps.and(coreProject.toDependency())).__.__;
        desktopProject.getArtifactProducer().makeAllArtifacts();
        final JkEclipseClasspathGenerator desktopGenerator =
                JkEclipseClasspathGenerator.of(desktopProject.getJavaIdeSupport());
        final String result2 = desktopGenerator.generate();
        System.out.println("\ndesktop .classpath");
        System.out.println(result2);
    }

    private void configureCompileLayout(JkProject javaProject) {
        javaProject
            .getConstruction()
                .getCompilation()
                    .getLayout()
                        .emptySources().addSource("src")
                        .emptyResources().addResource("res");
    }

    private void configureTestCompileLayout(JkProject javaProject) {
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
        JkZipTree.of(zip).copyTo(dest);
        System.out.println("unzipped in " + dest);
        return dest;
    }

}
