package dev.jeka.core.api.depmanagement;

import dev.jeka.core.api.depmanagement.tooling.JkScope;
import org.junit.Test;

import java.net.URL;

import static org.junit.Assert.*;

public class JkDependencySetTest {

    @Test
    public void testFromDescription()  {
        URL url = JkDependencySetTest.class.getResource("dependencies.txt");
        JkDependencySet dependencySet = JkDependencySet.ofTextDescription(url);
        assertEquals(4, dependencySet.getDeclaredScopes().size());
        assertEquals(10, dependencySet.getDependencies().size());
    }

    @Test
    public void testWithoutDuplicate() {
        JkScope scope = JkScope.of("scope");
        JkDependencySet dependencySet = JkDependencySet.of()
                .and("my.group:module:2.0")
                .and("my.group:module:2.8")
                .andFile("./foo.jar")
                .and("my.group:module")
                .and("other:other")
                .withDefaultScopes(scope)
                .minusDuplicates();
        assertEquals(3, dependencySet.getDependenciesDeclaredWith(scope).size());
        assertEquals("2.8", dependencySet.getVersion(JkModuleId.of("my.group:module")).getValue());

    }

}
