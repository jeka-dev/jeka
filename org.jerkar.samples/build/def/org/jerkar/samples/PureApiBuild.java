package org.jerkar.samples;

import org.jerkar.api.depmanagement.JkDependencySet;
import org.jerkar.api.depmanagement.JkJavaDepScopes;
import org.jerkar.api.depmanagement.JkPopularModules;
import org.jerkar.api.java.JkJavaVersion;
import org.jerkar.api.project.java.JkJavaProject;
import org.jerkar.tool.JkBuild;
import org.jerkar.tool.JkInit;

import java.nio.file.Paths;

/**
 * This should be run with org.jerkar.samples as working dir.
 */
public class PureApiBuild extends JkBuild {

    public void doDefault() {
        JkJavaProject javaProject = new JkJavaProject(this.baseDir());

        javaProject.setArtifactName("pure-api-build-project");

        // We want to output stuff in another place than build/output
        javaProject.setOutLayout(javaProject.getOutLayout().withOutputDir("build/output/alt-output"));

        JkDependencySet deps = JkDependencySet.of().and(JkPopularModules.JUNIT, "4.12",JkJavaDepScopes.TEST);

        javaProject.setDependencies(deps).setSourceVersion(JkJavaVersion.V6);

        javaProject.maker().clean();
        javaProject.maker().makeArtifact(javaProject.get().mainArtifactId());
        javaProject.maker().makeJavadocJar(Paths.get("javadoc.jar"));
    }

    public static void main(String[] args) {
        JkInit.instanceOf(PureApiBuild.class, args).doDefault();
    }
}
