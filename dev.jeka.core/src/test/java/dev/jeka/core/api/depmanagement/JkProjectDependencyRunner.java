package dev.jeka.core.api.depmanagement;

import dev.jeka.core.api.tooling.maven.JkMavenProject;
import dev.jeka.core.api.tooling.maven.JkMvn;

import java.nio.file.Path;
import java.nio.file.Paths;

public class JkProjectDependencyRunner {

    public void test() {
        final Path dir = Paths.get("../../mavenProject");
        final Path jar = Paths.get("target/mypProduct.jar");
        final JkComputedDependency dependency = JkComputedDependency.of(JkMavenProject.of(dir).cleanPackage(), jar);
        System.out.println(dependency);
    }

}
