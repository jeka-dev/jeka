package org.jerkar.api.ide.eclipse;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.jerkar.api.depmanagement.JkComputedDependency;
import org.jerkar.api.depmanagement.JkDependencySet;
import org.jerkar.api.depmanagement.JkPopularModules;
import org.jerkar.api.depmanagement.JkScopedDependency;
import org.jerkar.api.file.JkPathTree;
import org.jerkar.api.java.JkJavaVersion;
import org.jerkar.api.java.project.JkProjectSourceLayout;
import org.jerkar.api.java.project.JkJavaProject;
import org.junit.Test;

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
        baseGenerator.setRunDependencies(baseProject.getMaker().getDependencyResolver(),
                JkDependencySet.of().and(JkPopularModules.GUAVA, "21.0"));
        final String baseClasspath = baseGenerator.generate();
        System.out.println("\nbase .classpath");
        System.out.println(baseClasspath);

        final Path core = top.resolve("core");
        final JkJavaProject coreProject = JkJavaProject.of(sourceLayout.withBaseDir(core));
        final JkDependencySet coreDeps = JkDependencySet.of().and(baseProject);
        coreProject.setDependencies(coreDeps);
        coreProject.getMaker().getTestTasks().setRunner(
                coreProject.getMaker().getTestTasks().getRunner().withForking(true));
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

        // ----------------- Now,  try to applyCommonSettings generated .classpath to projects and compare if it matches

        final JkEclipseClasspathApplier classpathApplier = new JkEclipseClasspathApplier(false);

        final JkJavaProject baseProject2 = JkJavaProject.of(sourceLayout.withBaseDir(base));
        Files.deleteIfExists(base.resolve(".classpath"));
        Files.write(base.resolve(".classpath"), baseClasspath.getBytes(Charset.forName("UTF-8")));
        //JkUtilsFile.writeString(new File(base, ".classpath"), baseClasspath, false);
        JkEclipseProject.ofJavaNature("base").writeTo(base.resolve(".project"));
        classpathApplier.apply(baseProject2);
        final JkProjectSourceLayout base2Layout = baseProject2.getSourceLayout();
        final JkProjectSourceLayout baseLayout = baseProject.getSourceLayout();
        assertEquals(baseLayout.baseDir(), base2Layout.baseDir());
        final List<Path> srcFiles = base2Layout.sources().getFiles();
        assertEquals(2, srcFiles.size());
        assertEquals("Base.java", srcFiles.get(0).getFileName().toString());
        final List<Path> resFiles = base2Layout.resources().getFiles();
        assertEquals(1, resFiles.size());
        assertEquals("base.txt", resFiles.get(0).getFileName().toString());
        assertEquals(5, baseProject2.getDependencies().toList().size());

        final JkJavaProject coreProject2 = JkJavaProject.ofMavenLayout(core);

        Files.write(core.resolve(".classpath"), coreClasspath.getBytes(Charset.forName("utf-8")));
        //JkUtilsFile.writeString(new File(core, ".classpath"), coreClasspath, false);
        JkEclipseProject.ofJavaNature("core").writeTo(core.resolve(".project"));
        classpathApplier.apply(coreProject2);
        final List<JkScopedDependency> coreDeps2 = coreProject2.getDependencies().toList();
        assertEquals(1, coreDeps2.size());
        final JkComputedDependency baseProjectDep = (JkComputedDependency) coreDeps2.get(0).getDependency();
        assertEquals(base, baseProjectDep.getIdeProjectBaseDir());

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