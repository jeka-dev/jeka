package org.jerkar.api.ide.eclipse;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.jerkar.api.depmanagement.JkDependencies;
import org.jerkar.api.depmanagement.JkDependencyResolver;
import org.jerkar.api.depmanagement.JkRepos;
import org.jerkar.api.java.project.JkJavaProject;
import org.jerkar.api.java.project.JkProjectSourceLayout;
import org.jerkar.api.utils.JkUtilsFile;
import org.junit.Ignore;
import org.junit.Test;


public class JkEclipseClasspathGeneratorTest {

    @Test
    @Ignore
    public void generate() throws Exception {
        final File top = unzipToDir("sample-multi-scriptless.zip");

        JkProjectSourceLayout sourceLayout= JkProjectSourceLayout.simple()
                .withResources("res").withTestResources("res-test");
        JkDependencyResolver resolver = JkDependencyResolver.of(JkRepos.mavenCentral());
        Map<String, String> options = new HashMap<String, String>();

        File base = new File(top, "base");
        JkJavaProject baseProject = JkJavaProject.of(base);
        baseProject.setSourceLayout(sourceLayout);
        final JkEclipseClasspathGenerator baseGenerator =
                new JkEclipseClasspathGenerator(baseProject, resolver, options);
        final String result0 = baseGenerator.generate();
        System.out.println("\nbase .classpath");
        System.out.println(result0);

        final File core = new File(top, "core");
        final JkJavaProject coreProject = JkJavaProject.of(core);
        JkDependencies coreDeps = JkDependencies.of(baseProject.asDependency());
        coreProject.setSourceLayout(sourceLayout).setDependencies(coreDeps);
        final JkEclipseClasspathGenerator coreGenerator =
                new JkEclipseClasspathGenerator(coreProject, resolver, options);
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