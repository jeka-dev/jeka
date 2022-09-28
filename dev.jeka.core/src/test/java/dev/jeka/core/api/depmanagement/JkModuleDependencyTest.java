package dev.jeka.core.api.depmanagement;

import dev.jeka.core.api.depmanagement.JkCoordinate.JkArtifactSpecification;
import org.junit.Assert;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;

@SuppressWarnings("javadoc")
public class JkModuleDependencyTest {

    @Test
    public void testOf() {
        JkCoordinateDependency dep = JkCoordinateDependency.of("group:name:sources:zip:version")
                .withTransitivity(JkTransitivity.NONE);
        JkArtifactSpecification artifactSpecification =
                dep.getCoordinate().getArtifactSpecifications().iterator().next();
        Assert.assertEquals("sources", artifactSpecification.getClassifier());
        Assert.assertEquals("zip", artifactSpecification.getType());
        Assert.assertEquals(JkTransitivity.NONE, dep.getTransitivity());

        dep = JkCoordinateDependency.of("group:name:sources,javadoc:zip:");
        Assert.assertEquals(JkVersion.UNSPECIFIED, dep.getCoordinate().getVersion());
        List<JkArtifactSpecification> specs = new LinkedList<>(dep.getCoordinate().getArtifactSpecifications());
        Assert.assertEquals("sources", specs.get(0).getClassifier());
        Assert.assertEquals("javadoc", specs.get(1).getClassifier());
    }

}
