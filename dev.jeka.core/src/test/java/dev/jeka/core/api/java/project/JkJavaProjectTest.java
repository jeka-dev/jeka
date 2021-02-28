package dev.jeka.core.api.java.project;

import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.depmanagement.JkDependencySet.Hint;
import dev.jeka.core.api.depmanagement.JkTransitivity;
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

    @Test
    public void getTestDependencies_usingSetTestDependency_ok() {
        JkJavaProject project = JkJavaProject.of().simpleFacade()
                .setCompileDependencies(deps -> deps
                        .and("com.google.guava:guava:23.0", JkTransitivity.NONE)
                        .and("javax.servlet:javax.servlet-api:4.0.1"))
                .setRuntimeDependencies(deps -> deps
                        .and("org.postgresql:postgresql:42.2.19")
                        .withTransitivity("com.google.guava:guava", JkTransitivity.RUNTIME)
                        .minus("javax.servlet:javax.servlet-api"))
                .setTestDependencies(deps -> deps
                        .and(Hint.first(), "org.mockito:mockito-core:2.10.0")
                )
                .setPublishedModuleId("my:project").setPublishedVersion("MyVersion")
                .getProject();
        JkDependencySet testDependencies = project.getConstruction().getTesting().getCompilation().getDependencies();
        System.out.println(project.getInfo());
        Assert.assertEquals(JkTransitivity.RUNTIME, testDependencies.get("com.google.guava:guava").getTransitivity());
        Assert.assertNotNull(testDependencies.get("javax.servlet:javax.servlet-api"));
        Assert.assertEquals("org.mockito:mockito-core", testDependencies.getModuleDependencies().get(0)
                .getModuleId().toString());
    }

    @Test
    public void getTestDependencies_usingAddTestDependency_ok() {
        JkJavaProject project = JkJavaProject.of().simpleFacade()
                .setCompileDependencies(deps -> deps
                        .and("com.google.guava:guava:23.0", JkTransitivity.NONE)
                        .and("javax.servlet:javax.servlet-api:4.0.1"))
                .setRuntimeDependencies(deps -> deps
                        .and("org.postgresql:postgresql:42.2.19")
                        .withTransitivity("com.google.guava:guava", JkTransitivity.RUNTIME)
                        .minus("javax.servlet:javax.servlet-api"))
                .addTestDependencies(deps -> deps
                        .and("org.mockito:mockito-core:2.10.0")
                        .and("io.rest-assured:rest-assured:4.3.3")
                )
                .setPublishedModuleId("my:project").setPublishedVersion("MyVersion")
                .getProject();
        JkDependencySet testDependencies = project.getConstruction().getTesting().getCompilation().getDependencies();
        System.out.println(project.getInfo());
        Assert.assertEquals(JkTransitivity.RUNTIME, testDependencies.get("com.google.guava:guava").getTransitivity());
        Assert.assertNotNull(testDependencies.get("javax.servlet:javax.servlet-api"));
        Assert.assertEquals("org.mockito:mockito-core", testDependencies.getModuleDependencies().get(0)
                .getModuleId().toString());
        Assert.assertEquals("io.rest-assured:rest-assured", testDependencies.getModuleDependencies().get(1)
                .getModuleId().toString());
    }
}
