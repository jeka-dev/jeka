package dev.jeka.core.integrationtest;

import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.depmanagement.JkRepo;
import dev.jeka.core.api.depmanagement.JkTransitivity;
import dev.jeka.core.api.depmanagement.resolution.JkDependencyResolver;
import dev.jeka.core.api.depmanagement.resolution.JkResolveResult;
import org.junit.Assert;
import org.junit.Test;

public class ResolveApacheHttpClientIT {

    private static final String HTTP_CLIENT = "org.apache.httpcomponents:httpclient:4.5.3";

    @Test
    public void apacheHttpClient() {
        JkResolveResult result = resolver().resolve(
                JkDependencySet.of().and(HTTP_CLIENT, JkTransitivity.RUNTIME));
        System.out.println(result.getDependencyTree().toStringTree());
        Assert.assertEquals(1, result.getDependencyTree().getChildren().size());
        Assert.assertEquals(3, result.getDependencyTree().getChildren().get(0).getChildren().size());
    }

    @Test
    public void resolveWithoutExplicitTransitivity() {
        JkResolveResult result = resolver().resolve(JkDependencySet.of(HTTP_CLIENT));
        System.out.println(result.getDependencyTree().toStringTree());
        Assert.assertEquals(1, result.getDependencyTree().getChildren().size());
        Assert.assertEquals(3, result.getDependencyTree().getChildren().get(0).getChildren().size());
    }

    private JkDependencyResolver resolver() {
        return JkDependencyResolver.of().addRepos(JkRepo.ofMavenCentral());
    }

}
