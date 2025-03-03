package dev.jeka.core.api.depmanagement;

import org.junit.Assert;
import org.junit.Test;

public class JkCoordinateDependencyTest {

    @Test
    public void test() {
        final JkCoordinateDependency dep = JkCoordinateDependency.of("org.hibernate:hibernate-core:3.0.+");
        Assert.assertEquals("org.hibernate", dep.getCoordinate().getModuleId().getGroup());
        Assert.assertEquals("hibernate-core", dep.getCoordinate().getModuleId().getName());
    }
}
