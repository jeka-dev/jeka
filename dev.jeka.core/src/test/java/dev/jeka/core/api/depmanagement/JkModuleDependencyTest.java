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
        JkArtifactSpecification artifactSpecification = dep.getCoordinate().getArtifactSpecification();
        Assert.assertEquals("sources", artifactSpecification.getClassifier());
        Assert.assertEquals("zip", artifactSpecification.getType());
        Assert.assertEquals(JkTransitivity.NONE, dep.getTransitivity());
    }

}
