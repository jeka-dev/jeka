package org.jerkar.samples;

import org.jerkar.api.depmanagement.JkDependencySet;
import org.jerkar.api.depmanagement.JkJavaDepScopes;
import org.jerkar.tool.JkDoc;
import org.jerkar.tool.JkInit;
import org.jerkar.tool.JkRun;
import org.jerkar.tool.builtins.java.JkPluginJava;
import org.jerkar.tool.builtins.sonar.JkPluginSonar;
import org.jerkar.tool.builtins.sonar.JkSonar;

import static org.jerkar.api.depmanagement.JkPopularModules.GUAVA;
import static org.jerkar.api.depmanagement.JkPopularModules.JUNIT;

/**
 * This build deletes artifacts, compiles, tests and launches SonarQube analyse.
 */
public class SonarPluginBuild extends JkRun {

    JkPluginJava javaPlugin = getPlugin(JkPluginJava.class);

    @JkDoc("Sonar server environment")
    protected SonarEnv sonarEnv = SonarEnv.DEV;

    public SonarPluginBuild() {
        this.getPlugins().get(JkPluginSonar.class)
                .prop(JkSonar.BRANCH, "myBranch");
    }
    
    @Override
    protected void setup() {
        javaPlugin.getProject()
                .setVersionedModule("org.jerkar:samples", "0.1")
                .addDependencies(JkDependencySet.of()
                    .and(GUAVA, "18.0")
                    .and(JUNIT, "4.11", JkJavaDepScopes.TEST));
    }

    public void runSonar() {
        this.getPlugins().get(JkPluginSonar.class).run();
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
