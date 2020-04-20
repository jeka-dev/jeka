package dev.jeka.core.api.depmanagement;

import org.junit.Assert;
import org.junit.Test;

@SuppressWarnings("javadoc")
public class JkModuleDependencyTest {

    @Test
    public void testOf() {
        final JkModuleDependency dep = JkModuleDependency.of("group:name::sources:version")
                .withTransitive(true).withExt("zip");
        Assert.assertEquals("sources", dep.getClassifier());
        Assert.assertTrue(dep.withTransitive());

        final JkModuleDependency dep2 = JkModuleDependency.of("group:name:zip:sources:version");
        Assert.assertEquals("zip", dep2.getExt());

        final JkModuleDependency dep3 = JkModuleDependency.of("group:name:version:sources");

    }

}
