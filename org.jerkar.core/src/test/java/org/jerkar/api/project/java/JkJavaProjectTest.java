package org.jerkar.api.project.java;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.jerkar.api.depmanagement.JkDependencySet;
import org.jerkar.api.depmanagement.JkPopularModules;
import org.jerkar.api.file.JkPathTree;
import org.jerkar.api.ide.eclipse.JkEclipseClasspathGeneratorTest;
import org.jerkar.api.project.JkProjectSourceLayout;
import org.junit.Test;


public class JkJavaProjectTest {

    @Test
    public void generate() throws Exception {
        final Path top = unzipToDir("sample-multi-scriptless.zip");

        JkProjectSourceLayout sourceLayout= JkProjectSourceLayout.simple()
                .withResources("res").withTestResources("res-test");

        Path base = top.resolve("base");
        JkJavaProject baseProject = new JkJavaProject(base);
        baseProject.setSourceLayout(sourceLayout);
        baseProject.setDependencies(JkDependencySet.of().and(JkPopularModules.APACHE_HTTP_CLIENT, "4.5.3"));

        final Path core = top.resolve("core");
        final JkJavaProject coreProject = new JkJavaProject(core);
        JkDependencySet coreDeps = JkDependencySet.of().and(baseProject);
        coreProject.setSourceLayout(sourceLayout).setDependencies(coreDeps);
        coreProject.maker().setTester(
                coreProject.maker().getDefaultTester().forked(true));

        final Path desktop = top.resolve("desktop");
        final JkJavaProject desktopProject = new JkJavaProject(desktop);
        desktopProject.setSourceLayout(sourceLayout);
        final JkDependencySet deps = JkDependencySet.of().and(coreProject);
        desktopProject.setDependencies(deps);
        desktopProject.maker().defineFatJarArtifact("fat");
        desktopProject.maker().makeAllArtifacts();


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