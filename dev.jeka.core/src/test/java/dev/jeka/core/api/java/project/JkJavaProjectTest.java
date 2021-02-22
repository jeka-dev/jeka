package dev.jeka.core.api.java.project;

import dev.jeka.core.api.depmanagement.JkDependencySet;
import org.junit.Assert;
import org.junit.Test;

public class JkJavaProjectTest {

    @Test
    public void getTestDependencies_containsCompileDependencies() {
        JkJavaProject javaProject = JkJavaProject.of().simpleFacade()
                .addCompileDependencies(JkDependencySet.of("a:a"))
                .addTestDependencies(JkDependencySet.of("b:b"))
                .getProject();
        JkDependencySet compileDeps = javaProject.getConstruction()
                .getCompilation().getDependencies();
        JkDependencySet testCompileDeps = javaProject.getConstruction().getTesting()
                .getCompilation().getDependencies();
        Assert.assertNotNull(compileDeps.get("a:a"));
        Assert.assertNotNull(testCompileDeps.get("a:a"));
    }
}
