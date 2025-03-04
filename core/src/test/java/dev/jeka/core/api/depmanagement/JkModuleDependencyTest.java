package dev.jeka.core.api.depmanagement;

import dev.jeka.core.api.depmanagement.JkCoordinate.JkArtifactSpecification;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JkModuleDependencyTest {

    @Test
    public void testOf() {
        JkCoordinateDependency dep = JkCoordinateDependency.of("group:name:sources:zip:version")
                .withTransitivity(JkTransitivity.NONE);
        JkArtifactSpecification artifactSpecification = dep.getCoordinate().getArtifactSpecification();
        assertEquals("sources", artifactSpecification.getClassifier());
        assertEquals("zip", artifactSpecification.getType());
        assertEquals(JkTransitivity.NONE, dep.getTransitivity());
    }

}
