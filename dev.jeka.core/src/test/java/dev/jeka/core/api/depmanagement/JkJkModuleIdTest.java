package dev.jeka.core.api.depmanagement;

import org.junit.Assert;
import org.junit.Test;

@SuppressWarnings("javadoc")
public class JkJkModuleIdTest {

    @Test
    public void testOf() {
        final String moduleId = "org.springframework.boot:spring-boot-starter-data-rest";
        final JkModuleId jkModuleId1 = JkModuleId.of(moduleId);
        Assert.assertEquals("org.springframework.boot", jkModuleId1.getGroup());
        Assert.assertEquals("spring-boot-starter-data-rest", jkModuleId1.getName());

        final JkModuleId jkModuleId2 = JkModuleId.of("org.springframework.boot:spring-boot-dependencies::pom:");
        Assert.assertEquals("org.springframework.boot", jkModuleId2.getGroup());
        Assert.assertEquals("spring-boot-dependencies", jkModuleId2.getName());
    }

}
