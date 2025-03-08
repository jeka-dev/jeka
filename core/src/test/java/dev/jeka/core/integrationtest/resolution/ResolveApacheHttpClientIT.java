package dev.jeka.core.integrationtest.resolution;

import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.depmanagement.JkRepo;
import dev.jeka.core.api.depmanagement.JkTransitivity;
import dev.jeka.core.api.depmanagement.resolution.JkDependencyResolver;
import dev.jeka.core.api.depmanagement.resolution.JkResolveResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResolveApacheHttpClientIT {

    private static final String HTTP_CLIENT = "org.apache.httpcomponents:httpclient:4.5.3";

    @Test
    void apacheHttpClient() {
        JkResolveResult result = resolver().resolve(
                JkDependencySet.of().and(HTTP_CLIENT, JkTransitivity.RUNTIME));
        System.out.println(result.getDependencyTree().toStringTree());
        assertEquals(1, result.getDependencyTree().getChildren().size());
        assertEquals(3, result.getDependencyTree().getChildren().get(0).getChildren().size());
    }

    @Test
    public void resolveWithoutExplicitTransitivity() {
        JkResolveResult result = resolver().resolve(JkDependencySet.of(HTTP_CLIENT));
        System.out.println(result.getDependencyTree().toStringTree());
        assertEquals(1, result.getDependencyTree().getChildren().size());
        assertEquals(3, result.getDependencyTree().getChildren().get(0).getChildren().size());
    }

    private JkDependencyResolver resolver() {
        return JkDependencyResolver.of().addRepos(JkRepo.ofMavenCentral());
    }

}
