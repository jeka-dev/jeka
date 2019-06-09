package org.jerkar.samples;

import org.jerkar.api.depmanagement.JkDependencySet;
import org.jerkar.api.depmanagement.JkJavaDepScopes;
import org.jerkar.api.depmanagement.JkPopularModules;
import dev.jeka.core.api.java.JkJavaVersion;
import dev.jeka.core.api.java.project.JkJavaProject;
import dev.jeka.core.tool.JkInit;
import dev.jeka.core.tool.JkRun;

/**
 * This should be run with org.jerkar.samples as working dir.
 */
public class PureApiBuild extends JkRun {

    public void doDefault() {
        JkJavaProject javaProject = JkJavaProject.ofMavenLayout(this.getBaseDir());

        // We want to output stuff in another place than build/output
        javaProject.getMaker().setOutLayout(javaProject.getMaker().getOutLayout().withOutputDir("build/output/alt-output"));

        JkDependencySet deps = JkDependencySet.of().and(JkPopularModules.JUNIT, "4.12",JkJavaDepScopes.TEST);

        javaProject.setDependencies(deps).setSourceVersion(JkJavaVersion.V6);

        javaProject.getMaker().clean();
        javaProject.getMaker().makeAllArtifacts();
    }

    public static void main(String[] args) {
        JkInit.instanceOf(PureApiBuild.class, args).doDefault();
    }
}
