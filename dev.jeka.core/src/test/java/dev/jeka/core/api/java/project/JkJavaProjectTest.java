package dev.jeka.core.api.java.project;

import dev.jeka.core.api.depmanagement.JkDependencySet;
import org.junit.Assert;
import org.junit.Test;

public class JkJavaProjectTest {

    @Test
    public void getTestDependencies_containsCompileDependencies() {
        JkJavaProject javaProject = JkJavaProject.of().simpleFacade()
                .setCompileDependencies(deps -> deps.and("a:a"))
                .setTestDependencies(deps -> deps.and("b:b"))
                .getProject();
        JkDependencySet compileDeps = javaProject.getConstruction()
                .getCompilation().getDependencies();
        JkDependencySet testCompileDeps = javaProject.getConstruction().getTesting()
                .getCompilation().getDependencies();
        Assert.assertEquals(1, compileDeps.getEntries().size());
        Assert.assertNotNull(compileDeps.get("a:a"));
        Assert.assertEquals(2, testCompileDeps.getEntries().size());
        Assert.assertNotNull(testCompileDeps.get("a:a"));
        Assert.assertNotNull(testCompileDeps.get("b:b"));
    }
}
