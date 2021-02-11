package dev.jeka.core.integrationtest;

import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.depmanagement.JkPopularModules;
import dev.jeka.core.api.depmanagement.JkRepo;
import dev.jeka.core.api.depmanagement.JkVersionedModule;
import dev.jeka.core.api.depmanagement.resolution.JkDependencyResolver;
import dev.jeka.core.api.depmanagement.resolution.JkResolveResult;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by angibaudj on 19-06-17.
 */
public class ResolveHandleErrorIT {

    @Test
    public void artifactNotFound() {
        JkVersionedModule holder = JkVersionedModule.of("mygroup:myname:myversion");
        JkDependencySet deps = JkDependencySet.of()
                .and(JkPopularModules.JAVAX_SERVLET_API.version("2.5.3"));  // does not exist
        JkDependencyResolver resolver = JkDependencyResolver.of()
            .addRepos(JkRepo.ofMavenCentral())
            .setModuleHolder(holder);
        JkResolveResult resolveResult = resolver.resolve(deps);
        JkResolveResult.JkErrorReport errorReport = resolveResult.getErrorReport();
        System.out.println(errorReport.getModuleProblems());
        assertEquals(1, errorReport.getModuleProblems().size());

    }


}
