package org.jerkar.api.depmanagement;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

public class JkDependencySetTest {

    @Test
    public void testFromDescription() throws IOException {
        InputStream is = JkDependencySetTest.class.getResourceAsStream("dependencies.txt");
        JkDependencySet dependencySet = JkDependencySet.ofTextDescription(is);
        Assert.assertEquals(4, dependencySet.getDeclaredScopes().size());
        Assert.assertEquals(10, dependencySet.toList().size());
        is.close();
    }

}
