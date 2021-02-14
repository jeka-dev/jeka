package dev.jeka.core.integrationtest.resolution;

import dev.jeka.core.api.depmanagement.*;
import dev.jeka.core.api.depmanagement.resolution.JkDependencyResolver;
import dev.jeka.core.api.depmanagement.resolution.JkResolveResult;
import dev.jeka.core.api.depmanagement.resolution.JkResolvedDependencyNode;
import org.junit.Test;

import static dev.jeka.core.api.depmanagement.JkPopularModules.GUAVA;
import static dev.jeka.core.api.depmanagement.JkPopularModules.JAVAX_SERVLET_API;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class QualifiedDependencyResolutionIT {

    @Test
    public void resolve_qualifierWith2MasterConfigurations_ok() {
        JkQualifiedDependencies deps = JkQualifiedDependencies.of()
                .and("compile, runtime", "com.github.djeang:vincer-dom:1.3.0");
        JkDependencyResolver resolver = JkDependencyResolver.of().addRepos(JkRepo.ofMavenCentral());
        JkResolvedDependencyNode tree = resolver.resolve(deps).getDependencyTree();
        assertEquals(1, tree.getChildren().size());
    }

    @Test
    public void resolve_computedIdeDependencies_ok() {
        JkDependencySet compile = JkDependencySet.of()
                .and(GUAVA.version("19.0"))
                .and (JAVAX_SERVLET_API)
                .andVersionProvider(JkVersionProvider.of(JAVAX_SERVLET_API, "4.0.1"));
        JkDependencySet runtime = compile.minus(JAVAX_SERVLET_API);
        JkQualifiedDependencies qdeps = JkQualifiedDependencies.computeIdeDependencies(compile, runtime,
                JkDependencySet.of());
        JkDependencyResolver resolver = JkDependencyResolver.of().addRepos(JkRepo.ofMavenCentral());
        JkResolveResult resolveResult = resolver.resolve(qdeps);
        assertTrue(resolveResult.contains(JAVAX_SERVLET_API));
        assertTrue(resolveResult.contains(GUAVA));
        assertEquals(2, resolveResult.getDependencyTree().getResolvedVersions().getModuleIds().size());
    }
}
