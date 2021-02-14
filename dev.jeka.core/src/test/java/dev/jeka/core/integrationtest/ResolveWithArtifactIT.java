package dev.jeka.core.integrationtest;

import dev.jeka.core.api.depmanagement.*;
import dev.jeka.core.api.depmanagement.resolution.JkDependencyResolver;
import dev.jeka.core.api.depmanagement.resolution.JkResolveResult;
import dev.jeka.core.api.depmanagement.resolution.JkResolvedDependencyNode;
import dev.jeka.core.api.utils.JkUtilsString;
import org.junit.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Created by angibaudj on 19-06-17.
 */
public class ResolveWithArtifactIT {


    @Test
    public void artifactCountIsOk() {
        JkVersionedModule holder = JkVersionedModule.of("mygroup:myname:myversion");
        JkQualifiedDependencies deps = JkQualifiedDependencies.of()
                .and("comple, runtime", "org.springframework.boot:spring-boot-starter-web:1.5.3.RELEASE")
                .and("test", "org.springframework.boot:spring-boot-starter-test:1.5.+")
                .and("compile", "com.github.briandilley.jsonrpc4j:jsonrpc4j:1.5.0");
        JkDependencyResolver resolver = JkDependencyResolver.of()
                .addRepos(JkRepo.ofMavenCentral())
                .setModuleHolder(holder);
        JkResolveResult resolveResult = resolver.resolve(deps);
        JkResolvedDependencyNode tree = resolveResult.getDependencyTree();
        System.out.println(tree.toStringTree());
        System.out.println(JkUtilsString.join(resolveResult.getFiles(), "\n"));
        System.out.println("-----");
        System.out.println(JkUtilsString.join(tree.getResolvedFiles(), "\n"));
        //assertEquals(resolveResult.localFiles().size(), tree.allFiles().size());
        System.out.println("-----");
        for (Path file : resolveResult.getFiles()) {
            if (!tree.getResolvedFiles().contains(file)) {
                System.out.println(file);
            }
        }

    }


}
