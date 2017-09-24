package org.jerkar.samples;

import static org.jerkar.api.depmanagement.JkJavaDepScopes.TEST;
import static org.jerkar.api.depmanagement.JkPopularModules.GUAVA;
import static org.jerkar.api.depmanagement.JkPopularModules.JUNIT;

import java.io.File;

import org.jerkar.api.depmanagement.JkDependencies;
import org.jerkar.api.depmanagement.JkMavenPublicationInfo;
import org.jerkar.api.project.java.JkJavaProject;
import org.jerkar.tool.builtins.javabuild.JkJavaProjectBuild;

/**
 * This build demonstrate how to specified project metadata required to publish on 
 * Maven central ( see https://maven.apache.org/guides/mini/guide-central-repository-upload.html )
 * 
 * @author Jerome Angibaud
 */
public class OpenSourceJarBuild extends JkJavaProjectBuild {

    @Override
    protected JkJavaProject createProject(File baseDir) {
        JkDependencies deps = JkDependencies.builder()
                .on(GUAVA, "18.0") 
                .on(JUNIT, "4.11", TEST).build();
        
        JkMavenPublicationInfo info = JkMavenPublicationInfo
                .of("my project", "my description", "https://github.com/jerkar/samples")
                .withScm("https://github.com/jerkar/sample.git")
                .andApache2License()
                .andGitHubDeveloper("djeang", "dgeangdev@yahoo.fr");
        
        return new JkJavaProject(baseDir)
                .setVersionedModule("org.jerkar:sample-open-source", "1.3.1-SNAPSHOT")
                .setDependencies(deps)
                .setMavenPublicationInfo(info);
    }
   
}
