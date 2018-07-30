package org.jerkar.api.ide.intellij;

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
import org.jerkar.api.project.java.JkJavaProject;
import org.junit.Test;

/**
 * Created by angibaudj on 21-09-17.
 */
public class JkImlGeneratorTest {

    @Test
    public void generate() throws Exception {
        final Path top = unzipToDir("sample-multi-scriptless.zip");

        final JkProjectSourceLayout sourceLayout= JkProjectSourceLayout.simple()
                .withResources("res").withTestResources("res-test");

        final Path base = top.resolve("base");
        final JkJavaProject baseProject = new JkJavaProject(base);
        baseProject.setSourceLayout(sourceLayout);
        baseProject.setDependencies(JkDependencySet.of().and(JkPopularModules.APACHE_HTTP_CLIENT, "4.5.3"));
        final JkImlGenerator baseGenerator = new JkImlGenerator(baseProject);
        final String result0 = baseGenerator.generate();
        System.out.println("\nbase .classpath");
        System.out.println(result0);

        final Path core = top.resolve("core");
        final JkJavaProject coreProject = new JkJavaProject(core);
        final JkDependencySet coreDeps = JkDependencySet.of().and(baseProject);
        coreProject.setSourceLayout(sourceLayout).setDependencies(coreDeps);
        coreProject.maker().setTester(
                coreProject.maker().getDefaultTester().forked(true));
        final JkImlGenerator coreGenerator = new JkImlGenerator(coreProject);
        final String result1 = coreGenerator.generate();
        System.out.println("\ncore .classpath");
        System.out.println(result1);

        final Path desktop = top.resolve("desktop");
        final JkDependencySet deps = JkDependencySet.of().and(coreProject);
        final JkImlGenerator desktopGenerator =
                new JkImlGenerator(sourceLayout.withBaseDir(desktop), deps,
                        coreProject.maker().getDependencyResolver());
        final String result2 = desktopGenerator.generate();

        System.out.println("\ndesktop .classpath");
        System.out.println(result2);

        final JkJavaProject desktopProject = new JkJavaProject(desktop);
        desktopProject.setSourceLayout(sourceLayout);
        desktopProject.setDependencies(deps);
        desktopProject.maker().makeAllArtifacts();

        JkPathTree.of(top).deleteContent();
    }

    private static Path unzipToDir(String zipName) throws IOException, URISyntaxException {
        final Path dest = Files.createTempDirectory(JkEclipseClasspathGeneratorTest.class.getName());
        final Path zip = Paths.get(JkEclipseClasspathGeneratorTest.class.getResource(zipName).toURI());
        JkPathTree.ofZip(zip).copyTo(dest);
        System.out.println("unzipped in " + dest);
        return dest;
    }

}