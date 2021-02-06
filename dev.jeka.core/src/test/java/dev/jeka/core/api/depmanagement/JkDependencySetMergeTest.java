package dev.jeka.core.api.depmanagement;

import org.junit.Assert;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.*;

public class JkDependencySetMergeTest {

    @Test
    public void modulesAreProperlySorted() {
        JkDependencySet left = of("A", "B", "C", "D", "E", "F", "Z");
        JkDependencySet rigth = of("Z","A", "B", "Y", "V", "E", "X");
        JkDependencySet result = of("A", "B", "C", "D", "Y", "V", "E", "F", "Z", "X");
        JkDependencySetMerge merge = JkDependencySetMerge.of(left, rigth);
        Assert.assertEquals(result, merge.getResult());
    }


    private static JkDependencySet of (String ... depNames) {
        List<JkDependency> dependencies = new LinkedList<>();
        for (String depName : depNames) {
            dependencies.add(JkModuleDependency.of(depName + ":" + depName));
        }
        return JkDependencySet.of(dependencies);
    }

}