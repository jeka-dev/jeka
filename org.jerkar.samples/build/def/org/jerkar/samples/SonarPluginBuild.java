package org.jerkar.samples;

import static org.jerkar.api.depmanagement.JkPopularModules.GUAVA;
import static org.jerkar.api.depmanagement.JkPopularModules.JUNIT;

import org.jerkar.api.depmanagement.JkDependencySet;
import org.jerkar.api.depmanagement.JkJavaDepScopes;
import org.jerkar.plugins.sonar.JkPluginSonar;
import org.jerkar.plugins.sonar.JkSonar;
import org.jerkar.tool.JkDoc;
import org.jerkar.tool.JkInit;
import org.jerkar.tool.builtins.java.JkJavaProjectBuild;

/**
 * This build deletes artifacts, compiles, tests and launches SonarQube analyse.
 */
public class SonarPluginBuild extends JkJavaProjectBuild {

    @JkDoc("Sonar server environment")
    protected SonarEnv sonarEnv = SonarEnv.DEV;

    @Override
    public void setupOptionDefaults() {
        this.plugins().get(JkPluginSonar.class)
                .prop(JkSonar.BRANCH, "myBranch");
    }
    
    @Override
    protected void configurePlugins() {
        java().project()
                .setVersionedModule("org.jerkar:samples", "0.1")
                .setDependencies(JkDependencySet.of()
                    .and(GUAVA, "18.0")
                    .and(JUNIT, "4.11", JkJavaDepScopes.TEST));
    }


    public void runSonar() {
        this.plugins().get(JkPluginSonar.class).run();
    }

    enum SonarEnv {
        
        DEV("dev.myhost:81"), 
        QA("qa.myhost:81"), 
        PROD("prod.myhost:80");

        public final String url;

        SonarEnv(String url) {
            this.url = url;
        }
    }

    public static void main(String[] args) {
        JkInit.instanceOf(SonarPluginBuild.class, args).runSonar();
    }

}
