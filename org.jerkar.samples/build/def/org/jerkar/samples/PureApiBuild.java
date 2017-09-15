package org.jerkar.samples;

import org.jerkar.api.depmanagement.JkDependencies;
import org.jerkar.api.depmanagement.JkPopularModules;
import org.jerkar.api.depmanagement.JkJavaDepScopes;
import org.jerkar.api.java.JkJavaVersion;
import org.jerkar.api.project.JkProjectOutLayout;
import org.jerkar.api.project.java.JkJavaProject;
import org.jerkar.tool.JkBuild;
import org.jerkar.tool.JkInit;

import java.io.File;

/**
 * This should be run with org.jerkar.samples as workinf dir.
 */
public class PureApiBuild extends JkBuild {

    public void doDefault() {
        JkJavaProject javaProject = new JkJavaProject(this.baseDir().root());

        javaProject.setArtifactName("pure-api-build-project");

        // We want to output stuff in another place than build/output
        JkProjectOutLayout outLayaout =
                JkProjectOutLayout.classicJava().withOutputDir("build/output/alt-output");

        JkDependencies deps = JkDependencies.builder()
                .on(JkPopularModules.JUNIT, "4.12").scope(JkJavaDepScopes.TEST)
                .build();

        javaProject.setOutLayout(outLayaout)
            .setDependencies(deps)
            .setSourceVersion(JkJavaVersion.V6);

        javaProject.maker().clean();
        javaProject.makeMainJar();
        javaProject.maker().makeJavadocJar();
    }

    public static void main(String[] args) {
        JkInit.instanceOf(PureApiBuild.class, args).doDefault();
    }
}
