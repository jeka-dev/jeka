package dev.jeka.core.api.depmanagement;


import dev.jeka.core.api.utils.JkUtilsObject;
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor;
import org.apache.ivy.core.module.descriptor.DependencyArtifactDescriptor;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.junit.Test;

import java.util.Arrays;

import static dev.jeka.core.api.depmanagement.JkJavaDepScopes.DEFAULT_SCOPE_MAPPING;
import static dev.jeka.core.api.depmanagement.JkScopedDependencyTest.COMPILE;
import static dev.jeka.core.api.depmanagement.JkScopedDependencyTest.RUNTIME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by angibaudj on 08-03-17.
 */
public class IvyTranslationsTest {

    private static final  JkVersionedModule OWNER = JkVersionedModule.of("ownerGroup:ownerName:ownerVersion");

    @Test
    public void toPublicationLessModule() throws Exception {
        final JkScopeMapping mapping = DEFAULT_SCOPE_MAPPING;
        final JkVersionProvider versionProvider = JkVersionProvider.of();

        // handle multiple artifacts properly
        JkDependencySet deps = deps();
        final DefaultModuleDescriptor desc = IvyTranslations.toPublicationLessModule(OWNER, deps, mapping, versionProvider);
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
                .and("aGroup:aName::linux:1", RUNTIME, JkScope.of("toto"));
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