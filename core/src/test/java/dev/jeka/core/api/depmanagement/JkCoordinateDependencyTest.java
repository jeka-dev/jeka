package dev.jeka.core.api.depmanagement;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class JkCoordinateDependencyTest {

    @Test
    void test() {
        final JkCoordinateDependency dep = JkCoordinateDependency.of("org.hibernate:hibernate-core:3.0.+");
        Assertions.assertEquals("org.hibernate", dep.getCoordinate().getModuleId().getGroup());
        Assertions.assertEquals("hibernate-core", dep.getCoordinate().getModuleId().getName());
    }
}
