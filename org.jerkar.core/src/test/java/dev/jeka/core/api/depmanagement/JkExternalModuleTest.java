package dev.jeka.core.api.depmanagement;

import org.junit.Test;

@SuppressWarnings("javadoc")
public class JkExternalModuleTest {

    @SuppressWarnings("unused")
    @Test
    public void testOf() {
        JkDependency dep;
        dep = JkModuleDependency.of(JkModuleId.of("org.hibernate", "hibernate-core"),
                JkVersion.of("3.0.1.Final"));
        dep = JkModuleDependency.of("org.hibernate", "hibernate-core", "3.0.1.Final");
        dep = JkModuleDependency.of("org.hibernate:hibernate-core:3.0.1+");
    }

}
