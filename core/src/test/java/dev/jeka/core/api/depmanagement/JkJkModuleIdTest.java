package dev.jeka.core.api.depmanagement;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JkJkModuleIdTest {

    @Test
    void testOf() {
        final String moduleId = "org.springframework.boot:spring-boot-starter-data-rest";
        final JkModuleId jkModuleId1 = JkModuleId.of(moduleId);
        assertEquals("org.springframework.boot", jkModuleId1.getGroup());
        assertEquals("spring-boot-starter-data-rest", jkModuleId1.getName());

        final JkModuleId jkModuleId2 = JkModuleId.of("org.springframework.boot:spring-boot-dependencies::pom:");
        assertEquals("org.springframework.boot", jkModuleId2.getGroup());
        assertEquals("spring-boot-dependencies", jkModuleId2.getName());
    }

}
