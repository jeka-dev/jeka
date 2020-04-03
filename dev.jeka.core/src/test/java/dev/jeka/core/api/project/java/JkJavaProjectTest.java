package dev.jeka.core.api.project.java;

import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.depmanagement.JkPopularModules;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.java.project.JkJavaProject;
import dev.jeka.core.api.java.project.JkProjectSourceLayout;
import dev.jeka.core.api.tooling.eclipse.JkEclipseClasspathGeneratorTest;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


public class JkJavaProjectTest {

    @Test
    public void generate() throws Exception {
        final Path top = unzipToDir("sample-multi-scriptless.zip");
        Path base = top.resolve("base");

        JkProjectSourceLayout sourceLayout= JkProjectSourceLayout.ofSimpleStyle()
                .withResources("res").withTestResources("res-test");


        JkJavaProject baseProject = JkJavaProject.of(sourceLayout.withBaseDir(base));
        baseProject.getDependencyManagement().addDependencies(JkDependencySet.of()
                .and(JkPopularModules.APACHE_HTTP_CLIENT, "4.5.6"));

        final Path core = top.resolve("core");
        final JkJavaProject coreProject = JkJavaProject.of(sourceLayout.withBaseDir(core));
        JkDependencySet coreDeps = JkDependencySet.of().and(baseProject);
        coreProject.getDependencyManagement().addDependencies(coreDeps);
        coreProject.getSteps().getTesting().getTestProcessor().setForkingProcess(true);

        final Path desktop = top.resolve("desktop");
        final JkJavaProject desktopProject = JkJavaProject.of(sourceLayout.withBaseDir(desktop));
        final JkDependencySet deps = JkDependencySet.of().and(coreProject);
        desktopProject.getDependencyManagement().addDependencies(deps);
        desktopProject.getArtifactProducer().makeAllArtifacts();


        // Desktop.getDesktop().open(desktop);
        JkPathTree.of(top).deleteRoot();
    }

    private static Path unzipToDir(String zipName) throws IOException, URISyntaxException {
        final Path dest = Files.createTempDirectory(JkEclipseClasspathGeneratorTest.class.getName());
        final Path zip = Paths.get(JkEclipseClasspathGeneratorTest.class.getResource(zipName).toURI());
        JkPathTree.ofZip(zip).copyTo(dest);
        System.out.println("unzipped in " + dest);
        return dest;
    }

    private void sourceLayoutConfigurer(JkProjectSourceLayout projectSourceLayout) {

    }

}