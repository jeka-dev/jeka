package dev.jeka.core.api.depmanagement;

import org.junit.jupiter.api.Test;

class JkExternalModuleTest {

    @SuppressWarnings("unused")
    @Test
    void testOf() {
        JkDependency dep;
        dep = JkCoordinateDependency.of("org.hibernate:hibernate-core:3.0.1.Final");
        dep = JkCoordinateDependency.of("org.hibernate:hibernate-core:3.0.1+");
    }

}
