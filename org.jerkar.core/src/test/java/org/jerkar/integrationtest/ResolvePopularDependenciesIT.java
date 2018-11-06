package org.jerkar.integrationtest;

import org.jerkar.api.depmanagement.*;
import org.jerkar.api.system.JkLog;
import org.junit.Assert;
import org.junit.Test;

import static org.jerkar.api.depmanagement.JkJavaDepScopes.COMPILE;
import static org.jerkar.api.depmanagement.JkJavaDepScopes.TEST;

public class ResolvePopularDependenciesIT {

    private static final String HTTP_CLIENT = "org.apache.httpcomponents:httpclient:4.5.3";

    @Test
    public void apacheHttpClient() {
        JkResolveResult result = resolver().resolve(
                JkDependencySet.of().and(HTTP_CLIENT, COMPILE));
        System.out.println(result.getDependencyTree().toStringTree());
        Assert.assertEquals(1, result.getDependencyTree().getChildren().size());
        Assert.assertEquals(3, result.getDependencyTree().getChildren().get(0).getChildren().size());
    }

    @Test
    public void apacheHttpClientWithoutDeclaredAndResolvedScope() {
        JkResolveResult result = resolver().resolve(JkDependencySet.of(HTTP_CLIENT));
        System.out.println(result.getDependencyTree().toStringTree());
        Assert.assertEquals(1, result.getDependencyTree().getChildren().size());
        Assert.assertEquals(3, result.getDependencyTree().getChildren().get(0).getChildren().size());
    }

    @Test
    public void apacheHttpClientWithoutDeclaredScopeButResolvedScope() {
        JkLog.registerBasicConsoleHandler();
        JkLog.setVerbosity(JkLog.Verbosity.VERBOSE);
        JkResolveResult result = resolver().resolve(JkDependencySet.of(HTTP_CLIENT), COMPILE);
        System.out.println(result.getDependencyTree().toStringTree());
        Assert.assertEquals(1, result.getDependencyTree().getChildren().size());
        Assert.assertEquals(3, result.getDependencyTree().getChildren().get(0).getChildren().size());
    }

    @Test
    public void apacheHttpClientWithDeclaredAndResolvedScope() {
        JkResolveResult result = resolver().resolve(JkDependencySet.of().and(HTTP_CLIENT, COMPILE), COMPILE);
        System.out.println(result.getDependencyTree().toStringTree());
        Assert.assertEquals(1, result.getDependencyTree().getChildren().size());
        Assert.assertEquals(3, result.getDependencyTree().getChildren().get(0).getChildren().size());
    }

    @Test
    public void apacheHttpClientWithTestScope() {
        JkResolveResult result = resolver().resolve(
                JkDependencySet.of().and(HTTP_CLIENT).withDefaultScope(TEST));
        System.out.println(result.getDependencyTree().toStringTree());
        Assert.assertEquals(1, result.getDependencyTree().getChildren().size());
        Assert.assertEquals(3, result.getDependencyTree().getChildren().get(0).getChildren().size());
    }

    @Test
    public void apacheHttpClientWithTestToTestScopeMapping() {
        JkDependencySet deps =  JkDependencySet.of().and(HTTP_CLIENT,  TEST.mapTo("archive", "test"));
        JkResolveResult result = resolver().resolve(deps);
        System.out.println(result.getDependencyTree().toStringTree());
        Assert.assertEquals(1, result.getDependencyTree().getChildren().size());
        Assert.assertEquals(5, result.getDependencyTree().getChildren().get(0).getChildren().size());
    }

    @Test
    public void apacheHttpClientMultiScopeMapping() {
        JkDependencySet deps =  JkDependencySet.of().and(HTTP_CLIENT,  JkJavaDepScopes.DEFAULT_SCOPE_MAPPING);
        JkResolveResult result = resolver().resolve(deps, TEST);
        System.out.println(result.getDependencyTree().toStringTree());
        Assert.assertEquals(1, result.getDependencyTree().getChildren().size());
        Assert.assertEquals(3, result.getDependencyTree().getChildren().get(0).getChildren().size());
    }

    private JkDependencyResolver resolver() {
        return JkDependencyResolver.of(JkRepo.ofMavenCentral());
    }
}
