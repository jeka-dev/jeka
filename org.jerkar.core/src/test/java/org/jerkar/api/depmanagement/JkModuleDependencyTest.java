package org.jerkar.api.depmanagement;

import org.junit.Assert;
import org.junit.Test;

@SuppressWarnings("javadoc")
public class JkModuleDependencyTest {

    @Test
    public void testOf() {
        final JkModuleDependency dep = JkModuleDependency.of("group:name::sources:version")
                .isTransitive(true).withExt("zip");
        Assert.assertEquals("sources", dep.withClassifier());
        Assert.assertTrue(dep.isTransitive());

        final JkModuleDependency dep2 = JkModuleDependency.of("group:name:zip:sources:version");
        Assert.assertEquals("zip", dep2.withExt());

        final JkModuleDependency dep3 = JkModuleDependency.of("group:name:version:sources");

    }

}
