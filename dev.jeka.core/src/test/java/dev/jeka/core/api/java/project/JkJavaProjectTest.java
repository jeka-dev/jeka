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
        Assert.assertEquals(1, compileDeps.getDependencies().size());
        Assert.assertNotNull(compileDeps.get("a:a"));
        Assert.assertEquals(2, testCompileDeps.getDependencies().size());
        Assert.assertNotNull(testCompileDeps.get("a:a"));
        Assert.assertNotNull(testCompileDeps.get("b:b"));
    }
}
