package dev.jeka.core.api.depmanagement;

import org.junit.Test;

import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class JkDependencySetMergeTest {

    @Test
    public void of_distinctSets_mergeCorrectly() {
        JkDependencySet left = of("A", "B", "C", "D", "E", "F", "Z");
        JkDependencySet right = of("Z","A", "B", "Y", "V", "E", "X");
        JkDependencySet expectedResult = of("A", "B", "C", "D", "E", "F", "Z", "Y", "V", "X");
        JkDependencySetMerge merge = JkDependencySetMerge.of(left, right);
        assertEquals(of("Y", "V", "X").getDependencies(), merge.getAbsentDependenciesFromLeft());
        assertEquals(of("C", "D", "F").getDependencies(), merge.getAbsentDependenciesFromRight());
        assertEquals(expectedResult.getDependencies(), merge.getResult().getDependencies());
    }

    private static JkDependencySet of (String ... depNames) {
        List<JkDependency> dependencies = new LinkedList<>();
        for (String depName : depNames) {
            dependencies.add(JkModuleDependency.of(depName + ":" + depName));
        }
        return JkDependencySet.of(dependencies);
    }

}