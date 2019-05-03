package org.jerkar.samples;

import org.jerkar.api.depmanagement.JkDependencySet;
import org.jerkar.api.depmanagement.JkRepo;
import org.jerkar.api.depmanagement.JkRepoSet;
import org.jerkar.api.file.JkPathMatcher;
import org.jerkar.api.file.JkResourceProcessor;
import org.jerkar.tool.builtins.java.JkJavaProjectBuild;

import java.util.HashMap;
import java.util.Map;

import static org.jerkar.api.depmanagement.JkJavaDepScopes.TEST;
import static org.jerkar.api.depmanagement.JkPopularModules.*;

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
    protected void setup() {
        java().project()
                .setVersionedModule("org.jerkar:script-samples", "0.3-SNAPSHOT")
                .setDependencies(dependencies());
        java().project().getMaker().setDownloadRepos(JkRepoSet.of(JkRepo.of("http://my.repo1"), JkRepo.ofMavenCentral()));
        java().project().getMaker().getPublishTasks().setPublishRepos(publishRepositories());

    }

    JkDependencySet dependencies() {
        return JkDependencySet.of()
                .and(GUAVA, "21.0")	// Popular modules are available as Java constant
                .and(JERSEY_SERVER, "1.19")
                .and(JUNIT, "4.11", TEST);
    }

    static JkResourceProcessor.JkInterpolator interpolator() {
        Map<String, String> varReplacement = new HashMap<>();
        varReplacement.put("${server.ip}", "123.213.111.12");
        return JkResourceProcessor.JkInterpolator.of(JkPathMatcher.of(true, "**/*.xml"), varReplacement);
    }

    JkRepoSet publishRepositories() {
        return JkRepoSet.ofOssrhSnapshotAndRelease("myusername", "mypasword");
    }

}
