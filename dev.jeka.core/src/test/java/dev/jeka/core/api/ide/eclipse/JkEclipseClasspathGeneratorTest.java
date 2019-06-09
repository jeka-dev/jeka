package dev.jeka.core.api.ide.eclipse;

import dev.jeka.core.api.depmanagement.JkComputedDependency;
import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.depmanagement.JkPopularModules;
import dev.jeka.core.api.depmanagement.JkScopedDependency;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.java.JkJavaVersion;
import dev.jeka.core.api.java.project.JkJavaProject;
import dev.jeka.core.api.java.project.JkProjectSourceLayout;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.Assert.assertEquals;

// Test on a project structure as Base -> Core -> Desktop
public class JkEclipseClasspathGeneratorTest {

    @Test
    public void generate() throws Exception {
        final Path top = unzipToDir("sample-multi-scriptless.zip");
        // JkLog.silent(true);

        final JkProjectSourceLayout sourceLayout= JkProjectSourceLayout.ofSimpleStyle()
                .withResources("res").withTestResources("res-test");

        final Path base = top.resolve("base");
        final JkJavaProject baseProject = JkJavaProject.of(sourceLayout.withBaseDir(base));
        baseProject.setDependencies(JkDependencySet.of().and(JkPopularModules.APACHE_HTTP_CLIENT, "4.5.3"));
        final JkEclipseClasspathGenerator baseGenerator =
                JkEclipseClasspathGenerator.of(baseProject);
        baseGenerator.setUsePathVariables(true);
        baseGenerator.setRunDependencies(baseProject.getMaker().getDependencyResolver(),
                JkDependencySet.of().and(JkPopularModules.GUAVA, "21.0"));
        final String baseClasspath = baseGenerator.generate();
        System.out.println("\nbase .classpath");
        System.out.println(baseClasspath);

        final Path core = top.resolve("core");
        final JkJavaProject coreProject = JkJavaProject.of(sourceLayout.withBaseDir(core));
        final JkDependencySet coreDeps = JkDependencySet.of().and(baseProject);
        coreProject.setDependencies(coreDeps);
        coreProject.getMaker().getTasksForTesting().setRunner(
                coreProject.getMaker().getTasksForTesting().getRunner().withForking(true));
        final JkEclipseClasspathGenerator coreGenerator =
                JkEclipseClasspathGenerator.of(coreProject);
        final String coreClasspath = coreGenerator.generate();
        System.out.println("\ncore .classpath");
        System.out.println(coreClasspath);

        final Path desktop = top.resolve("desktop");
        final JkDependencySet deps = JkDependencySet.of().and(coreProject);
        final JkEclipseClasspathGenerator desktopGenerator =
                JkEclipseClasspathGenerator.of(sourceLayout.withBaseDir(desktop), deps,
                        coreProject.getMaker().getDependencyResolver(), JkJavaVersion.V8);
        final String result2 = desktopGenerator.generate();

        System.out.println("\ndesktop .classpath");
        System.out.println(result2);

        final JkJavaProject desktopProject = JkJavaProject.of(sourceLayout.withBaseDir(desktop));
        desktopProject.setDependencies(deps);
        desktopProject.getMaker().makeAllArtifacts();

        // ----------------- Now, try to apply generated .classpath to projects and compare if it matches

        final JkEclipseClasspathApplier classpathApplier = new JkEclipseClasspathApplier(false);

        final JkJavaProject baseProject2 = JkJavaProject.of(sourceLayout.withBaseDir(base));
        Files.deleteIfExists(base.resolve(".classpath"));
        Files.write(base.resolve(".classpath"), baseClasspath.getBytes(Charset.forName("UTF-8")));
        //JkUtilsFile.writeString(new File(base, ".classpath"), baseClasspath, false);
        JkEclipseProject.ofJavaNature("base").writeTo(base.resolve(".project"));
        classpathApplier.apply(baseProject2);
        System.out.println(baseProject2.getDependencies().toList());
        final JkProjectSourceLayout base2Layout = baseProject2.getSourceLayout();
        final JkProjectSourceLayout baseLayout = baseProject.getSourceLayout();
        assertEquals(baseLayout.getBaseDir(), base2Layout.getBaseDir());
        final List<Path> srcFiles = base2Layout.getSources().getFiles();
        assertEquals(2, srcFiles.size());
        assertEquals("Base.java", srcFiles.get(0).getFileName().toString());
        final List<Path> resFiles = base2Layout.getResources().getFiles();
        assertEquals(1, resFiles.size());
        assertEquals("base.txt", resFiles.get(0).getFileName().toString());
        assertEquals(4, baseProject2.getDependencies().toList().size());

        final JkJavaProject coreProject2 = JkJavaProject.ofMavenLayout(core);

        Files.write(core.resolve(".classpath"), coreClasspath.getBytes(Charset.forName("utf-8")));
        //JkUtilsFile.writeString(new File(core, ".classpath"), coreClasspath, false);
        JkEclipseProject.ofJavaNature("core").writeTo(core.resolve(".project"));
        classpathApplier.apply(coreProject2);
        final List<JkScopedDependency> coreDeps2 = coreProject2.getDependencies().toList();
        assertEquals(1, coreDeps2.size());
        final JkComputedDependency baseProjectDep = (JkComputedDependency) coreDeps2.get(0).getDependency();
        assertEquals(base, baseProjectDep.getIdeProjectBaseDir());

        //JkPathTree.of(top).deleteContent();
    }

    private static Path unzipToDir(String zipName) throws IOException, URISyntaxException {
        final Path dest = Files.createTempDirectory(JkEclipseClasspathGeneratorTest.class.getName());
        final Path zip = Paths.get(JkEclipseClasspathGeneratorTest.class.getResource(zipName).toURI());
        JkPathTree.ofZip(zip).copyTo(dest);
        System.out.println("unzipped in " + dest);
        return dest;
    }

}