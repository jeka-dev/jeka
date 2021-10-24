package dev.jeka.core.api.file;

import org.junit.Assert;
import org.junit.Test;

import java.nio.file.Paths;

public class JkPathTreeSetTest {

    @Test
    public void mergeDuplicateRoots_3treesIncluding2havingSameRoot_result2Trees() {
        JkPathTree tree1 = JkPathTree.of(Paths.get("a")).andMatching("**/*.txt");
        JkPathTree tree2 = JkPathTree.of(Paths.get("b")).andMatching("**/*.zip");
        JkPathTree tree3 = JkPathTree.of(Paths.get("a")).andMatching("**/*.xml");
        JkPathTreeSet treeSet = JkPathTreeSet.of(tree1, tree2, tree3).mergeDuplicateRoots();
        Assert.assertEquals(2, treeSet.toList().size());
        JkPathTree merged = treeSet.toList().get(0);
        Assert.assertEquals(tree1.getRoot(), merged.getRoot());
        Assert.assertTrue(merged.getMatcher().matches(merged.getRoot().resolve("foo/bar.txt")));
        Assert.assertTrue(merged.getMatcher().matches(merged.getRoot().resolve("foo/bar.xml")));
    }

}
