package org.jerkar.samples;

import static org.jerkar.api.depmanagement.JkJavaDepScopes.TEST;
import static org.jerkar.api.depmanagement.JkPopularModules.GUAVA;
import static org.jerkar.api.depmanagement.JkPopularModules.JERSEY_SERVER;
import static org.jerkar.api.depmanagement.JkPopularModules.JUNIT;
import static org.jerkar.api.depmanagement.JkPopularModules.MOCKITO_ALL;

import org.jerkar.api.depmanagement.JkDependencies;
import org.jerkar.api.depmanagement.JkPublishRepos;
import org.jerkar.api.depmanagement.JkRepo;
import org.jerkar.api.depmanagement.JkRepos;
import org.jerkar.api.project.java.JkJavaProject;
import org.jerkar.tool.builtins.java.JkJavaProjectBuild;

/**
 * Build sample for a jar project depending on several external modules. This
 * build produces :
 * - jar, fat-jar, sources-jar by launching 'jerkar'
 * - all this + javadoc by launching 'jerkar doDefault doc'
 * - all this + publish on remote repository by typing 'jerkar doDefault doc publish'
 * 
 * @author Jerome Angibaud
 */
public class MavenStyleBuild extends JkJavaProjectBuild {

    @Override
    protected void configurePlugins() {
        java().project()
                .setVersionedModule("org.jerkar:script-samples", "0.3-SNAPSHOT")
                .setDependencies(dependencies());
        java().project().maker().setDownloadRepos(JkRepos.of(JkRepo.maven("http://my.repo1"), JkRepo.mavenCentral()));
        java().project().maker().setPublishRepos(publishRepositories());
    }

    JkDependencies dependencies() {
        return JkDependencies.builder()
                .on(GUAVA, "21.0")	// Popular modules are available as Java constant
                .on(JERSEY_SERVER, "1.19")
             //   .on("com.orientechnologies:orientdb-client:2.2.35")
                .on(JUNIT, "4.11").scope(TEST)
             //   .on(MOCKITO_ALL, "1.9.5").scope(TEST)
                .build();
    }

    JkPublishRepos publishRepositories() {
        return JkPublishRepos.of(JkRepo.maven("http://my.snapshot.repo").asPublishSnapshotRepo())
                .and(JkRepo.maven("http://my.release.repo").asPublishReleaseRepo());
    }

}
