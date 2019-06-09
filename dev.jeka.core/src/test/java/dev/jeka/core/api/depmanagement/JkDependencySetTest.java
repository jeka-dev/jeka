package dev.jeka.core.api.depmanagement;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;

public class JkDependencySetTest {

    @Test
    public void testFromDescription() throws IOException {
        URL url = JkDependencySetTest.class.getResource("dependencies.txt");
        JkDependencySet dependencySet = JkDependencySet.ofTextDescription(url);
        Assert.assertEquals(4, dependencySet.getDeclaredScopes().size());
        Assert.assertEquals(10, dependencySet.toList().size());
    }

}
