package org.jerkar.api.depmanagement;

import java.io.File;

import org.jerkar.api.file.JkPath;

public class JkRepoRunner {

    public static void main(String[] args) {
	// spring();
	springClassifier();
    }

    public static void spring() {
	final JkModuleDependency dep = JkModuleDependency.of("org.springframework", "spring-jdbc", "3.0.+");
	System.out.println(IvyResolverRunner.REPOS.get(dep, false).entries().size());
    }

    public static void springClassifier() {
	final JkRepos repos = JkRepos.maven(new File("build/output/mavenRepo"));
	final JkModuleDependency dep = JkModuleDependency.of("mygroup2:mymodule2:0.0.12-SNAPSHOT:other");
	final JkPath path = repos.get(dep, true);
	System.out.println(path.entries().size());
	System.out.println(path.first());
    }

}
