package dev.jeka.core.api.file;

import org.junit.jupiter.api.Test;

import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JkPathTreeSetTest {

    @Test
    void mergeDuplicateRoots_3treesIncluding2havingSameRoot_result2Trees() {
        JkPathTree tree1 = JkPathTree.of(Paths.get("a")).andMatching("**/*.txt");
        JkPathTree tree2 = JkPathTree.of(Paths.get("b")).andMatching("**/*.zip");
        JkPathTree tree3 = JkPathTree.of(Paths.get("a")).andMatching("**/*.xml");
        JkPathTreeSet treeSet = JkPathTreeSet.of(tree1, tree2, tree3).mergeDuplicateRoots();
        assertEquals(2, treeSet.toList().size());
        JkPathTree merged = treeSet.toList().get(0);
        assertEquals(tree1.getRoot(), merged.getRoot());
        assertTrue(merged.getMatcher().matches(merged.getRoot().resolve("foo/bar.txt")));
        assertTrue(merged.getMatcher().matches(merged.getRoot().resolve("foo/bar.xml")));
    }

}
