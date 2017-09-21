package org.jerkar.integrationtest;

import org.jerkar.api.depmanagement.*;
import org.jerkar.tool.builtins.javabuild.JkJavaBuild;
import org.junit.Test;

import static org.jerkar.tool.builtins.javabuild.JkJavaBuild.*;
import static org.junit.Assert.assertEquals;

/**
 * Created by angibaudj on 19-06-17.
 */
public class ResolveHandleErrorIT {

    @Test
    public void artifactNotFound() {
        JkVersionedModule holder = JkVersionedModule.of("mygroup:myname", "myversion");
        JkDependencies deps = JkDependencies.builder()
                .on(JkPopularModules.JAVAX_SERVLET_API, "2.5.3").scope(COMPILE_AND_RUNTIME)  // does not exist
                .build();
        JkDependencyResolver resolver = JkDependencyResolver.of(JkRepos.mavenCentral())
                .withParams(JkResolutionParameters.defaultScopeMapping(JkJavaBuild.DEFAULT_SCOPE_MAPPING))
                .withModuleHolder(holder);
        JkResolveResult resolveResult = resolver.resolve(deps);
        JkResolveResult.JkErrorReport errorReport = resolveResult.errorReport();
        System.out.println(errorReport.moduleProblems());
        assertEquals(1, errorReport.moduleProblems().size());

    }


}
