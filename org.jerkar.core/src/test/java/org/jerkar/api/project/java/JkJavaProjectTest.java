package org.jerkar.api.project.java;

import org.jerkar.api.depmanagement.JkDependencies;
import org.jerkar.api.depmanagement.JkPopularModules;
import org.jerkar.api.ide.eclipse.JkEclipseClasspathGenerator;
import org.jerkar.api.java.JkJavaVersion;
import org.jerkar.api.project.JkProjectSourceLayout;
import org.jerkar.api.system.JkLog;
import org.jerkar.api.utils.JkUtilsFile;
import org.junit.Ignore;
import org.junit.Test;

import java.awt.*;
import java.io.File;


public class JkJavaProjectTest {

    @Test
    public void generate() throws Exception {
        final File top = unzipToDir("sample-multi-scriptless.zip");
        JkLog.silent(false);

        JkProjectSourceLayout sourceLayout= JkProjectSourceLayout.simple()
                .withResources("res").withTestResources("res-test");

        File base = new File(top, "base");
        JkJavaProject baseProject = new JkJavaProject(base);
        baseProject.setSourceLayout(sourceLayout);
        baseProject.setDependencies(JkDependencies.builder().on(JkPopularModules.APACHE_HTTP_CLIENT, "4.5.3").build());

        final File core = new File(top, "core");
        final JkJavaProject coreProject = new JkJavaProject(core);
        JkDependencies coreDeps = JkDependencies.of(baseProject);
        coreProject.setSourceLayout(sourceLayout).setDependencies(coreDeps);
        coreProject.maker().setJuniter(
                coreProject.maker().getJuniter().forked(true));

        final File desktop = new File(top, "desktop");
        final JkJavaProject desktopProject = new JkJavaProject(desktop);
        desktopProject.setSourceLayout(sourceLayout);
        final JkDependencies deps = JkDependencies.builder().on(coreProject).build();
        desktopProject.setDependencies(deps);
        desktopProject.addFatJarArtifactFile("fat");
        desktopProject.makeAllArtifactFiles();


        // Desktop.getDesktop().open(desktop);


        try {
            JkUtilsFile.deleteDir(top);
        } catch (RuntimeException e) {};
    }

    private static File unzipToDir(String zipName) {
        final File dest = JkUtilsFile.createTempDir(JkJavaProjectTest.class.getName());
        final File zip = JkUtilsFile.toFile(JkJavaProjectTest.class.getResource(zipName));
        JkUtilsFile.unzip(zip, dest);
        System.out.println("unzipped in " + dest.getAbsolutePath());
        return dest;
    }

}