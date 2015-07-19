package org.jerkar.api.depmanagement;


public class JkRepoRunner {

	public static void main(String[] args) {
		spring();
	}

	public static void spring() {
		final JkRepos repos = JkRepos.maven("http://i-net1102e-prod:8081/nexus/content/groups/bnppf-secured/");

		final JkModuleDependency dep = JkModuleDependency.of("org.springframework", "spring-jdbc", "3.0.+");
		System.out.println(repos.get(dep, false).entries().size());
	}



}
