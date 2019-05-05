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
import org.jerkar.api.java.project.JkProjectSourceLayout;
import org.jerkar.api.java.project.JkJavaProject;
import org.junit.Test;

/**
 * Created by angibaudj on 21-09-17.
 */
public class JkImlGeneratorTest {

    @Test
    public void generate() throws Exception {
        final Path top = unzipToDir("sample-multi-scriptless.zip");
        final Path base = top.resolve("base");

        final JkProjectSourceLayout sourceLayout= JkProjectSourceLayout.ofSimpleStyle()
                .withResources("res").withTestResources("res-test").withBaseDir(base);
        final JkJavaProject baseProject = JkJavaProject.of(sourceLayout);
        baseProject.setDependencies(JkDependencySet.of().and(JkPopularModules.APACHE_HTTP_CLIENT, "4.5.3"));
        final JkImlGenerator baseGenerator = JkImlGenerator.of(baseProject);
        final String result0 = baseGenerator.generate();
        System.out.println("\nbase .classpath");
        System.out.println(result0);

        final Path core = top.resolve("core");
        final JkJavaProject coreProject = JkJavaProject.of(sourceLayout.withBaseDir(core));
        final JkDependencySet coreDeps = JkDependencySet.of().and(baseProject);
        coreProject.setDependencies(coreDeps);
        coreProject.getMaker().getTasksForTesting().setRunner(
                coreProject.getMaker().getTasksForTesting().getRunner().withForking(true));
        final JkImlGenerator coreGenerator = JkImlGenerator.of(coreProject);
        final String result1 = coreGenerator.generate();
        System.out.println("\ncore .classpath");
        System.out.println(result1);

        final Path desktop = top.resolve("desktop");
        final JkDependencySet deps = JkDependencySet.of().and(coreProject);
        final JkImlGenerator desktopGenerator =
                JkImlGenerator.of(sourceLayout.withBaseDir(desktop), deps,
                        coreProject.getMaker().getDependencyResolver());
        final String result2 = desktopGenerator.generate();

        System.out.println("\ndesktop .classpath");
        System.out.println(result2);

        final JkJavaProject desktopProject = JkJavaProject.of(sourceLayout.withBaseDir(desktop));
        desktopProject.setDependencies(deps);
        desktopProject.getMaker().makeAllArtifacts();

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