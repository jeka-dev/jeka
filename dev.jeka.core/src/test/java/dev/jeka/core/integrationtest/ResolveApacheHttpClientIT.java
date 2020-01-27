package dev.jeka.core.integrationtest;

import dev.jeka.core.api.depmanagement.*;import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsSystem;
import org.junit.Assert;
import org.junit.Test;

import static dev.jeka.core.api.depmanagement.JkJavaDepScopes.COMPILE;
import static dev.jeka.core.api.depmanagement.JkScopedDependencyTest.TEST;

public class ResolveApacheHttpClientIT {

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
    public void resolveWithoutDeclaredAndResolvedScope() {
        JkResolveResult result = resolver().resolve(JkDependencySet.of(HTTP_CLIENT));
        System.out.println(result.getDependencyTree().toStringTree());
        Assert.assertEquals(1, result.getDependencyTree().getChildren().size());
        Assert.assertEquals(3, result.getDependencyTree().getChildren().get(0).getChildren().size());
    }

    @Test
    public void resolveWithoutDeclaredScopeButResolvedScope() {
        JkLog.registerHierarchicalConsoleHandler();
        JkLog.setVerbosity(JkLog.Verbosity.VERBOSE);
        JkResolveResult result = resolver().resolve(JkDependencySet.of(HTTP_CLIENT), COMPILE);
        System.out.println(result.getDependencyTree().toStringTree());
        Assert.assertEquals(1, result.getDependencyTree().getChildren().size());
        Assert.assertEquals(3, result.getDependencyTree().getChildren().get(0).getChildren().size());
    }

    @Test
    public void resolveWithDeclaredAndResolvedScope() {
        JkResolveResult result = resolver().resolve(JkDependencySet.of().and(HTTP_CLIENT, COMPILE), COMPILE);
        System.out.println(result.getDependencyTree().toStringTree());
        Assert.assertEquals(1, result.getDependencyTree().getChildren().size());
        Assert.assertEquals(3, result.getDependencyTree().getChildren().get(0).getChildren().size());
    }

    @Test
    public void resolveWithTestScope() {
        JkResolveResult result = resolver().resolve(
                JkDependencySet.of().and(HTTP_CLIENT).withDefaultScopes(TEST));
        System.out.println(result.getDependencyTree().toStringTree());
        Assert.assertEquals(1, result.getDependencyTree().getChildren().size());
        Assert.assertEquals(3, result.getDependencyTree().getChildren().get(0).getChildren().size());
    }

    @Test
    public void resolveWithTestToTestScopeMapping() {
        JkLog.registerHierarchicalConsoleHandler();
        JkLog.setVerbosity(JkLog.Verbosity.VERBOSE);
        JkDependencySet deps =  JkDependencySet.of().and(HTTP_CLIENT,  TEST.mapTo( "test"));
        JkResolveResult result = resolver().resolve(deps);
        System.out.println(result.getDependencyTree().toStringTree());
        Assert.assertEquals(1, result.getDependencyTree().getChildren().size());
        if (JkUtilsSystem.IS_WINDOWS) {
            // Test conf is declared as private so no it should resolve in no dependency
            // Assert.assertEquals(0, result.getDependencyTree().getChildren().get(0).getChildren().size());
            // TODO fail on macos ... and windows :_)
        }
    }

    @Test
    public void resolveWithMultiScopeMapping() {
        JkDependencySet deps = JkDependencySet.of().and(HTTP_CLIENT,  JkJavaDepScopes.DEFAULT_SCOPE_MAPPING);
        JkResolveResult result = resolver().resolve(deps, TEST);
        System.out.println(result.getDependencyTree().toStringTree());
        Assert.assertEquals(1, result.getDependencyTree().getChildren().size());
        Assert.assertEquals(3, result.getDependencyTree().getChildren().get(0).getChildren().size());
    }

    @Test
    public void resolveWithNoOccupiedScope() {
        JkDependencySet deps = JkDependencySet.of().and(HTTP_CLIENT, JkJavaDepScopes.RUNTIME);
        JkResolveResult result = resolver().resolve(deps, COMPILE);
        Assert.assertEquals(0, result.getFiles().getEntries().size());
    }

    private JkDependencyResolver resolver() {
        return JkDependencyResolver.of(JkRepo.ofMavenCentral());
    }

}
