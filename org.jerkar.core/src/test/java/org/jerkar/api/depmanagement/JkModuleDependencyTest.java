package org.jerkar.api.depmanagement;

import org.junit.Assert;
import org.junit.Test;

@SuppressWarnings("javadoc")
public class JkModuleDependencyTest {

    @Test
    public void testOf() {
        final JkModuleDependency dep = JkModuleDependency.of("group:name::sources:projectVersion")
                .transitive(true).ext("zip");
        Assert.assertEquals("sources", dep.classifier());
        Assert.assertTrue(dep.transitive());

        final JkModuleDependency dep2 = JkModuleDependency.of("group:name:zip:sources:projectVersion");
        Assert.assertEquals("zip", dep2.ext());

        final JkModuleDependency dep3 = JkModuleDependency.of("group:name:projectVersion:sources");

    }

}
