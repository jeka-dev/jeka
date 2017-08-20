package org.jerkar.api.ide.eclipse;

import java.io.File;

import org.jerkar.api.depmanagement.JkDependencies;
import org.jerkar.api.depmanagement.JkDependencyResolver;
import org.jerkar.api.depmanagement.JkRepos;
import org.jerkar.api.java.project.JkJavaProject;
import org.jerkar.api.java.project.JkJavaProjectStructure;
import org.jerkar.api.utils.JkUtilsFile;
import org.junit.Ignore;
import org.junit.Test;


public class JkEclipseClasspathGeneratorTest {

    @Test
    @Ignore
    public void generate() throws Exception {
        final File top = dir("sample-multi-scriptless.zip");

        final File core = new File(top, "core");
        final JkJavaProject coreProject = JkJavaProject.of(core);
        final JkJavaProjectStructure structure = coreProject.structure();
        structure.relocaliseOutputDir("build/output");
        structure.setEditedSources("src");
        structure.setEditedResources("res").setEditedTestResources("res-test");
        structure.setTestSources("test");
        final JkEclipseClasspathGenerator coreGenerator = new JkEclipseClasspathGenerator(structure);
        final String result = coreGenerator.generate();
        final String result1 = coreGenerator.generate();
        System.out.println(result1);


        final File desktop = new File(top, "desktop");
        final JkJavaProjectStructure desktopStructure = structure.clone().relocaliseBaseDir(desktop);
        final JkDependencies deps = JkDependencies.builder()
                .on(coreProject.asProjectDependency()).build();
        final JkEclipseClasspathGenerator desktopGenerator = new JkEclipseClasspathGenerator(desktopStructure);
        desktopGenerator.setDependencyResolver(JkDependencyResolver.managed(JkRepos.mavenCentral(), deps));
        final String result2 = desktopGenerator.generate();
        System.out.println(result2);

        JkUtilsFile.deleteDir(top);
    }

    private static File dir(String zipName) {
        final File dest = JkUtilsFile.createTempDir(JkEclipseClasspathGeneratorTest.class.getName());
        final File zip = JkUtilsFile.toFile(JkEclipseClasspathGeneratorTest.class.getResource(zipName));
        JkUtilsFile.unzip(zip, dest);
        System.out.println("unzipped in " + dest.getAbsolutePath());
        return dest;
    }

}