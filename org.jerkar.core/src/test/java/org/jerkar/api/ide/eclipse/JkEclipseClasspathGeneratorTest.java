package org.jerkar.api.ide.eclipse;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.jerkar.api.depmanagement.JkComputedDependency;
import org.jerkar.api.depmanagement.JkDependencies;
import org.jerkar.api.depmanagement.JkPopularModules;
import org.jerkar.api.depmanagement.JkScopedDependency;
import org.jerkar.api.file.JkPathTree;
import org.jerkar.api.java.JkJavaVersion;
import org.jerkar.api.project.JkProjectSourceLayout;
import org.jerkar.api.project.java.JkJavaProject;
import org.junit.Test;


public class JkEclipseClasspathGeneratorTest {

    @Test
    public void generate() throws Exception {
        final Path top = unzipToDir("sample-multi-scriptless.zip");
        // JkLog.silent(true);

        final JkProjectSourceLayout sourceLayout= JkProjectSourceLayout.simple()
                .setResources("res").setTestResources("res-test");

        final Path base = top.resolve("base");
        final JkJavaProject baseProject = new JkJavaProject(base);
        baseProject.setSourceLayout(sourceLayout);
        baseProject.setDependencies(JkDependencies.builder().on(JkPopularModules.APACHE_HTTP_CLIENT, "4.5.3").build());
        final JkEclipseClasspathGenerator baseGenerator =
                new JkEclipseClasspathGenerator(baseProject);
        baseGenerator.setBuildDependencyResolver(baseProject.maker().getDependencyResolver(),
                JkDependencies.builder().on(JkPopularModules.GUAVA, "21.0").build());
        final String baseClasspath = baseGenerator.generate();
        System.out.println("\nbase .classpath");
        System.out.println(baseClasspath);

        final Path core = top.resolve("core");
        final JkJavaProject coreProject = new JkJavaProject(core);
        final JkDependencies coreDeps = JkDependencies.of(baseProject);
        coreProject.setSourceLayout(sourceLayout).setDependencies(coreDeps);
        coreProject.maker().setJuniter(
                coreProject.maker().getJuniter().forked(true));
        final JkEclipseClasspathGenerator coreGenerator =
                new JkEclipseClasspathGenerator(coreProject);
        final String coreClasspath = coreGenerator.generate();
        System.out.println("\ncore .classpath");
        System.out.println(coreClasspath);

        final Path desktop = top.resolve("desktop");
        final JkDependencies deps = JkDependencies.of(coreProject);
        final JkEclipseClasspathGenerator desktopGenerator =
                new JkEclipseClasspathGenerator(sourceLayout.setBaseDir(desktop), deps,
                        coreProject.maker().getDependencyResolver(), JkJavaVersion.V8);
        final String result2 = desktopGenerator.generate();

        System.out.println("\ndesktop .classpath");
        System.out.println(result2);

        final JkJavaProject desktopProject = new JkJavaProject(desktop);
        desktopProject.setSourceLayout(sourceLayout);
        desktopProject.setDependencies(deps);
        desktopProject.maker().makeAllArtifactFiles();

        // ----------------- Now,  try to applyCommons generated .classpath to projects and compare if it matches

        final JkEclipseClasspathApplier classpathApplier = new JkEclipseClasspathApplier(false);

        final JkJavaProject baseProject2 = new JkJavaProject(base);
        Files.deleteIfExists(base.resolve(".classpath"));
        Files.write(base.resolve(".classpath"), baseClasspath.getBytes(Charset.forName("UTF-8")));
        //JkUtilsFile.writeString(new File(base, ".classpath"), baseClasspath, false);
        JkEclipseProject.ofJavaNature("base").writeTo(base.resolve(".project"));
        classpathApplier.apply(baseProject2);
        final JkProjectSourceLayout base2Layout = baseProject2.getSourceLayout();
        final JkProjectSourceLayout baseLayout = baseProject.getSourceLayout();
        assertEquals(baseLayout.getBaseDir(), base2Layout.getBaseDir());
        final List<Path> srcFiles = base2Layout.getSources().files();
        assertEquals(2, srcFiles.size());
        assertEquals("Base.java", srcFiles.get(0).getFileName().toString());
        final List<Path> resFiles = base2Layout.getResources().files();
        assertEquals(1, resFiles.size());
        assertEquals("base.txt", resFiles.get(0).getFileName().toString());
        assertEquals(5, baseProject2.getDependencies().list().size());

        final JkJavaProject coreProject2 = new JkJavaProject(core);

        Files.write(core.resolve(".classpath"), coreClasspath.getBytes(Charset.forName("utf-8")));
        //JkUtilsFile.writeString(new File(core, ".classpath"), coreClasspath, false);
        JkEclipseProject.ofJavaNature("core").writeTo(core.resolve(".project"));
        classpathApplier.apply(coreProject2);
        final List<JkScopedDependency> coreDeps2 = coreProject2.getDependencies().list();
        assertEquals(1, coreDeps2.size());
        final JkComputedDependency baseProjectDep = (JkComputedDependency) coreDeps2.get(0).dependency();
        assertEquals(base, baseProjectDep.ideProjectBaseDir());

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