package org.jerkar.api.ide.eclipse;

import java.io.File;

import org.jerkar.api.depmanagement.JkDependencies;
import org.jerkar.api.depmanagement.JkDependencyResolver;
import org.jerkar.api.depmanagement.JkPopularModules;
import org.jerkar.api.depmanagement.JkRepos;
import org.jerkar.api.project.java.JkJavaProject;
import org.jerkar.api.project.JkProjectSourceLayout;
import org.jerkar.api.system.JkLog;
import org.jerkar.api.utils.JkUtilsFile;
import org.junit.Ignore;
import org.junit.Test;


public class JkEclipseClasspathGeneratorTest {

    @Test
    @Ignore
    public void generate() throws Exception {
        final File top = unzipToDir("sample-multi-scriptless.zip");
        JkLog.silent(true);

        JkProjectSourceLayout sourceLayout= JkProjectSourceLayout.simple()
                .withResources("res").withTestResources("res-test");
        JkDependencyResolver resolver = JkDependencyResolver.of(JkRepos.mavenCentral());

        File base = new File(top, "base");
        JkJavaProject baseProject = new JkJavaProject(base);
        baseProject.setSourceLayout(sourceLayout);
        baseProject.setDependencies(JkDependencies.builder().on(JkPopularModules.APACHE_HTTP_CLIENT, "4.5.3").build());
        final JkEclipseClasspathGenerator baseGenerator =
                new JkEclipseClasspathGenerator(baseProject, resolver);
        final String result0 = baseGenerator.generate();
        System.out.println("\nbase .classpath");
        System.out.println(result0);

        final File core = new File(top, "core");
        final JkJavaProject coreProject = new JkJavaProject(core);
        JkDependencies coreDeps = JkDependencies.of(baseProject.asDependency());
        coreProject.setSourceLayout(sourceLayout).setDependencies(coreDeps);
        coreProject.setMaker(coreProject.maker().setJuniter(
                coreProject.maker().getJuniter().forked(true)));
        final JkEclipseClasspathGenerator coreGenerator =
                new JkEclipseClasspathGenerator(coreProject, resolver);
        final String result1 = coreGenerator.generate();
        System.out.println("\ncore .classpath");
        System.out.println(result1);

        final File desktop = new File(top, "desktop");
        final JkDependencies deps = JkDependencies.builder().on(coreProject.asDependency()).build();
        final JkEclipseClasspathGenerator desktopGenerator = new JkEclipseClasspathGenerator(sourceLayout.withBaseDir(desktop));
        desktopGenerator.setDependencyResolver(deps, resolver);
        final String result2 = desktopGenerator.generate();

        System.out.println("\ndestop .classpath");
        System.out.println(result2);


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