package org.jerkar.api.depmanagement;

import java.io.File;

import org.jerkar.api.tooling.JkMvn;

@SuppressWarnings("javadoc")
public class JkProjectDependencyRunner {

    public void test() {
        final File dir = new File("../../mavenProject");
        final File jar = new File(dir, "target/mypProduct.jar");
        final JkComputedDependency dependency = JkComputedDependency.of(JkMvn.of(dir)
                .cleanPackage(), jar);
        System.out.println(dependency);
    }

}
