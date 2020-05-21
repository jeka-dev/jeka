package dev.jeka.core.integrationtest;

import dev.jeka.core.api.depmanagement.*;
import dev.jeka.core.api.system.JkLog;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import static dev.jeka.core.api.depmanagement.JkScope.COMPILE;

public class ResolveGoogleApiClient {

    private static final String GOOGLE_API_CLIENT = "com.google.api-client:google-api-client:1.30.7";

    private static final JkModuleId GOOGLE_HTTP_CLIENT = JkModuleId.of("com.google.http-client:google-http-client");

    private static final JkVersion EXPECTED_HTTP_CLIENT_VERSION = JkVersion.of("1.34.0");

    @Test
    @Ignore // Ivy issue
    public void httpClientVersionProperlyResolver() {
        JkLog.setHierarchicalConsoleConsumer();
        JkLog.setVerbosity(JkLog.Verbosity.QUITE_VERBOSE);
        JkResolveResult result = resolver().resolve(
                JkDependencySet.of().and(GOOGLE_API_CLIENT, COMPILE));
       // System.out.println(result.getDependencyTree().toStringTree());
        JkVersion httpVersion = result.getDependencyTree().getResolvedVersions().getVersionOf(GOOGLE_HTTP_CLIENT);
        Assert.assertEquals(EXPECTED_HTTP_CLIENT_VERSION, httpVersion);
    }



    private JkDependencyResolver resolver() {
        return JkDependencyResolver.of().addRepos(JkRepo.ofMavenCentral());
    }

}
