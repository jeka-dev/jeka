package org.jerkar.api.depmanagement;

import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor;
import org.apache.ivy.core.module.descriptor.DependencyArtifactDescriptor;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.jerkar.api.utils.JkUtilsObject;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;
import static org.jerkar.api.depmanagement.JkJavaDepScopes.*;

/**
 * Created by angibaudj on 08-03-17.
 */
public class IvyTranslationsTest {

    private static final  JkVersionedModule OWNER = JkVersionedModule.of("ownerGroup:ownerName:ownerVersion");

    @Test
    public void toPublicationLessModule() throws Exception {
        JkScopeMapping mapping = DEFAULT_SCOPE_MAPPING;
        JkVersionProvider versionProvider = JkVersionProvider.empty();

        // handle multiple artifacts properly
        DefaultModuleDescriptor desc = IvyTranslations.toPublicationLessModule(OWNER, deps(), mapping, versionProvider, null);
        DependencyDescriptor[] dependencyDescriptors = desc.getDependencies();
        assertEquals(1, dependencyDescriptors.length);
        DependencyDescriptor depDesc = dependencyDescriptors[0];
        DependencyArtifactDescriptor[] artifactDescs = depDesc.getAllDependencyArtifacts();
        assertEquals(2, artifactDescs.length);
        DependencyArtifactDescriptor mainArt = findArtifactIn(artifactDescs, null);
        assertNotNull(mainArt);
        DependencyArtifactDescriptor linuxArt = findArtifactIn(artifactDescs, "linux");
        assertNotNull(linuxArt);
        System.out.println(Arrays.asList(linuxArt.getConfigurations()));

    }

    private static JkDependencies deps() {
        return JkDependencies.builder()
                .on("aGroup:aName:1", COMPILE)
                .on("aGroup:aName:1:linux", RUNTIME, JkScope.of("toto"))
                .build();
    }

    private DependencyArtifactDescriptor findArtifactIn(DependencyArtifactDescriptor[] artifactDescs, String classsifier) {
        for (DependencyArtifactDescriptor item : artifactDescs) {
            if (JkUtilsObject.equals(item.getAttribute("classifier"), classsifier)) {
                return item;
            }
        }
        return null;
    }





}