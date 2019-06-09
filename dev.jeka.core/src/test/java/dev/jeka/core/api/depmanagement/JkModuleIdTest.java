package dev.jeka.core.api.depmanagement;

import org.junit.Assert;
import org.junit.Test;

@SuppressWarnings("javadoc")
public class JkModuleIdTest {

    @Test
    public void testOf() {
        final String groupAndName = "org.springframework.boot:spring-boot-starter-data-rest";
        final JkModuleId jkModuleId = JkModuleId.of(groupAndName);
        Assert.assertEquals("org.springframework.boot", jkModuleId.getGroup());
        Assert.assertEquals("spring-boot-starter-data-rest", jkModuleId.getName());
    }

}
