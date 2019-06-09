package dev.jeka.core.api.depmanagement;

import org.junit.Assert;
import org.junit.Test;

@SuppressWarnings("javadoc")
public class JkDependencyTest {

    @Test
    public void test() {
        final JkModuleDependency dep = JkModuleDependency.of("org.hibernate:hibernate-core:3.0.+");
        Assert.assertEquals("org.hibernate", dep.getModuleId().getGroup());
        Assert.assertEquals("hibernate-core", dep.getModuleId().getName());
    }

}
