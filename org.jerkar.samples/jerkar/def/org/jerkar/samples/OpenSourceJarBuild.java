package org.jerkar.samples;

import org.jerkar.api.depmanagement.JkDependencySet;
import org.jerkar.api.depmanagement.JkMavenPublicationInfo;
import org.jerkar.tool.JkRun;
import org.jerkar.tool.builtins.java.JkPluginJava;

import static org.jerkar.api.depmanagement.JkJavaDepScopes.TEST;
import static org.jerkar.api.depmanagement.JkPopularModules.GUAVA;
import static org.jerkar.api.depmanagement.JkPopularModules.JUNIT;

/**
 * This build demonstrates how to specify project metadata required to publish on
 * Maven central ( see https://maven.apache.org/guides/mini/guide-central-repository-upload.html )
 * 
 * @author Jerome Angibaud
 */
public class OpenSourceJarBuild extends JkRun {

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
                .addDependencies(deps)
                .setMavenPublicationInfo(info);
    }
   
}
