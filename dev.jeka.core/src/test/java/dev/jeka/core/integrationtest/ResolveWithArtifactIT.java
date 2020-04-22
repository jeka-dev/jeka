package dev.jeka.core.integrationtest;

import dev.jeka.core.api.depmanagement.*;
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
    public void artifactsAreHandled() {
        JkVersionedModule holder = JkVersionedModule.of("mygroup:myname:myversion");
        JkModuleDependency jgllinux = JkModuleDependency.of("org.lwjgl:lwjgl::natives-linux:3.1.1");
        JkDependencySet deps = JkDependencySet.of()
                .and(jgllinux)
                .and(JkPopularModules.GUAVA, "19.0" )
                .and("org.lwjgl:lwjgl:3.1.1")
                .withDefaultScopes(JkScope.COMPILE);
        JkDependencyResolver resolver = JkDependencyResolver.of()
                .addRepos(JkRepo.ofMavenCentral())
                .setModuleHolder(holder);
        JkResolveResult resolveResult = resolver.resolve(deps);
        JkDependencyNode treeRoot = resolveResult.getDependencyTree();
        System.out.println(resolveResult.getFiles());
        System.out.println(treeRoot.toStringTree());

        // Even if there is 2 declared dependencies on lwjgl, as it is the same module (with different artifact),
        // it should results in a single node.
        // The classpath order will also place all artifacts jump a same module subsequently
        assertEquals(2, treeRoot.getChildren().size());
        assertEquals(3, treeRoot.getResolvedFiles().size());

        JkDependencyNode lwjglNode = treeRoot.getChildren().get(0);
        List<Path> lwjglFiles = lwjglNode.getNodeInfo().getFiles();
        System.out.println(lwjglFiles);
        assertEquals(2, lwjglFiles.size());

    }

    @Test
    public void artifactCountIsOk() {
        JkVersionedModule holder = JkVersionedModule.of("mygroup:myname:myversion");
        JkDependencySet deps = JkDependencySet.of()
                .and("org.springframework.boot:spring-boot-starter-web:1.5.3.RELEASE", JkScope.COMPILE_AND_RUNTIME)
                .and("org.springframework.boot:spring-boot-starter-test:1.5.+", JkScope.TEST)
                .and("com.github.briandilley.jsonrpc4j:jsonrpc4j:1.5.0", JkScope.COMPILE);
        JkDependencyResolver resolver = JkDependencyResolver.of()
                .addRepos(JkRepo.ofMavenCentral())
                .setModuleHolder(holder);
        JkResolveResult resolveResult = resolver.resolve(deps);
        JkDependencyNode tree = resolveResult.getDependencyTree();
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
