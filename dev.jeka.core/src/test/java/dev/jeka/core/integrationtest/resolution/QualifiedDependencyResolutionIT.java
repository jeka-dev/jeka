package dev.jeka.core.integrationtest.resolution;

import dev.jeka.core.api.depmanagement.JkQualifiedDependencies;
import dev.jeka.core.api.depmanagement.JkRepo;
import dev.jeka.core.api.depmanagement.resolution.JkDependencyResolver;
import dev.jeka.core.api.depmanagement.resolution.JkResolvedDependencyNode;
import org.junit.Test;

import static org.junit.Assert.*;

public class QualifiedDependencyResolutionIT {

    @Test
    public void resolve_qualifierWith2MasterConfigurations_ok() {
        JkQualifiedDependencies deps = JkQualifiedDependencies.of()
                .and("compile, runtime", "com.github.djeang:vincer-dom:1.3.0");
        JkDependencyResolver resolver = JkDependencyResolver.of().addRepos(JkRepo.ofMavenCentral());
        JkResolvedDependencyNode tree = resolver.resolve(deps).getDependencyTree();
        assertEquals(1, tree.getChildren().size());
    }
}
