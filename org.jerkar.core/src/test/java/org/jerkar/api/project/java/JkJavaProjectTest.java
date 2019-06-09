package org.jerkar.api.project.java;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.jerkar.api.depmanagement.JkDependencySet;
import org.jerkar.api.depmanagement.JkPopularModules;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.ide.eclipse.JkEclipseClasspathGeneratorTest;
import dev.jeka.core.api.java.project.JkJavaProject;
import dev.jeka.core.api.java.project.JkProjectSourceLayout;
import org.junit.Test;


public class JkJavaProjectTest {

    @Test
    public void generate() throws Exception {
        final Path top = unzipToDir("sample-multi-scriptless.zip");
        Path base = top.resolve("base");

        JkProjectSourceLayout sourceLayout= JkProjectSourceLayout.ofSimpleStyle()
                .withResources("res").withTestResources("res-test");


        JkJavaProject baseProject = JkJavaProject.of(sourceLayout.withBaseDir(base));
        baseProject.setDependencies(JkDependencySet.of().and(JkPopularModules.APACHE_HTTP_CLIENT, "4.5.3"));

        final Path core = top.resolve("core");
        final JkJavaProject coreProject = JkJavaProject.of(sourceLayout.withBaseDir(core));
        JkDependencySet coreDeps = JkDependencySet.of().and(baseProject);
        coreProject.setDependencies(coreDeps);
        coreProject.getMaker().getTasksForTesting().setRunner(
                coreProject.getMaker().getTasksForTesting().getRunner().withForking(true));

        final Path desktop = top.resolve("desktop");
        final JkJavaProject desktopProject = JkJavaProject.of(sourceLayout.withBaseDir(desktop));
        final JkDependencySet deps = JkDependencySet.of().and(coreProject);
        desktopProject.setDependencies(deps);
        desktopProject.getMaker().defineMainArtifactAsFatJar(false);
        desktopProject.getMaker().makeAllArtifacts();


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