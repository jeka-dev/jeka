package org.jerkar.integrationtest;

import org.jerkar.api.depmanagement.*;
import org.junit.Test;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

import static org.jerkar.api.depmanagement.JkJavaDepScopes.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by angibaudj on 19-06-17.
 */
public class MergeFileDepIT {

    @Test
    public void treeIsCorrectAfterFileDepInsert() throws URISyntaxException {
        JkVersionedModule holder = JkVersionedModule.of("mygroup:myname:myversion");
        Path dep0File = Paths.get(MergeFileDepIT.class.getResource("dep0").toURI());
        Path dep1File = Paths.get(MergeFileDepIT.class.getResource( "dep1").toURI());
        Path dep2File = Paths.get(MergeFileDepIT.class.getResource( "dep2").toURI());
        JkDependencySet deps = JkDependencySet.of()
                .and(dep0File, TEST)
                .and("org.springframework.boot:spring-boot-starter-web:1.5.3.RELEASE", COMPILE_AND_RUNTIME)
                .and(dep1File, TEST)
                .and("com.github.briandilley.jsonrpc4j:jsonrpc4j:1.5.0", COMPILE)
                .and(dep2File, COMPILE);
        JkDependencyResolver resolver = JkDependencyResolver.of(JkRepos.mavenCentral())
                .withParams(JkResolutionParameters.defaultScopeMapping(DEFAULT_SCOPE_MAPPING))
                .withModuleHolder(holder);
        JkDependencyNode tree = resolver.resolve(deps).dependencyTree();

        System.out.println(tree.toStringComplete());

        JkDependencyNode.ModuleNodeInfo root = tree.moduleInfo();
        assertTrue(root.declaredScopes().isEmpty());
        assertEquals(holder.moduleId(), tree.moduleInfo().moduleId());
        assertEquals(5, tree.children().size());

        JkDependencyNode file0Node = tree.children().get(0);
        List<Path> expected = new LinkedList<>();
        expected.add(dep0File);
        assertEquals(expected, file0Node.allFiles());

        JkDependencyNode starterwebNode = tree.children().get(1);
        assertEquals(JkModuleId.of("org.springframework.boot:spring-boot-starter-web"), starterwebNode.moduleInfo().moduleId());

        JkDependencyNode file1Node = tree.children().get(2);
        List<Path> expected1 = new LinkedList<>();
        expected1.add(dep1File);
        assertEquals(expected1, file1Node.allFiles());

        JkDependencyNode jsonRpcNode = tree.children().get(3);
        assertEquals(JkModuleId.of("com.github.briandilley.jsonrpc4j:jsonrpc4j"), jsonRpcNode.moduleInfo().moduleId());

        JkDependencyNode file2Node = tree.children().get(4);
        List<Path> expected2 = new LinkedList<>();
        expected2.add(dep2File);
        assertEquals(expected2, file2Node.allFiles());

        // Now check that file dependencies with Test Scope are not present in compile

        tree = resolver.resolve(deps, COMPILE).dependencyTree();
        System.out.println(tree.toStringComplete());

        root = tree.moduleInfo();
        assertTrue(root.declaredScopes().isEmpty());
        assertEquals(holder.moduleId(), tree.moduleInfo().moduleId());
        assertEquals(3, tree.children().size());


    }

    @Test
    public void flattenOnlyFileDeps() throws URISyntaxException {
        Path dep0File = Paths.get(MergeFileDepIT.class.getResource("dep0").toURI());
        Path dep1File = Paths.get(MergeFileDepIT.class.getResource("dep1").toURI());
        JkDependencySet deps = JkDependencySet.of()
                .and(dep0File, TEST)
                .and(dep1File, TEST);
        JkDependencyResolver resolver = JkDependencyResolver.of();
        JkDependencyNode tree = resolver.resolve(deps).dependencyTree();
        assertEquals(2, tree.flatten().size());
        resolver = JkDependencyResolver.of(JkRepos.mavenCentral());
        assertEquals(2, resolver.resolve(deps).dependencyTree().flatten().size());

    }


}
