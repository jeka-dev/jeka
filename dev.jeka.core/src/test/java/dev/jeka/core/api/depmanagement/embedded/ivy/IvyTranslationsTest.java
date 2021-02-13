package dev.jeka.core.api.depmanagement.embedded.ivy;


import dev.jeka.core.api.depmanagement.JkQualifiedDependencies;
import dev.jeka.core.api.depmanagement.JkVersionedModule;
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor;
import org.apache.ivy.core.module.descriptor.DependencyArtifactDescriptor;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by angibaudj on 08-03-17.
 */
public class IvyTranslationsTest {

    private static final JkVersionedModule OWNER = JkVersionedModule.of("ownerGroup:ownerName:ownerVersion");

    @Test
    public void toResolveModuleDescriptor_2identicalModuleWithDistinctClassifiers_leadsIn2dependencies() {
        JkQualifiedDependencies deps =JkQualifiedDependencies.of()
                .and(null, "aGroup:aName:1.0")
                .and(null, "aGroup:aName:linux:1.0")
                .and(null, "bGroup:bName:win:exe:1.0");
        final DefaultModuleDescriptor desc = IvyTranslatorToModuleDescriptor.toResolveModuleDescriptor(
                OWNER, deps);
        final DependencyDescriptor[] dependencyDescriptors = desc.getDependencies();
        assertEquals(3, dependencyDescriptors.length);
        DependencyArtifactDescriptor linuxArtifact = dependencyDescriptors[1].getAllDependencyArtifacts()[0];
        assertEquals("jar", linuxArtifact.getType());
        assertEquals("linux", linuxArtifact.getName());
        DependencyArtifactDescriptor winArtifact = dependencyDescriptors[2].getAllDependencyArtifacts()[0];
        assertEquals("exe", winArtifact.getType());
        assertEquals("win", winArtifact.getName());
    }


}