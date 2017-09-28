package org.jerkar.integrationtest;

import static org.jerkar.api.depmanagement.JkJavaDepScopes.COMPILE;
import static org.jerkar.api.depmanagement.JkJavaDepScopes.COMPILE_AND_RUNTIME;
import static org.jerkar.api.depmanagement.JkJavaDepScopes.DEFAULT_SCOPE_MAPPING;
import static org.jerkar.api.depmanagement.JkJavaDepScopes.TEST;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.jerkar.api.depmanagement.JkDependencies;
import org.jerkar.api.depmanagement.JkDependencyNode;
import org.jerkar.api.depmanagement.JkDependencyResolver;
import org.jerkar.api.depmanagement.JkModuleId;
import org.jerkar.api.depmanagement.JkRepos;
import org.jerkar.api.depmanagement.JkResolutionParameters;
import org.jerkar.api.depmanagement.JkVersionedModule;
import org.jerkar.api.utils.JkUtilsFile;
import org.jerkar.api.utils.JkUtilsIterable;
import org.junit.Test;

/**
 * Created by angibaudj on 19-06-17.
 */
public class MergeFileDepIT {

    @Test
    public void treeIsCorrectAfterFileDepInsert() {
        JkVersionedModule holder = JkVersionedModule.of("mygroup:myname", "myversion");
        File dep0File = JkUtilsFile.resourceAsFile(MergeFileDepIT.class, "dep0");
        File dep1File = JkUtilsFile.resourceAsFile(MergeFileDepIT.class, "dep1");
        File dep2File = JkUtilsFile.resourceAsFile(MergeFileDepIT.class, "dep2");
        JkDependencies deps = JkDependencies.builder()
                .on(dep0File).scope(TEST)
                .on("org.springframework.boot:spring-boot-starter-web:1.5.3.RELEASE").scope(COMPILE_AND_RUNTIME)
                .on(dep1File).scope(TEST)
                .on("com.github.briandilley.jsonrpc4j:jsonrpc4j:1.5.0").scope(COMPILE)
                .on(dep2File).scope(COMPILE)
                .build();
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
        assertEquals(JkUtilsIterable.listOf(dep0File), file0Node.allFiles());

        JkDependencyNode starterwebNode = tree.children().get(1);
        assertEquals(JkModuleId.of("org.springframework.boot:spring-boot-starter-web"), starterwebNode.moduleInfo().moduleId());

        JkDependencyNode file1Node = tree.children().get(2);
        assertEquals(JkUtilsIterable.listOf(dep1File), file1Node.allFiles());

        JkDependencyNode jsonRpcNode = tree.children().get(3);
        assertEquals(JkModuleId.of("com.github.briandilley.jsonrpc4j:jsonrpc4j"), jsonRpcNode.moduleInfo().moduleId());

        JkDependencyNode file2Node = tree.children().get(4);
        assertEquals(JkUtilsIterable.listOf(dep2File), file2Node.allFiles());

        // Now check that file dependencies with Test Scope are not present in compile

        tree = resolver.resolve(deps, COMPILE).dependencyTree();
        System.out.println(tree.toStringComplete());

        root = tree.moduleInfo();
        assertTrue(root.declaredScopes().isEmpty());
        assertEquals(holder.moduleId(), tree.moduleInfo().moduleId());
        assertEquals(3, tree.children().size());


    }

    @Test
    public void flattenOnlyFileDeps() {
        File dep0File = JkUtilsFile.resourceAsFile(MergeFileDepIT.class, "dep0");
        File dep1File = JkUtilsFile.resourceAsFile(MergeFileDepIT.class, "dep1");
        JkDependencies deps = JkDependencies.builder()
                .on(dep0File).scope(TEST)
                .on(dep1File).scope(TEST).build();
        JkDependencyResolver resolver = JkDependencyResolver.of();
        JkDependencyNode tree = resolver.resolve(deps).dependencyTree();
        assertEquals(2, tree.flatten().size());
        resolver = JkDependencyResolver.of(JkRepos.mavenCentral());
        assertEquals(2, resolver.resolve(deps).dependencyTree().flatten().size());

    }


}
