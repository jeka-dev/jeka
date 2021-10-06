package dev.jeka.core.api.depmanagement;

import dev.jeka.core.api.tooling.JkMvn;

import java.nio.file.Path;
import java.nio.file.Paths;

@SuppressWarnings("javadoc")
public class JkProjectDependencyRunner {

    public void test() {
        final Path dir = Paths.get("../../mavenProject");
        final Path jar = Paths.get("target/mypProduct.jar");
        final JkComputedDependency dependency = JkComputedDependency.of(JkMvn.of(dir).cleanPackage(), jar);
        System.out.println(dependency);
    }

}
