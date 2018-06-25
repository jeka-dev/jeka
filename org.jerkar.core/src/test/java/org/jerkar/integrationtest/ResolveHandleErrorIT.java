package org.jerkar.integrationtest;

import static org.jerkar.api.depmanagement.JkJavaDepScopes.COMPILE_AND_RUNTIME;
import static org.jerkar.api.depmanagement.JkJavaDepScopes.DEFAULT_SCOPE_MAPPING;
import static org.junit.Assert.assertEquals;

import org.jerkar.api.depmanagement.JkDependencySet;
import org.jerkar.api.depmanagement.JkDependencyResolver;
import org.jerkar.api.depmanagement.JkPopularModules;
import org.jerkar.api.depmanagement.JkRepos;
import org.jerkar.api.depmanagement.JkResolutionParameters;
import org.jerkar.api.depmanagement.JkResolveResult;
import org.jerkar.api.depmanagement.JkVersionedModule;
import org.junit.Test;

/**
 * Created by angibaudj on 19-06-17.
 */
public class ResolveHandleErrorIT {

    @Test
    public void artifactNotFound() {
        JkVersionedModule holder = JkVersionedModule.of("mygroup:myname", "myversion");
        JkDependencySet deps = JkDependencySet.of()
                .and(JkPopularModules.JAVAX_SERVLET_API, "2.5.3", COMPILE_AND_RUNTIME);  // does not exist
        JkDependencyResolver resolver = JkDependencyResolver.of(JkRepos.mavenCentral())
                .withParams(JkResolutionParameters.defaultScopeMapping(DEFAULT_SCOPE_MAPPING))
                .withModuleHolder(holder);
        JkResolveResult resolveResult = resolver.resolve(deps);
        JkResolveResult.JkErrorReport errorReport = resolveResult.errorReport();
        System.out.println(errorReport.moduleProblems());
        assertEquals(1, errorReport.moduleProblems().size());

    }


}
