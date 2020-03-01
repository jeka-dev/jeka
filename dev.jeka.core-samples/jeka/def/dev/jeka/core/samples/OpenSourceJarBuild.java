package dev.jeka.core.samples;

import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.depmanagement.JkMavenPublicationInfo;
import dev.jeka.core.tool.JkCommandSet;
import dev.jeka.core.tool.builtins.java.JkPluginJava;

import static dev.jeka.core.api.depmanagement.JkJavaDepScopes.TEST;
import static dev.jeka.core.api.depmanagement.JkPopularModules.GUAVA;
import static dev.jeka.core.api.depmanagement.JkPopularModules.JUNIT;

/**
 * This build demonstrates how to specify project metadata required to publish on
 * Maven central ( see https://maven.apache.org/guides/mini/guide-central-repository-upload.html )
 * 
 * @author Jerome Angibaud
 */
public class OpenSourceJarBuild extends JkCommandSet {

    JkPluginJava javaPlugin = getPlugin(JkPluginJava.class);

    @Override
    protected void setup() {
        JkDependencySet deps = JkDependencySet.of()
                .and(GUAVA, "18.0")
                .and(JUNIT, "4.11", TEST);
        
        JkMavenPublicationInfo info = JkMavenPublicationInfo
                .of("my project", "my description", "https://github.com/jerkar/samples")
                .withScm("https://github.com/jerkar/sample.git")
                .andApache2License()
                .andGitHubDeveloper("John Doe", "johndoe6591@gmail.com");
        
        javaPlugin.getProject()
                .setVersionedModule("org.jerkar:sample-open-source", "1.3.1-SNAPSHOT")
                .addDependencies(deps);

        javaPlugin.getProject().getMaker().getTasksForPublishing()
                .setMavenPublicationInfo(info);
    }
   
}
