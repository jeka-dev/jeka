package dev.jeka.core.api.depmanagement;

import org.junit.Assert;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;

@SuppressWarnings("javadoc")
public class JkModuleDependencyTest {

    @Test
    public void testOf() {
        JkModuleDependency dep = JkModuleDependency.of("group:name:sources:zip:version")
                .withTransitivity(JkTransitivity.NONE);
        JkModuleDependency.JkArtifactSpecification artifactSpecification =
                dep.getArtifactSpecifications().iterator().next();
        Assert.assertEquals("sources", artifactSpecification.getClassifier());
        Assert.assertEquals("zip", artifactSpecification.getType());
        Assert.assertEquals(JkTransitivity.NONE, dep.getTransitivity());

        dep = JkModuleDependency.of("group:name:sources,javadoc:zip:");
        Assert.assertEquals(JkVersion.UNSPECIFIED, dep.getVersion());
        List<JkModuleDependency.JkArtifactSpecification> specs = new LinkedList<>(dep.getArtifactSpecifications());
        Assert.assertEquals("sources", specs.get(0).getClassifier());
        Assert.assertEquals("javadoc", specs.get(1).getClassifier());
    }

}
