package dev.jeka.core.api.depmanagement;

import org.junit.Assert;
import org.junit.Test;

@SuppressWarnings("javadoc")
public class JkGroupAndNameTest {

    @Test
    public void testOf() {
        final String groupAndName = "org.springframework.boot:spring-boot-starter-data-rest";
        final JkCoordinate.GroupAndName groupAndName1 = JkCoordinate.GroupAndName.of(groupAndName);
        Assert.assertEquals("org.springframework.boot", groupAndName1.getGroup());
        Assert.assertEquals("spring-boot-starter-data-rest", groupAndName1.getName());
    }

}
