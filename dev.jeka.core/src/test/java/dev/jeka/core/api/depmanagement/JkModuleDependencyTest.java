package dev.jeka.core.api.depmanagement;

import org.junit.Assert;
import org.junit.Test;

@SuppressWarnings("javadoc")
public class JkModuleDependencyTest {

    @Test
    public void testOf() {
        final JkModuleDependency dep = JkModuleDependency.of("group:name:sources:zip:version")
                .withTransitivity(JkTransitivity.NONE);
        Assert.assertEquals("sources", dep.getClassifier());
        Assert.assertEquals("zip", dep.getType());
        Assert.assertEquals(JkTransitivity.NONE, dep.getTransitivity());
    }

}
