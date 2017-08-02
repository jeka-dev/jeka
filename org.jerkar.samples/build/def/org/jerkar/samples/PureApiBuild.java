package org.jerkar.samples;

import org.jerkar.api.depmanagement.JkDependencies;
import org.jerkar.api.depmanagement.JkPopularModules;
import org.jerkar.api.java.JkJavaCompiler;
import org.jerkar.api.java.build.JkJavaDepScopes;
import org.jerkar.api.java.build.JkJavaProject;

import java.io.File;

/**
 * This should be run with org.jerkar.samples as workinf dir.
 */
public class PureApiBuild {

    public static void main(String[] args) {
        JkJavaProject javaProject = JkJavaProject.ofCurrentWorkingDir();

        // We want to output stuff in another place than build/output
        javaProject.structure().relocaliseOutputDir(new File("build/output/alt-output"));

        javaProject.depResolver().setDependencies(JkDependencies.builder()
                .on(JkPopularModules.JUNIT, "4.12").scope(JkJavaDepScopes.TEST)
                .build());
        javaProject.depResolver().setForceRefresh(true);

        javaProject.setBaseCompiler(JkJavaCompiler.base()
                .withSourceVersion(JkJavaCompiler.V7)
                .withTargetVersion(JkJavaCompiler.V7)
        );

        javaProject.packager().addCheckSum("md5").setDoFatJar(true);

        javaProject.clean();
        javaProject.compile();
        javaProject.test();
        javaProject.pack();

    }
}
