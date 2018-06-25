package org.jerkar.api.depmanagement;

import static org.jerkar.api.depmanagement.JkJavaDepScopes.COMPILE;
import static org.jerkar.api.depmanagement.JkJavaDepScopes.DEFAULT_SCOPE_MAPPING;
import static org.jerkar.api.depmanagement.JkJavaDepScopes.RUNTIME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;

import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor;
import org.apache.ivy.core.module.descriptor.DependencyArtifactDescriptor;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.jerkar.api.utils.JkUtilsObject;
import org.junit.Test;

/**
 * Created by angibaudj on 08-03-17.
 */
public class IvyTranslationsTest {

    private static final  JkVersionedModule OWNER = JkVersionedModule.of("ownerGroup:ownerName:ownerVersion");

    @Test
    public void toPublicationLessModule() throws Exception {
        final JkScopeMapping mapping = DEFAULT_SCOPE_MAPPING;
        final JkVersionProvider versionProvider = JkVersionProvider.empty();

        // handle multiple artifacts properly
        final DefaultModuleDescriptor desc = IvyTranslations.toPublicationLessModule(OWNER, deps(), mapping, versionProvider);
        final DependencyDescriptor[] dependencyDescriptors = desc.getDependencies();
        assertEquals(1, dependencyDescriptors.length);
        final DependencyDescriptor depDesc = dependencyDescriptors[0];
        final DependencyArtifactDescriptor[] artifactDescs = depDesc.getAllDependencyArtifacts();
        assertEquals(2, artifactDescs.length);
        final DependencyArtifactDescriptor mainArt = findArtifactIn(artifactDescs, null);
        assertNotNull(mainArt);
        final DependencyArtifactDescriptor linuxArt = findArtifactIn(artifactDescs, "linux");
        assertNotNull(linuxArt);
        System.out.println(Arrays.asList(linuxArt.getConfigurations()));

    }

    private static JkDependencySet deps() {
        return JkDependencySet.of()
                .and("aGroup:aName:1", COMPILE)
                .and("aGroup:aName:1:linux", RUNTIME, JkScope.of("toto"));
    }

    private DependencyArtifactDescriptor findArtifactIn(DependencyArtifactDescriptor[] artifactDescs, String classsifier) {
        for (final DependencyArtifactDescriptor item : artifactDescs) {
            if (JkUtilsObject.equals(item.getAttribute("classifier"), classsifier)) {
                return item;
            }
        }
        return null;
    }





}