package dev.jeka.core.api.depmanagement.embedded.ivy;


import dev.jeka.core.api.depmanagement.JkCoordinate;
import dev.jeka.core.api.depmanagement.JkCoordinateDependency;
import dev.jeka.core.api.depmanagement.JkQualifiedDependencySet;
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor;
import org.apache.ivy.core.module.descriptor.DependencyArtifactDescriptor;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.id.ArtifactId;
import org.apache.ivy.core.module.id.ModuleId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;


/**
 * Created by angibaudj on 08-03-17.
 */
class IvyTranslationsTest {

    private static final JkCoordinate OWNER = JkCoordinate.of("ownerGroup:ownerName:ownerVersion");

    @Test
    void toResolveModuleDescriptor_2identicalModuleWithDistinctClassifiers_leadsIn1dependencies() {
        JkQualifiedDependencySet deps = JkQualifiedDependencySet.of()
                .and(null, JkCoordinateDependency.of(JkCoordinate.of("aGroup:aName:1.0").withClassifier("linux")))
                .and(null, "bGroup:bName:win:exe:1.0");
        final DefaultModuleDescriptor desc = IvyTranslatorToModuleDescriptor.toResolveModuleDescriptor(
                OWNER, deps);
        final DependencyDescriptor[] dependencyDescriptors = desc.getDependencies();
        assertEquals(2, dependencyDescriptors.length);
        DependencyArtifactDescriptor linuxArtifact = dependencyDescriptors[0].getAllDependencyArtifacts()[0];
        assertEquals("jar", linuxArtifact.getType());
        assertEquals("linux", linuxArtifact.getExtraAttribute("classifier"));
        DependencyArtifactDescriptor winArtifact = dependencyDescriptors[1].getAllDependencyArtifacts()[0];
        assertEquals("exe", winArtifact.getType());
        assertEquals("win", winArtifact.getExtraAttribute("classifier"));
    }

    @Test
    void toResolveModuleDescriptor_2identicalModuleIdWithDistinctClassifiers_leadsIn1dependencyWithTwoArtifacts() {
        JkQualifiedDependencySet deps = JkQualifiedDependencySet.of()
                .and(null, JkCoordinateDependency.of(JkCoordinate.of("aGroup:aName:linux:exe:1.0")))
                .and(null, JkCoordinateDependency.of(JkCoordinate.of("aGroup:aName:1.0")));
        final DefaultModuleDescriptor desc = IvyTranslatorToModuleDescriptor.toResolveModuleDescriptor(
                OWNER, deps);
        final DependencyDescriptor[] dependencyDescriptors = desc.getDependencies();

        assertEquals(1, dependencyDescriptors.length);
        DependencyArtifactDescriptor linuxArtifact = dependencyDescriptors[0].getAllDependencyArtifacts()[0];
        assertEquals("exe", linuxArtifact.getType());
        assertEquals("linux", linuxArtifact.getExtraAttribute("classifier"));
        DependencyArtifactDescriptor defaultArtifact = dependencyDescriptors[0].getAllDependencyArtifacts()[1];
        assertEquals("jar", defaultArtifact.getType());
        assertNull(defaultArtifact.getExtraAttribute("classifier"));
    }

    @Test
    void toResolveModuleDescriptor_wildcardExclusion_excludesTransitiveDependenciesOnly() {
        JkQualifiedDependencySet deps = JkQualifiedDependencySet.of()
                .and(null, JkCoordinateDependency.of("com.google.guava:guava:33.4.8-jre")
                        .andExclusions("com.google.guava:*", "*:*"));
        DependencyDescriptor guavaDesc = IvyTranslatorToModuleDescriptor.toResolveModuleDescriptor(OWNER, deps)
                .getDependencies()[0];
        String[] confs = guavaDesc.getModuleConfigurations();

        assertFalse(guavaDesc.doesExclude(confs, artifactId("com.google.guava", "guava")));
        assertTrue(guavaDesc.doesExclude(confs, artifactId("com.google.guava", "failureaccess")));
        assertTrue(guavaDesc.doesExclude(confs, artifactId("org.jspecify", "jspecify")));
    }

    private static ArtifactId artifactId(String group, String name) {
        return new ArtifactId(new ModuleId(group, name), name, "jar", "jar");
    }

}