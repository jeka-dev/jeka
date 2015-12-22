package org.jerkar.api.depmanagement;

import java.io.File;

import org.jerkar.api.system.JkLog;
import org.jerkar.tool.JkBuild;
import org.jerkar.tool.JkInit;

public class JkRepoRunner {

    public static void main(String[] args) {
        spring();
        //springClassifier();
    }

    public static void spring() {
        JkInit.instanceOf(JkBuild.class);
        JkLog.verbose(true);
        final JkModuleDependency dep = JkModuleDependency.of("org.springframework", "spring-core",
                "3.1.1.RELEASE").ext("pom");
        System.out.println(IvyResolverRunner.REPOS.get(dep));
    }

    public static void springClassifier() {
        final JkRepos repos = JkRepos.maven(new File("build/output/mavenRepo"));
        final JkModuleDependency dep = JkModuleDependency
                .of("mygroup2:mymodule2:0.0.12-SNAPSHOT:other");
        final File file = repos.get(dep);
        System.out.println(file);
    }

}
