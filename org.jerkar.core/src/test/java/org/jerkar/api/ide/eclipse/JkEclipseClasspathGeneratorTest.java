package org.jerkar.api.ide.eclipse;

import java.io.File;
import java.util.List;

import org.jerkar.api.depmanagement.JkComputedDependency;
import org.jerkar.api.depmanagement.JkDependencies;
import org.jerkar.api.depmanagement.JkPopularModules;
import org.jerkar.api.depmanagement.JkScopedDependency;
import org.jerkar.api.java.JkJavaVersion;
import org.jerkar.api.project.java.JkJavaProject;
import org.jerkar.api.project.JkProjectSourceLayout;
import org.jerkar.api.utils.JkUtilsFile;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;


public class JkEclipseClasspathGeneratorTest {

    @Test
    @Ignore
    public void generate() throws Exception {
        final File top = unzipToDir("sample-multi-scriptless.zip");
       // JkLog.silent(true);

        JkProjectSourceLayout sourceLayout= JkProjectSourceLayout.simple()
                .withResources("res").withTestResources("res-test");

        File base = new File(top, "base");
        JkJavaProject baseProject = new JkJavaProject(base);
        baseProject.setSourceLayout(sourceLayout);
        baseProject.setDependencies(JkDependencies.builder().on(JkPopularModules.APACHE_HTTP_CLIENT, "4.5.3").build());
        final JkEclipseClasspathGenerator baseGenerator =
                new JkEclipseClasspathGenerator(baseProject);
        final String baseClasspath = baseGenerator.generate();
        System.out.println("\nbase .classpath");
        System.out.println(baseClasspath);

        final File core = new File(top, "core");
        final JkJavaProject coreProject = new JkJavaProject(core);
        JkDependencies coreDeps = JkDependencies.of(baseProject);
        coreProject.setSourceLayout(sourceLayout).setDependencies(coreDeps);
        coreProject.maker().setJuniter(
                coreProject.maker().getJuniter().forked(true));
        final JkEclipseClasspathGenerator coreGenerator =
                new JkEclipseClasspathGenerator(coreProject);
        final String coreClasspath = coreGenerator.generate();
        System.out.println("\ncore .classpath");
        System.out.println(coreClasspath);

        final File desktop = new File(top, "desktop");
        final JkDependencies deps = JkDependencies.of(coreProject);
        final JkEclipseClasspathGenerator desktopGenerator =
                new JkEclipseClasspathGenerator(sourceLayout.withBaseDir(desktop), deps,
                        coreProject.maker().getDependencyResolver(), JkJavaVersion.V8);
        final String result2 = desktopGenerator.generate();

        System.out.println("\ndesktop .classpath");
        System.out.println(result2);

        final JkJavaProject desktopProject = new JkJavaProject(desktop);
        desktopProject.setSourceLayout(sourceLayout);
        desktopProject.setDependencies(deps);
        desktopProject.makeAllArtifactFiles();

        // ----------------- Now,  try to applyCommons generated .classpath to projects and compare if it matches

        JkEclipseClasspathApplier classpathApplier = new JkEclipseClasspathApplier(false);

        JkJavaProject baseProject2 = new JkJavaProject(base);
        JkUtilsFile.writeString(new File(base, ".classpath"), baseClasspath, false);
        Project.ofJavaNature("base").writeTo(new File(base, ".project"));
        classpathApplier.apply(baseProject2);
        JkProjectSourceLayout base2Layout = baseProject2.getSourceLayout();
        JkProjectSourceLayout baseLayout = baseProject.getSourceLayout();
        assertEquals(baseLayout.baseDir(), base2Layout.baseDir());
        List<File> srcFiles = base2Layout.sources().files(false);
        assertEquals(2, srcFiles.size());
        assertEquals("Base.java", srcFiles.get(0).getName());
        List<File> resFiles = base2Layout.resources().files(false);
        assertEquals(1, resFiles.size());
        assertEquals("base.txt", resFiles.get(0).getName());
        assertEquals(4, baseProject2.getDependencies().list().size());

        JkJavaProject coreProject2 = new JkJavaProject(core);
        JkUtilsFile.writeString(new File(core, ".classpath"), coreClasspath, false);
        Project.ofJavaNature("core").writeTo(new File(core, ".project"));
        classpathApplier.apply(coreProject2);
        List<JkScopedDependency> coreDeps2 = coreProject2.getDependencies().list();
        assertEquals(1, coreDeps2.size());
        JkComputedDependency baseProjectDep = (JkComputedDependency) coreDeps2.get(0).dependency();
        assertTrue(JkUtilsFile.isSame(base, baseProjectDep.ideProjectBaseDir()));

        //Desktop.getDesktop().open(top);

        JkUtilsFile.deleteDir(top);
    }

    private static File unzipToDir(String zipName) {
        final File dest = JkUtilsFile.createTempDir(JkEclipseClasspathGeneratorTest.class.getName());
        final File zip = JkUtilsFile.toFile(JkEclipseClasspathGeneratorTest.class.getResource(zipName));
        JkUtilsFile.unzip(zip, dest);
        System.out.println("unzipped in " + dest.getAbsolutePath());
        return dest;
    }

}