package org.jerkar.api.project.java;

import java.io.File;
import java.nio.file.Path;

import org.jerkar.api.depmanagement.JkDependencies;
import org.jerkar.api.depmanagement.JkPopularModules;
import org.jerkar.api.file.JkFileTree;
import org.jerkar.api.project.JkProjectSourceLayout;
import org.jerkar.api.system.JkLog;
import org.jerkar.api.utils.JkUtilsFile;
import org.junit.Test;


public class JkJavaProjectTest {

    @Test
    public void generate() throws Exception {
        final Path top = unzipToDir("sample-multi-scriptless.zip").toPath();
        JkLog.silent(false);

        JkProjectSourceLayout sourceLayout= JkProjectSourceLayout.simple()
                .withResources("res").withTestResources("res-test");

        Path base = top.resolve("base");
        JkJavaProject baseProject = new JkJavaProject(base);
        baseProject.setSourceLayout(sourceLayout);
        baseProject.setDependencies(JkDependencies.builder().on(JkPopularModules.APACHE_HTTP_CLIENT, "4.5.3").build());

        final Path core = top.resolve("core");
        final JkJavaProject coreProject = new JkJavaProject(core);
        JkDependencies coreDeps = JkDependencies.of(baseProject);
        coreProject.setSourceLayout(sourceLayout).setDependencies(coreDeps);
        coreProject.maker().setJuniter(
                coreProject.maker().getJuniter().forked(true));

        final Path desktop = top.resolve("desktop");
        final JkJavaProject desktopProject = new JkJavaProject(desktop);
        desktopProject.setSourceLayout(sourceLayout);
        final JkDependencies deps = JkDependencies.builder().on(coreProject).build();
        desktopProject.setDependencies(deps);
        desktopProject.addFatJarArtifactFile("fat");
        desktopProject.makeAllArtifactFiles();


        // Desktop.getDesktop().open(desktop);
        JkFileTree.of(top).deleteRoot();
    }

    private static File unzipToDir(String zipName) {
        final File dest = JkUtilsFile.createTempDir(JkJavaProjectTest.class.getName());
        final File zip = JkUtilsFile.toFile(JkJavaProjectTest.class.getResource(zipName));
        JkUtilsFile.unzip(zip, dest);
        System.out.println("unzipped in " + dest.getAbsolutePath());
        return dest;
    }

}