package org.jerkar.samples;

import static org.jerkar.api.depmanagement.JkPopularModules.GUAVA;
import static org.jerkar.api.depmanagement.JkPopularModules.JUNIT;

import org.jerkar.api.depmanagement.JkDependencies;
import org.jerkar.api.depmanagement.JkJavaDepScopes;
import org.jerkar.api.project.java.JkJavaProject;
import org.jerkar.plugins.jacoco.JkBuildPlugin2Jacoco;
import org.jerkar.plugins.sonar.JkBuildPlugin2Sonar;
import org.jerkar.plugins.sonar.JkSonar;
import org.jerkar.tool.JkDoc;
import org.jerkar.tool.JkInit;
import org.jerkar.tool.builtins.javabuild.JkJavaProjectBuild;

/**
 * This build deleteArtifacts, compile,test launch sonar analyse by default.
 */
public class SonarPluginBuild extends JkJavaProjectBuild {

    @JkDoc("Sonar server environment")
    protected SonarEnv sonarEnv = SonarEnv.DEV;

    JkBuildPlugin2Sonar pluginSonar;
    
    @Override
    protected JkJavaProject createProject(JkJavaProject project) {
        return project
                .setVersionedModule("org.jerkar:samples", "0.1")
                .setDependencies(JkDependencies.builder()
                .on(GUAVA, "18.0")
                .on(JUNIT, "4.11", JkJavaDepScopes.TEST).build());
    }

    @Override
    protected void setupPlugins() {
        new JkBuildPlugin2Jacoco().apply(this);
        pluginSonar = new JkBuildPlugin2Sonar()
           //     .prop(JkSonar.HOST_URL, sonarEnv.url)
                .prop(JkSonar.BRANCH, "myBranch");
        pluginSonar.apply(this);
    }

    public void doSonar() {
        clean();
        this.project().maker().test();
        pluginSonar.verify(this);
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
        JkInit.instanceOf(SonarPluginBuild.class, args).doSonar();
    }

}
